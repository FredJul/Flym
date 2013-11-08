/**
 * FeedEx
 *
 * Copyright (c) 2012-2013 Frederic Julian
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.fred.feedex.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ProgressBar;

import net.fred.feedex.Constants;
import net.fred.feedex.R;

public abstract class BaseActivity extends Activity {

    private static final String STATE_IS_FULLSCREEN = "STATE_IS_FULLSCREEN";

    private ProgressBar mProgressBar;
    private boolean mIsFullScreen;

    @Override
    protected void onResume() {
        if (Constants.NOTIF_MGR != null) {
            Constants.NOTIF_MGR.cancel(0);
        }

        if (mIsFullScreen && getActionBar().isShowing()) { // This is needed for the immersive mode
            mIsFullScreen = false;
            toggleFullScreen();
        }

        super.onResume();
    }

    @Override
    public void setContentView(View view) {
        init().addView(view);
    }

    @Override
    public void setContentView(int layoutResID) {
        getLayoutInflater().inflate(layoutResID, init(), true);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        init().addView(view, params);
    }

    private ViewGroup init() {
        super.setContentView(R.layout.activity_progress);
        mProgressBar = (ProgressBar) findViewById(R.id.activity_bar);
        return (ViewGroup) findViewById(R.id.activity_frame);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(STATE_IS_FULLSCREEN, mIsFullScreen);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState.getBoolean(STATE_IS_FULLSCREEN)) {
            toggleFullScreen();
        }

        super.onRestoreInstanceState(savedInstanceState);
    }

    public ProgressBar getProgressBar() {
        return mProgressBar;
    }

    public boolean isFullScreen() {
        return mIsFullScreen;
    }

    public void toggleFullScreen() {
        mIsFullScreen = !mIsFullScreen;

        if (mIsFullScreen) {
            if (android.os.Build.VERSION.SDK_INT >= 19) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            } else {
                getActionBar().hide();
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            }
        } else {
            if (android.os.Build.VERSION.SDK_INT >= 19) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            } else {
                getActionBar().show();
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        }
    }
}