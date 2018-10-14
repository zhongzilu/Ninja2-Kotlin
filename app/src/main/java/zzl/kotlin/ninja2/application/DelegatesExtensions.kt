package zzl.kotlin.ninja2.application

import android.content.Context
import android.preference.PreferenceManager
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * SharePreference读写代理类
 * Created by zhongzilu on 18-6-6.
 */
class Preference<T>(val context: Context, val name: String, val default: T) : ReadWriteProperty<Any?, T> {
    val prefs by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return findPreference(name, default)
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        putPreference(name, value)
    }

    private fun <T> findPreference(name: String, default: T): T = with(prefs) {
        val res: Any = when (default) {
            is Long -> getLong(name, default)
            is String -> getString(name, default)
            is Int -> getInt(name, default)
            is Boolean -> getBoolean(name, default)
            is Float -> getFloat(name, default)
            else -> throw IllegalArgumentException("This type can be saved into Preferences")
        }

        res as T
    }

    private fun <U> putPreference(name: String, value: U) = with(prefs.edit()) {
        when (value) {
            is Long -> putLong(name, value)
            is String -> putString(name, value)
            is Int -> putInt(name, value)
            is Boolean -> putBoolean(name, value)
            is Float -> putFloat(name, value)
            else -> throw IllegalArgumentException("This type can be saved into Preference")
        }.apply()
    }
}

fun <T : Any> Context.preference(name: String, default: T) = Preference(this, name, default)

/**
 * 操作SharePreference的Boolean扩展代理类，
 * 以更低的存储成本和更高的读写效率实现
 *
 * @param base 计算的基础整数
 * @param bit  指定二进制的第几位，该值必须大于等于0
 */
class preferenceBoolean(var base: Int, val bit: Int) : ReadWriteProperty<Boolean, Boolean> {

    @Throws(IllegalArgumentException::class)
    override fun getValue(thisRef: Boolean, property: KProperty<*>): Boolean {
        if (bit !in 0..31) throw IllegalArgumentException("bit必须为0～31闭区间里的数值")

        return base and (1 shl bit) > 0
    }

    override fun setValue(thisRef: Boolean, property: KProperty<*>, value: Boolean) {
        if (bit !in 0..31) throw IllegalArgumentException("bit必须为0～31闭区间里的数值")
        base = if (value) { // 将指定位改为1
            base or (1 shl bit)
        } else { // 将指定为改为0
            base and (1 shl bit).inv()
        }
    }

}