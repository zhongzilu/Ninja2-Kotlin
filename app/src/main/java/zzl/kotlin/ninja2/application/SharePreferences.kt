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
    var UA: String by DelegatesExt.preference(ctx, Key.UA, "")

    /**
     * 是否开启后台打开页面，默认为false
     */
    var IsOpenInBackground: Boolean by DelegatesExt.preference(ctx, Key.OpenInBg, false)

    /**
     * 搜索引擎
     */
    var SearchEngine: String by DelegatesExt.preference(ctx, Key.searchEngine, "0")

    /**
     * 是否开启网页截图
     */
    var canScreenshot: Boolean by DelegatesExt.preference(ctx, Key.screenShot, false)
}

object Key {
    private val ctx = App.instance

    val UA: String by lazy {
        ctx.resources.getString(R.string.preference_key_custom_user_agent)
    }

    val OpenInBg: String by lazy {
        ctx.resources.getString(R.string.preference_key_open_in_background)
    }

    val searchEngine: String by lazy {
        ctx.resources.getString(R.string.preference_key_search_engine_id)
    }

    val screenShot: String by lazy {
        ctx.resources.getString(R.string.preference_key_screenshot)
    }
}