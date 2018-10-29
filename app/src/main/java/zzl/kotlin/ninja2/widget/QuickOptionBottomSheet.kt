package zzl.kotlin.ninja2.widget

import android.content.Context
import android.support.design.widget.BottomSheetDialog
import android.widget.Switch
import android.widget.TextView
import kotlinx.android.synthetic.main.layout_quick_option.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import zzl.kotlin.ninja2.R
import zzl.kotlin.ninja2.application.*

/**
 * 长按网页弹出的快捷菜单
 * Created by zhongzilu on 2018-10-12
 */
class QuickOptionDialog(context: Context) : BottomSheetDialog(context, R.style.AppTheme_BottomSheetDialog) {

    init {
        setContentView(R.layout.layout_quick_option)
        //初始化界面控件
        initView()
        //初始化界面控件的事件
        initEvent()
    }

    /**
     * 是否开启快捷操作的开关。true为是，false为否
     */
    private lateinit var mOptionSwitch: Switch

    /**
     * 打开新标签页的菜单选项
     */
    private lateinit var mNewTab: TextView

    /**
     * 打开私有标签页的菜单选项
     */
    private lateinit var mNewPrivateTab: TextView

    /**
     * 下载图片菜单选项，默认为不可见{@link View#GONE}
     */
    private lateinit var mDownloadImg: TextView

    /**
     * 识别二维码菜单选项，默认为不可见{@link View#GONE}
     */
    private lateinit var mExtractQRImg: TextView

    /**
     * 拷贝链接菜单选项
     */
    private lateinit var mCopyUrl: TextView

    /**
     * 分享链接菜单选项
     */
    private lateinit var mShareUrl: TextView

    private fun initView() {
        mOptionSwitch = optionSwitch
        mNewTab = newTab
        mNewPrivateTab = privateTab
        mDownloadImg = downloadImg
        mExtractQRImg = decodeQRImg
        mCopyUrl = copyUrl
        mShareUrl = shareUrl
    }

    private var _method: ((String) -> Unit)? = null
        get() {
            val m = field
            field = null
            return m
        }

    private fun initEvent() {

        //set switch event
        mOptionSwitch.isChecked = SP.isOpenInBackground
        mOptionSwitch.setOnCheckedChangeListener { _, isChecked ->
            SP.isOpenInBackground = isChecked
            context?.toast(if (isChecked) R.string.options_background else R.string.options_foreground)
        }

        //set new tab option click even
        mNewTab.setOnClickListener {
            baseCallback?.let { _method = it.quickNewTab }
            dismiss()
        }

        //set new private tab option click even
        mNewPrivateTab.setOnClickListener {
            baseCallback?.let { _method = it.quickNewPrivateTab }
            dismiss()
        }

        //set download image option click even
        mDownloadImg.setOnClickListener {
            baseCallback?.let { _method = it.quickDownloadImg }
            dismiss()
        }

        //set extract QR code option click event
        mExtractQRImg.setOnClickListener {
            baseCallback?.quickExtractQR?.invoke(mExtractRes)
            dismiss()
        }

        //set copyToClipboard url option click even
        mCopyUrl.setOnClickListener {
            baseCallback?.let { _method = it.quickCopyUrl }
            dismiss()
        }

        //set share url option click even
        mShareUrl.setOnClickListener {
            baseCallback?.let { _method = it.quickShareUrl }
            dismiss()
        }

        //set dismiss listener
        setOnDismissListener { _method?.invoke(_url) }
    }

    private var _url = ""

    /**
     * 设置Url
     *
     * @url 这里是设置网页上长按获取到的连接
     */
    fun setUrl(url: String): QuickOptionDialog {
        _url = url.trim()
        parseUrl(url)
        return this
    }

    private var isQRImage = false
    private var mExtractRes = ""
    private fun parseUrl(url: String) {
        doAsync {
            val res = QR.decodeUrl(url)
            if (res == null) {
                isQRImage = false
                mExtractRes = ""
                uiThread { mExtractQRImg.gone() }
            } else {
                isQRImage = true
                mExtractRes = res
                uiThread { mExtractQRImg.visible() }
            }
        }
    }

    /**
     * 设置下载图片菜单选项显示
     */
    fun isImageUrl(bool: Boolean): QuickOptionDialog {
        if (bool) {
            mDownloadImg.visible()
        } else {
            mDownloadImg.gone()
        }
        return this
    }

    /**
     * 监听器包装类
     */
    private var baseCallback: QuickCallbackWrap? = null

    /**
     * 设置各个选项的监听器回调
     */
    fun setQuickListener(init: (QuickCallbackWrap.() -> Unit)): QuickOptionDialog {
        baseCallback = QuickCallbackWrap()
        baseCallback!!.context = context
        baseCallback!!.init() // 执行闭包，完成数据填充
        return this // 用于返回
    }
}

class QuickCallbackWrap {

    internal lateinit var context: Context

    var quickNewTab: ((String) -> Unit) = {}
    var quickNewPrivateTab: ((String) -> Unit) = {}
    var quickDownloadImg: ((String) -> Unit) = {}
    var quickExtractQR: ((String) -> Unit) = {}
    var quickCopyUrl: ((String) -> Unit) = {
        context.copyToClipboard(it)
    }
    var quickShareUrl: ((String) -> Unit) = {
        context.shareText(it)
    }

//    fun onQuickNewTab(onQuickNewTab: () -> Unit) {
//        quickNewTab = onQuickNewTab
//    }
//
//    fun onQuickNewPrivateTab(privateTab: () -> Unit) {
//        quickNewPrivateTab = privateTab
//    }
//
//    fun onQuickDownloadImage(downloadImg: () -> Unit) {
//        quickDownloadImg = downloadImg
//    }
//
//    fun onQuickCopyUrl(copyUrl: () -> Unit) {
//        quickCopyUrl = copyUrl
//    }
//
//    fun onQuickShareUrl(shareUrl: () -> Unit) {
//        quickShareUrl = shareUrl
//    }

}
