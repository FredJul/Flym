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

package net.fred.feedex.utils;

import android.database.ContentObserver;
import android.os.Handler;
import android.os.SystemClock;

public abstract class ThrottledContentObserver extends ContentObserver {

    private final long mUpdateThrottle;
    private final Handler mHandler;
    private Runnable mOnChangeTask = null;

    public ThrottledContentObserver(Handler handler, long delayMS) {
        super(handler);
        mUpdateThrottle = delayMS;
        mHandler = handler;
    }

    @Override
    public void onChange(final boolean selfChange) {
        if (mOnChangeTask == null) {
            mOnChangeTask = new Runnable() {
                @Override
                public void run() {
                    onChangeThrottled();
                    mOnChangeTask = null;
                }
            };

            long now = SystemClock.uptimeMillis();
            mHandler.postAtTime(mOnChangeTask, now + mUpdateThrottle);
        }
        super.onChange(selfChange);
    }

    abstract public void onChangeThrottled();
}
