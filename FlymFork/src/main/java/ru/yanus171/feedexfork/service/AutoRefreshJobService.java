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

public class AutoRefreshJobService extends JobService {
    public static final int AUTO_UPDATE_JOB_ID = 1;

    public AutoRefreshJobService() {
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        if (AutoService.isAutoUpdateEnabled() ) {
            Intent intent = AutoService.GetAutoRefreshServiceIntent();
            if (Build.VERSION.SDK_INT >= 26)
                getBaseContext().startForegroundService(intent);
            else
                getBaseContext().startService(intent);
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    static void initAutoRefresh(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (AutoService.isAutoUpdateEnabled() ) {
            if ( AutoService.GetPendingJobByID( jobScheduler, AUTO_UPDATE_JOB_ID ) == null ) {
                ComponentName serviceComponent = new ComponentName(context, AutoRefreshJobService.class);
                JobInfo.Builder builder =
                        new JobInfo.Builder(AUTO_UPDATE_JOB_ID, serviceComponent)
                                .setPeriodic(AutoService.getTimeIntervalInMSecs())
                                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
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
        } else
            jobScheduler.cancel(AUTO_UPDATE_JOB_ID);

    }

}
