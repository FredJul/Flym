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
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import net.fred.feedex.Constants;
import net.fred.feedex.MainApplication;
import net.fred.feedex.R;
import net.fred.feedex.adapter.DrawerAdapter;
import net.fred.feedex.fragment.EntriesListFragment;
import net.fred.feedex.provider.FeedData;
import net.fred.feedex.service.FetcherService;
import net.fred.feedex.service.RefreshService;
import net.fred.feedex.utils.PrefUtils;
import net.fred.feedex.utils.UiUtils;

public class MainActivity extends ProgressActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String STATE_CURRENT_DRAWER_POS = "STATE_CURRENT_DRAWER_POS";

    private static final int LOADER_ID = 0;

    private final SharedPreferences.OnSharedPreferenceChangeListener isRefreshingListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (PrefUtils.IS_REFRESHING.equals(key)) {
                getProgressBar().setVisibility(PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false) ? View.VISIBLE : View.GONE);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UiUtils.setPreferenceTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mEntriesFragment = (EntriesListFragment) getFragmentManager().findFragmentById(R.id.fragment);

        mTitle = getTitle();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mDrawerList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectDrawerItem(position);
                mDrawerLayout.closeDrawer(mDrawerList);
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
                startService(new Intent(MainActivity.this, FetcherService.class).setAction(FetcherService.ACTION_REFRESH_FEEDS));
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

        if (Constants.NOTIF_MGR != null) {
            Constants.NOTIF_MGR.cancel(0);
        }
    }

    @Override
    protected void onPause() {
        PrefUtils.unregisterOnPrefChangeListener(isRefreshingListener);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean isOpened = mDrawerLayout.isDrawerOpen(mDrawerList);
        if (isOpened && !mIsDrawerMoving || !isOpened && mIsDrawerMoving) {
            getActionBar().setTitle(R.string.app_name);
            getActionBar().setIcon(R.drawable.icon);

            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.drawer, menu);

            mEntriesFragment.setHasOptionsMenu(false);
        } else {
            refreshTitleAndIcon();
            invalidateOptionsMenu();

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
            case R.id.menu_edit:
                startActivity(new Intent(this, FeedsListActivity.class));
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

    private void selectDrawerItem(int position) {
        mCurrentDrawerPos = position;
        mIcon = null;

        Uri newUri;
        boolean showFeedInfo = true;

        switch (position) {
            case 0:
                newUri = FeedData.EntryColumns.CONTENT_URI;
                break;
            case 1:
                newUri = FeedData.EntryColumns.FAVORITES_CONTENT_URI;
                break;
            default:
                long feedOrGroupId = mDrawerAdapter.getItemId(position);
                if (mDrawerAdapter.isItemAGroup(position)) {
                    newUri = FeedData.EntryColumns.ENTRIES_FOR_GROUP_CONTENT_URI(feedOrGroupId);
                } else {
                    byte[] iconBytes = mDrawerAdapter.getItemIcon(position);
                    if (iconBytes != null && iconBytes.length > 0) {
                        int bitmapSizeInDip = UiUtils.dpToPixel(24);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
                        if (bitmap != null) {
                            if (bitmap.getHeight() != bitmapSizeInDip) {
                                bitmap = Bitmap.createScaledBitmap(bitmap, bitmapSizeInDip, bitmapSizeInDip, false);
                            }

                            mIcon = new BitmapDrawable(getResources(), bitmap);
                        }
                    }

                    newUri = FeedData.EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(feedOrGroupId);
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
        CursorLoader cursorLoader = new CursorLoader(this, FeedData.FeedColumns.GROUPED_FEEDS_CONTENT_URI, new String[]{FeedData.FeedColumns._ID, FeedData.FeedColumns.URL,
                FeedData.FeedColumns.NAME, FeedData.FeedColumns.IS_GROUP, FeedData.FeedColumns.GROUP_ID, FeedData.FeedColumns.ICON, FeedData.FeedColumns.LAST_UPDATE, FeedData.FeedColumns.ERROR,
                "(SELECT COUNT(*) FROM " + FeedData.EntryColumns.TABLE_NAME + " WHERE " + FeedData.EntryColumns.IS_READ + " IS NULL AND " + FeedData.EntryColumns.FEED_ID + "="
                        + FeedData.FeedColumns.TABLE_NAME + "." + FeedData.FeedColumns._ID + ")",
                "(SELECT COUNT(*) FROM " + FeedData.EntryColumns.TABLE_NAME + " WHERE " + FeedData.EntryColumns.IS_READ + " IS NULL)",
                "(SELECT COUNT(*) FROM " + FeedData.EntryColumns.TABLE_NAME + " WHERE " + FeedData.EntryColumns.IS_FAVORITE + Constants.DB_IS_TRUE + ")"}, null, null, null);
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
    }
}
