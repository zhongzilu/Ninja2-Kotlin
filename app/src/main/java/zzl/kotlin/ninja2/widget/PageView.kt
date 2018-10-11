package zzl.kotlin.ninja2.widget

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ContentValues.TAG
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Message
import android.preference.PreferenceManager
import android.util.AttributeSet
import android.view.View
import android.webkit.*
import android.webkit.WebChromeClient.FileChooserParams
import zzl.kotlin.ninja2.R
import zzl.kotlin.ninja2.application.*
import java.io.ByteArrayInputStream


/**
 * 自定义WebView
 * Created by zhongzilu on 2018/8/1 0001.
 */
class PageView : WebView, PageViewClient.Delegate, PageChromeClient.Delegate,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private val TAG = "PageView-->"

    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    interface Delegate {
        fun onReceivedWebThemeColor(str: String)

        fun onProgressChanged(progress: Int)

        fun onFormResubmission(dontResend: Message, resend: Message)

        fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback)

        fun onReceivedClientCertRequest(request: ClientCertRequest)

        fun onReceivedHttpAuthRequest(httpAuthHandler: HttpAuthHandler, str: String, str2: String)

        fun onPermissionRequest(request: PermissionRequest)

        fun onReceivedSslError(sslErrorHandler: SslErrorHandler, sslError: SslError)

        fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback)

        fun onReceivedTitle(url: String, title: String)

        fun onReceivedIcon(url: String, title: String, icon: Bitmap?)

        fun onPageStarted(url: String, title: String, icon: Bitmap?)

        fun onPageFinished(url: String, title: String, icon: Bitmap?)

        fun onDownloadStart(url: String, userAgent: String, contentDisposition: String, mimetype: String, contentLength: Long)

        fun onReceivedTouchIconUrl(url: String, precomposed: Boolean)

        fun onShowFileChooser(filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: FileChooserParams): Boolean

        fun onJsAlert(url: String, message: String, result: JsResult): Boolean

        fun onJsPrompt(url: String, message: String, defaultValue: String, result: JsPromptResult): Boolean

        fun onCreateWindow(isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean

        fun onPermissionRequestCanceled(request: PermissionRequest)

        fun onWebViewLongPress(url: String)

        fun onJsConfirm(url: String, message: String, result: JsResult): Boolean

        fun onJsBeforeUnload(url: String, message: String, result: JsResult): Boolean

        fun onCloseWindow()

        fun onHideCustomView()

        fun onGeolocationPermissionsHidePrompt()
    }

    private var userAgent: String? = null
    private var mPrivateFlag: Boolean = false
    private var isAdBlock: Boolean = false

    init {
        initWebView()
        initWebSetting()
        settingMultipleWindow()
        setWebContentsDebuggingEnabled(true)
        PreferenceManager.getDefaultSharedPreferences(context)
                .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onResume() {
        super.resumeTimers()
        super.onResume()
    }

    override fun onPause() {
        super.pauseTimers()
        super.onPause()
    }

    override fun destroy() {
        stopLoading()
        onPause()
        clearHistory()
        removeAllViews()
        destroyDrawingCache()
        super.destroy()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        PreferenceManager.getDefaultSharedPreferences(context).unregisterOnSharedPreferenceChangeListener(this)
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

        // AppRTC requires third party cookies to work
        CookieManager.getInstance().acceptThirdPartyCookies(this)

        isAdBlock = SP.adBlock
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebSetting() = with(settings) {
        allowContentAccess = true
        //支持引用文件
        allowFileAccess = true
        allowFileAccessFromFileURLs = true
        allowUniversalAccessFromFileURLs = true
        //设置支持本地存储
        setAppCacheEnabled(true)
        setAppCachePath(context.cacheDir.toString())
        cacheMode = WebSettings.LOAD_DEFAULT
        databaseEnabled = true
        domStorageEnabled = true
        saveFormData = true
        setSupportZoom(true)
        useWideViewPort = true
        builtInZoomControls = true
        displayZoomControls = false
        //禁止系统缩放字体大小
        textZoom = 100

        javaScriptEnabled = true
        javaScriptCanOpenWindowsAutomatically = SP.isEnableMultipleWindows
        //设置支持多窗口
        setSupportMultipleWindows(SP.isEnableMultipleWindows)

        blockNetworkImage = false
        blockNetworkLoads = false
        setGeolocationEnabled(true)
        defaultTextEncodingName = WebUtil.URL_ENCODE
        layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
        //缩放至屏幕的大小
        loadWithOverviewMode = true
        loadsImagesAutomatically = true
        //支持混合模式
        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        userAgent = userAgentString
        //不需要请求控制直接播放媒体文件即可以自动播放音乐
        mediaPlaybackRequiresUserGesture = false
        //支持插件
//        pluginState = WebSettings.PluginState.ON
    }

    private fun settingMultipleWindow() {
        val enableMultipleWindows = SP.isEnableMultipleWindows
        settings.javaScriptCanOpenWindowsAutomatically = enableMultipleWindows
        settings.setSupportMultipleWindows(enableMultipleWindows)
    }

    override fun loadUrl(url: String) {
        if (!shouldOverrideUrlLoading(url)) {
            super.loadUrl(url.parseUrl())
        }
    }

    /**
     * 设置浏览模式
     * @param isPrivate true：无痕浏览模式，false：普通浏览模式，默认为false
     */
    fun setupViewMode(isPrivate: Boolean) {
        mPrivateFlag = isPrivate

        if (mPrivateFlag) createNoTracePage()

        with(settings) {
            setAppCacheEnabled(!mPrivateFlag)
            cacheMode = if (mPrivateFlag) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
            databaseEnabled = !mPrivateFlag
            //是否开启DOM Storage
            domStorageEnabled = !mPrivateFlag
            saveFormData = !mPrivateFlag
            setGeolocationEnabled(!mPrivateFlag)
        }
    }

    private fun createNoTracePage() {
        loadUrl(Protocol.ABOUT_BLANK)
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        _scrollListener?.invoke(l - oldl, t - oldt)
    }

    /**
     * 滚动监听器
     */
    private var _scrollListener: ((dx: Int, dy: Int) -> Unit)? =     null

    /**
     * 设置滚动监听器
     */
    fun setScrollChangeListener(todo: ((dx: Int, dy: Int) -> Unit)?) {
        _scrollListener = todo
    }

    /**
     * 设置UserAgent
     */
    fun setUserAgent(type: Int) {
        when (type) {
            Type.UA_DESKTOP -> settings.userAgentString = WebUtil.UA_DESKTOP
            Type.UA_CUSTOM -> settings.userAgentString = SP.UA
            else -> settings.userAgentString = userAgent
        }
        reload()
    }

    /**
     * 计算网页内容的宽度
     */
    fun getContentWidth() = computeHorizontalScrollRange()

    /**
     * 计算网页内容的高度
     */
    override fun getContentHeight() = computeVerticalScrollRange()

    /**
     *
     */
    private var _delegate: Delegate? = null

    fun setPageViewDelegate(delegate: Delegate) {
        _delegate = delegate
    }

    private object mDownloadListener : DownloadListener {
        override fun onDownloadStart(url: String?, userAgent: String?, contentDisposition: String?, mimetype: String?, contentLength: Long) {
            L.e(TAG, "onDownloadStart")
        }

    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        L.e(TAG, "onSharedPreferenceChanged")
        when (key) {
            resources.getString(R.string.preference_key_adblock) -> {
                //set ad block
                isAdBlock = SP.adBlock
            }
            resources.getString(R.string.preference_key_enable_multiple_windows) -> {
                settings.setSupportMultipleWindows(SP.isEnableMultipleWindows)
            }
        }
    }

    override fun onFormResubmission(dontResend: Message, resend: Message) {
        L.e(TAG, "onFormResubmission")
        _delegate?.onFormResubmission(dontResend, resend)
    }

    override fun onReceivedClientCertRequest(request: ClientCertRequest) {
        L.e(TAG, "onReceivedClientCertRequest")
        _delegate?.onReceivedClientCertRequest(request)
    }

    override fun onReceivedHttpAuthRequest(handler: HttpAuthHandler, host: String, realm: String) {
        L.e(TAG, "onReceivedHttpAuthRequest")
        _delegate?.onReceivedHttpAuthRequest(handler, host, realm)
    }

    override fun onReceivedSslError(handler: SslErrorHandler, error: SslError) {
        L.e(TAG, "onReceivedSslError: $error")
        _delegate?.onReceivedSslError(handler, error)
    }

    override fun onPageStarted(url: String, title: String, icon: Bitmap?) {
        L.e(TAG, "onPageStarted $url : $title")
        _delegate?.onPageStarted(url, title, icon)
    }

    override fun onPageFinished(url: String, title: String, icon: Bitmap?) {
        L.e(TAG, "onPageFinished $url : $title")
        _delegate?.onPageFinished(url, title, icon)
    }

    override fun shouldOverrideUrlLoading(url: String): Boolean {
        L.e(TAG, "shouldOverrideUrlLoading $url")

        try {
            return when {
                url.isIntent() -> {
                    context.openIntentByDefault(url)
                    true
                }
                url.isEmailTo() -> {
                    context.sendMailTo(url)
                    true
                }
                url.isProtocolUrl() -> false
                else -> {
//                    context.openIntentByDefault(url)
                    false
                }
            }


        } catch (e: ActivityNotFoundException) {

        }

        return false
    }

    override fun doUpdateVisitedHistory(url: String, isReload: Boolean) {
        L.e(TAG, "doUpdateVisitedHistory $url isReload $isReload")
        if (mPrivateFlag)
            clearHistory()
        else if (!isReload) {
            SQLHelper.saveOrUpdateRecord(title, url)
        }
    }

    /**
     * 拦截网页加载资源的请求，验证是否为广告链接，如果为广告链接则返会空WebResourceResponse对象，
     * 如果不是广告链接，则不拦截，返回null，系统将正常加载资源
     */
    override fun shouldInterceptRequest(url: String): WebResourceResponse? {
        if (isAdBlock && AdBlock.isAd(url)) {
            return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(byteArrayOf()))
        }

        return null
    }

    override fun onCloseWindow() {
        L.e(TAG, "onCloseWindow")
        _delegate?.onCloseWindow()
    }

    override fun onProgressChanged(progress: Int) {
        L.e(TAG, "onProgressChanged $progress")
        _delegate?.onProgressChanged(progress)
    }

    override fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
        L.e(TAG, "onShowCustomView")
        _delegate?.onShowCustomView(view, callback)
    }

    override fun onPermissionRequest(request: PermissionRequest) {
        L.e(TAG, "onPermissionRequest")
        _delegate?.onPermissionRequest(request)
    }

    override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
        L.e(TAG, "onGeolocationPermissionsShowPrompt")
        _delegate?.onGeolocationPermissionsShowPrompt(origin, callback)
    }

    override fun onReceivedTitle(url: String, title: String) {
        L.e(TAG, "onReceivedTitle $title")
        _delegate?.onReceivedTitle(url, title)
    }

    override fun onReceivedIcon(url: String, title: String, icon: Bitmap?) {
        L.e(TAG, "onReceivedIcon ${icon == null}")
        _delegate?.onReceivedIcon(url, title, icon)
    }

    override fun onReceivedTouchIconUrl(url: String, precomposed: Boolean) {
        L.e(TAG, "onReceivedTouchIconUrl $url : precomposed $precomposed")
        _delegate?.onReceivedTouchIconUrl(url, precomposed)
    }

    override fun onShowFileChooser(filePathCallback: ValueCallback<Array<Uri>>,
                                   fileChooserParams: WebChromeClient.FileChooserParams): Boolean {
        L.e(TAG, "onShowFileChooser")
        return _delegate?.onShowFileChooser(filePathCallback, fileChooserParams) ?: false
    }

    override fun onJsAlert(url: String, message: String, result: JsResult): Boolean {
        L.e(TAG, "onJsAlert: $url")
        return _delegate?.onJsAlert(url, message, result) ?: false
    }

    override fun onJsPrompt(url: String, message: String, defaultValue: String, result: JsPromptResult): Boolean {
        L.e(TAG, "onJsPrompt: $url")
        return _delegate?.onJsPrompt(url, message, defaultValue, result) ?: false
    }

    override fun onCreateWindow(isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
        L.e(TAG, "onCreateWindow")
        return _delegate?.onCreateWindow(isDialog, isUserGesture, resultMsg) ?: false
    }

    override fun onHideCustomView() {
        L.e(TAG, "onHideCustomView")
        _delegate?.onHideCustomView()
    }

    override fun onPermissionRequestCanceled(request: PermissionRequest) {
        L.e(TAG, "onPermissionRequestCanceled")
        _delegate?.onPermissionRequestCanceled(request)
    }

    override fun onJsConfirm(url: String, message: String, result: JsResult): Boolean {
        L.e(TAG, "onJsConfirm: $url")
        return _delegate?.onJsConfirm(url, message, result) ?: false
    }

    override fun onGeolocationPermissionsHidePrompt() {
        L.e(TAG, "onGeolocationPermissionsHidePrompt")
        _delegate?.onGeolocationPermissionsHidePrompt()
    }

    override fun onJsBeforeUnload(url: String, message: String, result: JsResult): Boolean {
        L.e(TAG, "onJsBeforeUnload")
        return _delegate?.onJsBeforeUnload(url, message, result) ?: false
    }
}

