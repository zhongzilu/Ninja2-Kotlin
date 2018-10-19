package zzl.kotlin.ninja2.widget

import android.content.Context
import android.support.design.widget.BottomSheetDialog
import android.widget.TextView
import kotlinx.android.synthetic.main.layout_export_import.*
import zzl.kotlin.ninja2.R

/**
 * 自定义书签导入导出底部菜单栏
 * Created by zhongzilu on 2018-10-18
 */
class ExportImportBottomSheet(context: Context) : BottomSheetDialog(context, R.style.AppTheme_BottomSheetDialog) {

    init {
        setContentView(R.layout.layout_export_import)
        //初始化界面控件
        initView()
        //初始化界面控件的事件
        initEvent()
    }

    /**
     * 确定按钮
     */
    private lateinit var mExportOption: TextView

    /**
     * 取消按钮
     */
    private lateinit var mImportOption: TextView

    private fun initView() {
        mExportOption = exportOption
        mImportOption = importOption
    }

    private var _method: (() -> Unit)? = null
    private fun initEvent() {

        //set download image option click even
        mExportOption.setOnClickListener {
            _method = mExportEvent
            dismiss()
        }

        //set copyToClipboard url option click even
        mImportOption.setOnClickListener {
            _method = mImportEvent
            dismiss()
        }

        //set dismiss listener
        setOnDismissListener {
            _method?.invoke()
            _method = null
        }
    }

    private var mExportEvent: (() -> Unit)? = null
    fun setOnExportOptionClick(todo: (() -> Unit)?): ExportImportBottomSheet {
        mExportEvent = todo
        return this
    }

    private var mImportEvent: (() -> Unit)? = null
    fun setOnImportOptionClick(todo: (() -> Unit)?): ExportImportBottomSheet {
        mImportEvent = todo
        return this
    }
}