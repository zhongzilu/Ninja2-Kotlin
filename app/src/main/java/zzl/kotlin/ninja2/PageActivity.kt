package zzl.kotlin.ninja2

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.os.Message
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
import android.util.SparseArray
import android.view.*
import android.webkit.*
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams
import collections.forEach
import com.anthonycr.grant.PermissionsManager
import com.anthonycr.grant.PermissionsResultAction
import kotlinx.android.synthetic.main.activity_page.*
import kotlinx.android.synthetic.main.content_bottom_sheet.*
import kotlinx.android.synthetic.main.content_bottom_sheet.view.*
import kotlinx.android.synthetic.main.content_main.*
import org.jetbrains.anko.doAsync
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
     * If {@link Intent#FLAG_ACTIVITY_MULTIPLE_TASK} has not been used this Activity
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
     * 检查Intent传递的参数
     */
    private fun checkExtra() {
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

            uiThread {
                mPins.clear()
                mPins.addAll(pins)
                mPinsAdapter.notifyDataSetChanged()
//                mPinsAdapter.notifyItemRangeChanged(0, mPins.size)
            }
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
            //这里使用队列数据结构，当连续滑动删除几个item时可能会保存多个item数据，并需要记录删除循序。
            //val queue = ArrayBlockingQueue<Pin>(2)
            val array = SparseArray<Pin>()

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                mCurrentEditorPosition = viewHolder.adapterPosition

                // 如果是往右滑动，则进入编辑模式
                if (direction == ItemTouchHelper.RIGHT) {
                    editPinName(mCurrentEditorPosition)
                    mPinsAdapter.notifyItemChanged(mCurrentEditorPosition)
                    return
                }

                // 如果是往左滑动，则先取出记录存到删除队列中，以备撤销使用
                val pin = mPins[mCurrentEditorPosition]
                L.i(TAG, "onSwiped pin position: $mCurrentEditorPosition")
                array.put(viewHolder.adapterPosition, pin)

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
                L.i(TAG, "clearView array size: ${array.size()}")
                if (array.size() > 0) {
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
                val index = array.size() - 1
                val position = array.keyAt(index)
                val pin = array.valueAt(index)

                mPinsAdapter.addItem(position, pin)
                array.removeAt(index)

                L.i(TAG, "revoke array size: ${array.size()}")

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
                L.i(TAG, "array size: ${array.size()}")
                if (event != Snackbar.Callback.DISMISS_EVENT_CONSECUTIVE) {
                    //todo 处理滑动多条数据的删除

                    array.forEach { key, pin ->
                        L.i(TAG, "position: $key & name: ${pin.title}")
                    }
                    array.clear()
//                    val pin = array.valueAt(0)
//                    array.removeAt(0)
//                    doAsync {
//                        SQLHelper.deletePin(pin)
//                    }
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
     * 当拖拽底部菜单时，需要动态改变灰色背景{@link #mMaskView}的透明度
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
        }
    }

    override fun onReceivedWebThemeColor(str: String) {
        //todo 处理接收到的网站主题色，可以用来更换任务栏颜色或其他作用
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
        val host = mPageView.url.host()

        val builder = SpannableStringBuilder().apply {
            append(getString(R.string.dialog_message_ssl_error_prefix))
            append(host)
            setSpan(StyleSpan(android.graphics.Typeface.BOLD), length, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
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
        AuthLoginBottomSheet(this).apply {
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
        mProgress.progress = 0

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

            if (progress >= 99) gone()
        }
    }

    override fun onDownloadStart(url: String, userAgent: String, contentDisposition: String, mimetype: String, contentLength: Long) {
        //todo[Checked] 处理下载任务监听
        permission(Manifest.permission.WRITE_EXTERNAL_STORAGE) {
            Download.inBrowser(this, url, contentDisposition, mimetype)
        }
    }

    private var mQuickOptionDialog: QuickOptionDialog? = null
    override fun onWebViewLongPress(url: String) {
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

                        }
                    }

        }

        mQuickOptionDialog!!.setUrl(url).show()
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
        //todo 处理接收到的网站标题
    }

    override fun onReceivedIcon(url: String, title: String, icon: Bitmap?) {
        //todo 处理接收到网站图标
    }

    override fun onReceivedTouchIconUrl(url: String, precomposed: Boolean) {
        //todo 处理接收到网站设置的苹果图标资源
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
        jsResponseDialog(url, message, result)
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
        openUrl("", this.isPrivate, taskId)
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

        _closedTodo?.let {
            it.invoke()
            _closedTodo = null
        }
    }

    /**
     * 关闭底部菜单后执行任务
     */
    private var _closedTodo: (() -> Unit)? = null

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

        //todo[BUG] 当网站未设置favicon图标时，此方法会抛出异常
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
        mInputBox.setHint(R.string.hint_input_normal)
        mInputBox.clearFocus()
        mInputBox.setText("")
        mInputBox.hideKeyboard()

        mRecordRecycler?.visibleDo { it.gone() }
        mPageView?.visibleDo { it.gone() }
        mProgress?.visibleDo { it.gone() }
    }

    override fun onResume() {
        mPageView.onResume()
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

        finishAndRemoveTask()
        super.onBackPressed()
    }

    /**
     * 显示截图等待视图
     */
    private var mLoadingView: View? = null

    fun showLoading() {
        if (mLoadingView == null) {
            mLoadingView = mLoadingStub.inflate()
        }

        mLoadingView?.show()
    }

    /**
     * 隐藏截图等待视图
     */
    fun hideLoading() {
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
//                    if (bitmap.width > 512 || bitmap.height > 512) {
//                        toast(resources.getString(R.string.toast_select_icon_size_error, 512, 512))
//                        return@let
//                    }

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
}