package zzl.kotlin.ninja2.widget

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.layout_add_to_launcher.*
import zzl.kotlin.ninja2.R
import zzl.kotlin.ninja2.application.Func
import zzl.kotlin.ninja2.application.Func1
import zzl.kotlin.ninja2.application.addTextWatcher

/**
 * 将网站添加到桌面图标快捷方式
 * Created by zhongzilu on 2018/9/29
 */
class AddLauncherDialog(context: Context) : Dialog(context, R.style.AppTheme_Dialog) {

    private var mNegativeBtn: TextView? = null
    private var mPositiveBtn: TextView? = null
    private var mLauncherIcon: ImageView? = null
    private var mSelectBtn: TextView? = null
    private var mLabelEdit: EditText? = null

    init {
        setContentView(R.layout.layout_add_to_launcher)
        //初始化界面控件
        initView()
        //初始化界面控件的事件
        initEvent()
    }

    /**
     * 桌面快捷方式访问地址
     */
    private var url: String = ""

    fun setUrl(url: String): AddLauncherDialog {
        this.url = url
        return this
    }

    fun getUrl() = url

    /**
     * 桌面图标标题文字
     */
    private var mLabelText: CharSequence? = null

    /**
     * 获取桌面图标标题
     *
     * @return String 桌面图标标题
     */
    fun getTitle() = mLabelText?.toString() ?: ""

    /**
     * 设置桌面图标标题
     *
     * @param title 标题
     */
    fun setLabel(title: CharSequence?) = apply { mLabelText = title }

    /**
     * 设置桌面图标标题
     *
     * @param titleId 资源ID
     */
    override fun setTitle(titleId: Int) {
        mLabelText = context.resources.getString(titleId)
    }

    /**
     * 设置桌面图标标题
     *
     * @param title 标题
     */
    override fun setTitle(title: CharSequence?) {
        mLabelText = title
    }

    /**
     * 桌面图标Bitmap
     */
    private var mLauncherBitmap: Bitmap? = null

    /**
     * 获取桌面图标Bitmap
     *
     * @return Bitmap？桌面图标Bitmap，可能为空
     */
    fun getIcon(): Bitmap = mLauncherBitmap
            ?: Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888)

    /**
     * 设置桌面图标Bitmap
     *
     * @param bitmap 桌面图标bitmap
     */
    fun setIcon(bitmap: Bitmap?) = apply {
        mLauncherBitmap = bitmap
        mLauncherBitmap?.let { mLauncherIcon?.setImageBitmap(it) }
    }

    private fun initView() {
        mLauncherIcon = icon
        mSelectBtn = select
        mLabelEdit = label
        mNegativeBtn = negativeBtn
        mPositiveBtn = positiveBtn
    }

    private fun initEvent() {
        mSelectBtn?.setOnClickListener {
            _selectListener?.invoke()
        }
        mLauncherIcon?.setOnClickListener {
            _selectListener?.invoke()
        }

        mNegativeBtn?.setOnClickListener {
            mNegativeListener?.invoke(this)
            dismiss()
        }

        mPositiveBtn?.setOnClickListener {
            mPositiveListener?.invoke(this)
            dismiss()
        }

        mLabelEdit?.addTextWatcher {
            mPositiveBtn?.isEnabled = it.isNotEmpty()
        }
    }

    /**
     * 选择更换图标的监听器
     */
    private var _selectListener: Func? = null

    /**
     * 设置选择更换图标的监听器
     */
    fun setOnSelectListener(todo: Func) = apply { _selectListener = todo }

    private var mPositiveListener: Func1<AddLauncherDialog>? = null
    private var mPositiveBtnText: String = "确定"
    fun setOnPositiveClickListener(text: String = "确定", todo: Func1<AddLauncherDialog>) = apply {
        mPositiveBtnText = text
        mPositiveListener = todo
    }

    private var mNegativeListener: Func1<AddLauncherDialog>? = null
    private var mNegativeBtnText: String = "取消"
    fun setOnNegativeClickListener(text: String = "取消", todo: Func1<AddLauncherDialog>) = apply {
        mNegativeBtnText = text
        mNegativeListener = todo
    }

    override fun show() {
        if (mLabelText != null) {
            mLabelEdit?.setText(mLabelText)
        }

        if (mLauncherBitmap != null) {
            mLauncherIcon?.setImageBitmap(mLauncherBitmap)
        }

        if (mNegativeBtnText.isNotEmpty()) {
            mNegativeBtn?.text = mNegativeBtnText
        }
        if (mPositiveBtnText.isNotEmpty()) {
            mPositiveBtn?.text = mPositiveBtnText
        }

        super.show()
    }
}