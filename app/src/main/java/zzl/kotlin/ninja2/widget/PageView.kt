package zzl.kotlin.ninja2.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.webkit.*
import zzl.kotlin.ninja2.application.SP
import zzl.kotlin.ninja2.application.WebUtil


/**
 * Created by zhongzilu on 2018/8/1 0001.
 */
class PageView : WebView, PageViewClient.Delegate, PageChromeClient.Delegate {

    companion object {
        val TAG = "PageView-->"
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    interface Delegate : PageChromeClient.Delegate, PageViewClient.Delegate {
        fun onReceivedWebThemeColor(str: String)
    }

    private var userAgent: String? = null
    private var mPrivateFlag: Boolean = false
    private lateinit var set: WebSettings

    init {
        initWebView()
        initWebSetting()
    }

    override fun onResume() {
        super.onResume()
        super.resumeTimers()
    }

    override fun onPause() {
        super.onPause()
        super.pauseTimers()
    }

    override fun destroy() {
        stopLoading()
        super.destroy()
    }

    private fun initWebView() {
        isDrawingCacheEnabled = true
//        drawingCacheBackgroundColor = ContextCompat.getColor(context, R.color.white)
        drawingCacheQuality = DRAWING_CACHE_QUALITY_HIGH
        setWillNotCacheDrawing(false)
        setWebContentsDebuggingEnabled(true)
        isSaveEnabled = true
        isFocusable = true
        isFocusableInTouchMode = true
        isScrollbarFadingEnabled = true
        overScrollMode = OVER_SCROLL_ALWAYS

        webViewClient = PageViewClient(context, this)
        webChromeClient = PageChromeClient(this)
        setDownloadListener(mDownloadListener)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebSetting() {
        set = settings
        set.allowContentAccess = true
        set.allowFileAccess = true
        set.allowFileAccessFromFileURLs = true
        set.allowUniversalAccessFromFileURLs = true
        set.setAppCacheEnabled(true)
        set.setAppCachePath(context.cacheDir.toString())
        set.cacheMode = WebSettings.LOAD_DEFAULT
        set.databaseEnabled = true
        set.domStorageEnabled = true
        set.saveFormData = true
        set.setSupportZoom(true)
        set.useWideViewPort = true
        set.builtInZoomControls = true
        set.displayZoomControls = false
        set.textZoom = 100
        set.useWideViewPort = true

        set.javaScriptEnabled = true
        set.javaScriptCanOpenWindowsAutomatically = SP.isEnableMultipleWindows
        set.setSupportMultipleWindows(SP.isEnableMultipleWindows)

        set.mediaPlaybackRequiresUserGesture = true
        set.blockNetworkImage = false
        set.blockNetworkLoads = false
        set.setGeolocationEnabled(true)
        set.defaultTextEncodingName = WebUtil.URL_ENCODE
        set.layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
        set.loadWithOverviewMode = true
        set.loadsImagesAutomatically = true
        set.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        userAgent = set.userAgentString
        set.userAgentString = userAgent
    }

    /**
     * 设置浏览模式
     * @param isPrivate true：无痕浏览模式，false：普通浏览模式，默认为false
     */
    fun setupViewMode(isPrivate: Boolean) {
        mPrivateFlag = isPrivate
        set.setAppCacheEnabled(!mPrivateFlag)
        set.cacheMode = if (mPrivateFlag) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
        set.databaseEnabled = !mPrivateFlag
        set.domStorageEnabled = !mPrivateFlag
        set.saveFormData = !mPrivateFlag
        set.setGeolocationEnabled(!mPrivateFlag)
    }

    private var _delegate: Delegate? = null
    fun setPageViewDelegate(delegate: Delegate) {
        _delegate = delegate
    }

    private object mDownloadListener : DownloadListener {
        override fun onDownloadStart(url: String?, userAgent: String?, contentDisposition: String?, mimetype: String?, contentLength: Long) {
            Log.e(TAG, "onDownloadStart")
        }

    }

    override fun onFormResubmission(dontResend: Message, resend: Message) {
        Log.e(TAG, "onFormResubmission")
        _delegate?.onFormResubmission(dontResend, resend)
    }

    override fun onReceivedClientCertRequest(request: ClientCertRequest) {
        Log.e(TAG, "onReceivedClientCertRequest")
        _delegate?.onReceivedClientCertRequest(request)
    }

    override fun onReceivedHttpAuthRequest(handler: HttpAuthHandler, host: String, realm: String) {
        Log.e(TAG, "onReceivedHttpAuthRequest")
        _delegate?.onReceivedHttpAuthRequest(handler, host, realm)
    }

    override fun onReceivedSslError(handler: SslErrorHandler, error: SslError) {
        Log.e(TAG, "onReceivedSslError")
        _delegate?.onReceivedSslError(handler, error)
    }

    override fun onPageStarted(url: String, title: String, icon: Bitmap?) {
        Log.e(TAG, "onPageStarted $url : $title")
        _delegate?.onPageStarted(url, title, icon)
    }

    override fun onPageFinished(url: String, title: String, icon: Bitmap?) {
        Log.e(TAG, "onPageFinished $url : $title")
        _delegate?.onPageFinished(url, title, icon)
    }

    override fun shouldOverrideUrlLoading(url: String): Boolean {
        Log.e(TAG, "shouldOverrideUrlLoading $url")
        return _delegate?.shouldOverrideUrlLoading(url) ?: false
    }

    override fun doUpdateVisitedHistory(url: String, isReload: Boolean) {
        Log.e(TAG, "doUpdateVisitedHistory $url isReload $isReload")
        _delegate?.doUpdateVisitedHistory(url, isReload)
    }

    override fun onCloseWindow() {
        Log.e(PageView.TAG, "onCloseWindow")
        _delegate?.onCloseWindow()
    }

    override fun onProgressChanged(progress: Int) {
        Log.e(PageView.TAG, "onProgressChanged $progress")
        _delegate?.onProgressChanged(progress)
    }

    override fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
        Log.e(PageView.TAG, "onShowCustomView")
        _delegate?.onShowCustomView(view, callback)
    }

    override fun onPermissionRequest(request: PermissionRequest) {
        Log.e(PageView.TAG, "onPermissionRequest")
        _delegate?.onPermissionRequest(request)
    }

    override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
        Log.e(PageView.TAG, "onGeolocationPermissionsShowPrompt")
        _delegate?.onGeolocationPermissionsShowPrompt(origin, callback)
    }

