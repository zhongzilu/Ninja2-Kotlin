package zzl.kotlin.ninja2.application

import android.animation.Animator
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.MailTo
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.preference.PreferenceManager
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.content.ContextCompat
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.util.Patterns
import android.util.SparseArray
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import android.webkit.URLUtil
import android.widget.TextView
import com.anthonycr.grant.PermissionsManager
import com.anthonycr.grant.PermissionsResultAction
import org.jetbrains.anko.toast
import zzl.kotlin.ninja2.App
import zzl.kotlin.ninja2.R
import java.io.File
import java.io.FileOutputStream
import java.net.URISyntaxException
import java.net.URL
import java.util.regex.Pattern


/**
 * Created by zhongzilu on 18-7-25.
 */
//===========View相关==============
fun View.visible() {
    visibility = View.VISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

fun View.isVisible() = visibility == View.VISIBLE

fun View.isGone() = visibility == View.GONE

/**
 * 判断BottomSheet的状态是否为关闭，关闭时取决于@{link #peekHeight}的高度，默认为0
 */
fun <T : View> BottomSheetBehavior<T>.isCollapsed() = state == BottomSheetBehavior.STATE_COLLAPSED

/**
 * 设置BottomSheet的状态为关闭
 */
fun <T : View> BottomSheetBehavior<T>.collapsed() {
    state = BottomSheetBehavior.STATE_COLLAPSED
}

/**
 * 判断BottomSheet的状态是否为隐藏
 */
fun <T : View> BottomSheetBehavior<T>.isHidden() = state == BottomSheetBehavior.STATE_HIDDEN

/**
 * 设置BottomSheet的状态为隐藏
 */
fun <T : View> BottomSheetBehavior<T>.hidden() {
    state = BottomSheetBehavior.STATE_HIDDEN
}

/**
 * 判断BottomSheet的状态是否为展开
 */
fun <T : View> BottomSheetBehavior<T>.isExpanded() = state == BottomSheetBehavior.STATE_EXPANDED

/**
 * 设置BottomSheet的状态为展开
 */
fun <T : View> BottomSheetBehavior<T>.expanded() {
    state = BottomSheetBehavior.STATE_EXPANDED
}


fun View.show(animate: Boolean = true) {
    if (animate) {
        alpha = 0f
        visible()
        animate().apply {
            alpha = 1f
            duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
            interpolator = LinearInterpolator()
            setListener(null)
            start()
        }

        return
    }

    alpha = 1f
    visible()
}

fun View.hide(animate: Boolean = false) {
    if (animate) {
        animate().apply {
            alpha = 0f
            duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
            interpolator = LinearInterpolator()
            setListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(p0: Animator?) {}

                override fun onAnimationCancel(p0: Animator?) {}

                override fun onAnimationStart(p0: Animator?) {}

                override fun onAnimationEnd(p0: Animator?) {
                    gone()
                }

            })
            start()
        }

        return
    }

    gone()
}

/**
 * 弹出输入法
 */
fun View.showKeyboard() {
    post {
        requestFocus()
        val manager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        manager.showSoftInput(this, 1)
    }
}

/**
 * 隐藏输入法
 */
fun View.hideKeyboard() {
    post {
        clearFocus()
        val manager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        manager.hideSoftInputFromWindow(windowToken, 0)
    }
}

/**
 * 当View可见的时候执行的操作
 */
inline fun <T : View> T.visibleDo(todo: (T) -> Unit) {
    if (visibility == View.VISIBLE) {
        todo(this)
    }
}

/**
 * 封装通过Id频繁获取View的操作，并作类似ViewHolder机制的缓存
 */
fun <T : View> View.findViewOften(viewId: Int): T {
    val viewHolder: SparseArray<View> = tag as? SparseArray<View> ?: SparseArray()
    tag = viewHolder
    var childView: View? = viewHolder.get(viewId)
    if (null == childView) {
        childView = findViewById(viewId)
        viewHolder.put(viewId, childView)
    }
    return childView as T
}

//==========Context相关==============

