/**
 * Flym
 *
 * Copyright (c) 2012-2013 Frederic Julian
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * Some parts of this software are based on "Sparse rss" under the MIT license (see
 * below). Please refers to the original project to identify which parts are under the
 * MIT license.
 *
 * Copyright (c) 2010-2012 Stefan Handschuh
 *
 *     Permission is hereby granted, free of charge, to any person obtaining a copy
 *     of this software and associated documentation files (the "Software"), to deal
 *     in the Software without restriction, including without limitation the rights
 *     to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *     copies of the Software, and to permit persons to whom the Software is
 *     furnished to do so, subject to the following conditions:
 *
 *     The above copyright notice and this permission notice shall be included in
 *     all copies or substantial portions of the Software.
 *
 *     THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *     IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *     FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *     AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *     LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *     OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *     THE SOFTWARE.
 */

package net.fred.feedex.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

import net.fred.feedex.utils.UiUtils;

public class GeneralPrefsActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UiUtils.setPreferenceTheme(this);
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

//        addPreferencesFromResource(R.layout.activity_preferences);
//
//        setRingtoneSummary();
//
//        Preference preference = findPreference(PrefUtils.REFRESH_ENABLED);
//        preference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
//            @Override
//            public boolean onPreferenceChange(Preference preference, Object newValue) {
//                if (Boolean.TRUE.equals(newValue)) {
//                    startService(new Intent(GeneralPrefsActivity.this, RefreshService.class));
//                } else {
//                    PrefUtils.putLong(PrefUtils.LAST_SCHEDULED_REFRESH, 0);
//                    stopService(new Intent(GeneralPrefsActivity.this, RefreshService.class));
//                }
//                return true;
//            }
//        });
//
//        preference = findPreference(PrefUtils.LIGHT_THEME);
//        preference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
//            @Override
//            public boolean onPreferenceChange(Preference preference, Object newValue) {
//                PrefUtils.putBoolean(PrefUtils.LIGHT_THEME, Boolean.TRUE.equals(newValue));
//
//                PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext()).edit().commit(); // to be sure all prefs are written
//
//                android.os.Process.killProcess(android.os.Process.myPid()); // Restart the app
//
//                // this return statement will never be reached
//                return true;
//            }
//        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return true;
    }

    @Override
    protected void onResume() {

        // The ringtone summary text should be updated using
        // OnSharedPreferenceChangeListener(), but I can't get it to work.
        // Updating in onResume is a very simple hack that seems to work, but is inefficient.

        setRingtoneSummary();
        super.onResume();

    }

    private boolean setRingtoneSummary() {

//        Preference ringtone_preference = findPreference(PrefUtils.NOTIFICATIONS_RINGTONE);
//        Uri ringtoneUri = Uri.parse(PrefUtils.getString(PrefUtils.NOTIFICATIONS_RINGTONE, ""));
//        if (ringtoneUri == null || TextUtils.isEmpty(ringtoneUri.toString()) {
//            ringtone_preference.setSummary(R.string.settings_notifications_ringtone_none);
//        } else {
//            Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), ringtoneUri);
//            if (ringtone == null) {
//                ringtone_preference.setSummary(R.string.settings_notifications_ringtone_none);
//            } else {
//                ringtone_preference.setSummary(ringtone.getTitle(getApplicationContext()));
//            }
//        }

        return true;
    }


}
