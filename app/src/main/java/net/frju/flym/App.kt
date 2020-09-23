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

package net.frju.flym

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.os.strictmode.UntaggedSocketViolation
import android.util.Log
import net.fred.feedex.BuildConfig
import net.frju.flym.data.AppDatabase
import net.frju.flym.data.utils.PrefConstants
import net.frju.flym.utils.putPrefBoolean
import java.util.concurrent.Executors


class App : Application() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        @JvmStatic
        lateinit var context: Context
            private set

        @JvmStatic
        lateinit var db: AppDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()

        context = applicationContext
        db = AppDatabase.createDatabase(context)

        context.putPrefBoolean(PrefConstants.IS_REFRESHING, false) // init

        // Enable strict mode to find performance issues in debug build
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyFlashScreen()
                    .build())
            val vmPolicy = VmPolicy.Builder().detectAll()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                vmPolicy.penaltyListener(Executors.newSingleThreadExecutor(), {
                    // Hide UntaggedSocketViolations since they are useless and unfixable in okhttp and glide
                    if (it !is UntaggedSocketViolation) {
                        Log.d("StrictMode", "StrictMode policy violation: " + it.stackTrace)
                    }
                })
            } else {
                vmPolicy.penaltyLog()
            }
            StrictMode.setVmPolicy(vmPolicy.build())
        }
    }
}
