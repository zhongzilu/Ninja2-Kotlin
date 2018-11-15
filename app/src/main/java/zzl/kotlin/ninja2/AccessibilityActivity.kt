package zzl.kotlin.ninja2

import android.os.Bundle
import android.widget.SeekBar
import kotlinx.android.synthetic.main.activity_assistant_layout.*
import zzl.kotlin.ninja2.application.SP
import zzl.kotlin.ninja2.application.WebUtil
import zzl.kotlin.ninja2.widget.ZSeekBar

/**
 * 辅助功能设置页
 * Created by zhongzilu on 2018-11-13
 */
class AccessibilityActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assistant_layout)
        enableBackPress()

        initView()
        initEvent()
    }

    private fun initView() {
        val zoom = SP.textZoom
        val mini = SP.minimumFontSize

        val visualNames = resources.getStringArray(R.array.pref_text_size_choices)

        mWebViewDemo.apply {
            with(settings) {
                textZoom = zoom
                minimumFontSize = mini
            }

            loadDataWithBaseURL(null,
                    String.format(WebUtil.FONT_SIZE_PREVIEW_HTML_FORMAT, *visualNames),
                    WebUtil.MIME_TYPE_TEXT_HTML, WebUtil.URL_ENCODE,
                    null
            )
        }

        //set text zoom values
        mTextZoomText.text = "$zoom%"
        mTextZoomSeekBar.progress(zoom)

        //set text size values
        mTextSizeText.text = "${mini}PT"
        mTextSizeSeekBar.progress(mini)
    }

    private fun initEvent() {
        mTextZoomSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                mTextZoomText.text = "$progress%"
                mWebViewDemo.settings.textZoom = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (seekBar is ZSeekBar) {
                    SP.textZoom = seekBar.progress()
                }
            }

        })


//        mClickScaleSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
//            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
//                mClickScaleText.text = "$progress%"
//            }
//
//            override fun onStartTrackingTouch(seekBar: SeekBar?) {
//            }
//
//            override fun onStopTrackingTouch(seekBar: SeekBar?) {
//            }
//
//        })
//
        mTextSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                mTextSizeText.text = "${progress}PT"
                mWebViewDemo.settings.minimumFontSize = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar is ZSeekBar) {
                    SP.minimumFontSize = seekBar.progress()
                }
            }

        })
    }
}