package zzl.kotlin.ninja2.widget

import android.app.Dialog
import android.content.Context
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import kotlinx.android.synthetic.main.layout_auth_login.*
import zzl.kotlin.ninja2.R
import zzl.kotlin.ninja2.application.visible

/**
 * 网页认证登录弹出框
 * Created by zhongzilu on 2018-10-16
 */
class AuthLoginBottomSheet(context: Context) : Dialog(context, R.style.AppTheme_Dialog) {

    init {
        setContentView(R.layout.layout_auth_login)
        //初始化界面控件
        initView()
        //初始化界面控件的事件
        initEvent()
    }

    /**
     * 标题文本
     */
    private var mTitleText: CharSequence? = null

    /**
     * 标题文本，默认隐藏状态[android.view.View.GONE]
     */
    private lateinit var mTitle: TextView

    /**
     * 登录用户名输入框
     */
    private lateinit var mUserNameEdit: EditText

    /**
     * 登录密码输入框
     */
    private lateinit var mPasswordEdit: EditText

    /**
     * 登录按钮
     */
    private lateinit var mLoginBtn: Button

    /**
     * 取消按钮
     */
    private lateinit var mCancelBtn: Button

    private fun initView() {
        mTitle = title
        mUserNameEdit = username
        mPasswordEdit = password
        mLoginBtn = loginBtn
        mCancelBtn = cancelBtn
    }

    /**
     * 设置桌面图标标题
     *
     * @param titleId 资源ID
     */
    override fun setTitle(titleId: Int) {
        mTitleText = context.resources.getString(titleId)
    }

    /**
     * 设置桌面图标标题
     *
     * @param title 标题
     */
    override fun setTitle(title: CharSequence?) {
        mTitleText = title
    }

    /**
     * 获取输入的登录用户名
     * @return 输入的登录用户名
     */
    fun getUserName(): String = mUserNameEdit.text.toString().trim()

    /**
     * 获取输入的登录密码
     * @return 输入的登录密码
     */
    fun getPassword(): String = mPasswordEdit.text.toString().trim()

    private var _method: ((v: AuthLoginBottomSheet) -> Unit)? = null
    private fun initEvent() {

        setCancelable(false)

        //set download image option click even
        mLoginBtn.setOnClickListener {
            if (validate()) {
                _method = mPositiveListener
                dismiss()
            }
        }

        //set copyToClipboard url option click even
        mCancelBtn.setOnClickListener {
            _method = mNegativeListener
            dismiss()
        }

        //set dismiss listener
        setOnDismissListener {
            _method?.invoke(this@AuthLoginBottomSheet)
            _method = null
        }
    }

    /**
     * 验证输入内容
     */
    private fun validate(): Boolean {
        val name = mUserNameEdit.text.toString().trim()
        if (name.isEmpty()) {
            mUserNameEdit.apply {
                error = hint
                requestFocus()
            }

            return false
        }

        val pass = mPasswordEdit.text.toString().trim()
        if (pass.isEmpty()) {
            mPasswordEdit.apply {
                error = hint
                requestFocus()
            }
            return false
        }

        return true
    }

    override fun show() {
        mTitleText?.let {
            if (it.isNotEmpty()) {
                mTitle.text = mTitleText
                mTitle.visible()
            }
        }

        super.show()
    }

    private var mPositiveListener: ((v: AuthLoginBottomSheet) -> Unit)? = null
    fun setOnPositiveClickListener(todo: ((v: AuthLoginBottomSheet) -> Unit)?) = apply { mPositiveListener = todo }

    private var mNegativeListener: ((v: AuthLoginBottomSheet) -> Unit)? = null
    fun setOnNegativeClickListener(todo: ((v: AuthLoginBottomSheet) -> Unit)?) = apply { mNegativeListener = todo }
}
