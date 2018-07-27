package zzl.kotlin.ninja2.application

import android.content.Context
import android.preference.PreferenceManager
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Created by zhongzilu on 18-6-6.
 */
class Preference<T>(val context: Context, val name: String, val default: T) : ReadWriteProperty<Any?, T> {
    val prefs by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>) : T {
        return findPreference(name, default)
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T){
        putPreference(name, value)
    }

    private fun <T> findPreference(name: String, default: T) : T = with(prefs){
        val res: Any = when(default){
            is Long -> getLong(name, default)
            is String -> getString(name, default)
            is Int -> getInt(name, default)
            is Boolean -> getBoolean(name, default)
            is Float -> getFloat(name, default)
            else -> throw IllegalArgumentException("This type can be saved into Preferences")
        }

        res as T
    }

    private fun <U> putPreference(name: String, value: U) = with(prefs.edit()){
        when(value){
            is Long -> putLong(name, value)
            is String -> putString(name, value)
            is Int -> putInt(name, value)
            is Boolean -> putBoolean(name, value)
            is Float -> putFloat(name, value)
            else -> throw IllegalArgumentException("This type can be saved into Preference")
        }.apply()
    }
}

object DelegatesExt {
    fun <T : Any> preference(context: Context, name: String, default: T)
            = Preference(context, name, default)
}