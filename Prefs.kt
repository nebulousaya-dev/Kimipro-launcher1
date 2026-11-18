package com.kimipro.launcher.prefs

import androidx.preference.PreferenceManager
import com.kimipro.launcher.App

object Prefs {
    private val sp by lazy { PreferenceManager.getDefaultSharedPreferences(App.app) }

    fun get(key: String, def: Any?): Any? = when (def) {
        is Boolean -> sp.getBoolean(key, def)
        is Number -> sp.getFloat(key, def.toFloat()).toDouble()
        is String -> sp.getString(key, def)
        else -> sp.all[key]
    }

    fun set(key: String, value: Any?) {
        val e = sp.edit()
        when (value) {
            is Boolean -> e.putBoolean(key, value)
            is Number -> e.putFloat(key, value.toFloat())
            is String -> e.putString(key, value)
            null -> e.remove(key)
            else -> e.putString(key, value.toString())
        }
        e.apply()
    }

    fun remove(key: String) = sp.edit().remove(key).apply()
    fun all(): Map<String, *> = sp.all
}
