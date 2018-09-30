package zzl.kotlin.ninja2

import android.app.ActivityManager
import android.app.ActivityManager.AppTask
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import zzl.kotlin.ninja2.application.*


/**
 * Created by zhongzilu on 2018/7/27 0027.
 */
class RouteActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.apply {

            //验证是否有符合条件的Intent Action
            when(action){
                Intent.ACTION_MAIN -> {
                    val appTasks: List<AppTask> = (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).appTasks
                    if (appTasks.size > 1) {
                        appTasks[1].moveToFront()
                        finish()
                        return
                    }
                }

                //进行网页搜索
                Intent.ACTION_WEB_SEARCH -> {
                    openUrl(getStringExtra(SearchManager.QUERY))
                    finish()
                    return
                }
            }

            //
            dataString?.apply {

                //当data为空时，则直接进入PageActivity
                if (isEmpty()) return

                //当data为隐私浏览协议时，则启动隐私浏览模式，这种情况多半是桌面快捷方式启动隐私浏览
                if (this == Protocol.PRIVATE_TAB){
                    openUrl("", true)
                    finish()
                    return
                }

                //当data数据为快捷图标协议时，则截取出快捷图标访问的地址部分
                if (startsWith(Protocol.SHORTCUT)){
                    openUrl(substring(Protocol.SHORTCUT.length, length))
                    finish()
                    return
                }

                //当data为普通网络地址时，则直接打开新页面进行解析
                if (isWebUrl()){
                    supportM { overridePendingTransition(0, 0) }
                    openUrl(this)
                    finish()
                    return
                }

                //其他情况也直接打开新页面
                openUrl(this)
                finish()
                return
            }

            L.d(TAG, "intent data string is null or empty")
        }

        //默认情况下，不打开新页面直接进入PageActivity
        L.d(TAG, "intent is null")
        go<PageActivity>()
        finish()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        L.d(TAG, "onNewIntent")
    }
}