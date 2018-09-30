package zzl.kotlin.ninja2

import org.junit.Test
import zzl.kotlin.ninja2.application.parseUrl

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class WebUnitTest {
    @Test fun parseUrlTest() {

        val str = "http://www.baidu.com?s=111"
        str.parseUrl()
//        assertEquals(4, 2 + 2)
    }
}
