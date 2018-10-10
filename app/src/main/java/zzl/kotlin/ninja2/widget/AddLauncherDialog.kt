package zzl.kotlin.ninja2.widget

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.layout_add_to_launcher.*
import zzl.kotlin.ninja2.R

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

    fun setUrl(url: String): AddLauncherDialog{
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
    fun getTitle() = mLabelText?.toString()?:""

    /**
     * 设置桌面图标标题
     *
     * @param title 标题
     */
    fun setLabel(title: CharSequence?): AddLauncherDialog {
        mLabelText = title
        return this
    }

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
    fun getIcon(): Bitmap = mLauncherBitmap!!

    /**
     * 设置桌面图标Bitmap
     *
     * @param bitmap 桌面图标bitmap
     */
    fun setIcon(bitmap: Bitmap): AddLauncherDialog {
        mLauncherBitmap = bitmap
        mLauncherIcon?.setImageBitmap(mLauncherBitmap)
        return this
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

        mLabelEdit?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                mPositiveBtn?.isEnabled = s?.isNotEmpty() ?: false
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        })
    }

    /**
     * 选择更换图标的监听器
     */
    private var _selectListener: (() -> Unit)? = null

    /**
     * 设置选择更换图标的监听器
     */
    fun setOnSelectListener(todo: () -> Unit): AddLauncherDialog {
        _selectListener = todo
        return this
    }

    private var mPositiveListener: ((v: AddLauncherDialog) -> Unit)? = null
    private var mPositiveBtnText: String = "确定"
    fun setOnPositiveClickListener(text: String = "确定", todo: ((v: AddLauncherDialog) -> Unit)?): AddLauncherDialog {
        mPositiveBtnText = text
        mPositiveListener = todo
        return this
    }

    private var mNegativeListener: ((v: AddLauncherDialog) -> Unit)? = null
    private var mNegativeBtnText: String = "取消"
    fun setOnNegativeClickListener(text: String = "取消", todo: ((v: AddLauncherDialog) -> Unit)?): AddLauncherDialog {
        mNegativeBtnText = text
        mNegativeListener = todo
        return this
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