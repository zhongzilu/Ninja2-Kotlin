package zzl.kotlin.ninja2.application

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
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
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.webkit.URLUtil
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import zzl.kotlin.ninja2.R
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.net.URI
import java.net.URISyntaxException
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created by zhongzilu on 2018-08-01
 */
object WebUtil {

    const val MIME_TYPE_TEXT_HTML = "text/html"
    const val MIME_TYPE_TEXT_PLAIN = "text/plain"
    const val MIME_TYPE_IMAGE = "image/*"

    val SEARCH_ENGINE_GOOGLE = "https://www.google.com/search?q=%s"
    val SEARCH_ENGINE_DUCKDUCKGO = "https://duckduckgo.com/?q=%s"
    val SEARCH_ENGINE_BING = "http://www.bing.com/search?q=%s"
    val SEARCH_ENGINE_BAIDU = "http://www.baidu.com/s?wd=%s"
    val SEARCH_ENGINE_GITHUB = "https://github.com/search?q=%s&ref=opensearch"

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

object Bookmark {

    val FILE_NAME = "ninja2.%s.html"
    val ITEM = "<DT><A HREF=\"{url}\" ADD_DATE=\"{time}\">{title}</A></DT>"
    val TITLE = "{title}"
    val URL = "{url}"
    val TIME = "{time}"
    val TEMPLATE = "<!DOCTYPE NETSCAPE-Bookmark-file-1>\n" +
            "<!-- This is an automatically generated file.\n" +
            "     It will be read and overwritten.\n" +
            "     DO NOT EDIT! -->\n" +
            "<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">\n" +
            "<TITLE>Bookmarks</TITLE>\n" +
            "<H1>Bookmarks</H1>\n" +
            "<DL>%s</DL>"

    fun import() {

    }

    /**
     * 导出书签方法
     *
     * 注意：
     * 1.该方法未开新线程进行耗时操作，请自行在异步线程调用;
     * 2.该方法涉及文件写入操作，因此Android 6.0+ (API 23)上需要动态申请存储写入权限
     */
    fun export() {

        val stringBuilder = StringBuilder()
        val pins = SQLHelper.findAllPins()
        pins.forEach {
            val item = ITEM.replace(URL, it.url).replace(TIME, it.time).replace(TITLE, it.title)
            stringBuilder.append(item)
        }

        val fileString = TEMPLATE.format(stringBuilder)

        val file = File(Environment.getExternalStorageDirectory(),
                FILE_NAME.format(SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())))
        FileWriter(file).use {
            it.write(fileString)
            it.flush()
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

/**
 * 源代码来自：https://github.com/mthli/Ninja/blob/master/Ninja/src/io/github/mthli/Ninja/View/SwipeToBoundListener.java
 * Created by zhongzilu on 2018-10-18
 */
class SwipeToBoundListener(private val view: View, private val callback: BoundCallback) : View.OnTouchListener {

    private var targetWidth = 1
    private val slop: Int
    private val animTime: Long

    private var downX: Float = 0.toFloat()
    private var translationX: Float = 0.toFloat()
    private var swiping: Boolean = false
    private var swipingLeft: Boolean = false
    private var canSwitch: Boolean = false
    private var swipingSlop: Int = 0
    private var velocityTracker: VelocityTracker? = null

    interface BoundCallback {
        fun canSwipe(): Boolean
        fun onSwipe()
        fun onBound(canSwitch: Boolean, left: Boolean)
    }

    init {
        val configuration = ViewConfiguration.get(view.context)
        slop = configuration.scaledTouchSlop
        animTime = view.context.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        swiping = false
        swipingLeft = false
        canSwitch = false
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (!callback.canSwipe()) {
            return false
        }

        event.offsetLocation(translationX, 0f)
        if (targetWidth < 2) {
            targetWidth = view.width
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX

                velocityTracker = VelocityTracker.obtain()
                velocityTracker!!.addMovement(event)

                return false
            }
            MotionEvent.ACTION_UP -> {
                if (velocityTracker == null) {
                    return false
                }

                velocityTracker!!.addMovement(event)
                velocityTracker!!.computeCurrentVelocity(1000)

                if (swiping) {
                    view.animate()
                            .translationX(0f)
                            .setDuration(animTime)
                            .setListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    callback.onBound(canSwitch, swipingLeft)
                                }
                            })
                }

                downX = 0f
                translationX = 0f
                swiping = false
                velocityTracker!!.recycle()
                velocityTracker = null
            }
            MotionEvent.ACTION_CANCEL -> {
                if (velocityTracker == null) {
                    return false
                }

                view.animate()
                        .translationX(0f)
                        .setDuration(animTime)
                        .setListener(null)

                downX = 0f
                translationX = 0f
                swiping = false
                velocityTracker!!.recycle()
                velocityTracker = null
            }
            MotionEvent.ACTION_MOVE -> {
                if (velocityTracker == null) {
                    return false
                }

                velocityTracker!!.addMovement(event)

                val deltaX = event.rawX - downX
                if (Math.abs(deltaX) > slop) {
                    swiping = true
                    swipingLeft = deltaX < 0
                    canSwitch = Math.abs(deltaX) >= view.context.dip2px(48f) // Can switch tabs when deltaX >= 48 to prevent misuse
                    swipingSlop = if (deltaX > 0) slop else -slop
                    view.parent.requestDisallowInterceptTouchEvent(true)

                    val cancelEvent = MotionEvent.obtainNoHistory(event)
                    cancelEvent.action = MotionEvent.ACTION_CANCEL or (event.actionIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
                    view.onTouchEvent(cancelEvent)
                    cancelEvent.recycle()
                }

                if (swiping) {
                    translationX = deltaX
                    view.translationX = deltaX - swipingSlop
                    callback.onSwipe()

                    return true
                }
            }
        }

        return false
    }
}