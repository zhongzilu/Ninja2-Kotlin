package zzl.kotlin.ninja2.application

/**
 * Created by zhongzilu on 2018/8/1 0001.
 */
object WebUtil {

    val MIME_TYPE_TEXT_HTML = "text/html"
    val MIME_TYPE_TEXT_PLAIN = "text/plain"
    val MIME_TYPE_IMAGE = "image/*"

    val BASE_URL = "file:///android_asset/"
    val BOOKMARK_TYPE = "<dt><a href=\"{url}\" add_date=\"{time}\">{title}</a>"
    val BOOKMARK_TITLE = "{title}"
    val BOOKMARK_URL = "{url}"
    val BOOKMARK_TIME = "{time}"
    val INTRODUCTION_EN = "ninja_introduction_en.html"
    val INTRODUCTION_ZH = "ninja_introduction_zh.html"

    val SEARCH_ENGINE_GOOGLE = "https://www.google.com/search?q=%s"
    val SEARCH_ENGINE_DUCKDUCKGO = "https://duckduckgo.com/?q=%s"
    val SEARCH_ENGINE_BING = "http://www.bing.com/search?q=%s"
    val SEARCH_ENGINE_BAIDU = "http://www.baidu.com/s?wd=%s"

    val UA_DESKTOP = "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36"
    val URL_ENCODE = "UTF-8"

    val URL_PREFIX_GOOGLE_PLAY = "www.google.com/url?q="
    val URL_SUFFIX_GOOGLE_PLAY = "&sa"
    val URL_PREFIX_GOOGLE_PLUS = "plus.url.google.com/url?q="
    val URL_SUFFIX_GOOGLE_PLUS = "&rct"


}

object Protocol {
    val SHORTCUT = "ninja2://shortcut/"
    val PRIVATE_TAB = "ninja2://private/"
    val NEW_TAB = "ninja2://new_tab/"

    val ABOUT_BLANK = "about:blank"
    val ABOUT = "about:"
    val MAIL_TO = "mailto:"
    val FILE = "file://"
    val FTP = "ftp://"
    val HTTP = "http://"
    val HTTPS = "https://"
    val INTENT = "intent://"
}