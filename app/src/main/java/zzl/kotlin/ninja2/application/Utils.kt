package zzl.kotlin.ninja2.application

import android.content.Context
import android.util.Log
import org.jetbrains.anko.doAsync
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URI
import java.net.URISyntaxException
import java.util.*


/**
 * Created by zhongzilu on 2018-08-01
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

/**
 * Log Util
 * Created by zhongzilu on 2018-09-30
 */
object L {
    private const val DEBUG = 0x002
    private const val INFO = 0x003
    private const val WARE = 0x004
    private const val ERROR = 0x005
    private const val NOTHING = 0x006

    private val LEVEL = DEBUG

    fun d(tag: String, msg: String) {
        if (LEVEL <= DEBUG)
            Log.d(tag, msg)
    }

    fun i(tag: String, msg: String) {
        if (LEVEL <= INFO)
            Log.i(tag, msg)
    }

    fun w(tag: String, msg: String) {
        if (LEVEL <= WARE)
            Log.w(tag, msg)
    }

    fun e(tag: String, msg: String) {
        if (LEVEL <= ERROR)
            Log.e(tag, msg)
    }
}

object AdBlock {

    private const val FILE_NAME = "AdHosts"
    private val hostSet = HashSet<String>()
    private val locale = Locale.getDefault()

    fun init(context: Context) {

        if (hostSet.isNotEmpty()) return

        doAsync {
            val bufferedReader = BufferedReader(InputStreamReader(context.assets.open(FILE_NAME)))
            while (true) {
                val readLine = bufferedReader.readLine()
                if (readLine != null) {
                    AdBlock.hostSet.add(readLine.toLowerCase(AdBlock.locale))
                } else break
            }
        }
    }

    @Throws(URISyntaxException::class)
    private fun getHost(url: String): String {
        var hostUrl = url.toLowerCase(locale)
        val indexOf = hostUrl.indexOf("/", 8)
        if (indexOf != -1) {
            hostUrl = hostUrl.substring(0, indexOf)
        }
        val host = URI(hostUrl).host
        if (host.isEmpty()) {
            return hostUrl
        }
        return if (host.startsWith("www.")) host.substring(4) else host
    }

    /**
     * 验证是否为广告链接地址
     */
    fun isAd(url: String): Boolean {
        return try {
            hostSet.contains(getHost(url).toLowerCase())
        } catch (e: URISyntaxException) {
            e.printStackTrace()
            false
        }
    }
}