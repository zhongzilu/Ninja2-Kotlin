package zzl.kotlin.ninja2

import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.os.Message
import android.support.design.widget.BottomSheetBehavior
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import kotlinx.android.synthetic.main.activity_page.*
import kotlinx.android.synthetic.main.content_bottom_sheet.*
import kotlinx.android.synthetic.main.content_main.*
import zzl.kotlin.ninja2.application.*
import zzl.kotlin.ninja2.widget.MenuOptionListener
import zzl.kotlin.ninja2.widget.PageView


class PageActivity : AppCompatActivity(), PageView.Delegate {

    val TAG = "PageActivity-->"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page)
        setSupportActionBar(toolbar)

        initEvent()
        initInputBox()
    }

    /**
     * 初始化Pin的RecyclerView，由于采用了ViewStub来延迟加载
     * 因此尽量需要的时候才调用
     */
    private var mPinsRecycler: RecyclerView? = null

    private fun initPinRecycler() {
        if (mPinsRecycler == null) {
            mPinsRecycler = mPinsStub.inflate() as RecyclerView
        }


    }

    /**
     * 初始化Record的RecyclerView，由于采用了ViewStub来延迟加载
     * 因此尽量需要的时候才调用
     */
    private var mRecordRecycler: RecyclerView? = null

    private fun initRecordRecycler() {
        if (mRecordRecycler == null) {
            mRecordRecycler = mRecordsStub.inflate() as RecyclerView
        }

    }

    private lateinit var mBottomSheetBehavior: BottomSheetBehavior<*>
    private fun initEvent() {
        // Bottom Sheet content layout views
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout)

        // Capturing the callbacks for bottom sheet
        mBottomSheetBehavior.setBottomSheetCallback(mBottomSheetCallback)

        mMenuOptionWidget.setMenuOptionListener(mMenuOptionListener)

        webView.setPageViewDelegate(this)
    }

    /**
     * 初始化地址输入框
     */
    private fun initInputBox() {
        mInputBox.addTextChangedListener(mTextWatcher)
        mInputBox.setOnEditorActionListener { _, actionId, _ ->
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
    }

    private fun loadPage(url: String) {
        mRecordRecycler?.gone()
        webView.loadUrl(url)
        mInputBox.hideKeyboard()
    }

    /**
     * 地址栏输入框文字变化监听
     */
    private val mTextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }
    }

    /**
     * 底部菜单的展开/关闭状态监听器
     * 当拖拽底部菜单时，需要动态改变灰色背景{@link #mMaskView}的透明度
     */
    private val mBottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {

        override fun onStateChanged(bottomSheet: View, newState: Int) {

            //展开状态
            if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                afterBottomSheetExpanded()
            }

            //关闭状态
            else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                afterBottomSheetCollapsed()
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

    override fun onReceivedWebThemeColor(str: String) {
    }

    override fun onFormResubmission(dontResend: Message, resend: Message) {
    }


    override fun onReceivedClientCertRequest(request: ClientCertRequest) {
    }

    override fun onReceivedHttpAuthRequest(handler: HttpAuthHandler, host: String, realm: String) {
    }

    override fun onReceivedSslError(handler: SslErrorHandler, error: SslError) {
    }

    override fun onPageStarted(url: String, title: String, icon: Bitmap?) {
        mProgress.visible()
        webView.visible()
        mInputBox.setText(url)
        mStopMenu.isVisible = true
        mRefreshMenu.isVisible = false
    }

    override fun onPageFinished(url: String, title: String, icon: Bitmap?) {
        mProgress.gone()
        mStopMenu.isVisible = false
        mRefreshMenu.isVisible = true
    }

    override fun shouldOverrideUrlLoading(url: String): Boolean {
        return false
    }

    override fun doUpdateVisitedHistory(url: String, isReload: Boolean) {

    }

    override fun onCloseWindow() {
        Log.e(TAG, "onCloseWindow")
    }

    override fun onProgressChanged(progress: Int) {
        Log.e(TAG, "onProgressChanged $progress")
        mProgress.progress = progress

        if (progress >= 99){
            mProgress.gone()
        }
    }

    override fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
        Log.e(TAG, "onShowCustomView")
    }

    override fun onPermissionRequest(request: PermissionRequest) {
        Log.e(TAG, "onPermissionRequest")
    }

    override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
        Log.e(TAG, "onGeolocationPermissionsShowPrompt")
    }

    override fun onReceivedTitle(url: String, title: String) {
        Log.e(TAG, "onReceivedTitle $title")
    }

    override fun onReceivedIcon(url: String, title: String, icon: Bitmap?) {
        Log.e(TAG, "onReceivedIcon ${icon == null}")
    }

    override fun onReceivedTouchIconUrl(url: String, precomposed: Boolean) {
        Log.e(TAG, "onReceivedTouchIconUrl $url : precomposed $precomposed")
    }

    override fun onShowFileChooser(filePathCallback: ValueCallback<Array<Uri>>,
                                   fileChooserParams: WebChromeClient.FileChooserParams): Boolean {
        Log.e(TAG, "onShowFileChooser")
        return false
    }

    override fun onJsAlert(url: String, message: String, result: JsResult): Boolean {
        Log.e(TAG, "onJsAlert")
        return false
    }

    override fun onJsPrompt(url: String, message: String, defaultValue: String, result: JsPromptResult): Boolean {
        Log.e(TAG, "onJsPrompt")
        return false
    }

    override fun onCreateWindow(isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
        Log.e(TAG, "onCreateWindow")
        return false
    }

    override fun onHideCustomView() {
        Log.e(TAG, "onHideCustomView")
    }

    override fun onPermissionRequestCanceled(request: PermissionRequest) {
        Log.e(TAG, "onPermissionRequestCanceled")
    }

    override fun onJsConfirm(url: String, message: String, result: JsResult): Boolean {
        Log.e(TAG, "onJsConfirm")
        return false
    }

    override fun onGeolocationPermissionsHidePrompt() {
        Log.e(TAG, "onGeolocationPermissionsHidePrompt")
    }

    override fun onJsBeforeUnload(url: String, message: String, result: JsResult): Boolean {
        Log.e(TAG, "onJsBeforeUnload")
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

        mCollapsedRunnable?.let {
            it.run()
            mCollapsedRunnable = null
        }
    }

    /**
     * 关闭底部菜单后执行Runnable任务
     */
    private var mCollapsedRunnable: Runnable? = null

    private fun closeBottomSheet(runnable: Runnable? = null) {
        this.mCollapsedRunnable = runnable
        mBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    /**
     * 底部菜单栏各选项的事件监听回调
     */
    private val mMenuOptionListener = object : MenuOptionListener {
        override fun onDesktopCheckedChanged(check: Boolean) {
        }

        override fun onCustomUACheckedChanged(check: Boolean) {
        }

        override fun onScreenshotsClick() {
        }

        override fun onShareUrlClick() {
        }

        override fun onNewTabClick() {
        }

        override fun onPrivateTabClick() {
        }

        override fun onPinToHome() {
        }

        override fun addToLauncher() {
        }

        override fun onSettingsClick() {
            go<SettingsActivity>()
        }
    }

    private lateinit var mRefreshMenu: MenuItem
    private lateinit var mStopMenu: MenuItem
    private lateinit var mConfirmMenu: MenuItem
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
            R.id.refresh -> webView.reload()
            R.id.stop -> webView.stopLoading()
            R.id.confirm -> {
            }
        }
        return true
    }

    override fun onBackPressed() {
        if (webView.canGoBack()){
            webView.goBack()
            return
        }
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
        mLoadingView?.let {
            if (it.visibility == View.VISIBLE) {
                it.hide()
            }
        }
    }
}