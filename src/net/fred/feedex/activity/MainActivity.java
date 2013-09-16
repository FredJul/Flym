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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
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
import net.fred.feedex.PrefUtils;
import net.fred.feedex.R;
import net.fred.feedex.UiUtils;
import net.fred.feedex.adapter.DrawerAdapter;
import net.fred.feedex.fragment.EntriesListFragment;
import net.fred.feedex.fragment.FeedsListFragment;
import net.fred.feedex.provider.FeedData;
import net.fred.feedex.service.FetcherService;
import net.fred.feedex.service.RefreshService;

import java.util.Random;

public class MainActivity extends ProgressFragmentActivity {

    private static final String STATE_CURRENT_DRAWER_POS = "STATE_CURRENT_DRAWER_POS";

    private final SharedPreferences.OnSharedPreferenceChangeListener isRefreshingListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (PrefUtils.IS_REFRESHING.equals(key)) {
                getProgressBar().setVisibility(PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false) ? View.VISIBLE : View.GONE);
            }
        }
    };

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private DrawerAdapter mDrawerAdapter;
    private ActionBarDrawerToggle mDrawerToggle;

    private CharSequence mTitle;
    private int mCurrentDrawerPos;

    private final int loaderId = new Random().nextInt();//TODO

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UiUtils.setPreferenceTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mTitle = getTitle();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerAdapter = new DrawerAdapter(this, loaderId);
        mDrawerList.setAdapter(mDrawerAdapter);
        mDrawerList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectDrawerItem(position);
            }
        });
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(mTitle);
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(R.string.app_name);
                invalidateOptionsMenu();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if (savedInstanceState == null) {
            selectDrawerItem(0);

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
        } else {
            selectDrawerItem(savedInstanceState.getInt(STATE_CURRENT_DRAWER_POS));
        }

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
        if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.drawer, menu);

            getSupportFragmentManager().getFragments().get(0).setHasOptionsMenu(false);
        } else {
            getSupportFragmentManager().getFragments().get(0).setHasOptionsMenu(true);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.menu_sort_main:
                getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, Fragment.instantiate(MainActivity.this, FeedsListFragment.class.getName())).commit();
                setTitle(MainApplication.getContext().getString(R.string.overview));
                mDrawerLayout.closeDrawers();
                return true;
            case R.id.menu_settings_main:
                startActivity(new Intent(this, GeneralPrefsActivity.class));
                return true;
            case R.id.menu_refresh_main:
                if (!PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false)) {
                    MainApplication.getContext().startService(new Intent(MainApplication.getContext(), FetcherService.class).setAction(FetcherService.ACTION_REFRESH_FEEDS));
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void selectDrawerItem(int position) {
        mCurrentDrawerPos = position;

        Bundle args = new Bundle();
        args.putBoolean(EntriesListFragment.ARG_SHOW_FEED_INFO, true);

        switch (position) {
            case 0:
                args.putParcelable(EntriesListFragment.ARG_URI, FeedData.EntryColumns.CONTENT_URI);
                setTitle(MainApplication.getContext().getString(R.string.all));
                break;
            case 1:
                args.putParcelable(EntriesListFragment.ARG_URI, FeedData.EntryColumns.FAVORITES_CONTENT_URI);
                setTitle(MainApplication.getContext().getString(R.string.favorites));
                break;
            default:
                args.putParcelable(EntriesListFragment.ARG_URI, FeedData.EntryColumns.FAVORITES_CONTENT_URI);
                long feedOrGroupId = mDrawerAdapter.getItemId(position);
                if (mDrawerAdapter.isItemAGroup(position)) {
                    args.putParcelable(EntriesListFragment.ARG_URI, FeedData.EntryColumns.ENTRIES_FOR_GROUP_CONTENT_URI(feedOrGroupId));
                } else {
                    args.putParcelable(EntriesListFragment.ARG_URI, FeedData.EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(feedOrGroupId));
                    args.putBoolean(EntriesListFragment.ARG_SHOW_FEED_INFO, false);
                }
                setTitle(mDrawerAdapter.getItemName(position));
                break;
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, Fragment.instantiate(MainActivity.this, EntriesListFragment.class.getName(), args)).commit();

        mDrawerList.setItemChecked(position, true);
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getActionBar().setTitle(mTitle);
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
}
