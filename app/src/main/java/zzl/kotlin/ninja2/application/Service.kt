package zzl.kotlin.ninja2.application

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Handler
import android.os.Message
import android.support.v4.app.ActivityCompat
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
    private val ACTION_RECENT = 0         //打开最近任务Action
    private val ACTION_SCREEN_SPLIT = 1   //分屏Action

    companion object {
        //标记应用是否在前台
        var isForeground = false
    }

    override fun onInterrupt() {
        L.i(TAG, "onInterrupt")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
//        L.i(TAG, "event: $event")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        registerFingerprint()

        L.i(TAG, "Service started")
    }

    override fun onDestroy() {
        super.onDestroy()
        L.i(TAG, "Service destroy")
    }

    private lateinit var mFingerprintManager: FingerprintManager
    private lateinit var mCancellationSignal: CancellationSignal

    /**
     * 初始化指纹识别服务,在初始化FingerprintManager前，
     * 必须执行{@link #checkFingerprintManager}方法，
     * 之所以不用{@link #FingerprintManagerCompat}类，
     * 是因为该类默认在Android 6.0一下无指纹识别，但市面上仍然存在Android 5.0的机型上存在指纹识别器。
     * 为了兼容，因此利用反射去检查是否系统中是否存在FingerprintManager Api,
     * 如果存在才能进行初始化操作，不然会抛出异常
     */
    @SuppressLint("InlinedApi")
    private fun initFingerprintManager() {

        checkFingerprintManager()

        //详情请看: http://www.cnblogs.com/Fndroid/p/5204986.html
//        mFingerprintManager = FingerprintManagerCompat.from(this)
        if (SP.hasFingerprintManager) {
            mFingerprintManager = getSystemService(Application.FINGERPRINT_SERVICE) as FingerprintManager
            L.i(TAG, "init fingerprint manager")

            initHandler()

        } else {
            L.i(TAG, "have no fingerprint manager")
        }
    }

    /**
     * 注册指纹监听器
     */
    @SuppressLint("NewApi")
    @Synchronized
    private fun registerFingerprint() {

        initFingerprintManager()

        if (!SP.hasFingerprintManager) {
            L.i(TAG, "have no fingerprint manager")
            return
        }

        if (!hadFingerprintPermission()) {
            L.i(TAG, "have no permission")
            return
        }

        if (mFingerprintManager.hasEnrolledFingerprints()) {
            L.i(TAG, "Fingerprint has enrolled")
            mCancellationSignal = CancellationSignal()
            mFingerprintManager.authenticate(null, mCancellationSignal, 0, _authenticationCallback, null)
        } else {
            L.i(TAG, "Fingerprint have no enrolled")
        }
    }

    /**
     * 检查Android 6.0+的指纹动态权限
     */
    @SuppressLint("InlinedApi")
    private fun hadFingerprintPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 检查是否存在FingerprintManager Api，
     * 只有存在的情况下才能初始化{@link #mFingerprintManager}
     */
    private fun checkFingerprintManager() {
//        if (!SP.isFirstInstall) return

        try {
            // 通过反射判断是否存在该类
            Class.forName("android.hardware.fingerprint.FingerprintManager")
            L.i(TAG, "exist fingerprintManager")
            SP.hasFingerprintManager = true
        } catch (e: Exception) {
            e.printStackTrace()
            SP.hasFingerprintManager = false
        }
    }

    /**
     * 指纹识别结果回调处理
     */
    private val _authenticationCallback = object : FingerprintManager.AuthenticationCallback() {

        private val TAG = "AuthenticationCallback-->"

        // 当出现错误的时候回调此函数，比如多次尝试都失败了的时候，errString是错误信息
        override fun onAuthenticationError(errMsgId: Int, errString: CharSequence?) {
            super.onAuthenticationError(errMsgId, errString)
            L.i(TAG, "onAuthenticationError: $errString")
            mHandler.sendEmptyMessageDelayed(0, DELAY)
            cancelSignal()
        }

        override fun onAuthenticationHelp(helpMsgId: Int, helpString: CharSequence?) {
            super.onAuthenticationHelp(helpMsgId, helpString)
            L.i(TAG, "onAuthenticationHelp: $helpString")

            doAction(ACTION_RECENT)
        }

        override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult?) {
            super.onAuthenticationSucceeded(result)
            L.i(TAG, "onAuthenticationSucceeded: ${result.toString()}")
            mHandler.sendEmptyMessageDelayed(0, DELAY)

            doAction(ACTION_SCREEN_SPLIT)
        }

        // 当指纹验证失败的时候会回调此函数，失败之后允许多次尝试，失败次数过多会停止响应一段时间然后再停止sensor的工作
        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            L.i(TAG, "onAuthenticationFailed")

            doAction(ACTION_SCREEN_SPLIT)
        }
    }

    /**
     * 指纹认证失败时，取消监听
     */
    @SuppressLint("NewApi")
    private fun cancelSignal() {
        if (mFingerprintManager.isHardwareDetected) {
            mCancellationSignal.cancel()
        }
    }

    /**
     * Handler延迟发送消息的延迟时间，默认为5秒
     */
    private val DELAY = 10000L // 10s

    /**
     * 初始化Handler，用于循环开启指纹识别功能，因为为了省电，
     * 指纹识别器无论识别成功或失败，都会关闭传感器一段时间，网上说时间为30s,
     * 但我觉得有点长，于是改为了10s
     */
    private lateinit var mHandler: Handler

    private fun initHandler() {
        mHandler = object : Handler() {
            @SuppressLint("NewApi")
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                L.i("Handler-->", "restart fingerprint module")
                mFingerprintManager.authenticate(null, mCancellationSignal, 0, _authenticationCallback, mHandler)
            }
        }
    }

    /**
     * 发送本地广播
     */
    private fun doAction(action: Int) {

        if (!isForeground) return

        when (action) {
            ACTION_RECENT -> performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)

            ACTION_SCREEN_SPLIT -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    performGlobalAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
                } else {
                    performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
                }
            }
        }
    }

}