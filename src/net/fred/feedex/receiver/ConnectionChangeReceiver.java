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

package net.fred.feedex.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.SystemClock;

import net.fred.feedex.Constants;
import net.fred.feedex.MainApplication;
import net.fred.feedex.PrefsManager;
import net.fred.feedex.service.FetcherService;
import net.fred.feedex.service.RefreshService;

public class ConnectionChangeReceiver extends BroadcastReceiver {

    private boolean mConnection = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (mConnection && intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)) {
            mConnection = false;
        } else if (!mConnection && !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)) {
            mConnection = true;

            if (!FetcherService.isRefreshingFeeds && PrefsManager.getBoolean(PrefsManager.REFRESH_ENABLED, true)) {
                int time = 3600000;
                try {
                    time = Math.max(60000, Integer.parseInt(PrefsManager.getString(PrefsManager.REFRESH_INTERVAL, RefreshService.SIXTYMINUTES)));
                } catch (Exception ignored) {
                }

                long lastRefresh = PrefsManager.getLong(PrefsManager.LAST_SCHEDULED_REFRESH, 0);
                if (SystemClock.elapsedRealtime() - lastRefresh > time) {
                    MainApplication.getAppContext().sendBroadcast(
                            new Intent(Constants.ACTION_REFRESH_FEEDS).putExtra(Constants.FROM_AUTO_REFRESH, true));
                }
            }
        }
    }
}