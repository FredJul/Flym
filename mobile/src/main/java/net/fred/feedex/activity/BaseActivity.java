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
package net.fred.feedex.activity;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;

import net.fred.feedex.Constants;
import net.fred.feedex.utils.PrefUtils;

public abstract class BaseActivity extends AppCompatActivity {

    //private static final String STATE_IS_NORMAL_FULLSCREEN = "STATE_IS_NORMAL_FULLSCREEN";
    //private static final String STATE_IS_IMMERSIVE_FULLSCREEN = "STATE_IS_IMMERSIVE_FULLSCREEN";
    protected View mDecorView;
    //private OnFullScreenListener mOnFullScreenListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDecorView = getWindow().getDecorView();

        /*(// For immersive mode
        if (android.os.Build.VERSION.SDK_INT >= 19) {
            mDecorView.setOnSystemUiVisibilityChangeListener
                    (new View.OnSystemUiVisibilityChangeListener() {
                        @Override
                        public void onSystemUiVisibilityChange(int visibility) {
                            if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) { // We are not in fullscreen mode

                                if (mIsImmersiveFullScreen) { // It was fullscreen, we need to change it
                                    setImmersiveFullScreen(false);
                                    mIsImmersiveFullScreen = false;

                                    if (mOnFullScreenListener != null) {
                                        mOnFullScreenListener.onFullScreenDisabled();
                                    }
                                }
                            } else { // We are now in fullscreen mode
                                if (!mIsImmersiveFullScreen) { // It was not-fullscreen, we need to change it
                                    mIsImmersiveFullScreen = true;

                                    if (mOnFullScreenListener != null) {
                                        mOnFullScreenListener.onFullScreenEnabled(true, false);
                                    }
                                }
                            }
                        }
                    });
        }*/
        if (android.os.Build.VERSION.SDK_INT >= 21) { // Lollipop
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS); // Tweak to allow setting status bar color
        }
    }

    @Override
    protected void onResume() {
        if (Constants.NOTIF_MGR != null) {
            Constants.NOTIF_MGR.cancel(0);
        }

        super.onResume();
    }


    /*public void setImmersiveFullScreen(boolean actionBarHidden) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (actionBarHidden) {
                if (getSupportActionBar() != null)
                    getSupportActionBar().hide();
                mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            } else {

                if (getSupportActionBar() != null)
                    getSupportActionBar().show();
                mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }
        } else {
            setNormalFullScreen(actionBarHidden);
        }
    }

    public void setNormalFullScreen(boolean fullScreen) {
        if (fullScreen) {
            mIsNormalFullScreen = true;

            if (getSupportActionBar() != null) {
                getSupportActionBar().hide();
            }
            if (PrefUtils.getBoolean(PrefUtils.FULL_SCREEN_STATUSBAR_VISIBLE, false)) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            } else {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            }
//            if (mOnFullScreenListener != null) {
//                mOnFullScreenListener.onFullScreenEnabled(false, isImmersiveFallback);
//            }
        } else {
            mIsNormalFullScreen = false;

            if (getSupportActionBar() != null) {
                getSupportActionBar().show();
            }
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            if (mOnFullScreenListener != null) {
                mOnFullScreenListener.onFullScreenDisabled();
            }
        }
    }*/

}
