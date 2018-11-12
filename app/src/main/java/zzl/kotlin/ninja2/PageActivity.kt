package zzl.kotlin.ninja2

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.*
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Uri
import android.net.http.SslError
import android.os.*
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.security.KeyChain
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.Snackbar
import android.support.v7.util.DiffUtil
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.text.*
import android.text.style.StyleSpan
import android.view.*
import android.webkit.*
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams
import com.anthonycr.grant.PermissionsManager
import com.anthonycr.grant.PermissionsResultAction
import kotlinx.android.synthetic.main.activity_page.*
import kotlinx.android.synthetic.main.content_bottom_sheet.*
import kotlinx.android.synthetic.main.content_bottom_sheet.view.*
import kotlinx.android.synthetic.main.content_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import zzl.kotlin.ninja2.application.*
import zzl.kotlin.ninja2.widget.*
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList


class PageActivity : BaseActivity(), PageView.Delegate, SharedPreferences.OnSharedPreferenceChangeListener {

    var mCurrentMode = Type.MODE_DEFAULT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (SP.canScreenshot) {
            // 详情请看 https://www.jianshu.com/p/d0ef41470586
            WebView.enableSlowWholeDocumentDraw()
        }

        setContentView(R.layout.activity_page)
        setSupportActionBar(toolbar)

        checkExtra()

        initEvent()

