package zzl.kotlin.ninja2

import android.content.ContentValues
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_xwalk_view.*
import org.jetbrains.anko.toast
import org.xwalk.core.XWalkActivity
import org.xwalk.core.XWalkUIClient
import org.xwalk.core.XWalkView
import zzl.kotlin.ninja2.application.L

/**
 * Created by zhongzilu on 2018-11-12
 */
class XWalkTestActivity : XWalkActivity() {

    override fun onXWalkReady() {
        mXWalkView.apply {
            load("file:///android_asset/www/index.html", null)

            setUIClient(object : XWalkUIClient(this) {
                override fun onPageLoadStarted(view: XWalkView?, url: String?) {
                    super.onPageLoadStarted(view, url)
                    L.d(ContentValues.TAG, "onPageLoadStarted: $url")
                }

                override fun onPageLoadStopped(view: XWalkView?, url: String?, status: LoadStatus?) {
                    super.onPageLoadStopped(view, url, status)
                    L.d(ContentValues.TAG, "onPageLoadStopped: $url")
                }
            })
        }
        mInputBox.setOnEditorActionListener(TextView.OnEditorActionListener { v, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {

                val text = v.text.toString().trim()
                if (text.isEmpty()) return@OnEditorActionListener true

                toast(text)
                mXWalkView.stopLoading()
                mXWalkView.load(text, null)
                return@OnEditorActionListener true
            }
            false
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_xwalk_view)
    }


}