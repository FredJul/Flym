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
 * <p/>
 * <p/>
 * Some parts of this software are based on "Sparse rss" under the MIT license (see
 * below). Please refers to the original project to identify which parts are under the
 * MIT license.
 * <p/>
 * Copyright (c) 2010-2012 Stefan Handschuh
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.fred.feedex.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.IBinder;
import android.os.SystemClock;

import net.fred.feedex.Constants;
import net.fred.feedex.utils.PrefUtils;

public class RefreshService extends Service {
    public static final String SIXTY_MINUTES = "3600000";
    private final OnSharedPreferenceChangeListener mListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (PrefUtils.REFRESH_INTERVAL.equals(key)) {
                restartTimer(false);
            }
        }
    };
    private AlarmManager mAlarmManager;
    private PendingIntent mTimerIntent;

    @Override
    public IBinder onBind(Intent intent) {
        onRebind(intent);
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true; // we want to use rebind
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        PrefUtils.registerOnPrefChangeListener(mListener);
        restartTimer(true);
    }

    private void restartTimer(boolean created) {
        if (mTimerIntent == null) {
            mTimerIntent = PendingIntent.getBroadcast(this, 0, new Intent(this, RefreshAlarmReceiver.class), 0);
        } else {
            mAlarmManager.cancel(mTimerIntent);
        }

        int time = 3600000;
        try {
            time = Math.max(60000, Integer.parseInt(PrefUtils.getString(PrefUtils.REFRESH_INTERVAL, SIXTY_MINUTES)));
        } catch (Exception ignored) {
        }

        long elapsedRealTime = SystemClock.elapsedRealtime();
        long initialRefreshTime = elapsedRealTime + 10000;

        if (created) {
            long lastRefresh = PrefUtils.getLong(PrefUtils.LAST_SCHEDULED_REFRESH, 0);

            // If the system rebooted, we need to reset the last value
            if (elapsedRealTime < lastRefresh) {
                lastRefresh = 0;
                PrefUtils.putLong(PrefUtils.LAST_SCHEDULED_REFRESH, 0);
            }

            if (lastRefresh > 0) {
                // this indicates a service restart by the system
                initialRefreshTime = Math.max(initialRefreshTime, lastRefresh + time);
            }
        }

        mAlarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, initialRefreshTime, time, mTimerIntent);
    }

    @Override
    public void onDestroy() {
        if (mTimerIntent != null) {
            mAlarmManager.cancel(mTimerIntent);
        }
        PrefUtils.unregisterOnPrefChangeListener(mListener);
        super.onDestroy();
    }

    public static class RefreshAlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            context.startService(new Intent(context, FetcherService.class).setAction(FetcherService.ACTION_REFRESH_FEEDS).putExtra(Constants.FROM_AUTO_REFRESH, true));
        }
    }
}
