/**
 * FeedEx
 * 
 * Copyright (c) 2012-2013 Frederic Julian
 * Copyright (c) 2010-2012 Stefan Handschuh
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

package net.fred.feedex.service;

import net.fred.feedex.Constants;
import net.fred.feedex.MainApplication;
import net.fred.feedex.PrefsManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

public class RefreshService extends Service {
	public static final String SIXTYMINUTES = "3600000";

	private final OnSharedPreferenceChangeListener listener = new OnSharedPreferenceChangeListener() {
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			if (PrefsManager.REFRESH_INTERVAL.equals(key)) {
				restartTimer(false);
			}
		}
	};

	private final Intent refreshBroadcastIntent = new Intent(Constants.ACTION_REFRESH_FEEDS).putExtra(Constants.SCHEDULED, true);
	private AlarmManager alarmManager;
	private PendingIntent timerIntent;

	@Override
	public IBinder onBind(Intent intent) {
		onRebind(intent);
		return null;
	}

	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return true; // we want to use rebind
	}

	@Override
	public void onCreate() {
		super.onCreate();

		alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		PreferenceManager.getDefaultSharedPreferences(MainApplication.getAppContext()).registerOnSharedPreferenceChangeListener(listener);
		restartTimer(true);
	}

	private void restartTimer(boolean created) {
		if (timerIntent == null) {
			timerIntent = PendingIntent.getBroadcast(this, 0, refreshBroadcastIntent, 0);
		} else {
			alarmManager.cancel(timerIntent);
		}

		int time = 3600000;
		try {
			time = Math.max(60000, Integer.parseInt(PrefsManager.getString(PrefsManager.REFRESH_INTERVAL, SIXTYMINUTES)));
		} catch (Exception e) {
		}

		long initialRefreshTime = SystemClock.elapsedRealtime() + 10000;

		if (created) {
			long lastRefresh = PrefsManager.getLong(PrefsManager.LAST_SCHEDULED_REFRESH, 0);

			if (lastRefresh > 0) {
				// this indicates a service restart by the system
				initialRefreshTime = Math.max(SystemClock.elapsedRealtime() + 10000, lastRefresh + time);
			}
		}

		alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, initialRefreshTime, time, timerIntent);
	}

	@Override
	public void onDestroy() {
		if (timerIntent != null) {
			alarmManager.cancel(timerIntent);
		}
		PreferenceManager.getDefaultSharedPreferences(MainApplication.getAppContext()).unregisterOnSharedPreferenceChangeListener(listener);
		super.onDestroy();
	}
}
