package net.fred.feedex.utils;

import android.app.Activity;
import android.util.TypedValue;

import net.fred.feedex.MainApplication;

public class UiUtils {
    static public void setPreferenceTheme(Activity a) {
        if (!PrefUtils.getBoolean(PrefUtils.LIGHT_THEME, true)) {
            a.setTheme(android.R.style.Theme_Holo);
        }
    }

    static public int dpToPixel(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, MainApplication.getContext().getResources().getDisplayMetrics());
    }
}
