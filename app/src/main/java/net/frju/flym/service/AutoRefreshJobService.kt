/**
 * Flym
 *
 *
 * Copyright (c) 2012-2015 Frederic Julian
 *
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 *
 *
 *
 *
 * Some parts of this software are based on "Sparse rss" under the MIT license (see
 * below). Please refers to the original project to identify which parts are under the
 * MIT license.
 *
 *
 * Copyright (c) 2010-2012 Stefan Handschuh
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.frju.flym.service

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import net.frju.parentalcontrol.utils.PrefUtils
import org.jetbrains.anko.doAsync

class AutoRefreshJobService : JobService() {

    companion object {
        val SIXTY_MINUTES = "3600000"
        val JOB_ID = 0

        fun initAutoRefresh(context: Context) {

            var time = 3600000L
            try {
                time = Math.max(60L, java.lang.Long.parseLong(PrefUtils.getString(PrefUtils.REFRESH_INTERVAL, SIXTY_MINUTES)) / 1000)
            } catch (ignored: Exception) {
            }

            if (PrefUtils.getBoolean(PrefUtils.REFRESH_ENABLED, true)) {
                val builder = JobInfo.Builder(JOB_ID, ComponentName(context, AutoRefreshJobService::class.java))
                        .setPeriodic(time)
                        .setPersisted(true)
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)

                val tm = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                tm.schedule(builder.build())
            } else {
                val tm = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                tm.cancel(JOB_ID)
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
