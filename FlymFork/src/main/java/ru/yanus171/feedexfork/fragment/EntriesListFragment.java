/**
 * Flym
 * <p>
 * Copyright (c) 2012-2015 Frederic Julian
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ru.yanus171.feedexfork.fragment;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.activity.HomeActivity;
import ru.yanus171.feedexfork.adapter.EntriesCursorAdapter;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns;
import ru.yanus171.feedexfork.provider.FeedDataContentProvider;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.Timer;
import ru.yanus171.feedexfork.utils.UiUtils;
import ru.yanus171.feedexfork.view.StatusText;

public class EntriesListFragment extends /*SwipeRefreshList*/Fragment {

    private static final String STATE_CURRENT_URI = "STATE_CURRENT_URI";
    private static final String STATE_ORIGINAL_URI = "STATE_ORIGINAL_URI";
    private static final String STATE_SHOW_FEED_INFO = "STATE_SHOW_FEED_INFO";
    private static final String STATE_LIST_DISPLAY_DATE = "STATE_LIST_DISPLAY_DATE";
    private static final String STATE_SHOW_TEXT_IN_ENTRY_LIST = "STATE_SHOW_TEXT_IN_ENTRY_LIST";
    private static final String STATE_ORIGINAL_URI_SHOW_TEXT_IN_ENTRY_LIST = "STATE_ORIGINAL_URI_SHOW_TEXT_IN_ENTRY_LIST";
    private static final String STATE_SHOW_UNREAD = "STATE_SHOW_UNREAD";
    private static final String STATE_LAST_VISIBLE_ENTRY_ID = "STATE_LAST_VISIBLE_ENTRY_ID";
    private static final String STATE_LAST_VISIBLE_OFFSET = "STATE_LAST_VISIBLE_OFFSET";


    private static final int ENTRIES_LOADER_ID = 1;
    private static final int NEW_ENTRIES_NUMBER_LOADER_ID = 2;

    private Uri mCurrentUri, mOriginalUri;
    private boolean mOriginalUriShownEntryText = false;
    private boolean mShowFeedInfo = false;
    private boolean mShowTextInEntryList = false;
    private EntriesCursorAdapter mEntriesCursorAdapter;
    private Cursor mJustMarkedAsReadEntries;
    private FloatingActionButton mFab;
    private ListView mListView;
    private ProgressBar mProgressBar = null;
    public boolean mShowUnRead = false;
    private boolean mNeedSetSelection = false;
    private long mLastVisibleTopEntryID = 0;
    private int mLastListViewTopOffset = 0;
    private Menu mMenu = null;
    private long mListDisplayDate = new Date().getTime();
    private final LoaderManager.LoaderCallbacks<Cursor> mEntriesLoader = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            Timer.Start( ENTRIES_LOADER_ID, "EntriesListFragment.onCreateLoader" );

            String entriesOrder = PrefUtils.getBoolean(PrefUtils.DISPLAY_OLDEST_FIRST, false) || mShowTextInEntryList ? Constants.DB_ASC : Constants.DB_DESC;
            String where = "(" + EntryColumns.FETCH_DATE + Constants.DB_IS_NULL + Constants.DB_OR + EntryColumns.FETCH_DATE + "<=" + mListDisplayDate + ')';
            String[] projection = mShowTextInEntryList ? null : EntryColumns.PROJECTION_WITHOUT_TEXT;
            CursorLoader cursorLoader = new CursorLoader(getActivity(), mCurrentUri, projection, where, null, EntryColumns.DATE + entriesOrder);
            cursorLoader.setUpdateThrottle(150);
            return cursorLoader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            Timer.End(ENTRIES_LOADER_ID);
            Timer timer = new Timer( "EntriesListFragment.onCreateLoader" );

