package zzl.kotlin.ninja2.application

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.json.JSONObject
import zzl.kotlin.ninja2.R
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
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
    val SEARCH_ENGINE_GITHUB = "https://github.com/search?q=%s"

    const val UA_DESKTOP = "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36"
    const val URL_ENCODE = "UTF-8"

    const val URL_PREFIX_GOOGLE_PLAY = "www.google.com/url?q="
    const val URL_SUFFIX_GOOGLE_PLAY = "&sa"
    const val URL_PREFIX_GOOGLE_PLUS = "plus.url.google.com/url?q="
    const val URL_SUFFIX_GOOGLE_PLUS = "&rct"

    const val HEADER_CONTENT_DISPOSITION = "Content-Disposition: attachment;"

}

/**
 * 脚本注入工具类，包含注入脚本字符串和返回结果的解析方法
 * Created by zhongzilu on 2018-10-24
 */
object Evaluate {
    const val SCRIPT = "(function(){const a={theme_color:'',manifest:'',icons:[]};function d(){var g=document.querySelectorAll(\"link[rel='apple-touch-icon'],link[rel='apple-touch-icon-precomposed']\");var j=g.length;for(var e=0;e<j;e++){var f={sizes:'',type:'',src:''};var h=g[e];if(h.hasAttribute('sizes')){f.sizes=h.sizes[0]}f.type=h.rel;f.src=h.href;a.icons.push(f)}}function b(){var e=document.querySelector(\"meta[name='theme-color']\");if(e){a.theme_color=e.content}}function c(){var e=document.querySelector(\"link[rel='manifest']\");if(e){a.manifest=e.href;return true}return false}if(!c()){b();d()}return a})();"

    private const val THEME_NAME = "theme_color"
    private const val ICONS_NAME = "icons"
    private const val MANIFEST_NAME = "manifest"
    private const val ICON_SRC_NAME = "src"
    private const val ICON_SIZE_NAME = "sizes"
    private const val ICON_TYPE_NAME = "type"

    /**
     * 该数据类是对应[SCRIPT]脚本注入之后返回的json字符串，
     * 该数据类包含网站主题色配置、网站favicon不同尺寸图标的集合、针对Android平台设置的配置json文件路径
     */
    data class Result(
            var theme_color: String,
            var manifest: String,
            var icons: ArrayList<Icon>
    )

    /**
     * 网站针对Android平台特有的配置化文件，配置文件格式由谷歌制定，
     * 采用类似<link rel="manifest" href="http://www.example.com/xxx/manifest.json"/>的标签格式进行添加。
     * 截至2018-10-24，大部分网站都没有添加该配置，
     * 且不同网站的manifest.json文件中的配置字段可能不太一样，这里数据类的字段是根据
     * <a href="http://www.juejin.com/" target="_blank">掘金</a>网站的配置信息进行编写的，
     * 仅供参考！
     */
    data class Manifest(
//            var name: String,
            var icons: ArrayList<Icon>,
//            var background_color: String,
//            var display: String,
            var theme_color: String
    )

    /**
     * 网站图标数据类，包含图标地址，图标的尺寸信息，以及图标类型。
     * 该数据类中的[Icon.type]并没有使用到，可以考虑去掉；
     * [Icon.sizes]字段的格式为：NxN。比如：32x32，该尺寸信息需要再次处理后才能使用；
     * [Icon.src]字段为图标的网络地址，可以直接用于ImageView的显示
     */
    data class Icon(
            var src: String,
            var sizes: String,
            var type: String
    )

    /**
     * 用来解析脚本[SCRIPT]注入之后返回的JSON数据.
     * JSON数据主要包括网站主题色、网站图标集合、Android端特有的manifest.json配置文件。
     *
     * 由于manifest.json文件中已经定义了很全面的配置信息，因此如果网站上有该配置，
     * 则不需要再获取其他配置信息了。
     *
     * @param json
     * @return Evaluate#Result
     */
    fun parseResult(json: String?): Result {
        val result = Result("", "", arrayListOf())
        json?.apply { if (isEmpty()) return result } ?: return result

        JSONObject(json).apply {
            //parse manifest
            optString(MANIFEST_NAME).apply {
                if (isNotEmpty()) {
                    result.manifest = this
                    return result
                }
            }

            //parse theme color
            result.theme_color = optString(THEME_NAME)

            //parse icons
            optJSONArray(ICONS_NAME)?.apply {
                for (i in 0 until length()) {
                    result.icons.add(parseIcon(this[i] as JSONObject))
                }
            }
        }

        return result
    }

