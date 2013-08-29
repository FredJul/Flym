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

package net.fred.feedex.fragment;

import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import net.fred.feedex.Constants;
import net.fred.feedex.PrefsManager;
import net.fred.feedex.R;
import net.fred.feedex.activity.GeneralPrefsActivity;
import net.fred.feedex.adapter.EntriesCursorAdapter;
import net.fred.feedex.provider.FeedData.EntryColumns;
import net.fred.feedex.service.FetcherService;

import java.util.Random;

public class EntriesListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String ARG_URI = "uri";
    public static final String ARG_SHOW_FEED_INFO = "show_feedinfo";

    private Uri mUri;
    private boolean mShowFeedInfo = false;
    private EntriesCursorAdapter mEntriesCursorAdapter;

    private final int loaderId = new Random().nextInt();

    private final OnSharedPreferenceChangeListener prefListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (PrefsManager.SHOW_READ.equals(key)) {
                getLoaderManager().restartLoader(loaderId, null, EntriesListFragment.this);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            Bundle args = getArguments();

            if (args.containsKey(ARG_SHOW_FEED_INFO)) {
                mShowFeedInfo = getArguments().getBoolean(ARG_SHOW_FEED_INFO);
            }

            if (args.containsKey(ARG_URI)) {
                mUri = getArguments().getParcelable(ARG_URI);

                mEntriesCursorAdapter = new EntriesCursorAdapter(getActivity(), mUri, null, mShowFeedInfo);
                PrefsManager.registerOnSharedPreferenceChangeListener(prefListener);
                getLoaderManager().initLoader(loaderId, null, this);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.entry_list_fragment, container, false);

        if (mEntriesCursorAdapter != null)
            setListAdapter(mEntriesCursorAdapter);

        final ListView lv = (ListView) rootView.findViewById(android.R.id.list);
        lv.setFastScrollEnabled(true);

        return rootView;
    }

    @Override
    public void onDestroy() {
        PrefsManager.unregisterOnSharedPreferenceChangeListener(prefListener);
        super.onStop();
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        startActivity(new Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(mUri, id)));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.entry_list, menu);

        if (EntryColumns.FAVORITES_CONTENT_URI.equals(mUri)) {
            menu.findItem(R.id.menu_hide_read).setVisible(false);
        } else {
            menu.findItem(R.id.menu_share_starred).setVisible(false);

            if (!PrefsManager.getBoolean(PrefsManager.SHOW_READ, true)) {
                menu.findItem(R.id.menu_hide_read).setTitle(R.string.context_menu_show_read).setIcon(R.drawable.view_reads);
            }
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_share_starred: {
                String starredList = "";
                Cursor cursor = mEntriesCursorAdapter.getCursor();
                if (cursor != null && !cursor.isClosed()) {
                    int titlePos = cursor.getColumnIndex(EntryColumns.TITLE);
                    int linkPos = cursor.getColumnIndex(EntryColumns.LINK);
                    if (cursor.moveToFirst()) {
                        do {
                            starredList += cursor.getString(titlePos) + "\n" + cursor.getString(linkPos) + "\n\n";
                        } while (cursor.moveToNext());
                    }
                    startActivity(Intent.createChooser(
                            new Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_favorites_title))
                                    .putExtra(Intent.EXTRA_TEXT, starredList).setType(Constants.MIMETYPE_TEXT_PLAIN), getString(R.string.menu_share)));
                }
                return true;
            }
            case R.id.menu_refresh: {
                if (!FetcherService.isRefreshingFeeds) {
                    new Thread() {
                        @Override
                        public void run() {
                            getActivity().sendBroadcast(new Intent(Constants.ACTION_REFRESH_FEEDS));
                        }
                    }.start();
                }
                return true;
            }
            case R.id.menu_all_read: {
                mEntriesCursorAdapter.markAllAsRead();
                return true;
            }
            case R.id.menu_hide_read: {
                if (!PrefsManager.getBoolean(PrefsManager.SHOW_READ, true)) {
                    PrefsManager.putBoolean(PrefsManager.SHOW_READ, true);
                    item.setTitle(R.string.context_menu_hide_read).setIcon(R.drawable.hide_reads);
                } else {
                    PrefsManager.putBoolean(PrefsManager.SHOW_READ, false);
                    item.setTitle(R.string.context_menu_show_read).setIcon(R.drawable.view_reads);
                }
                return true;
            }
            case R.id.menu_settings: {
                startActivity(new Intent(getActivity(), GeneralPrefsActivity.class));
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader cursorLoader = new CursorLoader(getActivity(), mUri, null, PrefsManager.getBoolean(PrefsManager.SHOW_READ, true)
                || EntryColumns.FAVORITES_CONTENT_URI.equals(mUri) ? null : EntryColumns.WHERE_UNREAD, null, EntryColumns.DATE + Constants.DB_DESC);
        cursorLoader.setUpdateThrottle(Constants.UPDATE_THROTTLE_DELAY);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mEntriesCursorAdapter.changeCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mEntriesCursorAdapter.changeCursor(null);
    }
}
