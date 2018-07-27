package zzl.kotlin.ninja2.application

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.preference.PreferenceManager
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Toast

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
fun View.hideKeyboard(){
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