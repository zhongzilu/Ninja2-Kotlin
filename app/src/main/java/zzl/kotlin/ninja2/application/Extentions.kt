package zzl.kotlin.ninja2.application

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.MailTo
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Patterns
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import android.webkit.URLUtil
import android.widget.Toast
import zzl.kotlin.ninja2.App
import zzl.kotlin.ninja2.R
import java.net.URL
import java.util.regex.Pattern


/**
 * Created by zhongzilu on 18-7-25.
 */
//=========================
fun View.visible() {
    this.visibility = View.VISIBLE
}

fun View.gone() {
    this.visibility = View.GONE
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

fun View.hide(animate: Boolean = true) {
    if (animate) {
        animate().apply {
            alpha = 0f
            duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
            interpolator = LinearInterpolator()
            setListener(null)
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

fun <T : View> T.visibleDo(todo: (T) -> Unit) {
    if (this.visibility == View.VISIBLE) {
        todo(this)
    }
}

//========================

/**
 * 短时间提示
 */
fun Context.toast(content: CharSequence) {
    Toast.makeText(this, content, Toast.LENGTH_SHORT).show()
}

/**
 * 短时间提示，提示资源文件中的字符串内容
 */
fun Context.toast(resId: Int) {
    toast(this.resources.getString(resId))
}

//=========================
inline fun <reified T : Activity> Context.go() {
    startActivity(Intent(this, T::class.java))
}

/**
 * 带返回值的Activity跳转
 *
 * @param clazz
 * @param requestCode
 */
inline fun <reified T : Activity> Activity.go4Result(requestCode: Int) {
    startActivityForResult(Intent(this, T::class.java), requestCode)
}

//=========================

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

//==========================

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
    val intent = Intent(Intent.ACTION_SEND)
    val parse = MailTo.parse(mail)
    intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(parse.to))
    intent.putExtra(Intent.EXTRA_TEXT, parse.body)
    intent.putExtra(Intent.EXTRA_SUBJECT, parse.subject)
    intent.putExtra(Intent.EXTRA_CC, parse.cc)
    intent.type = "message/rfc822"
    startActivity(intent)
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

//==========================
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
    val foregroundColorSpan = ForegroundColorSpan(color)
    val builder = SpannableStringBuilder(this)
    builder.setSpan(foregroundColorSpan, 0, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    return builder
}

/**
 * 验证字符串是否是Intent
 */
fun String.isIntent(): Boolean {
    return this.isNotEmpty() && this.startsWith(Protocol.INTENT)
}

/**
 * 扩展属性
 */
val String.pattern
    get() = Pattern.compile("(?i)((?:http|https|file|ftp)://|(?:data|about|javascript|mailto):|(?:.*:.*@))(.*)")

/**
 * 验证是否是带有协议头的Url
 */
fun String.isProtocolUrl(): Boolean {
    return pattern.matcher(this).matches()
}

/**
 * 验证是否是Web路径
 */
fun String.isWebUrl(): Boolean {
    return Patterns.WEB_URL.matcher(this).matches()
}

/**
 * 获取Url的主机地址部分
 */
fun String.host(): String {
    return URL(this).host
}