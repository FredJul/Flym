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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import net.fred.feedex.Constants;

public abstract class BaseActivity extends Activity {

    public interface OnFullScreenListener {
        public void onFullScreenEnabled(boolean isImmersive);

        public void onFullScreenDisabled();
    }

    public interface OnRefreshListener extends SwipeRefreshLayout.OnRefreshListener {
        public boolean canChildScrollUp();
    }

    private static final String STATE_IS_FULLSCREEN = "STATE_IS_FULLSCREEN";

    private boolean mIsFullScreen;
    private View mDecorView;

    private OnFullScreenListener mOnFullScreenListener;
    private OnRefreshListener mOnRefreshListener;

    private SwipeRefreshLayout mRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRefreshLayout = new SwipeRefreshLayout(this) {
            @Override
            public boolean canChildScrollUp() {
                if (mOnRefreshListener != null) {
                    return mOnRefreshListener.canChildScrollUp();
                }

                return true;
            }
        };
        super.setContentView(mRefreshLayout);

        mDecorView = getWindow().getDecorView();
        mDecorView.setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) { // We are not un fullscreen mode

                            if (mIsFullScreen) { // It was fullscreen, we need to change it
                                toggleFullScreen();
                                mIsFullScreen = false;

                                if (mOnFullScreenListener != null) {
                                    mOnFullScreenListener.onFullScreenDisabled();
                                }
                            }
                        } else { // We are now in fullscreen mode
                            if (!mIsFullScreen) { // It was not-fullscreen, we need to change it
                                mIsFullScreen = true;

                                if (mOnFullScreenListener != null) {
                                    mOnFullScreenListener.onFullScreenEnabled(android.os.Build.VERSION.SDK_INT >= 19);
                                }
                            }
                        }
                    }
                });
    }

    @Override
    public void setContentView(int layoutResID) {
        View v = getLayoutInflater().inflate(layoutResID, mRefreshLayout, false);
        setContentView(v);
    }

    @Override
    public void setContentView(View view) {
        setContentView(view, view.getLayoutParams());
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        mRefreshLayout.addView(view, params);
        initSwipeOptions();
    }

    public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
        mOnRefreshListener = onRefreshListener;
        mRefreshLayout.setOnRefreshListener(onRefreshListener);
    }

    private void initSwipeOptions() {
        setAppearance();
        disableSwipe();
    }

    private void setAppearance() {
        mRefreshLayout.setColorScheme(android.R.color.holo_blue_bright,
                android.R.color.holo_blue_dark,
                android.R.color.holo_blue_bright,
                android.R.color.holo_blue_dark);
    }

    /**
     * It shows the SwipeRefreshLayout progress
     */
    public void showSwipeProgress() {
        mRefreshLayout.setRefreshing(true);
    }

    /**
     * It shows the SwipeRefreshLayout progress
     */
    public void hideSwipeProgress() {
        mRefreshLayout.setRefreshing(false);
    }

    /**
     * Enables swipe gesture
     */
    public void enableSwipe() {
        mRefreshLayout.setEnabled(true);
    }

    /**
     * Disables swipe gesture. It prevents manual gestures but keeps the option tu show
     * refreshing programatically.
     */
    public void disableSwipe() {
        mRefreshLayout.setEnabled(false);
    }

    /**
     * Get the refreshing status
     */
    public boolean isRefreshing() {
        return mRefreshLayout.isRefreshing();
    }

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

    public void setOnFullscreenListener(OnFullScreenListener listener) {
        mOnFullScreenListener = listener;
    }

    public boolean isFullScreen() {
        return mIsFullScreen;
    }

    @SuppressLint("InlinedApi")
    public void toggleFullScreen() {
        if (!mIsFullScreen) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            } else {
                mIsFullScreen = true;

                getActionBar().hide();
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

                if (mOnFullScreenListener != null) {
                    mOnFullScreenListener.onFullScreenEnabled(false);
                }
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            } else {
                mIsFullScreen = false;

                getActionBar().show();
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

                if (mOnFullScreenListener != null) {
                    mOnFullScreenListener.onFullScreenDisabled();
                }
            }
        }
    }
}