fun Context.openIntentByDefault(url: String) {
    val intent = url.parseIntent()
    val component = intent.resolveActivity(packageManager)
    if (component != null) {
        if (component.packageName == packageName) {
            startActivity(intent)
        } else {
            invokeApp(component.packageName) {
                toast(R.string.toast_no_handle_application)
            }
        }
    }
}

inline fun <reified T : Activity> Context.go() {
    startActivity(Intent(this, T::class.java))
}

fun Context.defaultSharePreferences() = PreferenceManager.getDefaultSharedPreferences(this)

/**
 * 获取应用的版本号
 */
fun Context.versionCode() = packageManager
        .getPackageInfo(packageName, PackageManager.GET_CONFIGURATIONS).versionCode

/**
 * 获取应用的版本名称
 */
fun Context.versionName() = packageManager
        .getPackageInfo(packageName, PackageManager.GET_CONFIGURATIONS).versionName

/**
 * 分享文本
 * @param text  分享的文本内容
 */
fun Context.shareText(text: String) {
    val intent = Intent(Intent.ACTION_SEND)
    intent.type = "text/plain"
    intent.putExtra(Intent.EXTRA_TEXT, text)
    startActivity(Intent.createChooser(intent, text))
}

/**
 * 发送邮件
 * @param mail  目标邮件地址
 */
fun Context.sendMailTo(mail: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        val parse = MailTo.parse(if (mail.startsWith(Protocol.MAIL_TO)) mail else Protocol.MAIL_TO + mail)
        putExtra(Intent.EXTRA_EMAIL, arrayOf(parse.to))
        putExtra(Intent.EXTRA_TEXT, parse.body)
        putExtra(Intent.EXTRA_SUBJECT, parse.subject)
        putExtra(Intent.EXTRA_CC, parse.cc)
        type = "message/rfc822"
    }

    supportIntent(intent) { startActivity(it) }
}

/**
 * 调用拨号器界面
 *
 * @param phone 目标电话号码
 */
