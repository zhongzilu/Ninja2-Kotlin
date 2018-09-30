package zzl.kotlin.ninja2.application

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.jetbrains.anko.toast
import zzl.kotlin.ninja2.R

/**
 * 添加桌面图标后的回调广播
 * Created by zhongzilu on 2018-09-30
 */
class ShortcutAddedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            val name = intent?.getStringExtra(Type.EXTRA_SHORTCUT_BROADCAST) ?: ""
            it.toast(it.getString(R.string.toast_add_to_launcher_successful, name))
        }
    }
}