    override fun onReceivedTitle(url: String, title: String) {
        Log.e(PageView.TAG, "onReceivedTitle $title")
        _delegate?.onReceivedTitle(url, title)
    }

    override fun onReceivedIcon(url: String, title: String, icon: Bitmap?) {
        Log.e(PageView.TAG, "onReceivedIcon ${icon == null}")
        _delegate?.onReceivedIcon(url, title, icon)
    }

    override fun onReceivedTouchIconUrl(url: String, precomposed: Boolean) {
        Log.e(PageView.TAG, "onReceivedTouchIconUrl $url : precomposed $precomposed")
        _delegate?.onReceivedTouchIconUrl(url, precomposed)
    }

    override fun onShowFileChooser(filePathCallback: ValueCallback<Array<Uri>>,
                                   fileChooserParams: WebChromeClient.FileChooserParams): Boolean {
        Log.e(PageView.TAG, "onShowFileChooser")
        return _delegate?.onShowFileChooser(filePathCallback, fileChooserParams) ?: false
    }

    override fun onJsAlert(url: String, message: String, result: JsResult): Boolean {
        Log.e(PageView.TAG, "onJsAlert")
        return _delegate?.onJsAlert(url, message, result) ?: false
    }

    override fun onJsPrompt(url: String, message: String, defaultValue: String, result: JsPromptResult): Boolean {
        Log.e(PageView.TAG, "onJsPrompt")
        return _delegate?.onJsPrompt(url, message, defaultValue, result) ?: false
    }

    override fun onCreateWindow(isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
        Log.e(PageView.TAG, "onCreateWindow")
        return _delegate?.onCreateWindow(isDialog, isUserGesture, resultMsg) ?: false
    }

