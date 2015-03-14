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

package net.fred.feedex.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.SystemClock;

import net.fred.feedex.Constants;
import net.fred.feedex.service.FetcherService;
import net.fred.feedex.service.RefreshService;
import net.fred.feedex.utils.PrefUtils;

public class ConnectionChangeReceiver extends BroadcastReceiver {

    private boolean mConnection = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (mConnection && intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)) {
            mConnection = false;
        } else if (!mConnection && !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)) {
            mConnection = true;

            if (!PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false) && PrefUtils.getBoolean(PrefUtils.REFRESH_ENABLED, true)) {
                int time = 3600000;
                try {
                    time = Math.max(60000, Integer.parseInt(PrefUtils.getString(PrefUtils.REFRESH_INTERVAL, RefreshService.SIXTY_MINUTES)));
                } catch (Exception ignored) {
                }

                long lastRefresh = PrefUtils.getLong(PrefUtils.LAST_SCHEDULED_REFRESH, 0);
                long elapsedRealTime = SystemClock.elapsedRealtime();

                // If the system rebooted, we need to reset the last value
                if (elapsedRealTime < lastRefresh) {
                    lastRefresh = 0;
                    PrefUtils.putLong(PrefUtils.LAST_SCHEDULED_REFRESH, 0);
                }

                if (lastRefresh == 0 || elapsedRealTime - lastRefresh > time) {
                    context.startService(new Intent(context, FetcherService.class).setAction(FetcherService.ACTION_REFRESH_FEEDS).putExtra(Constants.FROM_AUTO_REFRESH, true));
                }
            }
        }
    }
}