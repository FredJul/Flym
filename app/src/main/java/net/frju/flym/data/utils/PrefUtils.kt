/*
 * Copyright (c) 2012-2018 Frederic Julian
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package net.frju.flym.data.utils

import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.core.content.edit
import net.frju.flym.App
import org.jetbrains.anko.defaultSharedPreferences


object PrefUtils {

	const val FIRST_OPEN = "first_open"

	const val IS_REFRESHING = "is_refreshing"

	const val REFRESH_ENABLED = "refresh_enabled"
	const val REFRESH_INTERVAL = "refresh_interval"
	const val REFRESH_WIFI_ONLY = "refresh_wifi_only"

	const val DISPLAY_IMAGES = "display_images"

	const val PRELOAD_IMAGE_MODE = "preload_image_mode"
	const val PRELOAD_IMAGE_MODE__WIFI_ONLY = "WIFI_ONLY_PRELOAD"
	const val PRELOAD_IMAGE_MODE__ALWAYS = "ALWAYS_PRELOAD"

	const val KEEP_TIME = "keep_time"

	const val FONT_SIZE = "font_size"

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
