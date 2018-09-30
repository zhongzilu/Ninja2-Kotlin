package zzl.kotlin.ninja2.widget

import android.content.Context
import android.content.SharedPreferences
import android.support.v4.widget.NestedScrollView
import android.util.AttributeSet
import android.view.View
import kotlinx.android.synthetic.main.layout_page_menu.view.*
import zzl.kotlin.ninja2.application.*

/**
 * Created by zhongzilu on 2018/7/27 0027.
 */
class MenuOptionLayout : NestedScrollView, SharedPreferences.OnSharedPreferenceChangeListener{

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var mListener: MenuOptionListener? = null

    fun setMenuOptionListener(listener: MenuOptionListener?) {
        mListener = listener
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        context.defaultSharePreferences().registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        context.defaultSharePreferences().unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == Key.UA){
            initUserAgent(true)
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        //截图菜单
        mScreenshotMenu.setOnClickListener {
            mListener?.onScreenshotsClick()
        }

        //分享菜单
        mShareUrlMenu.setOnClickListener {
            mListener?.onShareUrlClick()
        }

        //桌面模式菜单
        mDesktopSwitch.setOnCheckedChangeListener { _, isChecked ->
            mListener?.onDesktopCheckedChanged(isChecked)
        }

        //自定义UA
        mCustomUASwitch.setOnCheckedChangeListener{_, isChecked ->
            mListener?.onCustomUACheckedChanged(isChecked)
        }

        //新建页面菜单
        mNewTabMenu.setOnClickListener {
            mListener?.onNewTabClick()
        }

        //新建隐私页面
        mNewPrivateTabMenu.setOnClickListener {
            mListener?.onPrivateTabClick()
        }

        //添加到首页
        mPin2HomeMenu.setOnClickListener {
            mListener?.onPinToHome()
        }

        //添加到桌面快捷方式
        mAdd2LauncherMenu.setOnClickListener {
            mListener?.addToLauncher()
        }

        //设置菜单
        mSettingsMenu.setOnClickListener {
            mListener?.onSettingsClick()
        }

        initUserAgent(false)
    }

    private fun initUserAgent(check: Boolean) {
        if (SP.UA.isEmpty()){
            mDesktopSwitch.visible()
            mCustomUASwitch.gone()
        } else {
            mDesktopSwitch.gone()
            mCustomUASwitch.visible()
        }

        if (check){
            mDesktopSwitch.isChecked = false
            mCustomUASwitch.isChecked = false
        }
    }

    /**
     * 加载网页时，显示出隐藏的网页菜单
     */
    fun showMoreMenu(){
        mScreenshotMenu.visibility = if (SP.canScreenshot) View.VISIBLE else View.GONE
        mShareUrlMenu.visible()
        mPin2HomeMenu.visible()
        mAdd2LauncherMenu.visible()
    }

    /**
     * 退出网页浏览模式时，隐藏网页菜单
     */
    fun hideMoreMenu(){
        mScreenshotMenu.gone()
        mShareUrlMenu.gone()
        mPin2HomeMenu.gone()
        mAdd2LauncherMenu.gone()
    }
}

interface MenuOptionListener {
    fun onDesktopCheckedChanged(check: Boolean)

    fun onCustomUACheckedChanged(check: Boolean)

    fun onScreenshotsClick()

    fun onShareUrlClick()

    fun onNewTabClick()

    fun onPrivateTabClick()

    fun onPinToHome()

    fun addToLauncher()

    fun onSettingsClick()
}