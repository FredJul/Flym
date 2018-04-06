package net.frju.flym.data.utils

import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.core.content.edit
import net.frju.flym.App
import org.jetbrains.anko.defaultSharedPreferences


object PrefUtils {

	val FIRST_OPEN = "FIRST_OPEN"
	val DISPLAY_TIP = "DISPLAY_TIP"

	val IS_REFRESHING = "IS_REFRESHING"

	val REFRESH_INTERVAL = "REFRESH_INTERVAL"
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

	fun getBoolean(key: String, defValue: Boolean) =
			App.context.defaultSharedPreferences.getBoolean(key, defValue)

	fun putBoolean(key: String, value: Boolean) {
		App.context.defaultSharedPreferences.edit { putBoolean(key, value) }
	}

	fun getInt(key: String, defValue: Int) =
			App.context.defaultSharedPreferences.getInt(key, defValue)

	fun putInt(key: String, value: Int) {
		App.context.defaultSharedPreferences.edit { putInt(key, value) }
	}

	fun getLong(key: String, defValue: Long) =
			App.context.defaultSharedPreferences.getLong(key, defValue)

	fun putLong(key: String, value: Long) {
		App.context.defaultSharedPreferences.edit { putLong(key, value) }
	}

	fun getString(key: String, defValue: String) =
			App.context.defaultSharedPreferences.getString(key, defValue)

	fun putString(key: String, value: String) {
		App.context.defaultSharedPreferences.edit { putString(key, value) }
	}

	fun getStringSet(key: String, defValue: MutableSet<String>) =
			App.context.defaultSharedPreferences.getStringSet(key, defValue)

	fun putStringSet(key: String, value: MutableSet<String>) {
		App.context.defaultSharedPreferences.edit { putStringSet(key, value) }
	}

	fun remove(key: String) {
		App.context.defaultSharedPreferences.edit { PrefUtils.remove(key) }
	}

	fun registerOnPrefChangeListener(listener: OnSharedPreferenceChangeListener) {
		try {
			App.context.defaultSharedPreferences.registerOnSharedPreferenceChangeListener(listener)
		} catch (ignored: Exception) { // Seems to be possible to have a NPE here... Why??
		}

	}

	fun unregisterOnPrefChangeListener(listener: OnSharedPreferenceChangeListener) {
		try {
			App.context.defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
		} catch (ignored: Exception) { // Seems to be possible to have a NPE here... Why??
		}

	}
}
