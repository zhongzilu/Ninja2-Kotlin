package zzl.kotlin.ninja2

import android.view.View
import android.view.animation.LinearInterpolator

/**
 * Created by zhongzilu on 18-7-25.
 */
//=========================
fun View.visible(){
    this.visibility = View.VISIBLE
}

fun View.gone(){
    this.visibility = View.GONE
}

fun View.show(animate: Boolean = true){
    if (animate){
        alpha = 0f
        visible()
        animate().apply {
            alpha = 1f
            duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
            interpolator = LinearInterpolator()
            setListener(null)
            start()
        }

        return
    }

    alpha = 1f
    visible()
}

fun View.hide(animate: Boolean = true){
    if (animate){
        animate().apply {
            alpha = 0f
            duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
            interpolator = LinearInterpolator()
            setListener(null)
            start()
        }

        return
    }

    gone()
}

//========================