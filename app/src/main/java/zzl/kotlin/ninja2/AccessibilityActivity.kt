package zzl.kotlin.ninja2

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_assistant_layout.*

/**
 * Created by zhongzilu on 2018-11-13
 */
class AccessibilityActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assistant_layout)
        enableBackPress()

        mTextScaleSeekBar.setOnSeekBarChangeListener {
            mTextScaleText.text = "$it%"
            mWebViewDemo.settings.textZoom = it
        }

        mWebViewDemo.loadUrl("file:///android_asset/www/text-scale.html")

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
//        mTextSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
//            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
//                mTextSizeText.text = "${progress}PT"
//            }
//
//            override fun onStartTrackingTouch(seekBar: SeekBar?) {
//            }
//
//            override fun onStopTrackingTouch(seekBar: SeekBar?) {
//            }
//
//        })
    }
}