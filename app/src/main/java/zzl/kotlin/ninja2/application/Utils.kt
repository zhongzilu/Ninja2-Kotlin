package zzl.kotlin.ninja2.application

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.Log
import android.webkit.URLUtil
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import zzl.kotlin.ninja2.R
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URI
import java.net.URISyntaxException
import java.util.*


/**
 * Created by zhongzilu on 2018-08-01
 */
object WebUtil {

    const val MIME_TYPE_TEXT_HTML = "text/html"
    const val MIME_TYPE_TEXT_PLAIN = "text/plain"
    const val MIME_TYPE_IMAGE = "image/*"

    val BOOKMARK_TYPE = "<dt><a href=\"{url}\" add_date=\"{time}\">{title}</a>"
    val BOOKMARK_TITLE = "{title}"
    val BOOKMARK_URL = "{url}"
    val BOOKMARK_TIME = "{time}"
    const val INTRODUCTION_EN = "ninja_introduction_en.html"
    const val INTRODUCTION_ZH = "ninja_introduction_zh.html"

    val SEARCH_ENGINE_GOOGLE = "https://www.google.com/search?q=%s"
    val SEARCH_ENGINE_DUCKDUCKGO = "https://duckduckgo.com/?q=%s"
    val SEARCH_ENGINE_BING = "http://www.bing.com/search?q=%s"
    val SEARCH_ENGINE_BAIDU = "http://www.baidu.com/s?wd=%s"

    const val UA_DESKTOP = "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36"
    const val URL_ENCODE = "UTF-8"

    const val URL_PREFIX_GOOGLE_PLAY = "www.google.com/url?q="
    const val URL_SUFFIX_GOOGLE_PLAY = "&sa"
    const val URL_PREFIX_GOOGLE_PLUS = "plus.url.google.com/url?q="
    const val URL_SUFFIX_GOOGLE_PLUS = "&rct"

    const val HEADER_CONTENT_DISPOSITION = "Content-Disposition: attachment;"
}

/**
 * 协议头
 */
object Protocol {
    const val SHORTCUT = "ninja2://shortcut/"
    const val PRIVATE_TAB = "ninja2://private/"
    const val NEW_TAB = "ninja2://new_tab/"

    const val ASSET_FILE = "file:///android_asset/"
    const val ABOUT_BLANK = "about:blank"
    const val ABOUT = "about:"
    const val MAIL_TO = "mailto:"
    const val FILE = "file:///"
    const val FTP = "ftp://"
    const val HTTP = "http://"
    const val HTTPS = "https://"
    const val INTENT = "intent:"
    const val INTENT_OLD = "#Intent;"
    const val TEL = "tel:"
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

/**
 * 广告拦截工具类 AdBlock Util
 * Created by zhongzilu on 2018-10-09
 */
object AdBlock {

    private const val FILE_NAME = "AdHosts"
    private val hostSet = HashSet<String>()
    private val locale = Locale.getDefault()

    /**
     * 初始化广告拦截名单，如果{@link #hostSet}不为空，则忽略，不再重新加载数据
     */
    fun init(context: Context) {
        if (hostSet.isNotEmpty()) return

        doAsync {
            val bufferedReader = BufferedReader(InputStreamReader(context.assets.open(FILE_NAME)))
            while (true) {
                val readLine = bufferedReader.readLine()
                if (readLine != null) {
                    hostSet.add(readLine.toLowerCase(locale))
                } else break
            }
        }
    }

    @Throws(URISyntaxException::class)
    private fun getHost(url: String): String {
        val hostUrl = url.toLowerCase(locale)
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
            if (hostSet.isEmpty()) throw NullPointerException("请先调用init()方法进行初始化")

            hostSet.contains(getHost(url))
        } catch (e: URISyntaxException) {
            e.printStackTrace()
            false
        }
    }
}

/**
 * 网页下载工具类
 * Created by zhongzilu on 2018-10-16
 */
object Download {

