package zzl.kotlin.ninja2.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.SeekBar
import zzl.kotlin.ninja2.R

/**
 * 自定义的SeekBar，实现设置拖拽Step数值，即每次拖动的变化值为[mStep]，
 * 以及设置最小值[mMin]
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
    private var _progress: Int = 1

    init {
        super.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                _progress = progress * mStep + mMin
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

    /**
     * 设置SeekBar步进值，默认步进值为1，
     * 设置之后，每次拖动进度条的变化值则为该值
     */
    fun setStep(step: Int) {
        mStep = step
    }

    fun min() = mMin

    /**
     * 设置SeekBar最小值，也可以在layout中设置`app:min`属性
     */
    fun min(min: Int){
        mMin = min
    }

    /**
     * 获取计算之后的进度值，该值并非SeekBar真实进度值
     */
    fun progress() = _progress

    /**
     * 初始化SeekBar的值，由于[setOnSeekBarChangeListener]设置的监听器,
     * 在回调`onProgressChanged`时，progress传值是经过计算后的，并非SeekBar真实的进度值，
     * 如果用户在下次初始化时想要恢复上次的获取的进度值，则需要进行值转换，直接调用[setProgress]方法
     * 设置进度值将会出现错误值，因此需要调用[progress]方法来进行计算补偿
     */
    fun progress(p: Int){
        progress = (p - mMin) / mStep
    }
}