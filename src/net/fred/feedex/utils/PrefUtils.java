/**
 * FeedEx
 *
 * Copyright (c) 2012-2013 Frederic Julian
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.fred.feedex.utils;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;

import net.fred.feedex.MainApplication;

public class PrefUtils {

    public static final String FIRST_OPEN = "FIRST_OPEN";

    public static final String IS_REFRESHING = "IS_REFRESHING";

    public static final String REFRESH_INTERVAL = "refresh.interval";
    public static final String REFRESH_ENABLED = "refresh.enabled";
    public static final String REFRESH_ON_OPEN_ENABLED = "refreshonopen.enabled";
    public static final String REFRESH_WIFI_ONLY = "refreshwifionly.enabled";

    public static final String NOTIFICATIONS_ENABLED = "notifications.enabled";
    public static final String NOTIFICATIONS_RINGTONE = "notifications.ringtone";
    public static final String NOTIFICATIONS_VIBRATE = "notifications.vibrate";
    public static final String NOTIFICATIONS_LIGHT = "notifications.light";

    public static final String LIGHT_THEME = "lighttheme";
    public static final String DISPLAY_IMAGES = "display_images";
    public static final String PRELOAD_IMAGE_MODE = "preload_image_mode";
    public static final String DISPLAY_OLDEST_FIRST = "display_oldest_first";
    public static final String DISPLAY_ENTRIES_FULLSCREEN = "display_entries_fullscreen";

    public static final String PROXY_ENABLED = "proxy.enabled";
    public static final String PROXY_PORT = "proxy.port";
    public static final String PROXY_HOST = "proxy.host";
    public static final String PROXY_WIFI_ONLY = "proxy.wifionly";
    public static final String PROXY_TYPE = "proxy.type";

    public static final String KEEP_TIME = "keeptime";

    public static final String FONT_SIZE = "fontsize";

    public static final String LAST_SCHEDULED_REFRESH = "lastscheduledrefresh";

    public static final String SHOW_READ = "show_read";

    public static boolean getBoolean(String key, boolean defValue) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext());
        return settings.getBoolean(key, defValue);
    }

    public static void putBoolean(String key, boolean value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext()).edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public static int getInt(String key, int defValue) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext());
        return settings.getInt(key, defValue);
    }

    public static void putInt(String key, int value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext()).edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public static long getLong(String key, long defValue) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext());
        return settings.getLong(key, defValue);
    }

    public static void putLong(String key, long value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext()).edit();
        editor.putLong(key, value);
        editor.commit();
    }

    public static String getString(String key, String defValue) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext());
        return settings.getString(key, defValue);
    }

    public static void putString(String key, String value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext()).edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static void remove(String key) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext()).edit();
        editor.remove(key);
        editor.commit();
    }

    public static void registerOnPrefChangeListener(OnSharedPreferenceChangeListener listener) {
        try {
            PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext()).registerOnSharedPreferenceChangeListener(listener);
        } catch (Exception ignored) { // Seems to be possible to have a NPE here... Why??
        }
    }

    public static void unregisterOnPrefChangeListener(OnSharedPreferenceChangeListener listener) {
        try {
            PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext()).unregisterOnSharedPreferenceChangeListener(listener);
        } catch (Exception ignored) { // Seems to be possible to have a NPE here... Why??
        }
    }
}
