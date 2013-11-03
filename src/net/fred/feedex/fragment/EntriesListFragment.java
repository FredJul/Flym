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

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;

import net.fred.feedex.Constants;
import net.fred.feedex.R;
import net.fred.feedex.adapter.EntriesCursorAdapter;
import net.fred.feedex.provider.FeedData.EntryColumns;
import net.fred.feedex.provider.FeedDataContentProvider;
import net.fred.feedex.service.FetcherService;
import net.fred.feedex.utils.PrefUtils;

public class EntriesListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public interface Callbacks {
        public void onEntriesListLoaded(Cursor data);

        public void onEntrySelected(Uri entryUri);
    }

    private static final String STATE_URI = "STATE_URI";
    private static final String STATE_NEED_DEFAULT_SELECTION = "STATE_NEED_DEFAULT_SELECTION";
    private static final String STATE_SHOW_FEED_INFO = "STATE_SHOW_FEED_INFO";

    private static final int LOADER_ID = 1;

    private Callbacks mCallbacks;
    private Uri mUri;
    private boolean mShowFeedInfo = false;
    private EntriesCursorAdapter mEntriesCursorAdapter;
    private ListView mListView;
    private boolean mNeedDefaultSelection = false;

    private final OnSharedPreferenceChangeListener prefListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (PrefUtils.SHOW_READ.equals(key)) {
                getLoaderManager().restartLoader(LOADER_ID, null, EntriesListFragment.this);
            }
        }
    };

    private class SwipeGestureListener extends SimpleOnGestureListener implements OnTouchListener {
        static final int SWIPE_MIN_DISTANCE = 120;
        static final int SWIPE_MAX_OFF_PATH = 150;
        static final int SWIPE_THRESHOLD_VELOCITY = 150;

        private GestureDetector mGestureDetector;

        public SwipeGestureListener(Context context) {
            mGestureDetector = new GestureDetector(context, this);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (mListView != null && e1 != null && e2 != null && Math.abs(e1.getY() - e2.getY()) <= SWIPE_MAX_OFF_PATH && Math.abs(velocityX) >= SWIPE_THRESHOLD_VELOCITY) {
                int position = mListView.pointToPosition(Math.round(e2.getX()), Math.round(e2.getY()));
                View view = mListView.getChildAt(position - mListView.getFirstVisiblePosition());

                if (view != null) {
                    if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE) {
                        ImageView star = (ImageView) view.findViewById(android.R.id.icon);
                        star.callOnClick();
                    } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE) {
                        CheckBox cb = (CheckBox) view.findViewById(android.R.id.checkbox);
                        cb.setChecked(!cb.isChecked());
                    }

                    // Before accepting the event, just simulate a CANCEL event to remove the item highlighting
                    MotionEvent motionEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0, 0, 0);
                    mListView.dispatchTouchEvent(motionEvent);
                    return true;
                }
            }

            return super.onFling(e1, e2, velocityX, velocityY);
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return mGestureDetector.onTouchEvent(event);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mUri = savedInstanceState.getParcelable(STATE_URI);
            mShowFeedInfo = savedInstanceState.getBoolean(STATE_SHOW_FEED_INFO);
            mNeedDefaultSelection = savedInstanceState.getBoolean(STATE_NEED_DEFAULT_SELECTION);

            mEntriesCursorAdapter = new EntriesCursorAdapter(getActivity(), mUri, null, mShowFeedInfo);
            getLoaderManager().initLoader(LOADER_ID, null, this);
        }

        PrefUtils.registerOnPrefChangeListener(prefListener);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mCallbacks = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(STATE_URI, mUri);
        outState.putBoolean(STATE_SHOW_FEED_INFO, mShowFeedInfo);
        outState.putBoolean(STATE_NEED_DEFAULT_SELECTION, mNeedDefaultSelection);

        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_entry_list, container, false);

        if (mEntriesCursorAdapter != null) {
            setListAdapter(mEntriesCursorAdapter);
        }

        mListView = (ListView) rootView.findViewById(android.R.id.list);
        mListView.setFastScrollEnabled(true);
        mListView.setOnTouchListener(new SwipeGestureListener(getActivity()));

        return rootView;
    }

    @Override
    public void onDestroy() {
        PrefUtils.unregisterOnPrefChangeListener(prefListener);
        super.onStop();
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        mCallbacks.onEntrySelected(ContentUris.withAppendedId(mUri, id));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear(); // This is needed to remove a bug on Android 4.0.3

        inflater.inflate(R.menu.entry_list, menu);

        if (EntryColumns.FAVORITES_CONTENT_URI.equals(mUri)) {
            menu.findItem(R.id.menu_hide_read).setVisible(false);
            menu.findItem(R.id.menu_refresh).setVisible(false);
        } else {
            menu.findItem(R.id.menu_share_starred).setVisible(false);

            if (!PrefUtils.getBoolean(PrefUtils.SHOW_READ, true)) {
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
                if (!PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false)) {
                    if (FeedDataContentProvider.URI_MATCHER.match(mUri) == FeedDataContentProvider.URI_ENTRIES_FOR_FEED) {
                        getActivity().startService(new Intent(getActivity(), FetcherService.class).setAction(FetcherService.ACTION_REFRESH_FEEDS).putExtra(Constants.FEED_ID,
                                mUri.getPathSegments().get(1)));
                    } else {
                        getActivity().startService(new Intent(getActivity(), FetcherService.class).setAction(FetcherService.ACTION_REFRESH_FEEDS));
                    }
                }
                return true;
            }
            case R.id.menu_all_read: {
                mEntriesCursorAdapter.markAllAsRead();

                // If we are on "all items" uri, we can remove the notification here
                if (EntryColumns.CONTENT_URI.equals(mUri) && Constants.NOTIF_MGR != null) {
                    Constants.NOTIF_MGR.cancel(0);
                }
                return true;
            }
            case R.id.menu_hide_read: {
                mNeedDefaultSelection = true;
                if (!PrefUtils.getBoolean(PrefUtils.SHOW_READ, true)) {
                    PrefUtils.putBoolean(PrefUtils.SHOW_READ, true);
                    item.setTitle(R.string.context_menu_hide_read).setIcon(R.drawable.hide_reads);
                } else {
                    PrefUtils.putBoolean(PrefUtils.SHOW_READ, false);
                    item.setTitle(R.string.context_menu_show_read).setIcon(R.drawable.view_reads);
                }
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public Uri getUri() {
        return mUri;
    }

    public void setData(Uri uri, boolean showFeedInfo) {
        mUri = uri;
        mShowFeedInfo = showFeedInfo;
        mNeedDefaultSelection = true;

        mEntriesCursorAdapter = new EntriesCursorAdapter(getActivity(), mUri, null, mShowFeedInfo);
        setListAdapter(mEntriesCursorAdapter);
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader cursorLoader = new CursorLoader(getActivity(), mUri, null, PrefUtils.getBoolean(PrefUtils.SHOW_READ, true)
                || EntryColumns.FAVORITES_CONTENT_URI.equals(mUri) ? null : EntryColumns.WHERE_UNREAD, null, EntryColumns.DATE + Constants.DB_DESC);
        cursorLoader.setUpdateThrottle(Constants.UPDATE_THROTTLE_DELAY);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mEntriesCursorAdapter.swapCursor(data);
        if (mNeedDefaultSelection) {
            if (data.getCount() > 0) {
                mCallbacks.onEntriesListLoaded(data);
            } else {
                mCallbacks.onEntriesListLoaded(null);
            }
        }

        mNeedDefaultSelection = false;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mEntriesCursorAdapter.swapCursor(null);
    }
}
