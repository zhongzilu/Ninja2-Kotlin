package zzl.kotlin.ninja2.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.SeekBar
import zzl.kotlin.ninja2.R

/**
 * 自定义的SeekBar，实现设置拖拽Step数值，即每次拖动的变化值为[mStep]
 * Created by zhongzilu on 2018-11-13
 */
class ZSeekBar : SeekBar {

    constructor(context: Context) : super(context)

    constructor(context: Context, attr: AttributeSet) : super(context, attr) {
        getAttrs(attr)
    }

    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr) {
        getAttrs(attr)
    }

    private var mStep: Int = 1
    private var mMin: Int = 0
    private var mMax: Int = 0
    private var _progress: Int = 1

    init {
        mMax = max
        max = (mMax - mMin) / mStep
        _progress = progress

        progress = (mMax - mMin) / _progress

        super.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                _progress = progress / mStep + mMin
                _listener?.invoke(_progress)
                mSeekChangeListener?.onProgressChanged(seekBar, _progress, fromUser)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                mSeekChangeListener?.onStartTrackingTouch(seekBar)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                mSeekChangeListener?.onStopTrackingTouch(seekBar)
            }

        })
    }

    private fun getAttrs(attr: AttributeSet) {
        context.obtainStyledAttributes(attr, R.styleable.ZSeekBar).apply {
            mStep = getInteger(R.styleable.ZSeekBar_step, 1)
            mMin = getInteger(R.styleable.ZSeekBar_min, 0)
            recycle()
        }
    }

    private var mSeekChangeListener: OnSeekBarChangeListener? = null
    override fun setOnSeekBarChangeListener(l: OnSeekBarChangeListener?) {
        mSeekChangeListener = l
    }

    private var _listener: ((Int) -> Unit)? = null
    fun setOnSeekBarChangeListener(f: (Int) -> Unit) {
        _listener = f
    }

    fun getStep() = mStep

    fun setStep(step: Int) {
        mStep = step
    }

    override fun getMax() = mMax

    override fun setMax(max: Int) {
        mMax = max
    }

    override fun getMin() = mMin

    override fun setMin(min: Int) {
        mMin = min
    }

    override fun getProgress() = _progress / mStep + mMin

}