package zzl.kotlin.ninja2

import androidx.test.ext.junit.runners.AndroidJUnit4
import android.util.SparseArray
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by zhongzilu on 2018/10/15.
 */
@RunWith(AndroidJUnit4::class)
class SparseArrayTest {

    private val array = SparseArray<String>()

    @Before fun setUp(){
        array.put(1, "Test1")
        array.put(2, "Test2")
    }

    @Test fun testPut2SparseArray() {
        // get size of array
        assertEquals(2, array.size())
    }

    @Test fun testGetFromArray(){

        // get key by index 0
        val key = array.keyAt(0)
        assertEquals(1, key)

        // get value by index 0
        val value = array.valueAt(0)
        assertEquals("Test1", value)
    }

    @Test fun testRemoveFromArray(){

        // Remove by index 0
        array.removeAt(0)
        assertEquals(1, array.size())

        // get key by index 0
        val key = array.keyAt(0)
        assertEquals(2, key)

        // get value by key
        val value = array[key]
        assertEquals("Test2", value)
    }

    @Test fun testAddSameKey(){

        // replace the value of key 1
        array.put(1, "Test3")
        assertEquals(2, array.size())

        // get value by key 1
        val value = array[1]
        assertEquals("Test3", value)

    }

    @Test fun testAddAndRemove(){

        // Add
        array.put(3, "Test3")
        assertEquals(3, array.size())

        // remove by index 0
        array.removeAt(0)
        assertEquals(2, array.size())

        // get value by index 0
        val value = array.valueAt(0)
        assertEquals("Test2", value)
    }

}