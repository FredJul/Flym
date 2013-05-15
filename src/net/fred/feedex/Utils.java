package net.fred.feedex;

import android.app.Activity;

public class Utils {
	static public void setPreferenceTheme(Activity a) {
		if (!PrefsManager.getBoolean(PrefsManager.LIGHT_THEME, true)) {
			a.setTheme(android.R.style.Theme_Holo);
		}
	}
}