        initPinRecycler()
        initInputBox()
        initRecordRecycler()
    }

    /**
     * If [Intent.FLAG_ACTIVITY_MULTIPLE_TASK] has not been used this Activity
     * will be reused.
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        L.i(TAG, "onNewIntent")
        // It is good pratice to remove a document from the overview stack if not needed anymore.
//        finishAndRemoveTask()
    }

    /**
     * 标记是否为隐私浏览模式，默认为false
     */
    private var isPrivate: Boolean = false

    /**
     *
     */
    private var mTargetUrl: String? = null

    /**
     * 父任务窗口的TaskId，用于寻找和回退到父任务窗口
     */
    private var mParentTaskId: Int = 0

    /**
     * 检查Intent传递的参数
     */
    private fun checkExtra() {
        mParentTaskId = intent.getIntExtra(EXTRA_TASK_ID, 0)
        isPrivate = intent.getBooleanExtra(EXTRA_PRIVATE, false)
        mPageView.setupViewMode(isPrivate)

        mTargetUrl = intent.getStringExtra(EXTRA_TARGET_URL)
        mTargetUrl?.let { if (it.isNotEmpty()) loadPage(it) }

        App.MESSAGE?.let { loadPage(it) }
    }

    /**
     * 初始化Pin的RecyclerView，由于采用了ViewStub来延迟加载
     * 因此尽量需要的时候才调用
     */
    private var mPinsRecycler: RecyclerView? = null

    private lateinit var mPinsAdapter: PinsAdapter
    private lateinit var mPinsLayoutManager: LinearLayoutManager
    private val mPins: ArrayList<Pin> = ArrayList()

    private fun initPinRecycler() {
        if (mPinsRecycler == null) {
            mPinsRecycler = mPinsStub.inflate() as RecyclerView
        }

        mPinsRecycler?.let {
            mPinsLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, SP.pinsReverse)
            it.layoutManager = mPinsLayoutManager
            it.itemAnimator = DefaultItemAnimator()
            it.setHasFixedSize(true)
            mPinsAdapter = PinsAdapter(this, mPins)
            mPinsAdapter.setOnClickListener { _, position ->
                //加载网址
                loadPage(mPins[position].url)
            }
            it.adapter = mPinsAdapter
        }

        if (SP.isFirstInstall) {
            firstInitPinRecyclerView()
        } else {
            loadPinsData()
        }

        setPinReversePadding()
        setItemTouchHelper()
    }

    /**
     * 异步加载本地所有的Pin记录
     */
    private fun loadPinsData() {
        doAsync {
            val pins = SQLHelper.findAllPins()
            mPinsAdapter.addAll(pins, true)
        }
    }

    //设置主页列表反序时呈现的padding距离
    private fun setPinReversePadding() {
        val padding = dip2px(10f)
        if (SP.pinsReverse) {
            mPinsRecycler?.setPadding(0, 0, 0, padding)
        } else {
            mPinsRecycler?.setPadding(0, padding, 0, 0)
        }
    }

    /**
     * 首次安装启动，添加介绍Pin记录
     */
    private fun firstInitPinRecyclerView() {
        SP.isFirstInstall = false
        val pin = Pin(
                title = getString(R.string.app_intro_title),
                url = getString(R.string.app_intro_url)
        )

        doAsync { SQLHelper.savePin(pin) }

        mPins.clear()
        mPins.add(pin)
        mPinsAdapter.notifyDataSetChanged()
    }

    private var mCurrentEditorPosition: Int = 0

    /**
     * 给RecyclerView设置ItemTouch监听器，用于监听滑动事件，实现左右侧滑和上下滑动交换功能
     * 代码参考
     * @see https://blog.csdn.net/hymanme/article/details/50931082
     */
    private fun setItemTouchHelper() {

        val callback = DefaultItemTouchHelperCallback(object : DefaultItemTouchHelperCallback.Callback {

            //保存被删除item信息，用于撤销操作
            val array = ArrayList<Pair<Int, Pin>>(2)

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                mCurrentEditorPosition = viewHolder.adapterPosition

                // 如果是往右滑动，则进入编辑模式
                if (direction == ItemTouchHelper.RIGHT) {
                    editPinName(mCurrentEditorPosition)
                    mPinsAdapter.notifyItemChanged(mCurrentEditorPosition)
                    return
                }

                // 如果是往左滑动，则先取出记录存到删除队列中，以备撤销使用
                val pin = mPinsAdapter.mList[mCurrentEditorPosition]
                L.i(TAG, "onSwiped pin position: $mCurrentEditorPosition")
                array.add(mCurrentEditorPosition to pin)

                array.forEach {
                    L.i(TAG, "key: ${it.first} & pin: ${it.second.title}")
                }

                // 移除该条记录
                mPinsAdapter.removeItem(mCurrentEditorPosition)
            }

            override fun onMove(srcPosition: Int, targetPosition: Int): Boolean {

                var srcP = srcPosition
                var targetP = targetPosition

                if (srcPosition <= targetPosition) {
                    srcP = targetPosition
                    targetP = srcPosition
                }

                Collections.swap(mPins, targetP, srcP)
                mPinsAdapter.notifyItemMoved(targetP, srcP)

                //交换对应位置的两个Pin记录的ID
                exchangePin(targetP, srcP)

                return true
            }

            override fun clearView(viewHolder: RecyclerView.ViewHolder) {
                if (array.size > 0) {
                    L.i(TAG, "clearView array size: ${array.size}")
                    //如果队列中有数据，说明刚才有删掉一些item
                    Snackbar.make(mInputBox, R.string.snackBar_message_delete_pin, Snackbar.LENGTH_LONG)
                            .setAction(R.string.snackBar_button_repeal) { revoke() }
                            .addCallback(mSnackCallback).show()
                }
            }

            /**
             * 撤销方法
             */
            fun revoke() {
                //todo[Checked] 处理最后一条Pin撤销时，RecyclerView中不显示的问题
                //SnackBar的撤销按钮被点击，队列中取出刚被删掉的数据，然后再添加到数据集合，实现数据被撤回的动作
                val index = array.size - 1
                val item = array[index]
                val position = item.first
                val pin = item.second

                L.i(TAG, "before revoke array size: ${array.size}")
                mPinsAdapter.addItem(position, pin)
                array.removeAt(index)

                L.i(TAG, "after revoked array size: ${array.size}")

                //实际开发中遇到一个bug：删除第一个item再撤销出现的视图延迟
                //手动将recyclerView滑到顶部可以解决这个bug
                if (position == 0) {
                    mPinsRecycler!!.smoothScrollToPosition(0)
                }
            }

            /**
             * 删除方法
             */
            fun delete(event: Int) {
                /*
                * event 为消失原因。
                * 连续删除多个item时，SnackBar挤掉前一个SnackBar导致的消失，将会直接删除，
                * event 为 DISMISS_EVENT_CONSECUTIVE。
                * DISMISS_EVENT_ACTION为点击Action导致的消失，本代码中Action执行的动作为撤销，
                * 因此不能执行删除操作，需要排除掉
                */
                if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                    //todo 处理滑动多条数据的删除
                    L.i(TAG, "before delete array size: ${array.size}")
                    array.forEach {
                        L.i(TAG, "position: ${it.first} & name: ${it.second.title}")
                    }
                    doAsync {
                        val pin = array[0].second
                        SQLHelper.deletePin(pin)
                        array.removeAt(0)

                        L.i(TAG, "after delete array size: ${array.size}")
                        array.forEach {
                            L.i(TAG, "position: ${it.first} & name: ${it.second.title}")
                        }
                    }

                }
            }

            private val mSnackCallback = object : Snackbar.Callback() {

                //不撤销将执行删除操作，监听SnackBar消失事件，
                //SnackBar消失（非排挤式消失）出队、删除数据。
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    super.onDismissed(transientBottomBar, event)
                    delete(event)
                }

            }
        })

        ItemTouchHelper(callback).attachToRecyclerView(mPinsRecycler)

    }

    /**
     * 交换Pin数据ID后，更新数据库中的数据记录，实现数据交换的目的
     *
     * ID | Title | Url                                             ID | Title | Url
     * ================   交换ID    -----------------   更新数据库   =================
     *  1 | 张三 | xxxx  ---  -->    3  | 张三 | xxxx  ----------->  1 | 李四 | ssss
     * ----------------     X       ----------------                ----------------
     *  3 | 李四 | ssss  ---  -->    1  | 李四 | ssss  ----------->  3 | 张三 | xxxx
     * ----------------             ----------------                ----------------
     *
     * @param targetPosition
     * @param srcPosition
     */
    private fun exchangePin(targetPosition: Int, srcPosition: Int) {

        val targetPin = mPins[targetPosition]
        val targetId = targetPin._id

        val srcPin = mPins[srcPosition]

        //交换ID
        targetPin._id = srcPin._id
        srcPin._id = targetId

        doAsync {
            SQLHelper.updatePinById(targetPin)
            SQLHelper.updatePinById(srcPin)
        }

    }

    /**
     * 编辑
     * @param position
     */
    private fun editPinName(position: Int) {
        if (position < 0) return

        // 设置当前的状态值为Pin编辑状态
        mCurrentMode = Type.MODE_PIN_EDIT

        val pin = mPins[position]
        mInputBox.setHint(R.string.hint_input_edit)
        mInputBox.setText(pin.title)
        mInputBox.setSelection(pin.title.length)

        mInputBox.showKeyboard()

        // 显示确认菜单按钮，隐藏其他菜单按钮
        showMenu(confirm = true)
    }

    /**
     * 初始化Record的RecyclerView，由于采用了ViewStub来延迟加载
     * 因此尽量需要的时候才调用
     */
    private var mRecordRecycler: RecyclerView? = null

    private lateinit var mRecordsAdapter: RecordsAdapter

    private val mRecords: ArrayList<Record> = ArrayList()

    private fun initRecordRecycler() {
        if (mRecordRecycler == null) {
            mRecordRecycler = mRecordsStub.inflate() as RecyclerView
        }

        mRecordRecycler?.let {
            mRecordsAdapter = RecordsAdapter(this, mRecords)
            it.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true)
            it.setHasFixedSize(true)
            it.itemAnimator = DefaultItemAnimator()
            mRecordsAdapter.setOnClickListener { _, position ->
                //加载网址
                loadPage(mRecords[position].url)
            }
            it.adapter = mRecordsAdapter
        }
    }

    private lateinit var mBottomSheetBehavior: BottomSheetBehavior<*>
    private fun initEvent() {
        // Bottom Sheet content layout views
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout)

        // Capturing the callbacks for bottom sheet
        mBottomSheetBehavior.setBottomSheetCallback(mBottomSheetCallback)

        mMenuOptionWidget.setMenuOptionListener(mMenuOptionListener)

        mPageView.setPageViewDelegate(this)
        setInputBoxNestScroll()

        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this)

        registerNetworkChangeBroadcastReceiver()

        registerVibrator()
    }

    private var mVibrator: Vibrator? = null
    private fun registerVibrator() {
        if (!SP.vibrate) {
            mVibrator = null
            return
        }
        mVibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private fun vibrate() {
        mVibrator?.apply {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // This ignores all exceptions to stay compatible with pre-O implementations.
                    vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrate(50)
                }
            } catch (iae: IllegalArgumentException) {
                L.e(TAG, "Failed to create VibrationEffect")
                iae.printStackTrace()
            }
        }
    }

    /**
     * 设置地址栏的嵌套滚动事件
     */
    private fun setInputBoxNestScroll() {

        mPageView.apply {
            if (SP.omniboxFixed) {
                mBottomSheetBehavior.isHideable = false
                val margin = resources.getDimension(R.dimen.action_bar_size_48).toInt()
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                        .apply { setMargins(0, 0, 0, margin) }
                setScrollChangeListener(null)
            } else {

                //设置BottomSheet为可隐藏，否则将BottomSheet的状态更改为隐藏时会抛出异常
                //mBottomSheetBehavior.isHideable = true

                //设置PageView为全屏，去掉底部的Margin
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                        .apply { setMargins(0, 0, 0, 0) }

                //设置滚动监听器
                setScrollChangeListener { _, dy ->

                    if (dy > 0) {// 手指上滑
                        mBottomSheetBehavior.let {
                            if (it.isCollapsed()) {
                                mBottomSheetBehavior.isHideable = true
                                it.hidden() //隐藏
                            }
                        }
                    } else if (dy < 0) {// 手指下滑
                        mBottomSheetBehavior.let {
                            if (it.isHidden()) {
                                it.collapsed() //显示
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 注册广播接收器，监听网络变化
     */
    private var mNetworkBroadcastReceiver: BroadcastReceiver? = null

    private fun registerNetworkChangeBroadcastReceiver() {
        mNetworkBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
                    val networkInfo = (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo
                    val available = networkInfo != null && networkInfo.isAvailable
                    mPageView.setNetworkAvailable(available)
                }
            }
        }

        registerReceiver(mNetworkBroadcastReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    /**
     * 初始化地址输入框
     */
    private fun initInputBox() = with(mInputBox) {
        addTextChangedListener(mTextWatcher)

        setOnEditorActionListener { _, actionId, _ ->
            if (actionId == KeyEvent.KEYCODE_ENDCALL) {
                val url = mInputBox.text.toString().trim()
                if (url.isEmpty()) {
                    toast("输入内容不能为空")
                    return@setOnEditorActionListener true
                }

                if (mCurrentMode == Type.MODE_PIN_EDIT) {
                    notifyPinUpdate(mCurrentEditorPosition)
                    return@setOnEditorActionListener true
                }

                loadPage(url)
            }
            return@setOnEditorActionListener true
        }

        if (isPrivate) {
            drawableLeft(R.drawable.ic_action_private)
            setPadding(-dip2px(5f), paddingTop, paddingRight, paddingBottom)
        }

        if (SP.isShowKeyboard) {
            showKeyboard()
        }

        //todo[Checked] 地址栏控制
        setOmniboxControlListener()
    }

    /**
     * 设置地址栏控制切换历史的监听器
     * Created by zhongzilu on 2018-10-18
     */
    private fun setOmniboxControlListener() {
        if (!SP.omniboxCtrl) {
            mInputBox.setOnTouchListener(null)
            return
        }

        mInputBox.setOnTouchListener(SwipeToBoundListener(toolbar, object : SwipeToBoundListener.BoundCallback {
            private val keyListener = mInputBox.keyListener
            override fun canSwipe(): Boolean = mBottomSheetBehavior.isCollapsed() && mCurrentMode == Type.MODE_WEB

            override fun onSwipe() {
                mInputBox.keyListener = null
                mInputBox.isFocusable = false
                mInputBox.isFocusableInTouchMode = false
                mInputBox.clearFocus()
            }

            override fun onBound(canSwitch: Boolean, left: Boolean) {
                mInputBox.keyListener = keyListener
                mInputBox.isFocusable = true
                mInputBox.isFocusableInTouchMode = true
                mInputBox.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                mInputBox.clearFocus()

                if (!canSwitch) return
                val msg: String
                if (left) {
                    if (mPageView.canGoBackOrForward(+1)) {
                        msg = mPageView.getBackOrForwardHistoryItem(+1).title
                        mPageView.goForward()
                    } else {
                        msg = getString(R.string.toast_msg_last_history)
                    }
                } else {
                    if (mPageView.canGoBackOrForward(-1)) {
                        msg = mPageView.getBackOrForwardHistoryItem(-1).title
                        mPageView.goBack()
                    } else {
                        restUiAndStatus()
                        return
                    }
                }
                toast(msg)
            }

        }))
    }


    /**
     * 根据Message信息加载网页页面
     */
    private fun loadPage(msg: Message) {
        val transport = msg.obj
        when (transport) {
            is WebView.WebViewTransport -> {
                transport.webView = mPageView
                msg.sendToTarget()
            }
        }
    }

    /**
     * 根据网络地址加载网页页面
     */
    private fun loadPage(url: String) {
        goneRecordRecycler()
        mInputBox.hideKeyboard()
        mPageView.loadUrl(url)
    }

    /**
     * 显示历史访问记录
     */
    private fun showRecordRecycler() {
        mRecordRecycler?.visible()
    }

    /**
     * 隐藏历史访问记录
     */
    private fun goneRecordRecycler() {
        mRecordRecycler?.gone()
    }

    /**
     * 地址栏输入框文字变化监听
     */
    private val mTextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            val input = s?.apply { toString().trim() } ?: ""

            //如果为Pin编辑模式，需要动态改变确认菜单的Enable状态
            if (mCurrentMode == Type.MODE_PIN_EDIT) {
                mConfirmMenu?.isEnabled = input.isNotEmpty()
                return
            }

            //如果为其他模式则需要动态搜索历史记录
            //当输入内容为空时则隐藏历史记录列表
            if (input.isEmpty()) {
                goneRecordRecycler()
                return
            }

            //当输入内容不为空时重新搜索匹配的记录并显示历史记录列表
            showRecordRecycler()
            searchRecord(input)

        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }
    }

    /**
     * 搜索历史记录
     */
    private fun searchRecord(input: CharSequence) {
        //todo[Checked] 后台查询匹配记录项，查到后进行差异比较，最后更新对应位置的数据
        doAsync {
            val records = SQLHelper.searchRecord(input.toString())

            val oldList = ArrayList<Record>(mRecords)

            mRecords.clear()
            mRecords.addAll(records)

            compareRecordListDiff(oldList, mRecords).dispatchUpdatesTo(mRecordsAdapter)
        }
    }

    /**
     * 对比Record记录列表的差异
     */
    private fun compareRecordListDiff(oldList: List<Record>, newList: List<Record>): DiffUtil.DiffResult {
        return DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                    oldList[oldItemPosition].url == newList[newItemPosition].url

            override fun getOldListSize(): Int = oldList.size

            override fun getNewListSize(): Int = newList.size

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldRecord = oldList[oldItemPosition]
                val newRecord = newList[newItemPosition]
                return oldRecord.title == newRecord.title && oldRecord.time == newRecord.time
            }

        })
    }

    /**
     * 底部菜单的展开/关闭状态监听器
     * 当拖拽底部菜单时，需要动态改变灰色背景[mMaskView]的透明度
     */
    private val mBottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            when (newState) {
            //展开状态
                BottomSheetBehavior.STATE_EXPANDED -> afterBottomSheetExpanded()

            //关闭状态
                BottomSheetBehavior.STATE_COLLAPSED -> afterBottomSheetCollapsed()

                BottomSheetBehavior.STATE_DRAGGING -> mBottomSheetBehavior.isHideable = false
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            val alpha = slideOffset * 0.3f
            mMaskView.alpha = if (alpha < 0) 0f else alpha
        }
    }

    /**
     * 设置遮罩触摸监听器
     * 展开底部菜单时出现的灰色背景遮罩，当触摸灰色遮罩时需要关闭底部菜单栏
     */
    private val mMaskTouchListener = View.OnTouchListener { v, _ ->
        if (v.id == R.id.mMaskView) {
            closeBottomSheet()
            return@OnTouchListener true
        }

        return@OnTouchListener false
    }

    /**
     * 实现监听SharedPreference的值的变化
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            getString(R.string.preference_key_homepage_reverse) -> {
                mPinsLayoutManager.stackFromEnd = SP.pinsReverse
                setPinReversePadding()
            }

        //监听地址栏固定设置
            getString(R.string.preference_key_omnibox_fixed) -> setInputBoxNestScroll()

        //地址栏控制设置发生变化
            getString(R.string.preference_key_omnibox_control) -> setOmniboxControlListener()

        //长按震动设置发生变化
            getString(R.string.preference_key_back_vibrate) -> registerVibrator()
        }
    }

    override fun onReceivedWebThemeColor(str: String) {
        //todo[Checked] 处理接收到的网站主题色，可以用来更换任务栏颜色或其他作用
        L.d(TAG, "onReceivedWebThemeColor: $str")
        setStatusBarColor(str)
    }

    private fun setStatusBarColor(color: String) {
        if (isPrivate) return
        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = if (color.isNotEmpty()) Color.parseColor(color) else Color.BLACK
        }
    }

    override fun onFormResubmission(dontResend: Message, resend: Message) {
        //todo[Checked] 处理表单重新提交数据
        dialogBuilder().setCancelable(false)
                .setMessage(R.string.dialog_message_form_resubmission)
                .setPositiveButton(R.string.dialog_button_resend) { dialog, _ ->
                    resend.sendToTarget()
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.dialog_button_cancel) { dialog, _ ->
                    dontResend.sendToTarget()
                    dialog.dismiss()
                }.create().show()
    }


    override fun onReceivedClientCertRequest(request: ClientCertRequest) {
        //todo[Checked] 处理接收到SSL认证请求
        KeyChain.choosePrivateKeyAlias(this, { alias ->
            alias?.let {
                if (it.isEmpty()) request.cancel()
                else proceedClientCertRequest(request, it)
            }
        }, request.keyTypes,
                request.principals,
                request.host,
                request.port,
                null)
    }

    /**
     * 处理SSL认证请求
     */
    private fun proceedClientCertRequest(request: ClientCertRequest, alias: String) {
        doAsync {
            try {
                request.proceed(KeyChain.getPrivateKey(this@PageActivity, alias),
                        KeyChain.getCertificateChain(this@PageActivity, alias))
            } catch (e: Exception) {
                e.printStackTrace()
                request.ignore()
            }
        }
    }

    /**
     * @see https://blog.csdn.net/Crazy_zihao/article/details/51557425
     */
    override fun onReceivedSslError(handler: SslErrorHandler, error: SslError) {
        //todo[Checked] 处理HTTPS的网站加载HTTP的内容安全问题
        val host = error.url.host()

        val builder = SpannableStringBuilder().apply {
            val prefix = getString(R.string.dialog_message_ssl_error_prefix)
            append(prefix)
            append(host)
            setSpan(StyleSpan(android.graphics.Typeface.BOLD), prefix.length, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            append(getString(R.string.dialog_message_ssl_error_middle))
            append(getSslErrorMsg(error))
        }

        dialogBuilder().setCancelable(false)
                .setMessage(builder)
                .setPositiveButton(R.string.dialog_button_cancel) { dialog, _ ->
                    handler.cancel()
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.dialog_button_process) { dialog, _ ->
                    handler.proceed()
                    dialog.dismiss()
                }.create().show()
    }

    /**
     * 获取Ssl错误提示信息文字
     *
     * @param sslError
     * @return String 错误提示文字
     */
    private fun getSslErrorMsg(sslError: SslError): String {
        return when (sslError.primaryError) {
            SslError.SSL_NOTYETVALID -> resources.getString(R.string.ssl_error_notyetvalid)
            SslError.SSL_EXPIRED -> resources.getString(R.string.ssl_error_expired)
            SslError.SSL_IDMISMATCH -> resources.getString(R.string.ssl_error_idmismatch)
            SslError.SSL_UNTRUSTED -> resources.getString(R.string.ssl_error_untrusted)
            SslError.SSL_DATE_INVALID -> resources.getString(R.string.ssl_error_date_invalid)
            else -> resources.getString(R.string.ssl_error_invalid)
        }
    }

    override fun onReceivedHttpAuthRequest(handler: HttpAuthHandler, host: String, realm: String) {
        //todo[Checked] 接收一个HTTP认证请求，使用提供的HttpAuthHandler对象设置WebView对HTTP认证请求的响应
        AuthLoginDialog(this).apply {
            setTitle(host)
            setOnPositiveClickListener { handler.proceed(getUserName(), getPassword()) }
            setOnNegativeClickListener { handler.cancel() }
        }.show()
    }

    override fun onPageStarted(url: String, title: String, icon: Bitmap?) {

        //set Web Mode
        mCurrentMode = Type.MODE_WEB

        mInputBox.hideKeyboard()
        mInputBox.text = url.toColorUrl()

        //only show pageView
        mPageView.onResume()
        mPageView.visible()
        goneRecordRecycler()
        mPinsRecycler?.gone()

        //show progress
        mProgress.visible()
//        mProgress.progress = 0

        //set menu item
        showMenu(stop = true)
    }

    override fun onPageFinished(url: String, title: String, icon: Bitmap?) {
        mProgress.gone()
        mMenuOptionWidget.showMoreMenu()

        showMenu(refresh = true)
    }

    override fun onProgressChanged(progress: Int) {
        mProgress.apply {
            if (isGone()) visible()
            this.progress = progress

            if (progress >= 80) gone()
        }
    }

    override fun onDownloadStart(url: String, userAgent: String, contentDisposition: String, mimetype: String, contentLength: Long) {
        //todo[Checked] 处理下载任务监听
        permission(Manifest.permission.WRITE_EXTERNAL_STORAGE) {

            var fileName = Download.parseFileName(url, contentDisposition, mimetype)

            if (fileName.length > 30) {
                fileName = fileName.toMask(10, 10)
            }

            Snackbar.make(mInputBox, getString(R.string.toast_download_confirm, fileName, contentLength.formatSize()), Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.dialog_button_confirm) {
                        Download.inBrowser(this, url, contentDisposition, mimetype)
                    }.show()
        }
    }

    private var mQuickOptionDialog: QuickOptionDialog? = null
    private val mImageType = arrayOf(WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE,
            WebView.HitTestResult.IMAGE_TYPE
    )

    override fun onWebViewLongPress(url: String, type: Int) {
        //todo[Checked] 处理网页长按事件，弹出快捷菜单浮窗
        if (mQuickOptionDialog == null) {
            mQuickOptionDialog = QuickOptionDialog(this)
                    .setQuickListener {
                        quickNewTab = {
                            if (SP.isOpenInBackground)
                                openUrlOverviewScreen(it, taskId = taskId)
                            else openUrl(it, taskId = taskId)
                        }

                        quickNewPrivateTab = {
                            if (SP.isOpenInBackground)
                                openUrlOverviewScreen(it, true, taskId)
                            else openUrl(it, true, taskId)
                        }

                        quickDownloadImg = {
                            permission(Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                                if (it.isEmpty()) return@permission
                                startDownloadImg(it)
                            }
                        }

                        quickExtractQR = {
                            toast(it)
                            if (SP.isOpenInBackground)
                                openUrlOverviewScreen(it, taskId = taskId)
                            else openUrl(it, taskId = taskId)
                        }
                    }
        }

        mQuickOptionDialog!!.setUrl(url).isImageUrl(type in mImageType).show()
    }

    /**
     * 开始下载图片，如果图片地址为base64格式的字符串，则将字符串转换成Bitmap后保存
     */
    private fun startDownloadImg(it: String) {
        if (it.isBase64Url()) {
            doAsync {
                it.base64ToBitmap()?.apply {
                    val file = save(System.currentTimeMillis().toString())
                    file.mediaScan(this@PageActivity)
                    uiThread { toast(getString(R.string.toast_save_to_path, file.absolutePath)) }
                    return@doAsync
                }

                uiThread { toast(R.string.toast_download_failed) }
            }
            return
        }

        Download.inBackground(this, it, WebUtil.HEADER_CONTENT_DISPOSITION, WebUtil.MIME_TYPE_IMAGE)
    }

    private var mCustomView: View? = null
    private var mOriginalSystemUiVisibility: Int = 0
    private var mOriginalOrientation: Int = 0
    private var mCustomViewCallback: WebChromeClient.CustomViewCallback? = null
    override fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
        //todo[Checked] 通知主应用程序当前页面已进入全屏模式
        mCustomView?.let {
            onHideCustomView()
            return
        }

        // 1. Stash the current state
        mCustomView = view
        mOriginalSystemUiVisibility = window.decorView.systemUiVisibility
        mOriginalOrientation = requestedOrientation

        // 2. Stash the custom view callback
        mCustomViewCallback = callback

        // 3. Add the custom view to the view hierarchy
//        val decor = window.decorView as FrameLayout
//        val decor = find<FrameLayout>(android.R.id.content)
        mCustomViewContainer.visible()
        mCustomViewContainer.addView(mCustomView, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT))


        // 4. Change the state of the window
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    @SuppressLint("WrongConstant")
    override fun onHideCustomView() {
        //todo[Checked] 通知主应用程序当前页面已退出全屏模式
        // 1. Remove the custom view
//        val decor = window.decorView as FrameLayout
//        val decor = find<FrameLayout>(android.R.id.content)
        mCustomViewContainer.removeView(mCustomView)
        mCustomViewContainer.gone()
        mCustomView = null

        // 2. Restore the state to it's original form
        window.decorView.systemUiVisibility = mOriginalSystemUiVisibility
        requestedOrientation = mOriginalOrientation

        // 3. Call the custom view callback
        mCustomViewCallback?.onCustomViewHidden()
        mCustomViewCallback = null
    }

    override fun onPermissionRequest(request: PermissionRequest) {
        //todo[Checked] 通知主应用程序Web内容正在请求访问指定资源的权限，当前权限未授予或拒绝该权限
        val permission = arrayOfNulls<String>(0)
        request.resources.forEach {
            when (it) {
                PermissionRequest.RESOURCE_VIDEO_CAPTURE -> permission[permission.size] = Manifest.permission.CAMERA
                PermissionRequest.RESOURCE_AUDIO_CAPTURE -> permission[permission.size] = Manifest.permission.RECORD_AUDIO
                else -> L.i(TAG, "request other permission: $it")
            }
        }

        if (permission.isNotEmpty()) {
            PermissionsManager.getInstance()
                    .requestPermissionsIfNecessaryForResult(this, permission,
                            object : PermissionsResultAction() {
                                override fun onGranted() {
                                    request.grant(permission)
                                }

                                override fun onDenied(permission: String) {
                                    request.deny()
                                    grantVideoAndAudioPermissionFail(permission)
                                }
                            })
        }
    }

    /**
     * 网页授权失败提示
     * @param permission 申请的权限
     */
    private fun grantVideoAndAudioPermissionFail(permission: String) {
        when (permission) {
            Manifest.permission.CAMERA -> toast(R.string.toast_camera_permission_denied)
            Manifest.permission.RECORD_AUDIO -> toast(R.string.toast_record_audio_permission_denied)
        }
    }

    override fun onPermissionRequestCanceled(request: PermissionRequest) {
        //通知主应用程序已经取消了请求访问指定资源的权限请求
    }

    override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
        //todo[Checked] 处理网站请求访问用户地理位置权限
        permission(Manifest.permission.ACCESS_FINE_LOCATION) {
            showRequestLocationPermissionDialog(origin, callback)
        }
    }

    override fun onGeolocationPermissionsHidePrompt() {
        //处理取消网站访问地理位置的请求
    }

    /**
     * 弹出网站请求获取用户地理位置权限的弹窗
     */
    private fun showRequestLocationPermissionDialog(origin: String, callback: GeolocationPermissions.Callback) {
        //todo[Checked] 容易出现获取host不正确的问题，解决方法：mPageView.url.host()改为使用origin参数
        val builder = SpannableStringBuilder().apply {
            val prefix = getString(R.string.dialog_message_allow_location_prefix)
            append(prefix)
            append(origin)
            setSpan(StyleSpan(android.graphics.Typeface.BOLD), prefix.length, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            append(getString(R.string.dialog_message_allow_location_suffix))
        }

        dialogBuilder().setCancelable(true)
                .setMessage(builder)
                .setPositiveButton(R.string.dialog_button_allow) { dialog, _ ->
                    callback.invoke(origin, true, true)
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.dialog_button_deny) { dialog, _ ->
                    callback.invoke(origin, false, false)
                    dialog.dismiss()
                }
                .setNeutralButton(R.string.dialog_button_block) { dialog, _ ->
                    callback.invoke(origin, false, true)
                    dialog.dismiss()
                }.setOnCancelListener {
                    callback.invoke(origin, false, false)
                }.create().show()
    }

    override fun onReceivedTitle(url: String, title: String) {
        //todo[Checked] 处理接收到的网站标题
        setAppTaskDescription(title, null)
    }

    /**
     * 回调处理网页上的Favicon图标，
     */
    override fun onReceivedIcon(url: String, title: String, icon: Bitmap?) {
        //todo[Checked] 处理接收到网站图标
//        setAppTaskDescription(title, icon)
    }

    /**
     * 回调处理网页上的配置化信息，该接口为自定义接口，所传参数的值也是通过js注入来获取的,
     * 因此回调处理上会有时间差，尽量不要用来实现需要即时性的功能，如果对时间要求不太严格，
     * 可以用来实现一些针对不同网站配置呈现不同视觉效果的功能，比如这里用来更改最近任务栏的样式
     */
    override fun onReceivedWebConfig(title: String, icon: Bitmap?, color: String) {
        //todo[Checked] 处理接收到的网站配置
        L.d(TAG, "onReceivedWebConfig $title : $color | ${icon != null}")
        setStatusBarColor(color)
        setAppTaskDescription(title, icon, color)
    }

    /**
     * 该方法回调处理网页上设置的苹果图标，该方法可能会有多次回调，
     * 这里可以用接收到的图标地址，下载后转为Bitmap，用做最近任务栏的图标，
     * 或者用来作为网站添加到桌面的快捷图标
     *
     * 详情查看[PageChromeClient.onReceivedTouchIconUrl]
     *
     * 推荐博客：https://droidyue.com/blog/2015/01/18/deal-with-touch-icon-in-android/
     */
    override fun onReceivedTouchIconUrl(url: String, precomposed: Boolean) {
        //todo 处理接收到网站设置的苹果图标资源
    }

    private var mAppName: String? = null
    private var mAppIcon: Bitmap? = null
    private fun defaultThemeColor(): Int = if (isPrivate) Color.BLACK else Color.WHITE

    /**
     * 设置应用最近任务栏的样式
     * 该方法是[Build.VERSION_CODES.LOLLIPOP]新增的API，
     * 用来修改应用在最近任务栏的默认样式，这里用来实现对应网页在最近任务栏的不同样式。
     *
     * @param title 网站的标题。如果没有标题，则默认使用应用的名称
     * @param icon  网站的favicon图标。网站有可能没有设置favicon图标，如果没有则默认使用应用的桌面图标
     * @param color 网站的主题色。网站极大可能没有设置主题色，因此此参数默认为空字符串，
     * 在没有网站主题色的情况下，如果当前为隐私模式时，主题色为[Color.BLACK],
     * 如果为普通模式时，主题色为[Color.WHITE]
     */
    private fun setAppTaskDescription(title: String, icon: Bitmap?, color: String = "") {
        var label = title
        var bitmap = icon
        var color2 = defaultThemeColor()

        if (title.isEmpty()) {
            if (mAppName == null) mAppName = getString(R.string.app_name)
            label = mAppName!!
        }

        if (bitmap == null) {
            if (mAppIcon == null) mAppIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
            bitmap = mAppIcon
        }

        if (color.isNotEmpty() && !isPrivate) {
            color2 = Color.parseColor(color)
        }

        setTaskDescription(ActivityManager.TaskDescription(label, bitmap, color2))
    }

    /**
     * 网页发起选择文件的回调Callback
     */
    private var mFileChooserCallback: ValueCallback<Array<Uri>>? = null

    /**
     * 网页发起选择文件
     */
    override fun onShowFileChooser(filePathCallback: ValueCallback<Array<Uri>>,
                                   fileChooserParams: WebChromeClient.FileChooserParams): Boolean {
        mFileChooserCallback = filePathCallback
        startActivityForResult(fileChooserParams.createIntent(), Type.CODE_CHOOSE_FILE)
        return true
    }

    override fun onJsAlert(url: String, message: String, result: JsResult): Boolean {
        //todo[Checked] 处理网站上的alert弹窗
//        jsResponseDialog(url, message, result)
        longToast(message)
        result.confirm()
        return true
    }

    override fun onJsPrompt(url: String, message: String, defaultValue: String, result: JsPromptResult): Boolean {
        //todo[Checked] 处理网站上的prompt弹窗
        dialogBuilder().setTitle(url.host())
                .setCancelable(false)
                .setMessage(message)
                .setPositiveButton(R.string.dialog_button_confirm) { dialog, _ ->
                    result.confirm(defaultValue)
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.dialog_button_cancel) { dialog, _ ->
                    result.cancel()
                    dialog.dismiss()
                }.create().show()
        return true
    }

    override fun onCreateWindow(isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
        //todo[Checked] 处理请求主应用程序创建一个新窗口
        App.MESSAGE = resultMsg
        openUrl("", isPrivate, taskId)
        return true
    }

    override fun onCloseWindow() {
        //todo[Checked] 关闭给定的WebView并在必要时将其从视图系统中删除
        finishAndRemoveTask()
    }

    override fun onJsConfirm(url: String, message: String, result: JsResult): Boolean {
        //todo[Checked] 处理网站上的confirm弹窗
        jsResponseDialog(url, message, result)
        return true
    }

    override fun onJsBeforeUnload(url: String, message: String, result: JsResult): Boolean {
        //todo[Checked] 告诉客户端显示一个对话框，以确认离开当前页面的导航
        jsResponseDialog(url, message, result)
        return true
    }

    /**
     * 底部菜单展开后执行
     */
    private fun afterBottomSheetExpanded() {
        mInputBox.isFocusable = false
        mInputBox.isFocusableInTouchMode = false
        mInputBox.clearFocus()
        mMaskView.setOnTouchListener(mMaskTouchListener)
        mInputBox.hideKeyboard()
    }

    /**
     * 底部菜单关闭后执行
     */
    private fun afterBottomSheetCollapsed() {
        mInputBox.isFocusable = true
        mInputBox.isFocusableInTouchMode = true
        mInputBox.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        mInputBox.clearFocus()
        mMaskView.setOnTouchListener(null)

        _closedTodo?.invoke()
    }

    /**
     * 关闭底部菜单后执行任务
     */
    private var _closedTodo: (() -> Unit)? = null
        get() {
            val m = field
            field = null
            return m
        }

    private fun closeBottomSheet(todo: (() -> Unit)? = null) {
        _closedTodo = todo
        mBottomSheetBehavior.collapsed()
    }

    /**
     * 底部菜单栏各选项的事件监听回调
     */
    private val mMenuOptionListener = object : MenuOptionListener {
        override fun onDesktopCheckedChanged(check: Boolean) {
            mPageView.setUserAgent(if (check) Type.UA_DESKTOP else Type.UA_DEFAULT)
            closeBottomSheet()
        }

        override fun onCustomUACheckedChanged(check: Boolean) {
            mPageView.setUserAgent(if (check) Type.UA_CUSTOM else Type.UA_DEFAULT)
            closeBottomSheet()
        }

        override fun onScreenshotsClick() {
            closeBottomSheet {
                permission(Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                    captureScreenshot2Bitmap()
                }
            }
        }

        override fun onShareUrlClick() {
            closeBottomSheet { shareText(mInputBox.text.toString()) }
        }

        override fun onNewTabClick() {
            closeBottomSheet { openUrl("", taskId = taskId) }
        }

        override fun onPrivateTabClick() {
            closeBottomSheet { openUrl("", true, taskId) }
        }

        override fun onPinToHome() {
            closeBottomSheet { startPinToHome() }
        }

        override fun addToLauncher() {
            closeBottomSheet { createLauncherIcon() }
        }

        override fun onSettingsClick() {
            closeBottomSheet { go<SettingsActivity>() }
        }
    }

    /**
     * 创建桌面启动图标的Dialog
     */
    private var mAddLauncherDialog: AddLauncherDialog? = null

    /**
     * 创建桌面启动图标
     */
    private fun createLauncherIcon() {
        if (mAddLauncherDialog == null) {
            mAddLauncherDialog = AddLauncherDialog(this)
                    .setOnPositiveClickListener {
                        createLauncherShortcut(it.getUrl(), it.getTitle(), it.getIcon())
                    }
                    .setOnSelectListener {
                        pickImage(Type.CODE_GET_IMAGE)
                    }
        }

        //todo[Checked] 当网站未设置favicon图标时，此方法会抛出异常
        mAddLauncherDialog!!
                .setUrl(mPageView.url)
                .setIcon(mPageView.favicon)
                .setLabel(mPageView.title)
                .show()
    }

    /**
     * 添加到主页书签
     */
    private fun startPinToHome() {
        val pin = Pin(title = mPageView.title, url = mPageView.url)
        doAsync {
            SQLHelper.savePin(pin)
            val newPins = SQLHelper.findAllPins()

            uiThread {

                //notify update pin list
                mPins.clear()
                mPins.addAll(newPins)
                mPinsAdapter.notifyDataSetChanged()

                toast(getString(R.string.toast_pin_to_homepage, mPageView.title))
            }
        }
    }

    private var mRefreshMenu: MenuItem? = null
    private var mStopMenu: MenuItem? = null
    private var mConfirmMenu: MenuItem? = null
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_page, menu)
        mRefreshMenu = menu.findItem(R.id.refresh)
        mStopMenu = menu.findItem(R.id.stop)
        mConfirmMenu = menu.findItem(R.id.confirm)

        return true
    }

    /**
     * 显示菜单按钮, 默认全隐藏
     *
     * @param confirm 确认菜单按钮，默认为false
     * @param stop 停止菜单按钮，默认为false
     * @param refresh 刷新菜单按钮，默认为false
     */
    private fun showMenu(confirm: Boolean = false, stop: Boolean = false, refresh: Boolean = false) {
        mConfirmMenu?.isVisible = confirm
        mRefreshMenu?.isVisible = refresh
        mStopMenu?.isVisible = stop
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.refresh -> {
                //默认模式时，刷新Pin列表
                if (mCurrentMode == Type.MODE_DEFAULT) {
                    loadPinsData()
                }

                //网页浏览模式时，刷新网页
                else if (mCurrentMode == Type.MODE_WEB) {
                    mPageView.reload()
                }
            }
            R.id.stop -> mPageView.stopLoading()
            R.id.confirm -> notifyPinUpdate(mCurrentEditorPosition)
        }
        return true
    }

    /**
     * 编辑Pin名称后更新UI和数据库
     *
     * @param position 编辑的Pin在RecyclerView中的position
     */
    private fun notifyPinUpdate(position: Int) {

        val pin = mPins[position]
        pin.title = mInputBox.text.toString().trim()

        doAsync {
            SQLHelper.updatePinById(pin)

            uiThread {
                mCurrentMode = Type.MODE_DEFAULT
                mPinsAdapter.notifyItemChanged(position)

                restUiAndStatus()
            }
        }
    }

    /**
     * 重置Ui和状态
     */
    private fun restUiAndStatus() {
        mCurrentMode = Type.MODE_DEFAULT

        //rest optionMenu
        showMenu(refresh = true)

        //rest inputBox
        mInputBox.apply {
            setHint(R.string.hint_input_normal)
            clearFocus()
            setText("")
            hideKeyboard()
        }

        mPageView?.apply {
            onBackPressed()
            clearHistory()
        }

        mPinsRecycler?.visible()
        mRecordRecycler?.visibleDo { it.gone() }
        mProgress?.visibleDo { it.gone() }
    }

    override fun onResume() {
        mPageView.onResume()
        FingerprintService.isForeground = true
        super.onResume()
    }

    override fun onPause() {
        mPageView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        //todo[BUG] 退出应用后，内存并没有得到释放
        mPageView.destroy()
        mInputBox.removeTextChangedListener(mTextWatcher)
        mMenuOptionWidget.setMenuOptionListener(null)
        unregisterReceiver(mNetworkBroadcastReceiver)
        FingerprintService.isForeground = false
        super.onDestroy()
    }

    override fun onBackPressed() {

        //当状态为Pin编辑模式时，无论当前在做什么，一律重置当前状态为普通模式
        if (mCurrentMode == Type.MODE_PIN_EDIT) {
            restUiAndStatus()
            return
        }

        //当历史纪录列表呈现时按返回按钮，则隐藏历史记录列表
        mRecordRecycler?.visibleDo {
            it.gone()
            it.hideKeyboard()
            return
        }

        //当底部菜单是展开状态时，则关闭底部菜单
        if (mBottomSheetBehavior.isExpanded()) {
            closeBottomSheet()
            return
        }

        if (mBottomSheetBehavior.isHidden()) {
            mBottomSheetBehavior.collapsed()
        }

        //当网页可以后退
        if (mPageView.canGoBack()) {
            mPageView.goBack()
            return
        }

        if (mPageView.isVisible()) {
            mCurrentMode = Type.MODE_DEFAULT
            mPageView.onBackPressed()
            mPinsRecycler?.visible()
            mMenuOptionWidget.hideMoreMenu()
            mInputBox.setText("")
            return
        }

        //todo[Checked] 处理多任务窗口，如果后台打开了多个窗口，可以从当前窗口回到上一个窗口
        listAppTask()

//        super.onBackPressed()
    }

    /**
     * 显示截图等待视图
     */
    private var mLoadingView: View? = null

    private fun showLoading() {
        if (mLoadingView == null) {
            mLoadingView = mLoadingStub.inflate()
        }

        mLoadingView?.show()
    }

    /**
     * 隐藏截图等待视图
     */
    private fun hideLoading() {
        mLoadingView?.hide()
    }

    /**
     * 抓取PageView网页视图为Bitmap
     */
    private fun captureScreenshot2Bitmap() {
        showLoading()

        mPageView.apply {
            try {
                val saveName = title

                doAsync {
                    val bitmap = toBitmap(getContentWidth().toFloat(), contentHeight.toFloat())
                    val file = bitmap.save(saveName)
                    file.mediaScan(this@PageActivity)

                    uiThread {
                        hideLoading()
                        toast(getString(R.string.toast_save_to_path, file.absolutePath))
                    }
                }

            } catch (e: Exception) {
                hideLoading()
                toast(R.string.toast_screenshot_failed)
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //网页选择文件回调
        if (requestCode == Type.CODE_CHOOSE_FILE) {
            mFileChooserCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data))
            return
        }

        //添加桌面图标时，选择更换图标
        if (requestCode == Type.CODE_GET_IMAGE) {
            data?.let {
                try {
                    var bitmap = MediaStore.Images.Media.getBitmap(contentResolver, data.data)
                    //todo[Checked] bitmap过大将不能创建桌面快捷方式，解决方法：裁剪或缩放或压缩图片的大小或尺寸
                    if (bitmap.width > 192 || bitmap.height > 192) {
                        bitmap = bitmap.scale(192f, 192f)
                    }

                    mAddLauncherDialog?.setIcon(bitmap)

                } catch (e: IOException) {
                    e.printStackTrace()
                    toast(R.string.toast_select_icon_failed)
                }
            }
        }
    }

    /**
     * 处理网页上的alert、confirm等弹窗事件
     * @param url
     * @param msg
     * @param jsResult
     */
    private fun jsResponseDialog(url: String, msg: String, jsResult: JsResult) {
        dialogBuilder().setTitle(url.host())
                .setCancelable(false)
                .setMessage(msg)
                .setPositiveButton(R.string.dialog_button_confirm) { dialog, _ ->
                    jsResult.confirm()
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.dialog_button_cancel) { dialog, _ ->
                    jsResult.cancel()
                    dialog.dismiss()
                }.create().show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode != KeyEvent.KEYCODE_BACK) {
            return super.onKeyDown(keyCode, event)
        }

        //处理部分 Android N(7.0+)以上系统不回调onKeyLongPress的问题
        supportN {
            CountDown.with { handleKeyLongPress() }.start()
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            CountDown.cancle()
        }

        return super.onKeyUp(keyCode, event)
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        L.d(TAG, "onKeyLongPress")

        if (handleKeyLongPress()) return true

        return super.onKeyLongPress(keyCode, event)
    }

    /**
     * 处理长按返回，需要根据配置进行震动反馈，并初始化页面UI
     */
    private fun handleKeyLongPress(): Boolean {
        if (mCurrentMode == Type.MODE_WEB) {
            //todo[Checked] 长按返回时震动
            vibrate()
            restUiAndStatus()
            return true
        }

        return false
    }

    private object CountDown : CountDownTimer(500L, 500L) {

        override fun onFinish() {
            _todo?.invoke()
        }

        override fun onTick(millisUntilFinished: Long) {}

        private var _todo: (() -> Unit)? = null
        fun with(f: (() -> Unit)): CountDown {
            _todo = f
            return this
        }

        fun cancle() {
            _todo = null
        }
    }

    private var mActivityManager: ActivityManager? = null

    /**
     * 罗列本应用的所有进程任务，如果存在多个后台任务窗口，则寻找打开该任务的上一个窗口（以下称为前窗口），
     * 如果存在前窗口，则将前窗口呈现出来，然后关闭本任务窗口；
     * 如果不存在前窗口，则呈现任务栈顶的窗口，然后关闭本任务窗口；
     * 其他情况，默认调用{@link super.onBackPressed}方法来处理
     */
    private fun listAppTask() {
        if (mActivityManager == null) {
            mActivityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        }

        mActivityManager!!.appTasks.forEach {
            L.i(TAG, "listAppTask taskInfo id: " + it.taskInfo.id)
            if (it.taskInfo.id == mParentTaskId) {
                finishAndRemoveTask()
                it.moveToFront()
                return
            }
        }

        finishAndRemoveTask()
        super.onBackPressed()
    }
}