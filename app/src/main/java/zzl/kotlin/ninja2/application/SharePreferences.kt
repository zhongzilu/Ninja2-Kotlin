package zzl.kotlin.ninja2.application

import zzl.kotlin.ninja2.App
import zzl.kotlin.ninja2.R

/**
 * Created by zhongzilu on 2018/7/27 0027.
 */
object SP {
    private val ctx = App.instance

    /**
     * 自定义User Agent，默认为空字符串
     */
    var UA: String by ctx.preference(Key.UA, "")

    /**
     * 是否开启后台打开页面，默认为false
     */
    var IsOpenInBackground: Boolean by ctx.preference(Key.OPEN_IN_BG, false)

    /**
     * 搜索引擎
     */
    var SearchEngine: String by ctx.preference(Key.SEARCH_ENGINE, "0")

    /**
     * 是否开启网页截图
     */
    var canScreenshot: Boolean by ctx.preference(Key.SCREENSHOT, false)

    /**
     * 是否开启多窗口
     */
    val isEnableMultipleWindows: Boolean by ctx.preference(Key.MULTIPLE_WINDOW, true)
}

object Key {
    private val ctx = App.instance

    val UA: String by lazy(LazyThreadSafetyMode.NONE) {
        ctx.resources.getString(R.string.preference_key_custom_user_agent)
    }

    val OPEN_IN_BG: String by lazy(LazyThreadSafetyMode.NONE) {
        ctx.resources.getString(R.string.preference_key_open_in_background)
    }

    val SEARCH_ENGINE: String by lazy(LazyThreadSafetyMode.NONE) {
        ctx.resources.getString(R.string.preference_key_search_engine_id)
    }

    val SCREENSHOT: String by lazy(LazyThreadSafetyMode.NONE) {
        ctx.resources.getString(R.string.preference_key_screenshot)
    }

    val MULTIPLE_WINDOW: String by lazy(LazyThreadSafetyMode.NONE) {
        ctx.resources.getString(R.string.preference_key_enable_multiple_windows)
    }
}