    override fun onHideCustomView() {
        Log.e(PageView.TAG, "onHideCustomView")
        _delegate?.onHideCustomView()
    }

    override fun onPermissionRequestCanceled(request: PermissionRequest) {
        Log.e(PageView.TAG, "onPermissionRequestCanceled")
        _delegate?.onPermissionRequestCanceled(request)
    }

    override fun onJsConfirm(url: String, message: String, result: JsResult): Boolean {
        Log.e(PageView.TAG, "onJsConfirm")
        return _delegate?.onJsConfirm(url, message, result) ?: false
    }

    override fun onGeolocationPermissionsHidePrompt() {
        Log.e(PageView.TAG, "onGeolocationPermissionsHidePrompt")
        _delegate?.onGeolocationPermissionsHidePrompt()
    }

    override fun onJsBeforeUnload(url: String, message: String, result: JsResult): Boolean {
        Log.e(PageView.TAG, "onJsBeforeUnload")
        return _delegate?.onJsBeforeUnload(url, message, result) ?: false
    }
}

/******************************
 *
 * 自定义的WebViewClient
 * Created by zhongzilu on 2018/8/1
 *
 * detail see https://blog.csdn.net/languobeibei/article/details/53929403
 *
 ******************************/
class PageViewClient(val context: Context, val delegate: Delegate) : WebViewClient() {

    interface Delegate {
        fun onFormResubmission(dontResend: Message, resend: Message)

        fun onReceivedClientCertRequest(request: ClientCertRequest)

        fun onReceivedHttpAuthRequest(handler: HttpAuthHandler, host: String, realm: String)

        fun onReceivedSslError(handler: SslErrorHandler, error: SslError)

        fun onPageStarted(url: String, title: String, icon: Bitmap?)

        fun onPageFinished(url: String, title: String, icon: Bitmap?)

        fun shouldOverrideUrlLoading(url: String): Boolean = false

        fun doUpdateVisitedHistory(url: String, isReload: Boolean)
    }

    /**
     * WebView加载H5界面，点击按钮发送短信或拨打电话，调不起短信编辑界面或拨打电话界面，
     * 通常都是因为没有重写shouldOverrideUrlLoading，拦截发送短信或拨打电话的url，处理H5界面请求。
     *
     * 例1：<a href="sms:182xxxxxx">一键短信发送</a>
     * 例2：<a href="tel:182xxxxxx">一键拨打电话</a>
     * @targetSdk < 24
     */
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        return delegate.shouldOverrideUrlLoading(url)
    }

    /**
     * @targetSdk >= 24
     */
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        return delegate.shouldOverrideUrlLoading(request.url.toString())
    }

    /**
     * 开始加载网页页面时
     */
    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        delegate.onPageStarted(url, view.title, favicon)
    }

    /**
     * 页面加载完成回调方法，获取对应url地址
     */
    override fun onPageFinished(view: WebView, url: String) {
        delegate.onPageFinished(url, view.title, view.favicon)
    }

    /**
     * 正在加载HTTP响应的body内容，页面加载完成可见。
     * 回调该方法，获取对应url地址
     */
    override fun onPageCommitVisible(view: WebView, url: String) {
        delegate.onPageFinished(url, view.title, view.favicon)
    }

    /**
     * 当前应用程序的WebView接收一个HTTP认证请求，使用提供的HttpAuthHandler对象设置WebView对HTTP认证请求的响应，
     * 默认WebView取消HTTP认证，即handler.cancel()
     */
    override fun onReceivedHttpAuthRequest(view: WebView, handler: HttpAuthHandler, host: String, realm: String) {
        delegate.onReceivedHttpAuthRequest(handler, host, realm)
    }

    /**
     * 由于页面需要使用POST方式获取结果，如果浏览器需要重发，
     * 回调onFormResubmission()方法，默认浏览器不重发请求数据。
     */
    override fun onFormResubmission(view: WebView, dontResend: Message, resend: Message) {
        delegate.onFormResubmission(dontResend, resend)
    }

    /**
     * 回调该方法，处理SSL认证请求
     */
    override fun onReceivedClientCertRequest(view: WebView, request: ClientCertRequest) {
        delegate.onReceivedClientCertRequest(request)
    }

    /**
     * 回调该方法，请求已授权用户自动登录
     */
    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        delegate.onReceivedSslError(handler, error)
    }

    /**
     * 更新浏览历史记录，获取更新的url地址
     * 当前WebView加载的url被重新加载，isReload为true，这一步可以判读是否执行刷新操作
     */
    override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
        delegate.doUpdateVisitedHistory(url, isReload)
    }
}

