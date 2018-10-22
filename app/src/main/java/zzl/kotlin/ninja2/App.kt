package zzl.kotlin.ninja2

import android.app.Application
import android.os.Message
import zzl.kotlin.ninja2.application.AdBlock
import kotlin.properties.Delegates


/**
 * Created by zhongzilu on 2018/7/27 0027.
 */
class App : Application() {

    companion object {
        var instance: App by Delegates.notNull()
        var MESSAGE: Message? = null
            get() {
                val msg = field
                field = null
                return msg
            }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        AdBlock.init(this)
    }
}