package zzl.kotlin.ninja2

import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_page.*
import kotlinx.android.synthetic.main.content_bottom_sheet.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.layout_page_menu.*
import zzl.kotlin.ninja2.application.go
import zzl.kotlin.ninja2.application.hide
import zzl.kotlin.ninja2.application.hideKeyboard
import zzl.kotlin.ninja2.application.show
import zzl.kotlin.ninja2.widget.MenuOptionListener


class PageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page)
        setSupportActionBar(toolbar)

        initEvent()

//        initPinRecycler()

    }

    /**
     * 初始化Pin的RecyclerView，由于采用了ViewStub来延迟加载
     * 因此尽量需要的时候才调用
     */
    private var mPinsRecycler: RecyclerView? = null
    private fun initPinRecycler() {
        if (mPinsRecycler == null){
            mPinsRecycler = mPinsStub.inflate() as RecyclerView
        }


    }

    /**
     * 初始化Record的RecyclerView，由于采用了ViewStub来延迟加载
     * 因此尽量需要的时候才调用
     */
    private var mRecordRecycler: RecyclerView? = null
    private fun initRecordRecycler(){
        if (mRecordRecycler == null){
            mRecordRecycler = mRecordsStub.inflate() as RecyclerView
        }

    }

    private lateinit var mBottomSheetBehavior: BottomSheetBehavior<*>
    private fun initEvent() {
        // Bottom Sheet content layout views
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout)

        // Capturing the callbacks for bottom sheet
        mBottomSheetBehavior.setBottomSheetCallback(mBottomSheetCallback)

        mMenuOptionWidget.setMenuOptionListener(mMenuOptionListener)
        mInputBox.addTextChangedListener(mTextWatcher)
        mInputBox.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == KeyEvent.KEYCODE_ENTER){

            }
            return@setOnEditorActionListener true
        }
    }

    /**
     * 地址栏输入框文字变化监听
     */
    private val mTextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }
    }

    /**
     * 底部菜单的展开/关闭状态监听器
     * 当拖拽底部菜单时，需要动态改变灰色背景{@link #mMaskView}的透明度
     */
    private val mBottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {

        override fun onStateChanged(bottomSheet: View, newState: Int) {

            //展开状态
            if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                afterBottomSheetExpanded()
            }

            //关闭状态
            else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                afterBottomSheetCollapsed()
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            val alpha = slideOffset * 0.3f
            mMaskView.alpha = if (alpha < 0) 0f else alpha
        }
    }

    /**
     * 设置遮罩触摸监听器
     * 展开底部菜单时出现的灰色背景遮罩，当触摸灰色遮罩时需要关闭底部菜单栏
     */
    private val mMaskTouchListener = View.OnTouchListener { v, _ ->
        if (v.id == R.id.mMaskView){
            closeBottomSheet()
            return@OnTouchListener true
        }

        return@OnTouchListener false
    }
    /**
     * 底部菜单展开后执行
     */
    private fun afterBottomSheetExpanded() {
        mInputBox.isFocusable = false
        mInputBox.isFocusableInTouchMode = false
        mInputBox.clearFocus()
        mMaskView.setOnTouchListener(mMaskTouchListener)
        mInputBox.hideKeyboard()
    }

    /**
     * 底部菜单关闭后执行
     */
    private fun afterBottomSheetCollapsed() {
        mInputBox.isFocusable = true
        mInputBox.isFocusableInTouchMode = true
        mInputBox.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        mInputBox.clearFocus()
        mMaskView.setOnTouchListener(null)

        mCollapsedRunnable?.let {
            it.run()
            mCollapsedRunnable = null
        }
    }

    /**
     * 关闭底部菜单后执行Runnable任务
     */
    private var mCollapsedRunnable: Runnable? = null
    private fun closeBottomSheet(runnable: Runnable? = null) {
        this.mCollapsedRunnable = runnable
        mBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    /**
     * 底部菜单栏各选项的事件监听回调
     */
    private val mMenuOptionListener = object : MenuOptionListener{
        override fun onDesktopCheckedChanged(check: Boolean) {
        }

        override fun onCustomUACheckedChanged(check: Boolean) {
        }

        override fun onScreenshotsClick() {
        }

        override fun onShareUrlClick() {
        }

        override fun onNewTabClick() {
        }

        override fun onPrivateTabClick() {
        }

        override fun onPinToHome() {
        }

        override fun addToLauncher() {
        }

        override fun onSettingsClick() {
            go<SettingsActivity>()
        }
    }

    private lateinit var mRefreshMenu: MenuItem
    private lateinit var mStopMenu: MenuItem
    private lateinit var mConfirmMenu: MenuItem
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_page, menu)
        mRefreshMenu = menu.findItem(R.id.refresh)
        mStopMenu = menu.findItem(R.id.stop)
        mConfirmMenu = menu.findItem(R.id.confirm)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.refresh -> {}
            R.id.stop -> {}
            R.id.confirm -> {}
        }
        return true
    }

    /**
     * 显示截图等待视图
     */
    private var mLoadingView: View? = null
    fun showLoading(){
        if (mLoadingView == null) {
            mLoadingView = mLoadingStub.inflate()
        }

        mLoadingView?.show()
    }

    /**
     * 隐藏截图等待视图
     */
    fun hideLoading(){
        mLoadingView?.let {
            if (it.visibility == View.VISIBLE){
                it.hide()
            }
        }
    }
}