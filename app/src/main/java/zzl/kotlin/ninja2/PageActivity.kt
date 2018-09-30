package zzl.kotlin.ninja2

import android.Manifest
import android.content.*
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.os.Message
import android.preference.PreferenceManager
import android.support.design.widget.BottomSheetBehavior
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import android.widget.FrameLayout.LayoutParams
import kotlinx.android.synthetic.main.activity_page.*
import kotlinx.android.synthetic.main.content_bottom_sheet.*
import kotlinx.android.synthetic.main.content_bottom_sheet.view.*
import kotlinx.android.synthetic.main.content_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import zzl.kotlin.ninja2.application.*
import zzl.kotlin.ninja2.widget.AddLauncherDialog
import zzl.kotlin.ninja2.widget.MenuOptionListener
import zzl.kotlin.ninja2.widget.PageView


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
        mTargetUrl = intent.getStringExtra(EXTRA_TARGET_URL)
        mTargetUrl?.let { if (it.isNotEmpty()) loadPage(it) }

        isPrivate = intent.getBooleanExtra(EXTRA_PRIVATE, false)
        mPageView.setupViewMode(isPrivate)
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
    }

    /**
     * 异步加载本地所有的Pin记录
     */
    private fun loadPinsData() {
        doAsync {
//            val pins = SugarRecord.listAll(Pin::class.java)
            val pins = SQLHelper.findAllPins()

            uiThread {
                mPins.clear()
                mPins.addAll(pins)
                mPinsAdapter.notifyDataSetChanged()
                mPinsAdapter.notifyItemRangeChanged(0, mPins.size)
            }
        }
    }

    //设置主页列表反序时呈现的padding距离
    private fun setPinReversePadding() {
        val padding = (dip2px(48.0f) - sp2px(24.0f)) / 2
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
            it.layoutManager = LinearLayoutManager(this)
            it.setHasFixedSize(true)
            it.itemAnimator = DefaultItemAnimator()
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
                    toast("请输入浏览地址")
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

    private fun loadPage(url: String) {
        mRecordRecycler?.gone()
        mInputBox.hideKeyboard()
        mPageView.loadUrl(url)
    }

    /**
     * 清空搜索记录并显示控件
     */
    private fun showRecordRecycler() {
        mRecords.clear()
        mRecordsAdapter.notifyDataSetChanged()
        mRecordRecycler?.visible()
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
                mRecordRecycler?.gone()
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
        //todo 后台查询匹配记录项，查到后进行差异比较，最后更新对应位置的数据

//        replaceSearchResult()
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
    }

    override fun onFormResubmission(dontResend: Message, resend: Message) {
    }


    override fun onReceivedClientCertRequest(request: ClientCertRequest) {
    }

    override fun onReceivedHttpAuthRequest(handler: HttpAuthHandler, host: String, realm: String) {
    }

    /**
     * @see https://blog.csdn.net/Crazy_zihao/article/details/51557425
     */
    override fun onReceivedSslError(handler: SslErrorHandler, error: SslError) {
        handler.proceed()
    }

    override fun onPageStarted(url: String, title: String, icon: Bitmap?) {

        //set Web Mode
        mCurrentMode = Type.MODE_WEB

        mInputBox.hideKeyboard()
        mInputBox.text = url.toColorUrl()

        //only show pageView
        mPageView.onResume()
        mPageView.visible()
        mRecordRecycler?.gone()
        mPinsRecycler?.gone()

        //show progress
        mProgress.visible()
        mProgress.progress = 0

        //set menu item
        mStopMenu?.isVisible = true
        mRefreshMenu?.isVisible = false

        mMenuOptionWidget.showMoreMenu()
    }

    override fun onPageFinished(url: String, title: String, icon: Bitmap?) {
        mProgress.gone()
        mStopMenu?.isVisible = false
        mRefreshMenu?.isVisible = true
    }

    override fun onCloseWindow() {
    }

    override fun onProgressChanged(progress: Int) {
        mProgress.progress = progress

        if (progress >= 99) {
            mProgress.gone()
        }
    }

    override fun onDownloadStart(url: String, userAgent: String, contentDisposition: String, mimetype: String, contentLength: Long) {

    }

    override fun onWebViewLongPress(url: String) {
    }

    override fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
    }

    override fun onPermissionRequest(request: PermissionRequest) {
    }

    override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
    }

    override fun onReceivedTitle(url: String, title: String) {
    }

    override fun onReceivedIcon(url: String, title: String, icon: Bitmap?) {
    }

    override fun onReceivedTouchIconUrl(url: String, precomposed: Boolean) {
    }

    override fun onShowFileChooser(filePathCallback: ValueCallback<Array<Uri>>,
                                   fileChooserParams: WebChromeClient.FileChooserParams): Boolean {
        return false
    }

    override fun onJsAlert(url: String, message: String, result: JsResult): Boolean {
        return false
    }

    override fun onJsPrompt(url: String, message: String, defaultValue: String, result: JsPromptResult): Boolean {
        return false
    }

    override fun onCreateWindow(isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
        return false
    }

    override fun onHideCustomView() {
    }

    override fun onPermissionRequestCanceled(request: PermissionRequest) {
    }

    override fun onJsConfirm(url: String, message: String, result: JsResult): Boolean {
        return false
    }

    override fun onGeolocationPermissionsHidePrompt() {
    }

    override fun onJsBeforeUnload(url: String, message: String, result: JsResult): Boolean {
        return false
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
     * 创建桌面启动图标
     */
    private fun createLauncherIcon() {
        AddLauncherDialog(this)
                .setIcon(mPageView.favicon)
                .setLabel(mPageView.title)
                .setOnPositiveClickListener {
                    mPageView?.let { createLauncherShortcut(it.url, it.title, it.favicon) }
                }
                .setOnSelectListener {
                    pickImage(0)
                }
                .show()
    }

    /**
     * 添加到主页书签
     */
    private fun startPinToHome() {
        val pin = Pin(title = mPageView.title, url = mPageView.url)
        doAsync {
            SQLHelper.savePin(pin)

            //todo notify pin update

            uiThread {
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.refresh -> {
                mPageView.visible()
                mPageView.reload()
            }
            R.id.stop -> mPageView.stopLoading()
            R.id.confirm -> {
            }
        }
        return true
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
        mPageView.destroy()
        mInputBox.removeTextChangedListener(mTextWatcher)
        mMenuOptionWidget.setMenuOptionListener(null)
        unregisterReceiver(mNetworkBroadcastReceiver)
        super.onDestroy()
    }

    override fun onBackPressed() {
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

        //当网页可
        if (mPageView.canGoBack()) {
            mPageView.goBack()
            return
        }

        if (mPageView.isVisible()) {
            mPageView.gone()
            mPageView.stopLoading()
            mPageView.onPause()
            mPinsRecycler?.visible()
            mMenuOptionWidget.hideMoreMenu()
            mInputBox.setText("")
            return
        }

        finishAndRemoveTask()
        super.onBackPressed()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

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
                val bitmap = toBitmap(getContentWidth().toFloat(), contentHeight.toFloat())

                doAsync {
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
}