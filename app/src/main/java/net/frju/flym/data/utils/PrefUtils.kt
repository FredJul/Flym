package net.frju.parentalcontrol.utils

import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.preference.PreferenceManager
import net.frju.flym.App


object PrefUtils {

    val FIRST_OPEN = "FIRST_OPEN"
    val DISPLAY_TIP = "DISPLAY_TIP"

    val IS_REFRESHING = "IS_REFRESHING"

    val REFRESH_INTERVAL = "refresh.interval"
    val REFRESH_ENABLED = "refresh.enabled"
    val REFRESH_ON_OPEN_ENABLED = "refreshonopen.enabled"
    val REFRESH_WIFI_ONLY = "refreshwifionly.enabled"

    val NOTIFICATIONS_ENABLED = "notifications.enabled"
    val NOTIFICATIONS_RINGTONE = "notifications.ringtone"
    val NOTIFICATIONS_VIBRATE = "notifications.vibrate"
    val NOTIFICATIONS_LIGHT = "notifications.light"

    val LIGHT_THEME = "lighttheme"
    val DISPLAY_IMAGES = "display_images"

    val PRELOAD_IMAGE_MODE = "preload_image_mode"
    val PRELOAD_IMAGE_MODE__WIFI_ONLY = "WIFI_ONLY_PRELOAD"
    val PRELOAD_IMAGE_MODE__ALWAYS = "ALWAYS_PRELOAD"

    val DISPLAY_OLDEST_FIRST = "display_oldest_first"
    val DISPLAY_ENTRIES_FULLSCREEN = "display_entries_fullscreen"

    val KEEP_TIME = "keeptime"

    val FONT_SIZE = "fontsize"

    fun getBoolean(key: String, defValue: Boolean): Boolean {
        val settings = PreferenceManager.getDefaultSharedPreferences(App.context)
        return settings.getBoolean(key, defValue)
    }

    fun putBoolean(key: String, value: Boolean) {
        val editor = PreferenceManager.getDefaultSharedPreferences(App.context).edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun getInt(key: String, defValue: Int): Int {
        val settings = PreferenceManager.getDefaultSharedPreferences(App.context)
        return settings.getInt(key, defValue)
    }

    fun putInt(key: String, value: Int) {
        val editor = PreferenceManager.getDefaultSharedPreferences(App.context).edit()
        editor.putInt(key, value)
        editor.apply()
    }

    fun getLong(key: String, defValue: Long): Long {
        val settings = PreferenceManager.getDefaultSharedPreferences(App.context)
        return settings.getLong(key, defValue)
    }

    fun putLong(key: String, value: Long) {
        val editor = PreferenceManager.getDefaultSharedPreferences(App.context).edit()
        editor.putLong(key, value)
        editor.apply()
    }

    fun getString(key: String, defValue: String): String {
        val settings = PreferenceManager.getDefaultSharedPreferences(App.context)
        return settings.getString(key, defValue)
    }

    fun putString(key: String, value: String) {
        val editor = PreferenceManager.getDefaultSharedPreferences(App.context).edit()
        editor.putString(key, value)
        editor.apply()
    }

    fun getStringSet(key: String, defValue: MutableSet<String>): MutableSet<String> {
        val settings = PreferenceManager.getDefaultSharedPreferences(App.context)
        return settings.getStringSet(key, defValue)
    }

    fun putStringSet(key: String, value: MutableSet<String>) {
        val editor = PreferenceManager.getDefaultSharedPreferences(App.context).edit()
        editor.putStringSet(key, value)
        editor.apply()
    }

    fun remove(key: String) {
        val editor = PreferenceManager.getDefaultSharedPreferences(App.context).edit()
        editor.remove(key)
        editor.apply()
    }

    fun registerOnPrefChangeListener(listener: OnSharedPreferenceChangeListener) {
        try {
            PreferenceManager.getDefaultSharedPreferences(App.context).registerOnSharedPreferenceChangeListener(listener)
        } catch (ignored: Exception) { // Seems to be possible to have a NPE here... Why??
        }

    }

    fun unregisterOnPrefChangeListener(listener: OnSharedPreferenceChangeListener) {
        try {
            PreferenceManager.getDefaultSharedPreferences(App.context).unregisterOnSharedPreferenceChangeListener(listener)
        } catch (ignored: Exception) { // Seems to be possible to have a NPE here... Why??
        }

    }
}
