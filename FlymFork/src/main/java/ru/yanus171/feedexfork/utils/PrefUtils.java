/**
 * Flym
 * <p/>
 * Copyright (c) 2012-2015 Frederic Julian
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ru.yanus171.feedexfork.utils;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;

import ru.yanus171.feedexfork.MainApplication;

public class PrefUtils {

    public static final String FIRST_OPEN = "FIRST_OPEN";
    public static final String DISPLAY_TIP = "DISPLAY_TIP";

    public static final String IS_REFRESHING = "IS_REFRESHING";

    public static final String REFRESH_INTERVAL = "refresh.interval";
    public static final String REFRESH_ENABLED = "refresh.enabled";
    public static final String REFRESH_ONLY_SELECTED = "refresh.only_selected";
    public static final String REFRESH_ON_OPEN_ENABLED = "refreshonopen.enabled";
    public static final String REFRESH_WIFI_ONLY = "refreshwifionly.enabled";

    public static final String NOTIFICATIONS_ENABLED = "notifications.enabled";
    public static final String NOTIFICATIONS_RINGTONE = "notifications.ringtone";
    public static final String NOTIFICATIONS_VIBRATE = "notifications.vibrate";
    public static final String NOTIFICATIONS_LIGHT = "notifications.light";

    public static final String LIGHT_THEME = "lighttheme";
    public static final String DISPLAY_IMAGES = "display_images";
    public static final String FULL_SCREEN_STATUSBAR_VISIBLE = "full_screen_statusbar_visible";
    public static final String PRELOAD_IMAGE_MODE = "preload_image_mode";
    public static final String DISPLAY_OLDEST_FIRST = "display_oldest_first";
    public static final String DISPLAY_ENTRIES_FULLSCREEN = "display_entries_fullscreen";
    public static final String ENTRY_FONT_BOLD = "entry_font_bold";
    public static final String TEXT_COLOR_BRIGHTNESS = "text_color_brightness";
    public static final String MAX_IMAGE_DOWNLOAD_COUNT = "max_image_download_count";
    public static final String MAX_IMAGE_DOWNLOAD_SIZE = "settings_max_image_download_size_kb";

    public static final String CONTENT_EXTRACT_RULES = "content_extract_rules";
    public static final String LOAD_COMMENTS = "load_comments";
    public static final String REMEBER_LAST_ENTRY = "remember_last_entry";
    public static final String TAP_ZONES_VISIBLE = "settings_tap_zones_visible";
    public static final String SHOW_PROGRESS_INFO = "settings_show_progress_info";


    public static final String LAST_ENTRY_URI = "last_entry_uri";
    public static final String LAST_ENTRY_SCROLL_Y = "last_entry_scroll_y";
    public static final String LAST_ENTRY_ID = "last_entry_id";


    public static final String VOLUME_BUTTONS_ACTION_DEFAULT = "Default";
    public static final String VOLUME_BUTTONS_ACTION_SWITCH_ENTRY = "SwithEntry";
    public static final String VOLUME_BUTTONS_ACTION_PAGE_UP_DOWN = "PageUpDown";


    public static final String KEEP_TIME = "keeptime";


    public static int getFontSize() {
        return Integer.parseInt(PrefUtils.getString("fontsize", "0"));
    }
    public static int getFontSizeEntryList() {
        return Integer.parseInt(PrefUtils.getString("fontsize_entrylist", "0"));
    }

    public static int getImageDownloadCount() {
        try {
            return Integer.parseInt(PrefUtils.getString(PrefUtils.MAX_IMAGE_DOWNLOAD_COUNT, "10"));
        } catch ( NumberFormatException e ) {
            return 10;
        }
    }

    public static int getImageMaxDownloadSizeInKb() {
        try {
            return Integer.parseInt(PrefUtils.getString(PrefUtils.MAX_IMAGE_DOWNLOAD_SIZE, "2048"));
        } catch ( NumberFormatException e ) {
            return 2048;
        }
    }

    public static boolean getBoolean(String key, boolean defValue) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext());
        return settings.getBoolean(key, defValue);
    }

    public static void putBoolean(String key, boolean value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext()).edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static int getInt(String key, int defValue) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext());
        return settings.getInt(key, defValue);
    }

    public static void putInt(String key, int value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext()).edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public static long getLong(String key, long defValue) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext());
        return settings.getLong(key, defValue);
    }

    public static void putLong(String key, long value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext()).edit();
        editor.putLong(key, value);
        editor.apply();
    }

    public static String getString(String key, String defValue) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext());
        return settings.getString(key, defValue);
    }

    public static String getString(String key, int defValue) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext());
        return settings.getString(key, MainApplication.getContext().getString(  defValue ));
    }

    public static void putString(String key, String value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext()).edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static void remove(String key) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext()).edit();
        editor.remove(key);
        editor.apply();
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
