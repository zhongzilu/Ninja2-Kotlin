package zzl.kotlin.ninja2

import android.app.Application
import android.os.Message
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat
import android.support.v4.os.CancellationSignal
import zzl.kotlin.ninja2.application.AdBlock
import zzl.kotlin.ninja2.application.L
import kotlin.properties.Delegates

/**
 * Created by zhongzilu on 2018/7/27 0027.
 */
class App : Application() {

    companion object {
        var instance: App by Delegates.notNull()
        var MESSAGE: Message? = null
            get() {
                val msg = field
                field = null
                return msg
            }
    }

    private lateinit var mFingerprintManager: FingerprintManagerCompat
    private lateinit var mCancellationSignal: CancellationSignal

    override fun onCreate() {
        super.onCreate()
        instance = this

        AdBlock.init(this)

        //详情请看: http://www.cnblogs.com/Fndroid/p/5204986.html
        mFingerprintManager = FingerprintManagerCompat.from(this)
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }

    override fun onTerminate() {
        super.onTerminate()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
    }

    @Synchronized
    fun registerFingerprint() {
        if (mFingerprintManager.hasEnrolledFingerprints()) {
            mCancellationSignal = CancellationSignal()
            mFingerprintManager.authenticate(null, 0, mCancellationSignal, _authenticationCallback, null)
        }
    }

    private val _authenticationCallback = object : FingerprintManagerCompat.AuthenticationCallback() {

        private val TAG = "AuthenticationCallback-->"

        // 当出现错误的时候回调此函数，比如多次尝试都失败了的时候，errString是错误信息
        override fun onAuthenticationError(errMsgId: Int, errString: CharSequence?) {
            super.onAuthenticationError(errMsgId, errString)
            L.i(TAG, "onAuthenticationError: $errString")
        }

        override fun onAuthenticationHelp(helpMsgId: Int, helpString: CharSequence?) {
            super.onAuthenticationHelp(helpMsgId, helpString)
            L.i(TAG, "onAuthenticationHelp: $helpString")
        }

        // 当验证的指纹成功时会回调此函数，然后不再监听指纹sensor
        override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult?) {
            super.onAuthenticationSucceeded(result)
            L.i(TAG, "onAuthenticationSucceeded: ${result.toString()}")
        }

        // 当指纹验证失败的时候会回调此函数，失败之后允许多次尝试，失败次数过多会停止响应一段时间然后再停止sensor的工作
        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            L.i(TAG, "onAuthenticationFailed")
        }
    }
}