fun Context.callPhone(phone: String) {
    val intent = Intent(Intent.ACTION_DIAL).apply {
        data = Uri.parse(if (phone.startsWith(Protocol.TEL)) phone else Protocol.TEL + phone)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    supportIntent(intent) { startActivity(it) }
}

/**
 * 将字符串拷贝到剪切板
 */
fun Context.copyToClipboard(text: String) {
    // Gets a handle to the clipboard service.
    val mClipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    // Creates a new text clip to put on the clipboard
    val clip = ClipData.newPlainText(null, text.trim())

    // Set the clipboard's primary clip.
    mClipboard.primaryClip = clip
    toast(resources.getString(R.string.toast_copy_url, text))
}

/**
 * 将dip转换成px
 */
fun Context.dip2px(dip: Float) = (resources.displayMetrics.density * dip + 0.5f).toInt()

/**
 * 将sp转换为px
 */
fun Context.sp2px(sp: Float) = (resources.displayMetrics.scaledDensity * sp + 0.5f).toInt()

//==========Activity相关=============
/**
 * 带返回值的Activity跳转
 *
 * @param clazz
 * @param requestCode
 */
inline fun <reified T : Activity> Activity.go4Result(requestCode: Int) {
    startActivityForResult(Intent(this, T::class.java), requestCode)
}

/**
 * 获取图片
 * @param requestCode   从相册选择图片
 */
fun Activity.pickImage(requestCode: Int) {
    val intent = Intent(Intent.ACTION_GET_CONTENT)
    intent.type = "image/*"
    startActivityForResult(intent, requestCode)
}

/**
 * 动态申请权限
 * @param permissions 权限集合
 * @param f 授权后执行的动作
 */
fun Activity.permission(vararg permissions: String, f: () -> Unit) {
    PermissionsManager.getInstance()
            .requestPermissionsIfNecessaryForResult(this, permissions,
                    object : PermissionsResultAction() {
                        override fun onGranted() {
                            f()
                        }

                        override fun onDenied(permission: String) {
                            toast(R.string.toast_storage_permission_denied)
                        }
                    })
}

//===========String===============
/**
 * 给地址着色，http协议用LightDark, https协议用Green, TextPrimary by Default
 * @param url 访问网址
 * @return 着色过后的Url
 */
fun String.toColorUrl(): SpannableStringBuilder {
    val color: Int
    val endIndex: Int
    when {
        URLUtil.isHttpUrl(this) -> {
            color = ContextCompat.getColor(App.instance, R.color.text_secondary)
            endIndex = 7
        }
        URLUtil.isHttpsUrl(this) -> {
            color = ContextCompat.getColor(App.instance, R.color.green)
            endIndex = 8
        }
        else -> {
            color = ContextCompat.getColor(App.instance, R.color.text_primary)
            endIndex = 0
        }
    }
    val builder = SpannableStringBuilder(this)
    if (endIndex > 0) {
        builder.setSpan(ForegroundColorSpan(color), 0, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    return builder
}

/**
 * 验证字符串是否是Intent
 */
fun String.isIntent(): Boolean {
    if (isEmpty()) return false
    return startsWith(Protocol.INTENT) || startsWith(Protocol.INTENT_OLD)
}

/**
 * 解析网页上的Intent字符串，代码来自：
 * @see https://droidyue.com/blog/2014/11/23/start-android-application-when-click-a-link/
 */
fun String.parseIntent(): Intent {
    var intent = Intent()
    // Parse intent URI into Intent Object
    var flags = 0
    var isIntentUri = false
    if (startsWith(Protocol.INTENT)) {
        isIntentUri = true
        flags = Intent.URI_INTENT_SCHEME
    } else if (startsWith(Protocol.INTENT_OLD)) {
        isIntentUri = true
    }
    if (isIntentUri) {
        try {
            intent = Intent.parseUri(this, flags)
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }

    }
    return intent
}

/**
 * 扩展属性
 */
val String.pattern
    get() = Pattern.compile("(?i)((?:http|https|file|ftp)://|(?:data|about|javascript|mailto):|(?:.*:.*@))(.*)")

/**
 * 验证是否是带有协议头的Url
 */
fun String.isProtocolUrl() = pattern.matcher(this).matches()

/**
 * 验证是否带有协议头{@link #Protocol.MAIL_TO}的Url
 */
fun String.isEmailTo() = isNotEmpty() && startsWith(Protocol.MAIL_TO)

/**
 * 验证是否带有协议头{@link #Protocol.TEL}的url
 */
fun String.isTel() = isNotEmpty() && startsWith(Protocol.TEL)

/**
 * 验证是否是Web路径
 */
fun String.isWebUrl() = Patterns.WEB_URL.matcher(this).matches()

/**
 * 获取Url的主机地址部分
 */
fun String.host(): String {
    return try {
        URL(this).host
    } catch (e: Exception) {
        e.printStackTrace()
        this
    }
}

/**
 * 将字符串转换为可用的Web URL
 */
fun String.parseUrl(): String {
    val trim = trim()
    val matcher = pattern.matcher(trim)
    when {
        matcher.matches() -> {
            val group0 = matcher.group(0)
            System.out.println("group0: $group0")
            val group1 = matcher.group(1)
            System.out.println("group1: $group1")
            val group2 = matcher.group(2)
            System.out.println("group2: $group2")
            return trim.replace(" ", "%20")
        }
        Patterns.WEB_URL.matcher(trim).matches() -> return URLUtil.guessUrl(trim)
        else -> {
            val search = when (SP.searchEngine) {
                Type.SEARCH_GOOGLE -> WebUtil.SEARCH_ENGINE_GOOGLE
                Type.SEARCH_DUCKDUCKGO -> WebUtil.SEARCH_ENGINE_DUCKDUCKGO
                Type.SEARCH_BING -> WebUtil.SEARCH_ENGINE_BING
                Type.SEARCH_BAIDU -> WebUtil.SEARCH_ENGINE_BAIDU
                else -> ""
            }

            return URLUtil.composeSearchUrl(trim, search, "%s")
        }
    }
}

//==========其他==========

fun View.toBitmap(w: Float, h: Float, scroll: Boolean = false): Bitmap {
    //todo[BUG] Genymotion Android 6.0上截图只能显示一屏的内容
    /*该方法在Android 8.0、7.0、5.0上没问题，但在Genymotion Android 6.0上有问题，真机6.0.1的系统没有问题*/
    if (!isDrawingCacheEnabled) isDrawingCacheEnabled = true

    var left = left
    var top = top

    if (scroll) {
        left = scrollX
        top = scrollY
    }

    return Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888).apply {
        eraseColor(Color.WHITE)

        Canvas(this).apply {
            val status = save()
            translate(-left.toFloat(), -top.toFloat())

            val scale = w / width
            scale(scale, scale, left.toFloat(), top.toFloat())

            draw(this)
            restoreToCount(status)

            val alphaPaint = Paint()
            alphaPaint.color = Color.TRANSPARENT

            drawRect(0f, 0f, 1f, h, alphaPaint)
            drawRect(w - 1f, 0f, w, h, alphaPaint)
            drawRect(0f, 0f, w, 1f, alphaPaint)
            drawRect(0f, h - 1f, w, h, alphaPaint)
            setBitmap(null)
        }
    }
}

/**
 * 将Bitmap保存到手机Picture目录
 * @param name 保存的文件名
 * @return 保存的文件File对象
 */
fun Bitmap.save(name: String): File {
    val externalStoragePublicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    var i = 0
    var file = File(externalStoragePublicDirectory, "$name.png")
    while (file.exists()) {
        file = File(externalStoragePublicDirectory, "$name.${i++}.png")
    }
    val fileOutputStream = FileOutputStream(file)
    compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
    fileOutputStream.flush()
    fileOutputStream.close()
    return file
}

/**
 * 通知系统扫描文件
 * @param context
 */
fun File.mediaScan(context: Context) {
    context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(this)))
}

