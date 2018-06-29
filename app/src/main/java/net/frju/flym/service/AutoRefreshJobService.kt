/*
 * Copyright (c) 2012-2018 Frederic Julian
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
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package net.frju.flym.service

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import net.frju.flym.data.utils.PrefUtils
import org.jetbrains.anko.doAsync

class AutoRefreshJobService : JobService() {

    companion object {
        private const val TWO_HOURS = "7200"
        private const val JOB_ID = 0

        fun initAutoRefresh(context: Context) {

            // DO NOT USE ANKO TO RETRIEVE THE SERVICE HERE (crash on API 21)
            val jobSchedulerService = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

            val time = Math.max(300, PrefUtils.getString(PrefUtils.REFRESH_INTERVAL, TWO_HOURS).toInt())

            if (PrefUtils.getBoolean(PrefUtils.REFRESH_ENABLED, true)) {
                val builder = JobInfo.Builder(JOB_ID, ComponentName(context, AutoRefreshJobService::class.java))
                        .setPeriodic(time * 1000L)
                        .setPersisted(true)
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)

                jobSchedulerService.schedule(builder.build())
            } else {
                jobSchedulerService.cancel(JOB_ID)
            }
        }
    }

    override fun onStartJob(params: JobParameters): Boolean {
        if (!PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false)) {
            doAsync {
                FetcherService.fetch(this@AutoRefreshJobService, true, FetcherService.ACTION_REFRESH_FEEDS)
                jobFinished(params, false)
            }
            return true
        }

        return false
    }

    override fun onStopJob(params: JobParameters): Boolean {
        return false
    }
}