            mEntriesCursorAdapter.swapCursor(data);
            if ( mShowTextInEntryList && mNeedSetSelection ) {
                mNeedSetSelection = false;
                mListView.setSelection(mEntriesCursorAdapter.GetFirstUnReadPos());
            }
            if ( mLastVisibleTopEntryID != -1 ) {
                int pos = mEntriesCursorAdapter.GetPosByID(mLastVisibleTopEntryID);
                if ( pos != -1 &&
                        ( pos > mListView.getLastVisiblePosition() || pos < mListView.getFirstVisiblePosition() )  )
                    mListView.setSelectionFromTop(pos, mLastListViewTopOffset);
            }
            timer.End();
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mEntriesCursorAdapter.swapCursor(Constants.EMPTY_CURSOR);
        }

    };

    private final OnSharedPreferenceChangeListener mPrefListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (PrefUtils.IS_REFRESHING.equals(key)) {
                //refreshSwipeProgress();
                UpdateActions();
            }
        }
    };

    private void UpdateActions() {
        if ( mMenu == null )
            return;

        if (EntryColumns.FAVORITES_CONTENT_URI.equals(mCurrentUri)) {
            mMenu.findItem(R.id.menu_refresh).setVisible(false);
        } else {
            mMenu.findItem(R.id.menu_share_starred).setVisible(true);
        }

        MenuItem item = mMenu.findItem( R.id.menu_toogle_toogle_unread_all );
        if (mShowUnRead) {
            item.setTitle(R.string.all_entries);
            item.setIcon(R.drawable.rounded_empty_white);
        } else {
            item.setTitle(R.string.unread_entries);
            item.setIcon(R.drawable.rounded_checbox_white);
        }

        if ( mCurrentUri != null ) {
            int uriMatch = FeedDataContentProvider.URI_MATCHER.match(mCurrentUri);
            item.setVisible(uriMatch != FeedDataContentProvider.URI_ENTRIES &&
                    uriMatch != FeedDataContentProvider.URI_UNREAD_ENTRIES &&
                    uriMatch != FeedDataContentProvider.URI_FAVORITES);
        }

        boolean isCanRefresh = !EntryColumns.FAVORITES_CONTENT_URI.equals( mCurrentUri );
        if ( mCurrentUri != null && mCurrentUri.getPathSegments().size() > 1 ) {
            String feedID = mCurrentUri.getPathSegments().get(1);
            isCanRefresh = !feedID.equals(FetcherService.GetExtrenalLinkFeedID());
        }
        boolean isRefresh = PrefUtils.getBoolean( PrefUtils.IS_REFRESHING, false );
        mMenu.findItem(R.id.menu_cancel_refresh).setVisible( isRefresh );
        mMenu.findItem(R.id.menu_refresh).setVisible( !isRefresh && isCanRefresh );


        if ( mProgressBar != null ) {
            if (isRefresh)
                mProgressBar.setVisibility(View.VISIBLE);
            else
                mProgressBar.setVisibility(View.GONE);
        }
    }

    private int mNewEntriesNumber, mOldUnreadEntriesNumber = -1;
    private boolean mAutoRefreshDisplayDate = false;
    private final LoaderManager.LoaderCallbacks<Cursor> mEntriesNumberLoader = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            Timer.Start( NEW_ENTRIES_NUMBER_LOADER_ID, "EntriesListFr.mEntriesNumberLoader" );
            final String EXPR_READ_COUNT = "SUM(" + EntryColumns.FETCH_DATE + '>' + mListDisplayDate + ")";
            final String EXPR_UNREAD_COUNT = "SUM(" + EntryColumns.FETCH_DATE + "<=" + mListDisplayDate + Constants.DB_AND + EntryColumns.WHERE_UNREAD + ")";
            CursorLoader cursorLoader = new CursorLoader(getActivity(), mCurrentUri, new String[]{ EXPR_READ_COUNT, EXPR_UNREAD_COUNT}, null, null, null);
            cursorLoader.setUpdateThrottle(150);
            return cursorLoader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            Timer.End(NEW_ENTRIES_NUMBER_LOADER_ID);
            if ( data == null )
                return;

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
        Timer timer = new Timer( "EntriesListFragment.onCreate" );

        setHasOptionsMenu(true);

        Dog.v( "EntriesListFragment.onCreate" );

        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mCurrentUri = savedInstanceState.getParcelable(STATE_CURRENT_URI);
            mOriginalUri = savedInstanceState.getParcelable(STATE_ORIGINAL_URI);
            mOriginalUriShownEntryText = savedInstanceState.getBoolean(STATE_ORIGINAL_URI_SHOW_TEXT_IN_ENTRY_LIST);
            mShowFeedInfo = savedInstanceState.getBoolean(STATE_SHOW_FEED_INFO);
            mListDisplayDate = savedInstanceState.getLong(STATE_LIST_DISPLAY_DATE);
            mShowTextInEntryList = savedInstanceState.getBoolean(STATE_SHOW_TEXT_IN_ENTRY_LIST);
            mShowUnRead = savedInstanceState.getBoolean(STATE_SHOW_UNREAD, PrefUtils.getBoolean( STATE_SHOW_UNREAD, false ));
            Dog.v( String.format( "EntriesListFragment.onCreate mShowUnRead = %b", mShowUnRead ) );

            //if ( mShowTextInEntryList )
            //    mNeedSetSelection = true;
            mEntriesCursorAdapter = new EntriesCursorAdapter(getActivity(), mCurrentUri, Constants.EMPTY_CURSOR, mShowFeedInfo, mShowTextInEntryList, mShowUnRead);
        } else
            mShowUnRead = PrefUtils.getBoolean( STATE_SHOW_UNREAD, false );

        timer.End();
    }

    @Override
    public void onStart() {
        super.onStart();
        Timer timer = new Timer( "EntriesListFragment.onStart" );

        refreshUI(); // Should not be useful, but it's a security
        //refreshSwipeProgress();
        PrefUtils.registerOnPrefChangeListener(mPrefListener);

        mFab = getActivity().findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                markAllAsRead();
            }
        });
        if ( !PrefUtils.getBoolean("show_mark_all_as_read_button", true) )
            mFab.hide();

        if (mCurrentUri != null) {
            // If the list is empty when we are going back here, try with the last display date
            if (mNewEntriesNumber != 0 && mOldUnreadEntriesNumber == 0) {
                mListDisplayDate = new Date().getTime();
            } else {
                mAutoRefreshDisplayDate = true; // We will try to update the list after if necessary
            }
            restartLoaders();
        }
        mLastVisibleTopEntryID = PrefUtils.getLong( STATE_LAST_VISIBLE_ENTRY_ID, -1 );
        mLastListViewTopOffset = PrefUtils.getInt( STATE_LAST_VISIBLE_OFFSET, 0 );
        UpdateActions();
        timer.End();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    //@Override
    //public View inflateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Timer timer = new Timer( "EntriesListFragment.onCreateView" );

        View rootView = inflater.inflate(R.layout.fragment_entry_list, container, true);
        new StatusText( (TextView)rootView.findViewById( R.id.statusText1 ),
                        FetcherService.Status()/*,
                        this*/);

        mProgressBar = rootView.findViewById(R.id.progressBar);

        mListView = rootView.findViewById(android.R.id.list);
        //mListView.setOnTouchListener(new SwipeGestureListener(mListView.getContext()));
        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if ( mShowTextInEntryList )
                    for ( int i = firstVisibleItem - 2; i <= firstVisibleItem - 2 && i < totalItemCount; i++ ) {
                        long id = mEntriesCursorAdapter.getItemId(i);
                        Uri uri = mEntriesCursorAdapter.EntryUri(id);
                        if (!EntriesCursorAdapter.mMarkAsReadList.contains(uri)) {
                            EntriesCursorAdapter.SetIsRead(mEntriesCursorAdapter.EntryUri(id), true, 0);
                            EntriesCursorAdapter.mMarkAsReadList.add(uri);
                        }
                    }
                else if ( firstVisibleItem > 0 ) {
                    mLastVisibleTopEntryID = mEntriesCursorAdapter.getItemId(firstVisibleItem);
                    View v = mListView.getChildAt(0);
                    mLastListViewTopOffset = (v == null) ? 0 : (v.getTop() - mListView.getPaddingTop());
                }
            }
        });

        if (mEntriesCursorAdapter != null) {
            //mListView.setListAdapter(mEntriesCursorAdapter);
            SetListViewAdapter();
        }

        if (PrefUtils.getBoolean(PrefUtils.DISPLAY_TIP, true) && mListView instanceof ListView ) {
            final TextView header = new TextView(mListView.getContext());
            header.setMinimumHeight(UiUtils.dpToPixel(70));
            int footerPadding = UiUtils.dpToPixel(10);
            header.setPadding(footerPadding, footerPadding, footerPadding, footerPadding);
            header.setText(R.string.tip_sentence);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.setCompoundDrawablePadding(UiUtils.dpToPixel(5));
            header.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_about, 0, R.drawable.ic_action_cancel_gray, 0);
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

        if ( mListView instanceof ListView )
            UiUtils.addEmptyFooterView(mListView, 90);

        mRefreshListBtn = rootView.findViewById(R.id.refreshListBtn);
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

        /*mListView.setOnItemClickListener( new AbsListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                if (id >= 0) { // should not happen, but I had a crash with this on PlayStore...
                    //startActivity(new Intent(Intent.ACTION_VIEW, EntryColumns.CONTENT_URI(id) ) );
                    startActivity(new Intent(Intent.ACTION_VIEW,  ContentUris.withAppendedId(mCurrentUri, id)));
                }
            }
        });*/

        /*mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, final long id) {
                if (id > 0) {
                    //mListView.setSelection( position );

                    ShowDeleteDialog(EntriesListFragment.this.getActivity(), mEntriesCursorAdapter.GetTitle( mListView, position ), id);
                    return true;
                }
                return false;
            }
        });*/

        TextView emptyView = new TextView( getContext() );
        emptyView.setText( getString( R.string.no_entries ) );
        mListView.setEmptyView( emptyView );


        //disableSwipe();

        timer.End();
        return rootView;
    }

    public void SetListViewAdapter() {
        mListView.setAdapter(mEntriesCursorAdapter);
        mNeedSetSelection = true;
    }

    public static void ShowDeleteDialog(Context context, final String title, final long id) {
        new AlertDialog.Builder(context) //
                .setIcon(android.R.drawable.ic_dialog_alert) //
                .setTitle( R.string.question_delete_entry ) //
                .setMessage( title ) //
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new Thread() {
                            @Override
                            public void run() {
                                ContentResolver cr = MainApplication.getContext().getContentResolver();
                                cr.delete(EntryColumns.CONTENT_URI(id), null, null);
                            }
                        }.start();
                    }
                }).setNegativeButton(android.R.string.no, null).show();
    }


    @Override
    public void onStop() {
        PrefUtils.unregisterOnPrefChangeListener(mPrefListener);

        PrefUtils.putBoolean( STATE_SHOW_UNREAD, mShowUnRead );
        PrefUtils.putLong( STATE_LAST_VISIBLE_ENTRY_ID, mLastVisibleTopEntryID );
        PrefUtils.putInt( STATE_LAST_VISIBLE_OFFSET, mLastListViewTopOffset );


        if (mJustMarkedAsReadEntries != null && !mJustMarkedAsReadEntries.isClosed()) {
            mJustMarkedAsReadEntries.close();
        }

        mFab = null;

        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(STATE_CURRENT_URI, mCurrentUri);
        outState.putParcelable(STATE_ORIGINAL_URI, mOriginalUri);
        outState.putBoolean(STATE_ORIGINAL_URI_SHOW_TEXT_IN_ENTRY_LIST, mOriginalUriShownEntryText);
        outState.putBoolean(STATE_SHOW_FEED_INFO, mShowFeedInfo);
        outState.putBoolean(STATE_SHOW_TEXT_IN_ENTRY_LIST, mShowTextInEntryList);
        outState.putLong(STATE_LIST_DISPLAY_DATE, mListDisplayDate);
        outState.putBoolean(STATE_SHOW_UNREAD, mShowUnRead);

        super.onSaveInstanceState(outState);
    }

    /*@Override
    public void onRefresh() {
        startRefresh();
    }*/

    /*@Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        if (id >= 0) { // should not happen, but I had a crash with this on PlayStore...
            startActivity(new Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(mCurrentUri, id)));
        }
    }*/


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mMenu = menu;
        menu.clear(); // This is needed to remove a bug on Android 4.0.3

        inflater.inflate(R.menu.entry_list, menu);

        MenuItem searchItem = menu.findItem(R.id.menu_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        if (EntryColumns.isSearchUri(mCurrentUri)) {
            searchItem.expandActionView();
            searchView.post(new Runnable() { // Without that, it just does not work
                @Override
                public void run() {
                    searchView.setQuery(mCurrentUri.getLastPathSegment(), false);
                    searchView.clearFocus();
                }
            });
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)) {
                    setData(mOriginalUri, true, true, mOriginalUriShownEntryText);
                } else {
                    setData(EntryColumns.SEARCH_URI(newText), true, true, false);
                }
                return false;
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                setData(mOriginalUri, true, false, mOriginalUriShownEntryText);
                return false;
            }
        });


        UpdateActions();

        super.onCreateOptionsMenu(menu, inflater);

        //UpdateActions();
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

            case R.id.menu_cancel_refresh: {
                FetcherService.cancelRefresh();
                return true;
            }

            case R.id.menu_toggle_theme: {
                PrefUtils.ToogleTheme( new Intent(getContext(), HomeActivity.class) );
                return true;
            }


            case R.id.menu_delete_all: {
                if ( FeedDataContentProvider.URI_MATCHER.match(mCurrentUri) == FeedDataContentProvider.URI_ENTRIES_FOR_FEED ) {
                    new AlertDialog.Builder(getContext()) //
                            .setIcon(android.R.drawable.ic_dialog_alert) //
                            .setTitle( R.string.question ) //
                            .setMessage( R.string.deleteAllEntries ) //
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    new Thread() {
                                        @Override
                                        public void run() {
                                            FetcherService.deleteAllFeedEntries(mCurrentUri.getPathSegments().get(1));
                                        }
                                    }.start();
                                }
                            }).setNegativeButton(android.R.string.no, null).show();
                }
                return true;
            }
            case R.id.menu_mark_all_as_read: {
                markAllAsRead();
                return true;

            }
            case R.id.menu_create_test_data: {
                FetcherService.createTestData();
                return true;
            }
            case R.id.menu_toogle_toogle_unread_all: {
                int uriMatch = FeedDataContentProvider.URI_MATCHER.match(mCurrentUri);
                mShowUnRead = !mShowUnRead;
                if ( uriMatch == FeedDataContentProvider.URI_ENTRIES_FOR_FEED ||
                     uriMatch == FeedDataContentProvider.URI_UNREAD_ENTRIES_FOR_FEED ) {
                    long feedID = Long.parseLong( mCurrentUri.getPathSegments().get(1) );
                    Uri uri = mShowUnRead ? EntryColumns.UNREAD_ENTRIES_FOR_FEED_CONTENT_URI(feedID) : EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(feedID);
                    setData( uri, mShowFeedInfo, false, mShowTextInEntryList );
                } else if ( uriMatch == FeedDataContentProvider.URI_ENTRIES_FOR_GROUP ||
                            uriMatch == FeedDataContentProvider.URI_UNREAD_ENTRIES_FOR_GROUP ) {
                    long groupID = Long.parseLong( mCurrentUri.getPathSegments().get(1) );
                    Uri uri = mShowUnRead ? EntryColumns.UNREAD_ENTRIES_FOR_GROUP_CONTENT_URI(groupID) : EntryColumns.ENTRIES_FOR_GROUP_CONTENT_URI(groupID);
                    setData( uri, mShowFeedInfo, false, mShowTextInEntryList );
                }
                UpdateActions();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }


    public void markAllAsRead() {
        if (mEntriesCursorAdapter != null) {
            Snackbar snackbar = Snackbar.make(getActivity().findViewById(R.id.coordinator_layout), R.string.marked_as_read, Snackbar.LENGTH_LONG)
                    .setActionTextColor(ContextCompat.getColor(getActivity(), R.color.light_theme_color_primary))
                    .setAction(R.string.undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new Thread() {
                                @Override
                                public void run() {
                                    if (mJustMarkedAsReadEntries != null && !mJustMarkedAsReadEntries.isClosed()) {
                                        ArrayList<Integer> ids = new ArrayList<>();
                                        while (mJustMarkedAsReadEntries.moveToNext()) {
                                            ids.add(mJustMarkedAsReadEntries.getInt(0));
                                        }
                                        ContentResolver cr = MainApplication.getContext().getContentResolver();
                                        String where = BaseColumns._ID + " IN (" + TextUtils.join(",", ids) + ')';
                                        cr.update(FeedData.EntryColumns.CONTENT_URI, FeedData.getUnreadContentValues(), where, null);

                                        mJustMarkedAsReadEntries.close();
                                    }
                                }
                            }.start();
                        }
                    });
            snackbar.getView().setBackgroundResource(R.color.material_grey_900);
            snackbar.show();

            new Thread() {
                @Override
                public void run() {
                    ContentResolver cr = MainApplication.getContext().getContentResolver();
                    //String where = EntryColumns.WHERE_UNREAD + Constants.DB_AND + '(' + EntryColumns.FETCH_DATE + Constants.DB_IS_NULL + Constants.DB_OR + EntryColumns.FETCH_DATE + "<=" + mListDisplayDate + ')';
                    String where = EntryColumns.WHERE_UNREAD;
                    if (mJustMarkedAsReadEntries != null && !mJustMarkedAsReadEntries.isClosed()) {
                        mJustMarkedAsReadEntries.close();
                    }
                    mJustMarkedAsReadEntries = cr.query(mCurrentUri, new String[]{BaseColumns._ID}, where, null, null);
                    if ( mCurrentUri != null && Constants.NOTIF_MGR != null  ) {
                        Constants.NOTIF_MGR.cancel( Constants.NOTIFICATION_ID_NEW_ITEMS_COUNT );
                        Constants.NOTIF_MGR.cancel( Constants.NOTIFICATION_ID_MANY_ITEMS_MARKED_STARRED );
                        if ( mJustMarkedAsReadEntries.moveToFirst() )
                            do {
                                Constants.NOTIF_MGR.cancel( mJustMarkedAsReadEntries.getInt(0) );
                            } while ( mJustMarkedAsReadEntries.moveToNext());
                        mJustMarkedAsReadEntries.moveToFirst();
                    }

                    cr.update(mCurrentUri, FeedData.getReadContentValues(), where, null);
                }
            }.start();


        }
    }

    private void startRefresh() {
        if ( mCurrentUri != null && !PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false)) {
            int uriMatcher = FeedDataContentProvider.URI_MATCHER.match(mCurrentUri);
            if ( uriMatcher == FeedDataContentProvider.URI_ENTRIES_FOR_FEED ||
                 uriMatcher == FeedDataContentProvider.URI_UNREAD_ENTRIES_FOR_FEED ) {
                getActivity().startService(new Intent(getActivity(), FetcherService.class)
                        .setAction(FetcherService.ACTION_REFRESH_FEEDS)
                        .putExtra(Constants.FEED_ID, mCurrentUri.getPathSegments().get(1)));
            } else if ( FeedDataContentProvider.URI_MATCHER.match(mCurrentUri) == FeedDataContentProvider.URI_ENTRIES_FOR_GROUP ) {
                getActivity().startService(new Intent(getActivity(), FetcherService.class)
                        .setAction(FetcherService.ACTION_REFRESH_FEEDS)
                        .putExtra(Constants.GROUP_ID, mCurrentUri.getPathSegments().get(1)));
            } else {
                getActivity().startService(new Intent(getActivity(), FetcherService.class)
                        .setAction(FetcherService.ACTION_REFRESH_FEEDS));
            }
        }

        //refreshSwipeProgress();
    }

    public Uri getUri() {
        return mOriginalUri;
    }

    public void setData(Uri uri, boolean showFeedInfo, boolean isSearchUri, boolean showTextInEntryList) {
        if ( getActivity() == null ) // during configuration changes
            return;
        Timer timer = new Timer( "EntriesListFragment.setData" );

        Dog.v( String.format( "EntriesListFragment.setData( %s )", uri.toString() ) );
        mCurrentUri = uri;
        if (!isSearchUri) {
            mOriginalUri = mCurrentUri;
            mOriginalUriShownEntryText = showTextInEntryList;
        }

        mShowFeedInfo = showFeedInfo;
        mShowTextInEntryList = showTextInEntryList;

        //if ( mShowTextInEntryList )
        //    mNeedSetSelection = true;
        mEntriesCursorAdapter = new EntriesCursorAdapter(getActivity(), mCurrentUri, Constants.EMPTY_CURSOR, mShowFeedInfo, mShowTextInEntryList, mShowUnRead);
        SetListViewAdapter();
        if ( mListView instanceof ListView )
            mListView.setDividerHeight( mShowTextInEntryList ? 10 : 0 );
        mListDisplayDate = new Date().getTime();
        if (mCurrentUri != null) {
            restartLoaders();
        }

        refreshUI();

        //getActivity().invalidateOptionsMenu();
        //if (showTextInEntryList)
            //setSelection( mEntriesCursorAdapter.GetFirstUnReadPos() );//    setSelection( mEntriesCursorAdapter.getCount() - 1 );
        timer.End();
    }

    private void restartLoaders() {

        LoaderManager loaderManager = getLoaderManager();

        //HACK: 2 times to workaround a hard-to-reproduce bug with non-refreshing loaders...
        Timer.Start( ENTRIES_LOADER_ID, "EntriesListFr.restartLoaders() mEntriesLoader" );
        loaderManager.restartLoader(ENTRIES_LOADER_ID, null, mEntriesLoader);
        Timer.Start( NEW_ENTRIES_NUMBER_LOADER_ID, "EntriesListFr.restartLoaders() mEntriesNumberLoader" );
        loaderManager.restartLoader(NEW_ENTRIES_NUMBER_LOADER_ID, null, mEntriesNumberLoader);

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


    /*private class SwipeGestureListener extends SimpleOnGestureListener implements OnTouchListener {
        static final int SWIPE_MIN_DISTANCE = 120;
        static final int SWIPE_MAX_OFF_PATH = 150;
        static final int SWIPE_THRESHOLD_VELOCITY = 150;

        private final GestureDetector mGestureDetector;

        public SwipeGestureListener(Context context) {
            mGestureDetector = new GestureDetector(context, this);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (mListView != null && e1 != null && e2 != null &&
                    Math.abs(e1.getY() - e2.getY()) <= SWIPE_MAX_OFF_PATH &&
                    Math.abs(velocityX) >= SWIPE_THRESHOLD_VELOCITY) {
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
    }*/


}
