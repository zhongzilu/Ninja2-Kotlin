package zzl.kotlin.ninja2

import org.junit.Assert.*
import org.junit.Test

/**
 * Created by zhongzilu on 2018/10/14.
 */
class BitTest {

    private var FLAG = 0

    @Test
    fun testBitShl() {
        assertEquals(0, FLAG and (1 shl 0))
        assertEquals(1, FLAG or 1)
        assertEquals(2, FLAG or (1 shl 1))
        assertEquals(4, FLAG or (1 shl 2))
    }

    @Test
    fun testBitShlCount() {
        FLAG = FLAG or 1
        assertEquals(1, FLAG)

        FLAG = FLAG or (1 shl 1)
        assertEquals(3, FLAG)

        FLAG = FLAG or (1 shl 2)
        assertEquals(7, FLAG)
    }

    @Test
    fun testBit2Boolean() {
        assertTrue("测试使用错误位数，返回默认值", bit2Boolean(FLAG, -1, true))
        assertFalse(bit2Boolean(FLAG, 1, true))
        assertFalse(bit2Boolean(FLAG, 31, true))
        assertTrue("测试位数超过32位后，返回默认值", bit2Boolean(FLAG, 33, true))
        FLAG = 2
        assertTrue(bit2Boolean(FLAG, 1, true))
    }

    /**
     * 判断32位整数的二进制[bit]位是否为1，为1返回true，为0返回false
     * @param base      计算的基础整数
     * @param bit       指定二进制的第几位，该值必须为0～31闭区间的整数
     */
    private fun bit2Boolean(base: Int, bit: Int, default: Boolean): Boolean {
        return when (bit) {
            in 0..31 -> base and (1 shl bit) > 0
            else -> default
        }
    }

    @Test fun testBoolean2Bit(){
        var base = 0

        //指定第1位为1
        base = boolean2Bit(base, 0, true)
        assertEquals(1, base)

        //指定第2位为1
        base = boolean2Bit(base, 1, true)
        assertEquals(3, base)

        //指定第3位为1
        base = boolean2Bit(base, 2, true)
        assertEquals(7, base)

        //指定第2位为0
        base = boolean2Bit(base, 1, false)
        assertEquals(5, base)

        //指定第3位为0
        base = boolean2Bit(base, 2, false)
        assertEquals(1, base)

        //指定第1位为0
        base = boolean2Bit(base, 0, false)
        assertEquals(0, base)

    }

    /**
     * 将32位整数{@param base}的二进制的{@param bit}位赋值位0或1，
     *
     * @param base 基础整数
     * @param bit   指定二进制的第几位，该值必须为0～31闭区间的整数
     * @param value 如果为true，则指定位赋值为1，如果为false，则指定位赋值位0
     */
    private fun boolean2Bit(base: Int, bit: Int, value: Boolean): Int {
        assertTrue(bit in 0..31)

        return if (value) { // 将指定位改为1
            base or (1 shl bit)
        } else { // 将指定位改为0
            base and (1 shl bit).inv()
        }
    }
}