package zzl.kotlin.ninja2.application

/**
 * Created by zhongzilu on 2018/9/20.
 */
object Type {
    const val MODE_DEFAULT  = 0x000 //默认模式
    const val MODE_PIN_EDIT = 0x001 //Pin编辑模式
    const val MODE_WEB      = 0x002 //浏览网页模式

    const val CODE_CHOOSE_FILE  = 0x003 //选择文件的请求码
    const val CODE_GET_IMAGE    = 0x004 //选择图片的请求码

    const val UA_DEFAULT    = 0 //默认的UserAgent
    const val UA_DESKTOP    = 1 //桌面式UserAgent
    const val UA_CUSTOM     = 2 //自定义UserAgent

    const val SEARCH_GOOGLE = "0"       //Google
    const val SEARCH_DUCKDUCKGO = "1"   //DuckDuckGo
    const val SEARCH_BING = "2"         //Bing
    const val SEARCH_BAIDU = "3"        //Baidu
    const val SEARCH_GITHUB = "4"       //Github

    const val EXTRA_SHORTCUT_BROADCAST = "name" //添加桌面图标的广播Intent Extra标识
}