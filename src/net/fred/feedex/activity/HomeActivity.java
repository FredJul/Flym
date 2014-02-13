/**
 * FeedEx
 *
 * Copyright (c) 2012-2013 Frederic Julian
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.fred.feedex.activity;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import net.fred.feedex.Constants;
import net.fred.feedex.MainApplication;
import net.fred.feedex.R;
import net.fred.feedex.adapter.DrawerAdapter;
import net.fred.feedex.fragment.EntriesListFragment;
import net.fred.feedex.provider.FeedData;
import net.fred.feedex.provider.FeedData.EntryColumns;
import net.fred.feedex.provider.FeedData.FeedColumns;
import net.fred.feedex.service.FetcherService;
import net.fred.feedex.service.RefreshService;
import net.fred.feedex.utils.PrefUtils;
import net.fred.feedex.utils.UiUtils;

public class HomeActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String STATE_CURRENT_DRAWER_POS = "STATE_CURRENT_DRAWER_POS";

    private static final String FEED_UNREAD_NUMBER = new StringBuilder("(SELECT COUNT(*) FROM ").append(EntryColumns.TABLE_NAME).append(" WHERE ").append(EntryColumns.IS_READ)
            .append(" IS NULL AND ").append(EntryColumns.FEED_ID).append('=').append(FeedColumns.TABLE_NAME).append('.').append(FeedColumns._ID).append(')').toString();

    private static final String WHERE_UNREAD_ONLY = new StringBuilder("(SELECT COUNT(*) FROM ").append(EntryColumns.TABLE_NAME).append(" WHERE ").append(EntryColumns.IS_READ)
            .append(" IS NULL AND ").append(EntryColumns.FEED_ID).append("=").append(FeedColumns.TABLE_NAME).append('.').append(FeedColumns._ID).append(") > 0").append(" OR (")
            .append(FeedColumns.IS_GROUP).append("=1 AND (SELECT COUNT(*) FROM ").append(FeedData.ENTRIES_TABLE_WITH_FEED_INFO).append(" WHERE ").append(EntryColumns.IS_READ)
            .append(" IS NULL AND ").append(FeedColumns.GROUP_ID).append('=').append(FeedColumns.TABLE_NAME).append('.').append(FeedColumns._ID).append(") > 0)").toString();

    private static final int LOADER_ID = 0;

    private final SharedPreferences.OnSharedPreferenceChangeListener isRefreshingListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (PrefUtils.IS_REFRESHING.equals(key)) {
                getProgressBar().setVisibility(PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false) ? View.VISIBLE : View.GONE);
            }
            if (PrefUtils.SHOW_READ.equals(key)) {
                getLoaderManager().restartLoader(LOADER_ID, null, HomeActivity.this);
            }
        }
    };

    private EntriesListFragment mEntriesFragment;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private DrawerAdapter mDrawerAdapter;
    private ActionBarDrawerToggle mDrawerToggle;

    private CharSequence mTitle;
    private BitmapDrawable mIcon;
    private int mCurrentDrawerPos;

    private boolean mIsDrawerMoving = false;

    private boolean mCanQuit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UiUtils.setPreferenceTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        mEntriesFragment = (EntriesListFragment) getFragmentManager().findFragmentById(R.id.entries_list_fragment);

        mTitle = getTitle();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mDrawerList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectDrawerItem(position);
                mDrawerLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mDrawerLayout.closeDrawer(mDrawerList);
                    }
                }, 50);
            }
        });
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerStateChanged(int newState) {
                if (mIsDrawerMoving && newState == DrawerLayout.STATE_IDLE) {
                    mIsDrawerMoving = false;
                    invalidateOptionsMenu();
                } else if (!mIsDrawerMoving) {
                    mIsDrawerMoving = true;
                    invalidateOptionsMenu();
                }

                super.onDrawerStateChanged(newState);
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if (savedInstanceState != null) {
            mCurrentDrawerPos = savedInstanceState.getInt(STATE_CURRENT_DRAWER_POS);
        }

        getLoaderManager().initLoader(LOADER_ID, null, this);

        if (PrefUtils.getBoolean(PrefUtils.REFRESH_ENABLED, true)) {
            // starts the service independent to this activity
            startService(new Intent(this, RefreshService.class));
        } else {
            stopService(new Intent(this, RefreshService.class));
        }
        if (PrefUtils.getBoolean(PrefUtils.REFRESH_ON_OPEN_ENABLED, false)) {
            if (!PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false)) {
                startService(new Intent(HomeActivity.this, FetcherService.class).setAction(FetcherService.ACTION_REFRESH_FEEDS));
            }
        }
    }

    private void refreshTitleAndIcon() {
        getActionBar().setTitle(mTitle);
        switch (mCurrentDrawerPos) {
            case 0:
                getActionBar().setTitle(R.string.all);
                getActionBar().setIcon(R.drawable.ic_statusbar_rss);
                break;
            case 1:
                getActionBar().setTitle(R.string.favorites);
                getActionBar().setIcon(R.drawable.dimmed_rating_important);
                break;
            case 2:
                getActionBar().setTitle(android.R.string.search_go);
                getActionBar().setIcon(R.drawable.action_search);
                break;
            default:
                getActionBar().setTitle(mTitle);
                if (mIcon != null) {
                    getActionBar().setIcon(mIcon);
                }
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_CURRENT_DRAWER_POS, mCurrentDrawerPos);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getProgressBar().setVisibility(PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false) ? View.VISIBLE : View.GONE);
        PrefUtils.registerOnPrefChangeListener(isRefreshingListener);
    }

    @Override
    protected void onPause() {
        PrefUtils.unregisterOnPrefChangeListener(isRefreshingListener);
        super.onPause();
    }

    @Override
    public void finish() {
        if (mCanQuit) {
            super.finish();
            return;
        }

        Toast.makeText(this, R.string.back_again_to_quit, Toast.LENGTH_SHORT).show();
        mCanQuit = true;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mCanQuit = false;
            }
        }, 3000);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        boolean isOpened = mDrawerLayout.isDrawerOpen(mDrawerList);
        if (isOpened && !mIsDrawerMoving || !isOpened && mIsDrawerMoving) {
            getActionBar().setTitle(R.string.app_name);
            getActionBar().setIcon(R.drawable.icon);

            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.drawer, menu);

            if (!PrefUtils.getBoolean(PrefUtils.SHOW_READ, true)) {
                menu.findItem(R.id.menu_hide_read_main).setTitle(R.string.context_menu_show_read).setIcon(R.drawable.view_reads);
            }

            mEntriesFragment.setHasOptionsMenu(false);
        } else {
            refreshTitleAndIcon();
            mEntriesFragment.setHasOptionsMenu(true);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.menu_hide_read_main:
                if (!PrefUtils.getBoolean(PrefUtils.SHOW_READ, true)) {
                    PrefUtils.putBoolean(PrefUtils.SHOW_READ, true);
                    item.setTitle(R.string.context_menu_hide_read).setIcon(R.drawable.hide_reads);
                } else {
                    PrefUtils.putBoolean(PrefUtils.SHOW_READ, false);
                    item.setTitle(R.string.context_menu_show_read).setIcon(R.drawable.view_reads);
                }
                return true;
            case R.id.menu_edit_main:
                startActivity(new Intent(this, EditFeedsListActivity.class));
                return true;
            case R.id.menu_refresh_main:
                if (!PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false)) {
                    MainApplication.getContext().startService(new Intent(MainApplication.getContext(), FetcherService.class).setAction(FetcherService.ACTION_REFRESH_FEEDS));
                }
                return true;
            case R.id.menu_settings_main:
                startActivity(new Intent(this, GeneralPrefsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        CursorLoader cursorLoader = new CursorLoader(this, FeedColumns.GROUPED_FEEDS_CONTENT_URI, new String[]{FeedColumns._ID, FeedColumns.URL, FeedColumns.NAME,
                FeedColumns.IS_GROUP, FeedColumns.GROUP_ID, FeedColumns.ICON, FeedColumns.LAST_UPDATE, FeedColumns.ERROR, FEED_UNREAD_NUMBER},
                PrefUtils.getBoolean(PrefUtils.SHOW_READ, true) ? "" : WHERE_UNREAD_ONLY, null, null);
        cursorLoader.setUpdateThrottle(Constants.UPDATE_THROTTLE_DELAY);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (mDrawerAdapter != null) {
            mDrawerAdapter.setCursor(cursor);
        } else {
            mDrawerAdapter = new DrawerAdapter(this, cursor);
            mDrawerList.setAdapter(mDrawerAdapter);

            // We don't have any menu yet, we need to display it
            mDrawerList.post(new Runnable() {
                @Override
                public void run() {
                    selectDrawerItem(mCurrentDrawerPos);
                    refreshTitleAndIcon();
                }
            });
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mDrawerAdapter.setCursor(null);
    }

    private void selectDrawerItem(int position) {
        mCurrentDrawerPos = position;
        mIcon = null;

        Uri newUri;
        boolean showFeedInfo = true;

        switch (position) {
            case 0:
                newUri = EntryColumns.ALL_ENTRIES_CONTENT_URI;
                break;
            case 1:
                newUri = EntryColumns.FAVORITES_CONTENT_URI;
                break;
            case 2:
                newUri = EntryColumns.SEARCH_URI(mEntriesFragment.getCurrentSearch());
                break;
            default:
                long feedOrGroupId = mDrawerAdapter.getItemId(position);
                if (mDrawerAdapter.isItemAGroup(position)) {
                    newUri = EntryColumns.ENTRIES_FOR_GROUP_CONTENT_URI(feedOrGroupId);
                } else {
                    byte[] iconBytes = mDrawerAdapter.getItemIcon(position);
                    Bitmap bitmap = UiUtils.getScaledBitmap(iconBytes, 24);
                    if (bitmap != null) {
                        mIcon = new BitmapDrawable(getResources(), bitmap);
                    }

                    newUri = EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(feedOrGroupId);
                    showFeedInfo = false;
                }
                mTitle = mDrawerAdapter.getItemName(position);
                break;
        }

        if (!newUri.equals(mEntriesFragment.getUri())) {
            mEntriesFragment.setData(newUri, showFeedInfo);
        }

        mDrawerList.setItemChecked(position, true);

        // First open => we open the drawer for you
        if (PrefUtils.getBoolean(PrefUtils.FIRST_OPEN, true)) {
            PrefUtils.putBoolean(PrefUtils.FIRST_OPEN, false);
            mDrawerLayout.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDrawerLayout.openDrawer(mDrawerList);
                }
            }, 500);
        }
    }
}