/******************************
 *
 * 自定义的WebViewClient
 * Created by zhongzilu on 2018/8/1
 *
 * detail @see <a href="https://blog.csdn.net/languobeibei/article/details/53929403">点击这里</a>
 *
 ******************************/
class PageViewClient(val context: Context, val delegate: Delegate?) : WebViewClient() {

    interface Delegate {
        fun onFormResubmission(dontResend: Message, resend: Message)

        fun onReceivedClientCertRequest(request: ClientCertRequest)

        fun onReceivedHttpAuthRequest(handler: HttpAuthHandler, host: String, realm: String)

        fun onReceivedSslError(handler: SslErrorHandler, error: SslError)

        fun onPageStarted(url: String, title: String, icon: Bitmap?)

        fun onPageFinished(url: String, title: String, icon: Bitmap?)

        fun shouldOverrideUrlLoading(url: String): Boolean = false

        fun doUpdateVisitedHistory(url: String, isReload: Boolean)

        fun onReceivedError(url: String, code: Int, desc: String) {}

        fun shouldInterceptRequest(url: String): WebResourceResponse?
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
        return delegate?.shouldOverrideUrlLoading(url)
                ?: super.shouldOverrideUrlLoading(view, url)
    }

    /**
     * @targetSdk >= 24
     */
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        return delegate?.shouldOverrideUrlLoading(request.url.toString())
                ?: super.shouldOverrideUrlLoading(view, request)
    }

    /**
     * 开始加载网页页面时
     */
    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        delegate?.onPageStarted(url, view.title, favicon)
                ?: super.onPageStarted(view, url, favicon)
    }

    /**
     * 页面加载完成回调方法，获取对应url地址
     */
    override fun onPageFinished(view: WebView, url: String) {
        delegate?.onPageFinished(url, view.title, view.favicon)
                ?: super.onPageFinished(view, url)
    }

    /**
     * 正在加载HTTP响应的body内容，页面加载完成可见。
     * 回调该方法，获取对应url地址
     */
    override fun onPageCommitVisible(view: WebView, url: String) {
        delegate?.onPageFinished(url, view.title, view.favicon)
                ?: super.onPageCommitVisible(view, url)
    }

    /**
     * 当前应用程序的WebView接收一个HTTP认证请求，使用提供的HttpAuthHandler对象设置WebView对HTTP认证请求的响应，
     * 默认WebView取消HTTP认证，即handler.cancel()
     */
    override fun onReceivedHttpAuthRequest(view: WebView, handler: HttpAuthHandler, host: String, realm: String) {
        delegate?.onReceivedHttpAuthRequest(handler, host, realm)
                ?: super.onReceivedHttpAuthRequest(view, handler, host, realm)
    }

    /**
     * 由于页面需要使用POST方式获取结果，如果浏览器需要重发，
     * 回调onFormResubmission()方法，默认浏览器不重发请求数据。
     */
    override fun onFormResubmission(view: WebView, dontResend: Message, resend: Message) {
        delegate?.onFormResubmission(dontResend, resend)
                ?: super.onFormResubmission(view, dontResend, resend)
    }

    /**
     * 回调该方法，处理SSL认证请求
     */
    override fun onReceivedClientCertRequest(view: WebView, request: ClientCertRequest) {
        delegate?.onReceivedClientCertRequest(request)
                ?: super.onReceivedClientCertRequest(view, request)
    }

    /**
     * 回调该方法，请求已授权用户自动登录
     */
    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        delegate?.onReceivedSslError(handler, error)
                ?: super.onReceivedSslError(view, handler, error)
    }

    /**
     * 更新浏览历史记录，获取更新的url地址
     * 当前WebView加载的url被重新加载，isReload为true，这一步可以判读是否执行刷新操作
     */
    override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
        delegate?.doUpdateVisitedHistory(url, isReload)
                ?: super.doUpdateVisitedHistory(view, url, isReload)
    }

    /**
     * 网络不好或服务器请求超时，请求不到数据，加载出错时，让WebView加载指定页面
     * 自己定义加载错误处理效果，比如：定义在没有网络时候，显示一张无网络的图片
     * @targetSdk >= 23
     */
