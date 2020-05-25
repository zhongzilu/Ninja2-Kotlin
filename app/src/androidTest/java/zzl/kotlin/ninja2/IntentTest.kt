package zzl.kotlin.ninja2

import android.content.ComponentName
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import zzl.kotlin.ninja2.application.parseIntent


/**
 * Created by zhongzilu on 2018/10/15.
 */
@RunWith(AndroidJUnit4::class)
class IntentTest {

    private lateinit var intent: Intent

    private val outUri = "intent:#Intent;component=com.mx.app.mxhaha/com.mx.app.MxMainActivity;end"
    private val outURI = "#Intent;component=com.mx.app.mxhaha/com.mx.app.MxMainActivity;end"
    private val weChat = "intent:#Intent;action=android.intent.action.MAIN;category=android.intent.category.LAUNCHER;launchFlags=0x10000000;component=com.tencent.mm/.ui.LauncherUI;end"

    @Before fun setUp(){
        intent = Intent(Intent.ACTION_VIEW)
        intent.component = ComponentName("com.mx.app.mxhaha", "com.mx.app.MxMainActivity")
    }

    @Test fun testIntentToUri(){
        assertEquals(outUri, intent.toUri(Intent.URI_INTENT_SCHEME))
    }

    @Test fun testIntentToURI(){
        assertEquals(outURI, intent.toURI())
    }

    @Test fun testParseIntent(){
        assertEquals(intent.component, outUri.parseIntent().component)
        assertEquals(intent.component, outURI.parseIntent().component)
    }

    @Test fun testInvokeWeChat(){
        intent = Intent()
        val cmp = ComponentName("com.tencent.mm","com.tencent.mm.ui.LauncherUI")
        intent.action = Intent.ACTION_MAIN
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.component = cmp

        assertEquals(weChat, intent.toUri(Intent.URI_INTENT_SCHEME))
        assertEquals(intent.component, weChat.parseIntent().component)
    }
}