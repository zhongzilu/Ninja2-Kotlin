package zzl.kotlin.ninja2

import org.junit.Assert.assertEquals
import org.junit.Test
import zzl.kotlin.ninja2.application.parseUrl
import java.net.URI
import java.util.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class WebUnitTest {
    @Test
    fun parseUrlTest() {

        val str = "http://www.baidu.com?s=111"
        str.parseUrl()
//        assertEquals(4, 2 + 2)
    }

    @Test
    fun testGetHttpsHost() {
        val url = "https://www.baidu.com"
        val host = getHost(url)
        println(host)
        assertEquals("baidu.com", host)
    }

    @Test
    fun testGetFTPHost() {
        val url = "ftp://www.baidu.com"
        val host = getHost(url)
        println(host)
        assertEquals("baidu.com", host)
    }

    @Test
    fun testGetWithoutWWWHost() {
        val url = "http://08.185.87.165.liveadvert.com"
        val host = getHost(url)
        println(host)
        assertEquals("08.185.87.165.liveadvert.com", host)
    }

    fun getHost(url: String): String {
        val hostUrl = url.toLowerCase(Locale.getDefault())
        val host = URI(hostUrl).host
        if (host.isEmpty()) {
            return hostUrl
        }
        return if (host.startsWith("www.")) host.substring(4) else host
    }

}
