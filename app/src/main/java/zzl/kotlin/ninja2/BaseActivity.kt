package zzl.kotlin.ninja2

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.pm.ShortcutInfoCompat
import android.support.v4.content.pm.ShortcutManagerCompat
import android.support.v4.graphics.drawable.IconCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import zzl.kotlin.ninja2.application.Protocol
import zzl.kotlin.ninja2.application.ShortcutAddedReceiver
import zzl.kotlin.ninja2.application.Type
import zzl.kotlin.ninja2.application.supportN


/**
 * Created by zhongzilu on 2018/8/6 0006.
 */
abstract class BaseActivity : AppCompatActivity() {

    open lateinit var TAG: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TAG = "${this::class.java.simpleName}-->"
    }

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
    protected val EXTRA_TASK_ID = "EXTRA_TASK_ID"

    /**
     * 设置开启ActionBar/Toolbar的返回按钮
     */
    fun enableBackPress() = supportActionBar?.setDisplayHomeAsUpEnabled(true)

    /**
     * 创建桌面快捷方式
     * @param url       快捷方式访问地址
     * @param name      快捷方式名称
     * @param bitmap    图标bitmap
     */
    fun createLauncherShortcut(url: String, name: String, bitmap: Bitmap) {

        if (ShortcutManagerCompat.isRequestPinShortcutSupported(this)) {

            //快捷方式点击
            val shortcutInfoIntent = Intent(this, RouteActivity::class.java).apply {
                action = Intent.ACTION_VIEW //action必须设置，不然报错
                data = Uri.parse(Protocol.SHORTCUT + url)
            }

            //构建适配的ShortcutInfo
            val info = ShortcutInfoCompat.Builder(this, url)
                    .setIcon(IconCompat.createWithBitmap(bitmap))
                    .setShortLabel(name)
                    .setIntent(shortcutInfoIntent)
                    .build()

            //当添加快捷方式的确认弹框弹出来时，将会把该Intent广播出去
            val broadcastIntent = Intent(this, ShortcutAddedReceiver::class.java).apply {
                action = Intent.ACTION_CREATE_SHORTCUT
                putExtra(Type.EXTRA_SHORTCUT_BROADCAST, name)
            }

            //当添加快捷方式的确认弹框弹出来时，将被回调
            val shortcutCallbackIntent = PendingIntent.getBroadcast(this, 0, broadcastIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT)
            ShortcutManagerCompat.requestPinShortcut(this, info, shortcutCallbackIntent.intentSender)
        }
    }

    /**
     * 使用普通方式打开网址
     * @param context   上下文Context
     * @param url       访问的目标URL
     * @param isPrivate 是否为隐私访问
     * @param taskId    任务
     */
    fun openUrl(url: String, private: Boolean = false, taskId: Int = 0) {
        startActivity(new(url, private, taskId))
    }

    /**
     * 像谷歌浏览器那样呈现分组式的切换界面，详情请查看：http://mthli.github.io/Chrome-Overview-Screen
     * @param context   上下文Context
     * @param url       访问的目标URL
     * @param isPrivate 是否为隐私访问
     * @param taskId    任务
     */
    fun openUrlOverviewScreen(url: String, private: Boolean = false, taskId: Int = 0) {
        startActivity(new(url, private, taskId), ActivityOptions.makeTaskLaunchBehind().toBundle())
    }

    /**
     * Returns an new {@link Intent} to start {@link PageActivity} as a new document in
     * overview menu.
     *
     * To start a new document task {@link Intent#FLAG_ACTIVITY_NEW_DOCUMENT} must be used. The
     * system will search through existing tasks for one whose Intent matches the Intent component
     * name and the Intent data. If it finds one then that task will be brought to the front and the
     * new Intent will be passed to onNewIntent().
     *
     * Activities launched with the NEW_DOCUMENT flag must be created with launchMode="standard".
     */
    private fun new(url: String, private: Boolean = false, taskId: Int = 0): Intent {
        return Intent(this, PageActivity::class.java).apply {
            putExtra(EXTRA_TARGET_URL, url)
            putExtra(EXTRA_PRIVATE, private)
            putExtra(EXTRA_TASK_ID, taskId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)

            /*
            When {@linkIntent#FLAG_ACTIVITY_NEW_DOCUMENT} is used with {@link Intent#FLAG_ACTIVITY_MULTIPLE_TASK}
            the system will always create a new task with the target activity as the root. This allows the same
            document to be opened in more than one task.
            */
            addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)

            supportN {
                //                    addFlags(CodedOutputStream.DEFAULT_BUFFER_SIZE); //4096
                addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
            }
        }
    }

    private var mAlertDialog: AlertDialog.Builder? = null
    fun dialogBuilder(): AlertDialog.Builder {
        if (mAlertDialog == null) {
            mAlertDialog = AlertDialog.Builder(this)
        }

        return mAlertDialog!!
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

}