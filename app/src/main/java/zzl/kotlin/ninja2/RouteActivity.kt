package zzl.kotlin.ninja2

import android.os.Bundle
import zzl.kotlin.ninja2.application.go

/**
 * Created by zhongzilu on 2018/7/27 0027.
 */
class RouteActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        go<PageActivity>()
        finish()
    }
}