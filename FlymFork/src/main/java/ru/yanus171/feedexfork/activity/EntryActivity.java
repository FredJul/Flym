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

package ru.yanus171.feedexfork.activity;

import android.content.ContentResolver;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.fragment.EntryFragment;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.UiUtils;

import static ru.yanus171.feedexfork.utils.PrefUtils.DISPLAY_ENTRIES_FULLSCREEN;
import static ru.yanus171.feedexfork.utils.PrefUtils.getBoolean;

public class EntryActivity extends BaseActivity {

    public EntryFragment mEntryFragment = null;

    private static final String STATE_IS_STATUSBAR_HIDDEN = "STATE_IS_STATUSBAR_HIDDEN";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UiUtils.setPreferenceTheme(this);
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_entry);
        mEntryFragment = (EntryFragment) getSupportFragmentManager().findFragmentById(R.id.entry_fragment);
        if (savedInstanceState == null) { // Put the data only the first time (the fragment will save its state)
            mEntryFragment.setData(getIntent().getData());
        }
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });


        FetcherService.mCancelRefresh = false;

        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        // Note that system bars will only be "visible" if none of the
                        // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
                        //setFullScreen( (visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0, GetIsActionBarHidden() );
                        //PrefUtils.putBoolean(STATE_IS_STATUSBAR_HIDDEN, (visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) != 0);
                    }
                });

        if (getBoolean(DISPLAY_ENTRIES_FULLSCREEN, false))
            setFullScreen(true, true);

    }

    @Override
    public void onWindowFocusChanged (boolean hasFocus) {
        if ( hasFocus )
            setFullScreen();
    }

    private static final String STATE_IS_ACTIONBAR_HIDDEN = "STATE_IS_ACTIONBAR_HIDDEN";


    //public boolean mIsStatusBarHidden, mIsActionBarHidden;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Bundle b = getIntent().getExtras();
            if (b != null && b.getBoolean(Constants.INTENT_FROM_WIDGET, false)) {
                Intent intent = new Intent(this, HomeActivity.class);
                startActivity(intent);
            }
            finish();
            return true;
        }

        return false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        mEntryFragment.setData(intent.getData());
    }

    @Override
    public void onBackPressed() {
        /*SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext()).edit();
        editor.putLong(PrefUtils.LAST_ENTRY_ID, 0);
        editor.putString(PrefUtils.LAST_ENTRY_URI, "");
        editor.commit();*/
        PrefUtils.putLong(PrefUtils.LAST_ENTRY_ID, 0);
        PrefUtils.putString(PrefUtils.LAST_ENTRY_URI, "");
        FetcherService.clearActiveEntryID();
        new Thread() {
            @Override
            public void run() {
                ContentResolver cr = getContentResolver();
                cr.delete(FeedData.TaskColumns.CONTENT_URI, FeedData.TaskColumns.ENTRY_ID + " = " + mEntryFragment.getCurrentEntryID(), null);
                FetcherService.setDownloadImageCursorNeedsRequery( true );
            }
        }.start();

        super.onBackPressed();
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {

//        outState.putBoolean(STATE_IS_ACTIONBAR_HIDDEN, mIsActionBarHidden);
//        outState.putBoolean(STATE_IS_STATUSBAR_HIDDEN, mIsStatusBarHidden);

        super.onSaveInstanceState(outState);
    }
    @Override
    //protected void onRestoreInstanceState(Bundle savedInstanceState) {
    protected void onResume() {
//        setFullScreen(savedInstanceState.getBoolean(STATE_IS_STATUSBAR_HIDDEN),
//                savedInstanceState.getBoolean(STATE_IS_ACTIONBAR_HIDDEN));

        //super.onRestoreInstanceState(savedInstanceState);
        super.onResume();
        setFullScreen();
    }

    public void setFullScreen() {
        setFullScreen(GetIsStatusBarHidden(), GetIsActionBarHidden());
    }

    static public boolean GetIsStatusBarHidden() {
        return PrefUtils.getBoolean(STATE_IS_STATUSBAR_HIDDEN, false);
    }
    static public boolean GetIsActionBarHidden() {
        return PrefUtils.getBoolean(STATE_IS_ACTIONBAR_HIDDEN, false);
    }

    public void setFullScreen(boolean statusBarHidden, boolean actionBarHidden ) {
        //mIsStatusBarHidden = statusBarHidden;
        //mIsActionBarHidden = actionBarHidden;
        PrefUtils.putBoolean(STATE_IS_STATUSBAR_HIDDEN, statusBarHidden);
        PrefUtils.putBoolean(STATE_IS_ACTIONBAR_HIDDEN, actionBarHidden);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (statusBarHidden) {
                mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

            } else {
                mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }
        } else {
            setFullScreenOld( statusBarHidden );
        }

        if (getSupportActionBar() != null) {
            if ( actionBarHidden )
                getSupportActionBar().hide();
            else
                getSupportActionBar().show();
        }
        if ( mEntryFragment != null ) {
            mEntryFragment.UpdateProgress();
            mEntryFragment.UpdateClock();
        }
    }

    private void setFullScreenOld(boolean fullScreen ) {
        if (fullScreen) {

            if (GetIsStatusBarHidden()) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            } else {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            }
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }
}