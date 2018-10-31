package zzl.kotlin.ninja2.widget

import android.app.Dialog
import android.content.Context
import android.widget.Button
import android.widget.EditText
import kotlinx.android.synthetic.main.layout_custom_ua.*
import zzl.kotlin.ninja2.R

/**
 * 自定义UA弹出框
 * Created by zhongzilu on 2018-10-18
 */
class CustomUADialog(context: Context) : Dialog(context, R.style.AppTheme_Dialog) {

    init {
        setContentView(R.layout.layout_custom_ua)
        //初始化界面控件
        initView()
        //初始化界面控件的事件
        initEvent()
    }

    /**
     * UA编辑输入框
     */
    private lateinit var mUAEdit: EditText

    /**
     * 确定按钮
     */
    private lateinit var mConfirmBtn: Button

    /**
     * 取消按钮
     */
    private lateinit var mCancelBtn: Button

    private fun initView() {
        mUAEdit = uaEdit
        mConfirmBtn = confirmBtn
        mCancelBtn = cancelBtn
    }

    /**
     * 获取输入的UA
     * @return 输入的UA
     */
    fun getUA(): String = mUAEdit.text.toString().trim()

    private var mUA: String? = null
    fun setUA(ua: String) = apply { mUA = ua }

    override fun show() {
        mUA?.let { mUAEdit.setText(it) }
        super.show()
    }

    private var _method: ((v: CustomUADialog) -> Unit)? = null
    private fun initEvent() {

        //set download image option click even
        mConfirmBtn.setOnClickListener {
            _method = mPositiveListener
            dismiss()
        }

        //set copyToClipboard url option click even
        mCancelBtn.setOnClickListener {
            _method = mNegativeListener
            dismiss()
        }

        //set dismiss listener
        setOnDismissListener {
            _method?.invoke(this@CustomUADialog)
            _method = null
        }
    }

    private var mPositiveListener: ((v: CustomUADialog) -> Unit)? = null
    fun setOnPositiveClickListener(todo: ((v: CustomUADialog) -> Unit)?) = apply { mPositiveListener = todo }

    private var mNegativeListener: ((v: CustomUADialog) -> Unit)? = null
    fun setOnNegativeClickListener(todo: ((v: CustomUADialog) -> Unit)?) = apply { mNegativeListener = todo }
}