/******************************
 *
 * 自定义的WebChromeClient
 * Created by zhongzilu on 2018/8/1
 *
 * detail see https://www.cnblogs.com/baiqiantao/p/7390276.html
 *
 ******************************/
class PageChromeClient(val delegate: Delegate) : WebChromeClient() {

    interface Delegate {
        fun onCloseWindow()

        fun onProgressChanged(progress: Int)

        fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback)

        fun onPermissionRequest(request: PermissionRequest)

        fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback)

        fun onReceivedTitle(url: String, title: String) {}

        fun onReceivedIcon(url: String, title: String, icon: Bitmap?)

        fun onReceivedTouchIconUrl(url: String, precomposed: Boolean)

        fun onShowFileChooser(filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: WebChromeClient.FileChooserParams): Boolean

        fun onJsAlert(url: String, message: String, result: JsResult): Boolean

        fun onJsPrompt(url: String, message: String, defaultValue: String, result: JsPromptResult): Boolean

        fun onCreateWindow(isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean

        fun onHideCustomView()

        fun onPermissionRequestCanceled(request: PermissionRequest)

        fun onJsConfirm(url: String, message: String, result: JsResult): Boolean

        fun onGeolocationPermissionsHidePrompt()

        fun onJsBeforeUnload(url: String, message: String, result: JsResult): Boolean
    }

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        delegate.onProgressChanged(newProgress)
    }

    override fun onReceivedTitle(view: WebView, title: String) {
        delegate.onReceivedTitle(view.url, title)
    }

    override fun onReceivedIcon(view: WebView, icon: Bitmap?) {
        delegate.onReceivedIcon(view.url, view.title, icon)
    }

    override fun onReceivedTouchIconUrl(view: WebView, url: String, precomposed: Boolean) {
        delegate.onReceivedTouchIconUrl(url, precomposed)
    }

    override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
        return delegate.onCreateWindow(isDialog, isUserGesture, resultMsg)
    }

    override fun onCloseWindow(window: WebView) {
        delegate.onCloseWindow()
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        delegate.onShowCustomView(view, callback)
    }

    override fun onShowCustomView(view: View, requestedOrientation: Int, callback: CustomViewCallback) {
        delegate.onShowCustomView(view, callback)
    }

    override fun onHideCustomView() {
        delegate.onHideCustomView()
    }

    override fun onJsBeforeUnload(view: WebView, url: String, message: String, result: JsResult): Boolean {
        return delegate.onJsBeforeUnload(url, message, result)
    }

    override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
        return delegate.onJsAlert(url, message, result)
    }

    override fun onJsConfirm(view: WebView, url: String, message: String, result: JsResult): Boolean {
        return delegate.onJsConfirm(url, message, result)
    }

    override fun onJsPrompt(view: WebView, url: String, message: String, defaultValue: String, result: JsPromptResult): Boolean {
        return delegate.onJsPrompt(url, message, defaultValue, result)
    }

    override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
        delegate.onGeolocationPermissionsShowPrompt(origin, callback)
    }

    override fun onGeolocationPermissionsHidePrompt() {
        delegate.onGeolocationPermissionsHidePrompt()
    }

    override fun onPermissionRequest(request: PermissionRequest) {
        delegate.onPermissionRequest(request)
    }

    override fun onPermissionRequestCanceled(request: PermissionRequest) {
        delegate.onPermissionRequestCanceled(request)
    }

    override fun onShowFileChooser(webView: WebView, filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: FileChooserParams): Boolean {
        return delegate.onShowFileChooser(filePathCallback, fileChooserParams)
    }

}