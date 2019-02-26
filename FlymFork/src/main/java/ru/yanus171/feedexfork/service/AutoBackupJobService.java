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

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;

public class AutoBackupJobService extends JobService {
    public static final int AUTO_BACKUP_JOB_ID = 3;

    public AutoBackupJobService() {
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        //if (AutoService.isAutoUpdateEnabled() ) {
            FetcherService.StartService( new Intent(MainApplication.getContext(), FetcherService.class).putExtra( Constants.FROM_AUTO_BACKUP, true ) );
        //}
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void initAutoBackup(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if ( AutoService.GetPendingJobByID( jobScheduler, AUTO_BACKUP_JOB_ID ) == null ) {
                ComponentName serviceComponent = new ComponentName(context, AutoBackupJobService.class);
                JobInfo.Builder builder =
                        new JobInfo.Builder(AUTO_BACKUP_JOB_ID, serviceComponent)
                                .setPeriodic(AutoService.getTimeIntervalInMSecs())
                                .setRequiresCharging(false)
                                .setPersisted(true)
                        //.setRequiresDeviceIdle(true)
                        ;
                if (Build.VERSION.SDK_INT >= 26) {
                    builder.setRequiresStorageNotLow(true)
                            .setRequiresBatteryNotLow(true);
                }
                jobScheduler.schedule(builder.build());
            }

    }


}
