package zzl.kotlin.ninja2

import android.app.Application
import zzl.kotlin.ninja2.application.AdBlock
import kotlin.properties.Delegates

/**
 * Created by zhongzilu on 2018/7/27 0027.
 */
class App : Application() {

    companion object {
        var instance: App by Delegates.notNull()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        AdBlock.init(this)
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }

    override fun onTerminate() {
        super.onTerminate()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
    }

}