/**
 * 判断是否是Android M（6.0）以上的系统版本
 */
inline fun supportM(todo: () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        todo()
    }
}

/**
 * 判断是否是Android N（7.0）以上的系统版本
 */
inline fun supportN(todo: () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        todo()
    }
}

/**
 * 判断Intent是否有效
 */
inline fun Context.supportIntent(intent: Intent, todo: (Intent) -> Unit) {
    if (intent.resolveActivity(packageManager) != null) {
        todo(intent)
    } else {
        toast(R.string.toast_no_handle_application)
    }
}

/**
 * 通过包名启动其他应用，假如应用已经启动了在后台运行，则会将应用切到前台
 */
fun Context.invokeApp(pkg: String, todo: () -> Unit) {
    val intent = packageManager.getLaunchIntentForPackage(pkg)
    if (intent != null) {
        startActivity(intent)
    } else {
        todo()
    }
}

//========================
/**
 * 给TextView的左边添加drawable
 */
fun <T : TextView> T.drawableLeft(resId: Int) {
    ContextCompat.getDrawable(context, resId)?.apply {
        setBounds(0, 0, minimumWidth, minimumHeight)
        setCompoundDrawables(this, null, null, null)
    }
}

/**
 * 给TextView的右边添加drawable
 */
fun <T : TextView> T.drawableRight(resId: Int) {
    ContextCompat.getDrawable(context, resId)?.apply {
        setBounds(0, 0, minimumWidth, minimumHeight)
        setCompoundDrawables(null, null, this, null)
    }
}

/**
 * 给TextView的上边添加drawable
 */
fun <T : TextView> T.drawableTop(resId: Int) {
    ContextCompat.getDrawable(context, resId)?.apply {
        setBounds(0, 0, minimumWidth, minimumHeight)
        setCompoundDrawables(null, this, null, null)
    }
}

/**
 * 给TextView的下边添加drawable
 */
fun <T : TextView> T.drawableBottom(resId: Int) {
    ContextCompat.getDrawable(context, resId)?.apply {
        setBounds(0, 0, minimumWidth, minimumHeight)
        setCompoundDrawables(null, null, null, this)
    }
}

/**
 * 给TextView或继承自TextView的组件设置{@link #TextView.addTextChangedListener}
 */
fun <T : TextView> T.addTextWatcher(watcher: (text: String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            watcher(s?.toString() ?: "")
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    })
}