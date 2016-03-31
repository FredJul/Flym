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

import android.content.Context;
import android.content.Intent;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.google.android.gms.gcm.TaskParams;

import net.fred.feedex.Constants;
import net.fred.feedex.utils.PrefUtils;

public class AutoRefreshService extends GcmTaskService {
    public static final String SIXTY_MINUTES = "3600000";
    public static final String TASK_TAG_PERIODIC = "TASK_TAG_PERIODIC";

    @Override
    public int onRunTask(TaskParams taskParams) {
        getBaseContext().startService(new Intent(getBaseContext(), FetcherService.class).setAction(FetcherService.ACTION_REFRESH_FEEDS).putExtra(Constants.FROM_AUTO_REFRESH, true));

        return GcmNetworkManager.RESULT_SUCCESS;
    }

    public static void initAutoRefresh(Context context) {
        GcmNetworkManager gcmNetworkManager = GcmNetworkManager.getInstance(context);

        long time = 3600L;
        try {
            time = Math.max(60L, Long.parseLong(PrefUtils.getString(PrefUtils.REFRESH_INTERVAL, SIXTY_MINUTES)) / 1000);
        } catch (Exception ignored) {
        }

        if (PrefUtils.getBoolean(PrefUtils.REFRESH_ENABLED, true)) {
            PeriodicTask task = new PeriodicTask.Builder()
                    .setService(AutoRefreshService.class)
                    .setTag(TASK_TAG_PERIODIC)
                    .setPeriod(time)
                    .setPersisted(true)
                    .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                    .setUpdateCurrent(true)
                    .build();

            gcmNetworkManager.schedule(task);
        } else {
            gcmNetworkManager.cancelTask(TASK_TAG_PERIODIC, AutoRefreshService.class);
        }
    }
}