//    @RequiresApi(Build.VERSION_CODES.M)
//    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
//        delegate?.onReceivedError(view.url, error.errorCode, error.description.toString())
//                ?: super.onReceivedError(view, request, error)
//    }

    /**
     * 报告Web资源加载错误
     * @targetSdk < 23
     */
//    override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
//        delegate?.onReceivedError(failingUrl, errorCode, description)
//                ?: super.onReceivedError(view, errorCode, description, failingUrl)
//    }

    /**
     * 提醒主机应用程序，请求已授权用户自动登录
     */
//    override fun onReceivedLoginRequest(view: WebView, realm: String, account: String, args: String) {
//        super.onReceivedLoginRequest(view, realm, account, args)
//    }

    /**
     * 通知主应用程序加载URL已被安全浏览标记。
     */
//    override fun onSafeBrowsingHit(view: WebView, request: WebResourceRequest, threatType: Int, callback: SafeBrowsingResponse) {
//        super.onSafeBrowsingHit(view, request, threatType, callback)
//    }

    /**
     * 加载指定url资源，比如判读是否为.apk文件，然后启动系统的文件下载管理器，下载指定的文件
     */
//    override fun onLoadResource(view: WebView, url: String) {
//        super.onLoadResource(view, url)
//    }

    /**
     * 应用程序在加载资源时从服务器收到HTTP错误
     */
    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
        delegate?.onReceivedError(view.url, errorResponse.statusCode, errorResponse.reasonPhrase)
                ?: super.onReceivedHttpError(view, request, errorResponse)
    }

    /**
     * 实现对webview的资源请求进行处理，这里可以用来屏蔽网页上的广告资源加载，但仍可能会有广告存在
     *
     * 广告屏蔽相关博客：
     * @see http://wangbaiyuan.cn/realization-of-android-webview-advertising-filtering.html
     * @see https://github.com/adblockplus/libadblockplus-android
     * @see https://github.com/adblockplus/adblockplussbrowser
     * @targetSdk >= 21
     */
    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        return delegate?.shouldInterceptRequest(request.url.toString()) ?: super.shouldInterceptRequest(view, request)
    }

    /**
     * 实现对webview的资源请求进行处理
     * @targetSdk > 11 && < 21
     */
    override fun shouldInterceptRequest(view: WebView?, url: String): WebResourceResponse? {
        return delegate?.shouldInterceptRequest(url) ?: super.shouldInterceptRequest(view, url)
    }
}

