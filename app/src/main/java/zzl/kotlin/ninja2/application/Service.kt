package zzl.kotlin.ninja2.application

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * 指纹识别无障碍服务
 *
 * @see https://www.jianshu.com/p/4cd8c109cdfb
 *
 * Created by zhongzilu on 2018-10-19
 */
class FingerprintService : AccessibilityService() {

    private val TAG = "FingerprintService-->"
    companion object {
        var isStarted = false
    }

    override fun onInterrupt() {
        L.i(TAG, "onInterrupt")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        L.i(TAG, "event: $event")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isStarted = true
        L.i(TAG, "Service started")

    }

    override fun onDestroy() {
        super.onDestroy()
        L.i(TAG, "Service destroy")
    }

}