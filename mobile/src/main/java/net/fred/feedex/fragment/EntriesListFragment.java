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

package net.fred.feedex.fragment;

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
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.melnykov.fab.FloatingActionButton;

import net.fred.feedex.Constants;
import net.fred.feedex.R;
import net.fred.feedex.adapter.EntriesCursorAdapter;
import net.fred.feedex.provider.FeedData;
import net.fred.feedex.provider.FeedData.EntryColumns;
import net.fred.feedex.provider.FeedDataContentProvider;
import net.fred.feedex.service.FetcherService;
import net.fred.feedex.utils.PrefUtils;
import net.fred.feedex.utils.UiUtils;

import java.util.Date;

public class EntriesListFragment extends SwipeRefreshListFragment {

    private static final String STATE_CURRENT_URI = "STATE_CURRENT_URI";
    private static final String STATE_ORIGINAL_URI = "STATE_ORIGINAL_URI";
    private static final String STATE_SHOW_FEED_INFO = "STATE_SHOW_FEED_INFO";
    private static final String STATE_LIST_DISPLAY_DATE = "STATE_LIST_DISPLAY_DATE";

    private static final int ENTRIES_LOADER_ID = 1;
    private static final int NEW_ENTRIES_NUMBER_LOADER_ID = 2;

    private Uri mCurrentUri, mOriginalUri;
    private boolean mShowFeedInfo = false;
    private EntriesCursorAdapter mEntriesCursorAdapter;
    private ListView mListView;
    private FloatingActionButton mHideReadButton;
    private long mListDisplayDate = new Date().getTime();
    private final LoaderManager.LoaderCallbacks<Cursor> mEntriesLoader = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            String entriesOrder = PrefUtils.getBoolean(PrefUtils.DISPLAY_OLDEST_FIRST, false) ? Constants.DB_ASC : Constants.DB_DESC;
            String where = "(" + EntryColumns.FETCH_DATE + Constants.DB_IS_NULL + Constants.DB_OR + EntryColumns.FETCH_DATE + "<=" + mListDisplayDate + ')';
            if (!FeedData.shouldShowReadEntries(mCurrentUri)) {
                where += Constants.DB_AND + EntryColumns.WHERE_UNREAD;
            }
            CursorLoader cursorLoader = new CursorLoader(getActivity(), mCurrentUri, null, where, null, EntryColumns.DATE + entriesOrder);
            cursorLoader.setUpdateThrottle(150);
            return cursorLoader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            mEntriesCursorAdapter.swapCursor(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mEntriesCursorAdapter.swapCursor(Constants.EMPTY_CURSOR);
        }
    };
    private final OnSharedPreferenceChangeListener mPrefListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (PrefUtils.SHOW_READ.equals(key)) {
                getLoaderManager().restartLoader(ENTRIES_LOADER_ID, null, mEntriesLoader);
                UiUtils.updateHideReadButton(mHideReadButton);
            } else if (PrefUtils.IS_REFRESHING.equals(key)) {
                refreshSwipeProgress();
            }
        }
    };
    private int mNewEntriesNumber, mOldUnreadEntriesNumber = -1;
    private boolean mAutoRefreshDisplayDate = false;
    private final LoaderManager.LoaderCallbacks<Cursor> mEntriesNumberLoader = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            CursorLoader cursorLoader = new CursorLoader(getActivity(), mCurrentUri, new String[]{"SUM(" + EntryColumns.FETCH_DATE + '>' + mListDisplayDate + ")", "SUM(" + EntryColumns.FETCH_DATE + "<=" + mListDisplayDate + Constants.DB_AND + EntryColumns.WHERE_UNREAD + ")"}, null, null, null);
            cursorLoader.setUpdateThrottle(150);
            return cursorLoader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            data.moveToFirst();
            mNewEntriesNumber = data.getInt(0);
            mOldUnreadEntriesNumber = data.getInt(1);

            if (mAutoRefreshDisplayDate && mNewEntriesNumber != 0 && mOldUnreadEntriesNumber == 0) {
                mListDisplayDate = new Date().getTime();
                restartLoaders();
            } else {
                refreshUI();
            }

            mAutoRefreshDisplayDate = false;
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    };
    private Button mRefreshListBtn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mCurrentUri = savedInstanceState.getParcelable(STATE_CURRENT_URI);
            mOriginalUri = savedInstanceState.getParcelable(STATE_ORIGINAL_URI);
            mShowFeedInfo = savedInstanceState.getBoolean(STATE_SHOW_FEED_INFO);
            mListDisplayDate = savedInstanceState.getLong(STATE_LIST_DISPLAY_DATE);

            mEntriesCursorAdapter = new EntriesCursorAdapter(getActivity(), mCurrentUri, Constants.EMPTY_CURSOR, mShowFeedInfo);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        refreshUI(); // Should not be useful, but it's a security
        refreshSwipeProgress();
        PrefUtils.registerOnPrefChangeListener(mPrefListener);

        if (mCurrentUri != null) {
            // If the list is empty when we are going back here, try with the last display date
            if (mNewEntriesNumber != 0 && mOldUnreadEntriesNumber == 0) {
                mListDisplayDate = new Date().getTime();
            } else {
                mAutoRefreshDisplayDate = true; // We will try to update the list after if necessary
            }

            restartLoaders();
        }
    }

    @Override
    public View inflateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_entry_list, container, true);

        if (mEntriesCursorAdapter != null) {
            setListAdapter(mEntriesCursorAdapter);
        }

        mListView = (ListView) rootView.findViewById(android.R.id.list);
        mListView.setOnTouchListener(new SwipeGestureListener(mListView.getContext()));

        if (PrefUtils.getBoolean(PrefUtils.DISPLAY_TIP, true)) {
            final TextView header = new TextView(mListView.getContext());
            header.setMinimumHeight(UiUtils.dpToPixel(70));
            int footerPadding = UiUtils.dpToPixel(10);
            header.setPadding(footerPadding, footerPadding, footerPadding, footerPadding);
            header.setText(R.string.tip_sentence);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.setCompoundDrawablePadding(UiUtils.dpToPixel(5));
            header.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_about, 0, R.drawable.ic_action_cancel, 0);
            header.setClickable(true);
            header.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mListView.removeHeaderView(header);
                    PrefUtils.putBoolean(PrefUtils.DISPLAY_TIP, false);
                }
            });
            mListView.addHeaderView(header);
        }

        UiUtils.addEmptyFooterView(mListView, 90);

        mHideReadButton = (FloatingActionButton) rootView.findViewById(R.id.hide_read_button);
        mHideReadButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                UiUtils.displayHideReadButtonAction(mListView.getContext());
                return true;
            }
        });
        UiUtils.updateHideReadButton(mHideReadButton);

        mRefreshListBtn = (Button) rootView.findViewById(R.id.refreshListBtn);
        mRefreshListBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mNewEntriesNumber = 0;
                mListDisplayDate = new Date().getTime();

                refreshUI();
                if (mCurrentUri != null) {
                    restartLoaders();
                }
            }
        });

        disableSwipe();

        return rootView;
    }

    @Override
    public void onStop() {
        PrefUtils.unregisterOnPrefChangeListener(mPrefListener);
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(STATE_CURRENT_URI, mCurrentUri);
        outState.putParcelable(STATE_ORIGINAL_URI, mOriginalUri);
        outState.putBoolean(STATE_SHOW_FEED_INFO, mShowFeedInfo);
        outState.putLong(STATE_LIST_DISPLAY_DATE, mListDisplayDate);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRefresh() {
        startRefresh();
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        if (id >= 0) { // should not happen, but I had a crash with this on PlayStore...
            startActivity(new Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(mCurrentUri, id)));
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear(); // This is needed to remove a bug on Android 4.0.3

        inflater.inflate(R.menu.entry_list, menu);

        SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)) {
                    setData(mOriginalUri, true);
                } else {
                    setData(EntryColumns.SEARCH_URI(newText), true, true);
                }
                return false;
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                setData(mOriginalUri, true);
                return false;
            }
        });

        if (EntryColumns.FAVORITES_CONTENT_URI.equals(mCurrentUri)) {
            menu.findItem(R.id.menu_refresh).setVisible(false);
        } else {
            menu.findItem(R.id.menu_share_starred).setVisible(false);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_share_starred: {
                if (mEntriesCursorAdapter != null) {
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
                                        .putExtra(Intent.EXTRA_TEXT, starredList).setType(Constants.MIMETYPE_TEXT_PLAIN), getString(R.string.menu_share)
                        ));
                    }
                }
                return true;
            }
            case R.id.menu_refresh: {
                startRefresh();
                return true;
            }
            case R.id.menu_all_read: {
                if (mEntriesCursorAdapter != null) {
                    mEntriesCursorAdapter.markAllAsRead(mListDisplayDate);

                    // If we are on "all items" uri, we can remove the notification here
                    if (mCurrentUri != null && EntryColumns.CONTENT_URI.equals(mCurrentUri) && Constants.NOTIF_MGR != null) {
                        Constants.NOTIF_MGR.cancel(0);
                    }
                }
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void startRefresh() {
        if (!PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false)) {
            if (mCurrentUri != null && FeedDataContentProvider.URI_MATCHER.match(mCurrentUri) == FeedDataContentProvider.URI_ENTRIES_FOR_FEED) {
                getActivity().startService(new Intent(getActivity(), FetcherService.class).setAction(FetcherService.ACTION_REFRESH_FEEDS).putExtra(Constants.FEED_ID,
                        mCurrentUri.getPathSegments().get(1)));
            } else {
                getActivity().startService(new Intent(getActivity(), FetcherService.class).setAction(FetcherService.ACTION_REFRESH_FEEDS));
            }
        }

        refreshSwipeProgress();
    }

    public Uri getUri() {
        return mOriginalUri;
    }

    public void setData(Uri uri, boolean showFeedInfo) {
        setData(uri, showFeedInfo, false);
    }

    private void setData(Uri uri, boolean showFeedInfo, boolean isSearchUri) {
        mCurrentUri = uri;
        if (!isSearchUri) {
            mOriginalUri = mCurrentUri;
        }

        mShowFeedInfo = showFeedInfo;

        mEntriesCursorAdapter = new EntriesCursorAdapter(getActivity(), mCurrentUri, Constants.EMPTY_CURSOR, mShowFeedInfo);
        setListAdapter(mEntriesCursorAdapter);

        mListDisplayDate = new Date().getTime();
        if (mCurrentUri != null) {
            restartLoaders();
        }
        refreshUI();
    }

    private void restartLoaders() {
        LoaderManager loaderManager = getLoaderManager();
        loaderManager.restartLoader(ENTRIES_LOADER_ID, null, mEntriesLoader);
        loaderManager.restartLoader(NEW_ENTRIES_NUMBER_LOADER_ID, null, mEntriesNumberLoader);
    }

    private void refreshUI() {
        if (mNewEntriesNumber > 0) {
            mRefreshListBtn.setText(getResources().getQuantityString(R.plurals.number_of_new_entries, mNewEntriesNumber, mNewEntriesNumber));
            mRefreshListBtn.setVisibility(View.VISIBLE);
        } else {
            mRefreshListBtn.setVisibility(View.GONE);
        }
    }

    private void refreshSwipeProgress() {
        if (PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false)) {
            showSwipeProgress();
        } else {
            hideSwipeProgress();
        }
    }

    private class SwipeGestureListener extends SimpleOnGestureListener implements OnTouchListener {
        static final int SWIPE_MIN_DISTANCE = 120;
        static final int SWIPE_MAX_OFF_PATH = 150;
        static final int SWIPE_THRESHOLD_VELOCITY = 150;

        private final GestureDetector mGestureDetector;

        public SwipeGestureListener(Context context) {
            mGestureDetector = new GestureDetector(context, this);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (mListView != null && e1 != null && e2 != null && Math.abs(e1.getY() - e2.getY()) <= SWIPE_MAX_OFF_PATH && Math.abs(velocityX) >= SWIPE_THRESHOLD_VELOCITY) {
                long id = mListView.pointToRowId(Math.round(e2.getX()), Math.round(e2.getY()));
                int position = mListView.pointToPosition(Math.round(e2.getX()), Math.round(e2.getY()));
                View view = mListView.getChildAt(position - mListView.getFirstVisiblePosition());

                if (view != null) {
                    // Just click on views, the adapter will do the real stuff
                    if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE) {
                        mEntriesCursorAdapter.toggleReadState(id, view);
                    } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE) {
                        mEntriesCursorAdapter.toggleFavoriteState(id, view);
                    }

                    // Just simulate a CANCEL event to remove the item highlighting
                    mListView.post(new Runnable() { // In a post to avoid a crash on 4.0.x
                        @Override
                        public void run() {
                            MotionEvent motionEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0, 0, 0);
                            mListView.dispatchTouchEvent(motionEvent);
                            motionEvent.recycle();
                        }
                    });
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


}