/******************************
 *
 * 自定义的WebChromeClient
 * Created by zhongzilu on 2018/8/1
 *
 * detail @see <a href="https://blog.csdn.net/zhanwubus/article/details/80340025">戳这里</a><br/>
 * <a href="https://www.cnblogs.com/baiqiantao/p/7390276.html">还有这里</a>
 *
 ******************************/
class PageChromeClient(val delegate: Delegate) : WebChromeClient() {

    interface Delegate {
        fun onProgressChanged(progress: Int)

        fun onReceivedTitle(url: String, title: String) {}

        fun onReceivedIcon(url: String, title: String, icon: Bitmap?)

        fun onReceivedTouchIconUrl(url: String, precomposed: Boolean)

        fun onShowFileChooser(filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: WebChromeClient.FileChooserParams): Boolean

        fun onJsAlert(url: String, message: String, result: JsResult): Boolean

        fun onJsPrompt(url: String, message: String, defaultValue: String, result: JsPromptResult): Boolean

        fun onJsConfirm(url: String, message: String, result: JsResult): Boolean

        fun onJsBeforeUnload(url: String, message: String, result: JsResult): Boolean

        fun onCreateWindow(isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean

        fun onCloseWindow()

        fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback)

        fun onHideCustomView()

        fun onPermissionRequest(request: PermissionRequest)