    /**
     * 将字符串解析成Icon对象，
     * 该方法可以用来解析manifest.json文件中的icon集合，
     * 也可以用来解析脚本[SCRIPT]注入之后返回的[ICONS_NAME]字段数据
     *
     * @param json
     * @return Evaluate#Icon
     */
    fun parseIcon(json: JSONObject?): Icon {
        val icon = Icon("", "", "")
        if (json == null) return icon

        json.apply {
            icon.src = optString(ICON_SRC_NAME)
            icon.sizes = optString(ICON_SIZE_NAME)
            icon.type = optString(ICON_TYPE_NAME)
        }

        return icon
    }

    /**
     * 解析manifest.json文件中的字段，该方法只解析了网站主题色字段以及网站favicon图标集合
     * 如要添加解析字段，可以自行添加修改
     */
    fun parseManifest(json: String): Manifest {
        val result = Manifest(arrayListOf(), "")
        if (json.isEmpty()) return result

        JSONObject(json).apply {
            result.theme_color = optString(THEME_NAME)

            // parse manifest icons
            optJSONArray(ICONS_NAME)?.apply {
                for (i in 0 until length()) {
                    result.icons.add(parseIcon(this[i] as JSONObject))
                }
            }
        }

        return result
    }

    /**
     * 解析[Evaluate.Icon]的[Icon.sizes]字段，将NxN格式的字段串解析并截取出尺寸信息。
     * 例：32x32的字符串，解析后将返回整数型32
     *
     * @param size 尺寸信息字段串，该字符串符合格式：NxN
     * @return 解析出的尺寸信息
     */
    fun parseSize(size: String): Int {
        return try {
            size.split("x")[0].toInt()
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }
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

    const val BASE64 = "data:image/"
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
     * 初始化广告拦截名单，如果[hostSet]不为空，则忽略，不再重新加载数据
     */
    fun init(context: Context) {
        if (hostSet.isNotEmpty()) return

        doAsync {
            context.assets.open(FILE_NAME).reader().useLines {
                hostSet.add(it.toString().toLowerCase(locale))
            }
//            val bufferedReader = BufferedReader(InputStreamReader(context.assets.open(FILE_NAME)))
//            while (true) {
//                val readLine = bufferedReader.readLine()
//                if (readLine != null) {
//                    hostSet.add(readLine.toLowerCase(locale))
//                } else break
//            }
        }
    }

    /**
     * 获取url的Host地址，有一定的概率会获取失败，抛出异常
     * @throws URISyntaxException 如果[java.net.URI]类解析失败，则会抛出该异常
     */
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
     *
     * @throws NullPointerException 如果再调用该方法前没有调用init()方法的话，就会抛出该异常
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
        val guessFileName = parseFileName(url, contentDisposition, mimeType)
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

    /**
     * 解析下载文件的文件名
     * @param url 下载地址
     * @param contentDisposition 内容描述
     * @param mimeType 文件类型
     */
    fun parseFileName(url: String, contentDisposition: String, mimeType: String): String {
        return URLUtil.guessFileName(url, contentDisposition, mimeType).apply {
            if (endsWith(".bin") && url.indexOf(".") > 0) {
                // 临时解决下载文件后缀名不匹配的问题
                val suffix = url.substring(url.lastIndexOf("."))
                replace(".bin", suffix)
            }
        }
    }
}

/**
 * 书签导入导出辅助工具类
 * Created by zhongzilu on 2018-10-09
 */
object Bookmark {