    /**
     * 调用浏览器进行下载，如果不存在浏览器，则调用下载管理器进行下载
     * @param context Context
     * @param url 下载地址
     * @param contentDisposition 内容描述
     * @param mimeType 文件类型
     */
    fun inBrowser(context: Context, url: String, contentDisposition: String, mimeType: String) {
        val intent = Intent(Intent.ACTION_DEFAULT)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setDataAndType(Uri.parse(url), mimeType)
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            inBackground(context, url, contentDisposition, mimeType)
        }
    }

    /**
     * 使用下载管理器进行下载
     * @param context Context
     * @param url 下载地址
     * @param contentDisposition 内容描述
     * @param mimeType 文件类型
     */
    fun inBackground(context: Context, url: String, contentDisposition: String, mimeType: String) {
        var guessFileName = URLUtil.guessFileName(url, contentDisposition, mimeType)

        // 临时解决下载文件后缀名不匹配的问题
        val suffix = url.substring(url.lastIndexOf("."))
        guessFileName = guessFileName.replace(".bin", suffix)

        L.i("DownloadUtil-->", "download guessFileName: $guessFileName")
        try {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                allowScanningByMediaScanner()
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setTitle(guessFileName)
                setMimeType(mimeType)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, guessFileName)
            }
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            context.toast(context.getString(R.string.toast_download_in_background, guessFileName))
        } catch (e: Exception) {
            e.printStackTrace()
            context.toast(e.localizedMessage)
        }
    }
}

/**
 * 实现RecyclerView的ItemTouchHelper.Callback
 * Created by zhongzilu on 2018-10-09
 */
class DefaultItemTouchHelperCallback(callback: Callback?) : ItemTouchHelper.Callback() {

    /**
     * Item操作的回调
     */
    private var mListener: Callback? = null

    /**
     * 是否可以拖拽，默认可以拖拽
     */
    private var isCanDrag = true

    /**
     * 是否可以被滑动，默认可以滑动
     */
    private var isCanSwipe = true

    init {
        this.mListener = callback
    }

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val layoutManager = recyclerView.layoutManager

        var dragFlag = 0
        var swipeFlag = 0

        // 如果布局管理器是GridLayoutManager
        if (layoutManager is GridLayoutManager) {
            // flag如果值是0，相当于这个功能被关闭
            dragFlag = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT or ItemTouchHelper.UP or ItemTouchHelper.DOWN

            // create make
            return ItemTouchHelper.Callback.makeMovementFlags(dragFlag, swipeFlag)
        }

        // 如果布局管理器是LinearLayoutManager
        if (layoutManager is LinearLayoutManager) {

            when (layoutManager.orientation) {
                LinearLayoutManager.HORIZONTAL -> { // 如果是横向布局
                    swipeFlag = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                    dragFlag = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                }

                LinearLayoutManager.VERTICAL -> { // 如果是纵向布局
                    swipeFlag = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                    dragFlag = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                }
            }

            return ItemTouchHelper.Callback.makeMovementFlags(dragFlag, swipeFlag)
        }

        return 0
    }

    /**
     * 当Item被拖拽的时候被回调
     * @param recyclerView RecyclerView
     * @param viewHolder 拖拽的ViewHolder
     * @param target 目的地的ViewHolder
     * @return
     */
    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        return mListener?.onMove(viewHolder.adapterPosition, target.adapterPosition) ?: false
    }

    /**
     * 当Item被滑动的时候回调
     * @param viewHolder 滑动的ViewHolder
     * @param direction 滑动方向
     */
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        mListener?.onSwiped(viewHolder, direction)
    }

    override fun clearView(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder) {
        mListener?.clearView(viewHolder)
    }

    /**
     * 设置Item操作的回调，去更新UI和数据源
     * @param listener
     */
    fun setOnItemTouchCallbackListener(listener: Callback) {
        this.mListener = listener
    }

    /**
     * 设置是否可以被拖拽
     * @param canDrag true:是 false:否
     */
    fun setDragEnable(canDrag: Boolean) {
        isCanDrag = canDrag
    }

    /**
     * 设置是否可以被滑动
     * @param canSwipe true:是 false:否
     */
    fun setSwipeEnable(canSwipe: Boolean) {
        isCanSwipe = canSwipe
    }

    /**
     * 当Item被长按的时候是否可以被拖拽
     * @return
     */
    override fun isLongPressDragEnabled(): Boolean {
        return isCanDrag
    }

    /**
     * Item是否可以被滑动（H：左右滑动，V：上下滑动）
     * @return
     */
    override fun isItemViewSwipeEnabled(): Boolean {
        return isCanSwipe
    }

    interface Callback {

        /**
         * 当某个Item被滑动删除的时候
         * @param viewHolder viewHolder
         */
        fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int)

        /**
         * 当两个Item位置互换的时候被回调
         * @param srcPosition       拖拽的item的position
         * @param targetPosition    目的地的item的position
         * @return 开发者处理了操作应该返回true,开发者没有处理就返回false
         */
        fun onMove(srcPosition: Int, targetPosition: Int): Boolean

        /**
         * 滑动删除清理视图
         * @param viewHolder ViewHolder
         */
        fun clearView(viewHolder: RecyclerView.ViewHolder)
    }
}
