package zzl.kotlin.ninja2.application

import zzl.kotlin.ninja2.App
import zzl.kotlin.ninja2.R

/**
 * Created by zhongzilu on 2018/7/27 0027.
 */
object SP {
    private val ctx = App.instance

    /**
     * 是否启用广告屏蔽，默认为false
     */
    var adBlock: Boolean by ctx.preference(Key.AD, false)

    /**
     * 是否开启网页截图, 默认为false
     */
    var canScreenshot: Boolean by ctx.preference(Key.SCREENSHOT, false)

    /**
     * 新建标签页时是否自动弹出软键盘，默认为false
     */
    var isShowKeyboard: Boolean by ctx.preference(Key.KEYBOARD, false)

    /**
     * 主页列表自底向上排序，默认为false
     */
    var pinsReverse: Boolean by ctx.preference(Key.PINS_REVERSE, false)

    /**
     * 是否开启地址栏控制，默认为false
     */
    var omniboxCtrl: Boolean by ctx.preference(Key.OMNIBOX_CTRL, false)

    /**
     * 是否开启返回时震动，默认为false
     */
    var vibrate: Boolean by ctx.preference(Key.VIBRATE, false)

    /**
     * 搜索引擎
     */
    var searchEngine: String by ctx.preference(Key.SEARCH_ENGINE, "0")

    /**
     * 自定义User Agent，默认为空字符串
     */
    var UA: String by ctx.preference(Key.UA, "")

    /**
     * 是否开启多窗口
     */
    val isEnableMultipleWindows: Boolean by ctx.preference(Key.MULTIPLE_WINDOW, true)

    /**
     * 是否开启后台打开页面，默认为false
     */
    var isOpenInBackground: Boolean by ctx.preference(Key.OPEN_IN_BG, false)
}

object Key {
    private val ctx = App.instance

    val AD: String by lazy(LazyThreadSafetyMode.NONE) {
        ctx.resources.getString(R.string.preference_key_adblock)
    }

    val SCREENSHOT: String by lazy(LazyThreadSafetyMode.NONE) {
        ctx.resources.getString(R.string.preference_key_screenshot)
    }

    val KEYBOARD: String by lazy(LazyThreadSafetyMode.NONE) {
        ctx.resources.getString(R.string.preference_key_show_soft_keyboard)
    }

    val PINS_REVERSE: String by lazy(LazyThreadSafetyMode.NONE) {
        ctx.resources.getString(R.string.preference_key_homepage_reverse)
    }

    val OMNIBOX_CTRL: String by lazy(LazyThreadSafetyMode.NONE) {
        ctx.resources.getString(R.string.preference_key_omnibox_control)
    }

    val VIBRATE: String by lazy(LazyThreadSafetyMode.NONE) {
        ctx.resources.getString(R.string.preference_key_back_vibrate)
    }

    val UA: String by lazy(LazyThreadSafetyMode.NONE) {
        ctx.resources.getString(R.string.preference_key_custom_user_agent)
    }

    val OPEN_IN_BG: String by lazy(LazyThreadSafetyMode.NONE) {
        ctx.resources.getString(R.string.preference_key_open_in_background)
    }

    val SEARCH_ENGINE: String by lazy(LazyThreadSafetyMode.NONE) {
        ctx.resources.getString(R.string.preference_key_search_engine_id)
    }

    val MULTIPLE_WINDOW: String by lazy(LazyThreadSafetyMode.NONE) {
        ctx.resources.getString(R.string.preference_key_enable_multiple_windows)
    }
}