        fun onPermissionRequestCanceled(request: PermissionRequest)

        fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback)

        fun onGeolocationPermissionsHidePrompt()
    }

    /**
     * 获取网页Video的默认显示图
     */
    override fun getDefaultVideoPoster(): Bitmap? {
        return super.getDefaultVideoPoster()
    }

    /**
     * 获取访问的历史纪录列表
     */
    override fun getVisitedHistory(callback: ValueCallback<Array<String>>?) {
        super.getVisitedHistory(callback)
    }

    /**
     * 告诉主应用程序加载页面的当前进度
     */
    override fun onProgressChanged(view: WebView, newProgress: Int) {
        delegate.onProgressChanged(newProgress)
    }

    /**
     * 通知主应用程序文档标题的更改
     */
    override fun onReceivedTitle(view: WebView, title: String) {
        delegate.onReceivedTitle(view.url, title)
    }

    /**
     * 通知主应用程序当前页面的新图标
     */
    override fun onReceivedIcon(view: WebView, icon: Bitmap?) {
        delegate.onReceivedIcon(view.url, view.title, icon)
    }

    /**
     * 通知主应用程序当前页面的苹果图标
     * 苹果为iOS设备配备了apple-touch-icon私有属性，添加该属性，
     * 在iPhone,iPad,iTouch的safari浏览器上可以使用添加到主屏按钮将网站添加到主屏幕上，
     * 方便用户以后访问。apple-touch-icon 标签支持sizes属性，可以用来放置对应不同的设备
     */
    override fun onReceivedTouchIconUrl(view: WebView, url: String, precomposed: Boolean) {
        delegate.onReceivedTouchIconUrl(url, precomposed)
    }

    /**
     * 请求主应用程序创建一个新窗口
     */
    override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
        return delegate.onCreateWindow(isDialog, isUserGesture, resultMsg)
    }

    /**
     * 通知主应用程序关闭给定的WebView并在必要时将其从视图系统中删除。
     */
    override fun onCloseWindow(window: WebView) {
        delegate.onCloseWindow()
    }

    /**
     * 通知主应用程序当前页面已进入全屏模式
     * @targetSdk >= 23
     */
    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        delegate.onShowCustomView(view, callback)
    }

    /**
     * 通知主应用程序当前页面已进入全屏模式
     * @targetSdk < 23
     */
    override fun onShowCustomView(view: View, requestedOrientation: Int, callback: CustomViewCallback) {
        delegate.onShowCustomView(view, callback)
    }

    /**
     * 通知主应用程序当前页面已退出全屏模式
     */
    override fun onHideCustomView() {
        delegate.onHideCustomView()
    }

    /**
     * 告诉客户端显示一个对话框，以确认离开当前页面的导航
     */
    override fun onJsBeforeUnload(view: WebView, url: String, message: String, result: JsResult): Boolean {
        return delegate.onJsBeforeUnload(url, message, result)
    }

    /**
     * 告诉客户端显示一个JavaScript警告对话框
     */
    override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
        return delegate.onJsAlert(url, message, result)
    }

    /**
     * 告诉客户端向用户显示一个确认对话框
     */
    override fun onJsConfirm(view: WebView, url: String, message: String, result: JsResult): Boolean {
        return delegate.onJsConfirm(url, message, result)
    }

    /**
     * 告诉客户端向用户显示一个提示对话框
     */
    override fun onJsPrompt(view: WebView, url: String, message: String, defaultValue: String, result: JsPromptResult): Boolean {
        return delegate.onJsPrompt(url, message, defaultValue, result)
    }

    /**
     * 通知主应用程序指定来源的Web内容正在尝试使用Geolocation API，但目前没有为该来源设置权限状态
     */
    override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
        delegate.onGeolocationPermissionsShowPrompt(origin, callback)
    }

    /**
     * 通知主应用程序，用先前对{@link #onGeolocationPermissionsShowPrompt}的调用进行的地理位置权限请求已被取消
     */
    override fun onGeolocationPermissionsHidePrompt() {
        delegate.onGeolocationPermissionsHidePrompt()
    }

    /**
     * 通知主应用程序Web内容正在请求访问指定资源的权限，当前权限未授予或拒绝该权限
     */
    override fun onPermissionRequest(request: PermissionRequest) {
        delegate.onPermissionRequest(request)
    }

    /**
     * 通知主应用程序已经取消了给定的权限请求
     */
    override fun onPermissionRequestCanceled(request: PermissionRequest) {
        delegate.onPermissionRequestCanceled(request)
    }

    /**
     * 告诉客户显示文件选择器
     */
    override fun onShowFileChooser(webView: WebView, filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: FileChooserParams): Boolean {
        return delegate.onShowFileChooser(filePathCallback, fileChooserParams)
    }

}