    const val SUFFIX = ".html"
    const val FILE_NAME = "Ninja2.%s.html"
    const val ITEM = "<DT><A HREF=\"{url}\" ADD_DATE=\"{time}\">{title}</A>\n"
    const val TITLE = "{title}"
    const val URL = "{url}"
    const val TIME = "{time}"
    const val TEMPLATE = "<!DOCTYPE NETSCAPE-Bookmark-file-1>\n" +
            "<!-- This is an automatically generated file.\n" +
            "     It will be read and overwritten.\n" +
            "     DO NOT EDIT! -->\n" +
            "<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">\n" +
            "<META NAME=\"viewport\" CONTENT=\"width=device-width, initial-scale=1.0\">\n" +
            "<TITLE>Bookmarks</TITLE>\n" +
            "<H1>Bookmarks</H1>\n" +
            "<DL>\n%s</DL>"

    /**
     * 导入书签
     *
     * <p><b>注意：
     * 1.该方法未开新线程进行文件IO耗时操作，请自行在异步线程调用
     * 2.该方法涉及文件读取操作，因此Android 6.0+ (API 23)上需要动态申请存储读取权限</b></p>
     */
    fun import(uri: Uri) {
        val file = File(URI(uri.toString()))

        FileReader(file).forEachLine {
            if (it.trim().startsWith("<DT><A ", true)) {

                val title = readTitle(it)
                val url = readUrl(it)

                SQLHelper.savePin(title, url)
            }
        }
    }

    /**
     * 导出书签方法
     *
     * <p><b>注意：
     * 1.该方法未开新线程进行耗时操作，请自行在异步线程调用;
     * 2.该方法涉及文件写入操作，因此Android 6.0+ (API 23)上需要动态申请存储写入权限</b></p>
     */
    fun export(): File {

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

        return file
    }

    /**
     * 读取书签标题
     */
    private fun readTitle(line: String): String {
        val split = line.removeSuffix("</A>").split(">")
        return split[split.size - 1]
    }

    /**
     * 读取书签Url
     */
    private fun readUrl(line: String): String {
        line.split(" ").forEach {
            if (it.startsWith("HREF=\"", true)) {
                return it.substring(6, it.length - 1)
            }
        }
        return ""
    }
}

/**
 * 二维码辅助工具类，用于解析二维码内容和判断网络地址是否为二维码图片，
 */
object QR {

    /**
     * 解析Bitmap，判断是否为二维码图片
     * @param bitmap 图片Bitmap
     * @return String? 如果bitmap为二维码则返回解析结果，否则返回null
     */
    fun decodeBitmap(bitmap: Bitmap): String? {
        val hints = HashMap<DecodeHintType, Any>()

        //添加可以解析的编码类型
        hints[DecodeHintType.POSSIBLE_FORMATS] = Vector<BarcodeFormat>().apply {
            addAll(DecodeFormatManager.ONE_D_FORMATS)
            addAll(DecodeFormatManager.QR_CODE_FORMATS)
            addAll(DecodeFormatManager.DATA_MATRIX_FORMATS)
            addAll(DecodeFormatManager.AZTEC_FORMATS)
            addAll(DecodeFormatManager.PDF417_FORMATS)
        }

        return parseCode(bitmap, hints)
    }

    /**
     * 解析图片地址，判断是否为二维码图片,该图片地址有可能为base64格式的字符串，
     * @param url 图片网络地址
     * @return String? 如果为二维码图片则返回解析结果，如果不是二维码图片则返回null
     */
    fun decodeUrl(url: String): String? {
        var bitmap = url.base64ToBitmap()

        if (bitmap == null) {
            bitmap = BitmapFactory.decodeStream(URL(url).openStream())
        }

        if (bitmap == null) return null

        return decodeBitmap(bitmap)
    }

    /**
     * 解析一维码/二维码图片
     * @param bitmap
     * @param hints 解析编码类型
     * @return String? 解析的字符串可能为空k
     */
    fun parseCode(bitmap: Bitmap, hints: Map<DecodeHintType, Any>): String? {
        try {
            val reader = MultiFormatReader()
            reader.setHints(hints)
            return reader.decodeWithState(getBinaryBitmap(bitmap)).text
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 获取二进制图片
     * @param bitmap
     * @return
     */
    private fun getBinaryBitmap(bitmap: Bitmap): BinaryBitmap {
        val width = bitmap.width
        val height = bitmap.height

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source = RGBLuminanceSource(width, height, pixels)
        //得到二进制图片
        return BinaryBitmap(HybridBinarizer(source))
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