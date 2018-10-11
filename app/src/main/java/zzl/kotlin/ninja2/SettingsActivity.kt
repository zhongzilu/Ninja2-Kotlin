package zzl.kotlin.ninja2

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceScreen
import android.provider.Settings
import android.support.v7.app.AlertDialog
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebViewDatabase
import android.widget.Toast
import zzl.kotlin.ninja2.application.Key
import zzl.kotlin.ninja2.application.SP
import zzl.kotlin.ninja2.application.versionName

/**
 * 设置页面
 * Created by zhongzilu on 2018/7/27 0027.
 */
class SettingsActivity : BaseActivity() {

    private lateinit var mFragment: SettingPreferenceFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableBackPress()

        val beginTransaction = fragmentManager.beginTransaction()
        mFragment = SettingPreferenceFragment()

        /*
          这里很奇怪，如果新建一个布局文件，里面包含一个id名为content的FrameLayout,然后把android.R.id.content
          替换为R.id.content，则会出现R.xml.preferences中的ListPreference包空指针异常
         */
        beginTransaction.replace(android.R.id.content, mFragment)
        beginTransaction.commit()
    }

    override fun onBackPressed() {
        if (!mFragment.isWaiting()) {
            super.onBackPressed()
        }
    }
}

class SettingPreferenceFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener{

    private var isWaiting = false
    fun isWaiting() = isWaiting

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        addPreferencesFromResource(R.xml.preferences)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setVersionSummary()
    }

    override fun onResume() {
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        setSearchSummary()
        super.onResume()
    }

    override fun onPause() {
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    /**
     * 处理设置选项的点击事件
     */
    override fun onPreferenceTreeClick(preferenceScreen: PreferenceScreen?, preference: Preference): Boolean {
        when(preference.titleRes){
            //清空Cookies
            R.string.preference_title_clear_cookies -> CookieManager.getInstance().apply {
                    flush()
                    removeAllCookies{ flag ->
                        if (flag) toast(R.string.toast_clear_cookies)
                        else toast(R.string.toast_clear_cookies_error)
                    }
                }

            //清空表单数据
            R.string.preference_title_clear_form_data -> dialog(R.string.preference_title_clear_form_data){
                WebViewDatabase.getInstance(activity).clearFormData()
                toast(R.string.toast_clear_form_data)
            }

            //清空历史记录
            R.string.preference_title_clear_history -> dialog(R.string.preference_title_clear_history){
                //todo 清空历史纪录
            }

            //清空网站密码
            R.string.preference_title_clear_passwords -> dialog(R.string.preference_title_clear_passwords){
                WebViewDatabase.getInstance(activity).clearHttpAuthUsernamePassword()
                toast(R.string.toast_clear_passwords)
            }

            //指纹识别扩展
            R.string.preference_title_fingerprint_extension ->
                activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

            //书签导入导出
            R.string.preference_title_import_export_pin -> {
                //todo 处理书签的导入/导出
            }

            //自定义UA
            R.string.preference_title_custom_user_agent -> {
                //todo 实现UA的自定设置
            }

            //反馈
            R.string.preference_title_feedback -> openUrl(getString(R.string.app_feedback_url))

            //开源协议
            R.string.preference_title_licenses -> openUrl(getString(R.string.app_licenses_url))

            //版本
            R.string.preference_title_version -> {
                //todo 应用版本的问题
            }
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference)
    }

    /**
     * 处理SharePreference值改变的事件
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {

        when(key){
            Key.SCREENSHOT -> {
                if (SP.canScreenshot) {
                    val manager = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

                    //清除并结束掉所有AppTask
                    manager.appTasks.forEach {
                        it.finishAndRemoveTask()
                    }
                }
            }

            Key.SEARCH_ENGINE -> {
                setSearchSummary()
            }
        }
    }

    private var mAlertDialog: AlertDialog.Builder? = null
    private fun dialog(msg: Int, confirm: () -> Unit){
        if (mAlertDialog == null) {
            mAlertDialog = AlertDialog.Builder(activity)
                    .setNegativeButton(R.string.dialog_button_cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
        }

        mAlertDialog!!
                .setMessage(msg)
                .setPositiveButton(R.string.dialog_button_confirm) { _, _ ->
                    confirm()
                }.show()

    }

    /**
     * 设置搜索引擎选项的摘要文字
     */
    private fun setSearchSummary() {
        findPreference(getString(R.string.preference_key_search_engine_id)).summary = resources
                .getStringArray(R.array.preference_entries_search_engine_id)[SP.searchEngine.toInt()]
    }

    /**
     * 设置版本信息的摘要文字
     */
    private fun setVersionSummary() {
        findPreference(getString(R.string.preference_key_version)).summary = activity.versionName()
    }

    private fun setSearchEngines(){
        //todo 动态添加搜索引擎
        val engines: ListPreference = findPreference(getString(R.string.preference_key_search_engine_id)) as ListPreference
//        engines.entries
    }

    /**
     * 设置Cookie大小的摘要文字
     */
    private fun setCookieSummary(){
        findPreference(getString(R.string.preference_key_clear_cookies)).summary = "0Mb"
    }

    private fun toast(msg: Int){
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
    }

    /**
     * 以新页面打开网络地址
     * @param url 目标访问地址
     */
    private fun openUrl(url: String){
        val target = activity
        if (target is BaseActivity){
            target.openUrl(url)
        }
    }
}