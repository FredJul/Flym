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

package ru.yanus171.feedexfork.service;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.utils.PrefUtils;

public class AutoService {
    private static final String SIXTY_MINUTES = "3600000";
    //public static final String TASK_TAG_PERIODIC = "TASK_TAG_PERIODIC";

    /*@Override
    public int onRunTask(TaskParams taskParams) {
        FetcherService.StartService(GetAutoRefreshServiceIntent());
        return GcmNetworkManager.RESULT_SUCCESS;
    }
    */
    static Intent GetAutoRefreshServiceIntent() {
        return FetcherService.GetStartIntent().putExtra( Constants.FROM_AUTO_REFRESH, true );
    }

    public static void init(Context context) {
        if (Build.VERSION.SDK_INT >= 21 ) {
            AutoRefreshJobService.initAutoRefresh(context);
            AutoBackupJobService.initAutoBackup( context );
        }
        /*else {
            GcmNetworkManager gcmNetworkManager = GcmNetworkManager.getInstance(context);
            if (isAutoUpdateEnabled()) {
                PeriodicTask task = new PeriodicTask.Builder()
                        .setService(AutoRefreshService.class)
                        .setTag(TASK_TAG_PERIODIC)
                        .setPeriod(getTimeIntervalInMSecs() / 1000)
                        .setPersisted(true)
                        .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                        .setUpdateCurrent(true)
                        .build();

                gcmNetworkManager.schedule(task);
            } else {
                gcmNetworkManager.cancelTask(TASK_TAG_PERIODIC, AutoRefreshService.class);
            }

        }*/
    }

    static boolean isBatteryLow(Context context) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent battery = context.registerReceiver(null, ifilter);
        int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = level / (float)scale * 100;

        long lowLevelPct = 20;
        try {
            lowLevelPct = Math.max(50, Long.parseLong(PrefUtils.getString("refresh.min_update_battery_level", 20)) );
        } catch (Exception ignored) {
        }
        return batteryPct < lowLevelPct;
    }

    static boolean isAutoUpdateEnabled() {
        return PrefUtils.getBoolean(PrefUtils.REFRESH_ENABLED, true);
    }

    static long getTimeIntervalInMSecs() {

        long time = 3600L * 1000;
        try {
            time = Math.max(60L * 1000, Long.parseLong(PrefUtils.getString(PrefUtils.REFRESH_INTERVAL, SIXTY_MINUTES)));
        } catch (Exception ignored) {
        }
        return time;
    }


    static JobInfo GetPendingJobByID(JobScheduler jobScheduler, int ID) {
        if ( Build.VERSION.SDK_INT >= 24 ) {
            return jobScheduler.getPendingJob( ID );
        } else {
            for ( JobInfo item: jobScheduler.getAllPendingJobs() )
                if ( item.getId() == ID )
                    return item;
            return null;
        }
    }
}
