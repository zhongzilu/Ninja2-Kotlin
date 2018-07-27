package zzl.kotlin.ninja2

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceScreen
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.Toast
import zzl.kotlin.ninja2.application.Key
import zzl.kotlin.ninja2.application.SP
import zzl.kotlin.ninja2.application.versionName


/**
 * Created by zhongzilu on 2018/7/27 0027.
 */
class SettingsActivity : Activity() {

    private lateinit var mFragment: SettingPreferenceFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val beginTransaction = fragmentManager.beginTransaction()
        this.mFragment = SettingPreferenceFragment()

        /*
          这里很奇怪，如果新建一个布局文件，里面包含一个id名为content的FrameLayout,然后把android.R.id.content
          替换为R.id.content，则会出现R.xml.preferences中的ListPreference包空指针异常，这意味着SettingsActivity
          不能有自己的布局文件
         */
        beginTransaction.replace(android.R.id.content, this.mFragment)
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
            R.string.preference_title_clear_cookies -> {
                toast(R.string.toast_clear_cookies)
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference)
    }

    /**
     * 处理SharePreference值改变的事件
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {

        when(key){
            Key.screenShot -> {
                if (SP.canScreenshot) {
                    val manager = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

                    //清除并结束掉所有AppTask
                    manager.appTasks.forEach {
                        it.finishAndRemoveTask()
                    }
                }
            }

            Key.searchEngine -> {
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

    private fun setSearchSummary() {
        findPreference(getString(R.string.preference_key_search_engine_id)).summary = resources
                .getStringArray(R.array.preference_entries_search_engine_id)[SP.SearchEngine.toInt()]
    }

    private fun setVersionSummary() {
        findPreference(getString(R.string.preference_key_version)).summary = activity.versionName()
    }

    private fun toast(msg: Int){
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
    }
}