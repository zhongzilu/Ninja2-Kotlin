package zzl.kotlin.ninja2

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import zzl.kotlin.ninja2.application.Protocol


/**
 * Created by zhongzilu on 2018/8/6 0006.
 */
abstract class BaseActivity : AppCompatActivity() {

    /**
     * 目标访问地址的Extra key
     */
    protected val EXTRA_TARGET_URL = "EXTRA_TARGET_URL"

    /**
     * 标记是否打开为隐私模式
     */
    protected val EXTRA_PRIVATE = "EXTRA_PRIVATE"

    /**
     * 页面堆栈Task ID
     */
    protected val EXTRA_TRACK_TASK_ID = "EXTRA_TRACK_TASK_ID"

    /**
     * 创建桌面快捷方式
     * @param url       快捷方式访问地址
     * @param name      快捷方式名称
     * @param bitmap    图标bitmap
     */
    fun createLauncherShortcut(url: String, name: String, bitmap: Bitmap) {
        val intent = Intent(this, RouteActivity::class.java)
        intent.data = Uri.parse(Protocol.SHORTCUT + url)
        val intent2 = Intent()
        intent2.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent)
        intent2.putExtra(Intent.EXTRA_SHORTCUT_NAME, name)
        intent2.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap)
        intent2.action = "com.android.launcher.action.INSTALL_SHORTCUT"
        sendBroadcast(intent2)
    }

    /**
     * 打开新页面
     */
    fun openUrl(url: String, isPrivate: Boolean = false, taskId: Int = 0) {
        startActivity(getNewPageIntent(url, isPrivate, taskId))
    }

    /**
     * 像谷歌浏览器那样呈现分组式的切换界面，详情请查看：http://mthli.github.io/Chrome-Overview-Screen
     * @param context
     * @param url
     * @param isPrivate
     * @param taskId
     */
    fun openUrlOverviewScreen(url: String, isPrivate: Boolean = false, taskId: Int = 0) {
        startActivity(getNewPageIntent(url, isPrivate, taskId), ActivityOptions.makeTaskLaunchBehind().toBundle())
    }

    /**
     * 获取跳转新页面的Intent
     */
    private fun getNewPageIntent(url: String, isPrivate: Boolean, taskId: Int): Intent {
        val intent = Intent(this, PageActivity::class.java)
        return with(intent){
            putExtra(EXTRA_TARGET_URL, url)
            putExtra(EXTRA_PRIVATE, isPrivate)
            putExtra(EXTRA_TRACK_TASK_ID, taskId)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
            addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                //intent.addFlags(CodedOutputStream.DEFAULT_BUFFER_SIZE); //4096
                addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
            }
            this
        }
    }

}