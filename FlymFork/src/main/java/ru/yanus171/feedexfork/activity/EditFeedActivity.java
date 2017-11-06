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
 * <p/>
 * <p/>
 * Some parts of this software are based on "Sparse rss" under the MIT license (see
 * below). Please refers to the original project to identify which parts are under the
 * MIT license.
 * <p/>
 * Copyright (c) 2010-2012 Stefan Handschuh
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package ru.yanus171.feedexfork.activity;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.ResourceCursorAdapter;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.adapter.FiltersCursorAdapter;
import ru.yanus171.feedexfork.fragment.EditFeedsListFragment;
import ru.yanus171.feedexfork.loader.BaseLoader;
import ru.yanus171.feedexfork.provider.FeedData.FeedColumns;
import ru.yanus171.feedexfork.provider.FeedData.FilterColumns;
import ru.yanus171.feedexfork.provider.FeedDataContentProvider;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.NetworkUtils;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.UiUtils;

public class EditFeedActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    static final String FEED_SEARCH_TITLE = "title";
    static final String FEED_SEARCH_URL = "website";//"url";
    static final String FEED_SEARCH_DESC = "description";//"contentSnippet";
    private static final String STATE_CURRENT_TAB = "STATE_CURRENT_TAB";
    private static final String[] FEED_PROJECTION =
        new String[]{FeedColumns.NAME, FeedColumns.URL, FeedColumns.RETRIEVE_FULLTEXT, FeedColumns.IS_GROUP, FeedColumns.SHOW_TEXT_IN_ENTRY_LIST, FeedColumns.IS_AUTO_REFRESH, FeedColumns.GROUP_ID };
    private final ActionMode.Callback mFilterActionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.edit_context_menu, menu);
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

            switch (item.getItemId()) {
                case R.id.menu_edit:
                    Cursor c = mFiltersCursorAdapter.getCursor();
                    if (c.moveToPosition(mFiltersCursorAdapter.getSelectedFilter())) {
                        final View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter_edit, null);
                        final EditText filterText = (EditText) dialogView.findViewById(R.id.filterText);
                        final CheckBox regexCheckBox = (CheckBox) dialogView.findViewById(R.id.regexCheckBox);
                        final RadioButton applyTitleRadio = (RadioButton) dialogView.findViewById(R.id.applyTitleRadio);
                        final RadioButton applyContentRadio = (RadioButton) dialogView.findViewById(R.id.applyContentRadio);
                        final RadioButton acceptRadio = (RadioButton) dialogView.findViewById(R.id.acceptRadio);
                        final RadioButton rejectRadio = (RadioButton) dialogView.findViewById(R.id.rejectRadio);

                        filterText.setText(c.getString(c.getColumnIndex(FilterColumns.FILTER_TEXT)));
                        regexCheckBox.setChecked(c.getInt(c.getColumnIndex(FilterColumns.IS_REGEX)) == 1);
                        if (c.getInt(c.getColumnIndex(FilterColumns.IS_APPLIED_TO_TITLE)) == 1) {
                            applyTitleRadio.setChecked(true);
                        } else {
                            applyContentRadio.setChecked(true);
                        }
                        if (c.getInt(c.getColumnIndex(FilterColumns.IS_ACCEPT_RULE)) == 1) {
                            acceptRadio.setChecked(true);
                        } else {
                            rejectRadio.setChecked(true);
                        }

                        final long filterId = mFiltersCursorAdapter.getItemId(mFiltersCursorAdapter.getSelectedFilter());
                        new AlertDialog.Builder(EditFeedActivity.this) //
                                .setTitle(R.string.filter_edit_title) //
                                .setView(dialogView) //
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        new Thread() {
                                            @Override
                                            public void run() {
                                                String filter = filterText.getText().toString();
                                                if (!filter.isEmpty()) {
                                                    ContentResolver cr = getContentResolver();
                                                    ContentValues values = new ContentValues();
                                                    values.put(FilterColumns.FILTER_TEXT, filter);
                                                    values.put(FilterColumns.IS_REGEX, regexCheckBox.isChecked());
                                                    values.put(FilterColumns.IS_APPLIED_TO_TITLE, applyTitleRadio.isChecked());
                                                    values.put(FilterColumns.IS_ACCEPT_RULE, acceptRadio.isChecked());
                                                    if (cr.update(FilterColumns.CONTENT_URI, values, FilterColumns._ID + '=' + filterId, null) > 0) {
                                                        cr.notifyChange(
                                                                FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(getIntent().getData().getLastPathSegment()),
                                                                null);
                                                    }
                                                }
                                            }
                                        }.start();
                                    }
                                }).setNegativeButton(android.R.string.cancel, null).show();
                    }

                    mode.finish(); // Action picked, so close the CAB
                    return true;
                case R.id.menu_delete:
                    final long filterId = mFiltersCursorAdapter.getItemId(mFiltersCursorAdapter.getSelectedFilter());
                    new AlertDialog.Builder(EditFeedActivity.this) //
                            .setIcon(android.R.drawable.ic_dialog_alert) //
                            .setTitle(R.string.filter_delete_title) //
                            .setMessage(R.string.question_delete_filter) //
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    new Thread() {
                                        @Override
                                        public void run() {
                                            ContentResolver cr = getContentResolver();
                                            if (cr.delete(FilterColumns.CONTENT_URI, FilterColumns._ID + '=' + filterId, null) > 0) {
                                                cr.notifyChange(FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(getIntent().getData().getLastPathSegment()),
                                                        null);
                                            }
                                        }
                                    }.start();
                                }
                            }).setNegativeButton(android.R.string.no, null).show();

                    mode.finish(); // Action picked, so close the CAB
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mFiltersCursorAdapter.setSelectedFilter(-1);
            mFiltersListView.invalidateViews();
        }
    };
    private TabHost mTabHost;
    private EditText mNameEditText, mUrlEditText;
    private CheckBox mRetrieveFulltextCb;
    private CheckBox mShowTextInEntryListCb;
    private CheckBox mIsAutoRefreshCb;
    private ListView mFiltersListView;
    private Spinner mGroupSpinner;
    private CheckBox mHasGroupCb;
    private FiltersCursorAdapter mFiltersCursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UiUtils.setPreferenceTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_feed_edit);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setResult(RESULT_CANCELED);

        Intent intent = getIntent();

        mTabHost = (TabHost) findViewById(R.id.tabHost);
        mNameEditText = (EditText) findViewById(R.id.feed_title);
        mUrlEditText = (EditText) findViewById(R.id.feed_url);
        mRetrieveFulltextCb = (CheckBox) findViewById(R.id.retrieve_fulltext);
        mShowTextInEntryListCb = (CheckBox) findViewById(R.id.show_text_in_entry_list);
        mIsAutoRefreshCb =  (CheckBox) findViewById(R.id.auto_refresh);
        mFiltersListView = (ListView) findViewById(android.R.id.list);
        mGroupSpinner = (Spinner) findViewById(R.id.spin_group);
        mHasGroupCb = (CheckBox) findViewById(R.id.has_group);
        mHasGroupCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                UpdateSpinnerGroup();
            }
        });
        View tabWidget = findViewById(android.R.id.tabs);

        mIsAutoRefreshCb.setVisibility(  PrefUtils.getBoolean( PrefUtils.REFRESH_ONLY_SELECTED, false ) ? View.VISIBLE : View.GONE );

        mTabHost.setup();
        mTabHost.addTab(mTabHost.newTabSpec("feedTab").setIndicator(getString(R.string.tab_feed_title)).setContent(R.id.feed_tab));
        mTabHost.addTab(mTabHost.newTabSpec("filtersTab").setIndicator(getString(R.string.tab_filters_title)).setContent(R.id.filters_tab));

        mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String s) {
                invalidateOptionsMenu();
            }
        });

        if (savedInstanceState != null) {
            mTabHost.setCurrentTab(savedInstanceState.getInt(STATE_CURRENT_TAB));
        }

        ResourceCursorAdapter adapter =
                new ResourceCursorAdapter( this,
                                           android.R.layout.simple_spinner_item,
                                           getContentResolver().query( FeedColumns.GROUPS_CONTENT_URI, new String[] { FeedColumns._ID, FeedColumns.NAME }, null, null, FeedColumns.NAME ),
                                           0 ) {
            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                TextView nameTextView = (TextView) view.findViewById(android.R.id.text1);
                nameTextView.setText(cursor.getString(1));
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mGroupSpinner.setAdapter( adapter );


        if (intent.getAction().equals(Intent.ACTION_INSERT) || intent.getAction().equals(Intent.ACTION_SEND)) {
            setTitle(R.string.new_feed_title);

            tabWidget.setVisibility(View.GONE);

            if (intent.hasExtra(Intent.EXTRA_TEXT)) {
                mUrlEditText.setText(intent.getStringExtra(Intent.EXTRA_TEXT));
            }
        } else if (intent.getAction().equals(Intent.ACTION_VIEW)) {
            setTitle(R.string.new_feed_title);

            tabWidget.setVisibility(View.GONE);
            mUrlEditText.setText(intent.getDataString());
        } else if (intent.getAction().equals(Intent.ACTION_EDIT)) {
            setTitle(R.string.edit_feed_title);

            mFiltersCursorAdapter = new FiltersCursorAdapter(this, Constants.EMPTY_CURSOR);
            mFiltersListView.setAdapter(mFiltersCursorAdapter);
            mFiltersListView.setOnItemLongClickListener(new OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    startSupportActionMode(mFilterActionModeCallback);
                    mFiltersCursorAdapter.setSelectedFilter(position);
                    mFiltersListView.invalidateViews();
                    return true;
                }
            });

            getLoaderManager().initLoader(0, null, this);

            if (savedInstanceState == null) {
                Cursor cursor = getContentResolver().query(intent.getData(), FEED_PROJECTION, null, null, null);

                if (cursor != null && cursor.moveToNext()) {
                    mNameEditText.setText(cursor.getString(0));
                    mUrlEditText.setText(cursor.getString(1));
                    mRetrieveFulltextCb.setChecked(cursor.getInt(2) == 1);
                    mShowTextInEntryListCb.setChecked(cursor.getInt(4) == 1);
                    mIsAutoRefreshCb.setChecked(cursor.getInt(5) == 1);
                    mGroupSpinner.setSelection( -1);
                    mHasGroupCb.setChecked(!cursor.isNull(6));
                    UpdateSpinnerGroup();
                    if ( !cursor.isNull(6) )
                        for ( int i = 0; i < mGroupSpinner.getCount(); i ++ )
                            if ( mGroupSpinner.getItemIdAtPosition( i ) == cursor.getInt(6) ) {
                                mGroupSpinner.setSelection( i );
                                break;
                            }
                    if (cursor.getInt(3) == 1) { // if it's a group, we cannot edit it
                        finish();
                    }
                } else {
                    UiUtils.showMessage(EditFeedActivity.this, R.string.error);
                    finish();
                }

                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    private void UpdateSpinnerGroup() {
        mGroupSpinner.setVisibility( mHasGroupCb.isChecked() ? View.VISIBLE : View.GONE );
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_CURRENT_TAB, mTabHost.getCurrentTab());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        if (getIntent().getAction().equals(Intent.ACTION_EDIT)) {
            String url = mUrlEditText.getText().toString();
            ContentResolver cr = getContentResolver();

            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(FeedColumns.CONTENT_URI, FeedColumns.PROJECTION_ID,
                        FeedColumns.URL + Constants.DB_ARG, new String[]{url}, null);

                if (cursor != null && cursor.moveToFirst() && !getIntent().getData().getLastPathSegment().equals(cursor.getString(0))) {
                    UiUtils.showMessage(EditFeedActivity.this, R.string.error_feed_url_exists);
                } else {
                    ContentValues values = new ContentValues();

                    if (!url.startsWith(Constants.HTTP_SCHEME) && !url.startsWith(Constants.HTTPS_SCHEME)) {
                        url = Constants.HTTP_SCHEME + url;
                    }
                    values.put(FeedColumns.URL, url);

                    String name = mNameEditText.getText().toString();

                    values.put(FeedColumns.NAME, name.trim().length() > 0 ? name : null);
                    values.put(FeedColumns.RETRIEVE_FULLTEXT, mRetrieveFulltextCb.isChecked() ? 1 : null);
                    values.put(FeedColumns.SHOW_TEXT_IN_ENTRY_LIST, mShowTextInEntryListCb.isChecked() ? 1 : null);
                    values.put(FeedColumns.IS_AUTO_REFRESH, mIsAutoRefreshCb.isChecked() ? 1 : null);
                    values.put(FeedColumns.FETCH_MODE, 0);
                    if ( mHasGroupCb.isChecked() && mGroupSpinner.getSelectedItemId() != AdapterView.INVALID_ROW_ID )
                        values.put(FeedColumns.GROUP_ID, mGroupSpinner.getSelectedItemId() );
                    else
                        values.putNull(FeedColumns.GROUP_ID);

                    values.putNull(FeedColumns.ERROR);

                    synchronized (HomeActivity.mFeedSetupChanged ) {
                        HomeActivity.mFeedSetupChanged = true;
                        cr.update(getIntent().getData(), values, null, null);
                    }
                }
            } catch (Exception ignored) {
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

        }

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_feed, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mTabHost.getCurrentTab() == 0) {
            menu.findItem(R.id.menu_add_filter).setVisible(false);
        } else {
            menu.findItem(R.id.menu_add_filter).setVisible(true);
        }

        boolean edit = getIntent() != null && getIntent().getAction().equals(Intent.ACTION_EDIT);
        boolean insert = getIntent() != null && ( getIntent().getAction().equals(Intent.ACTION_INSERT)  || getIntent().getAction().equals(Intent.ACTION_SEND) );
        menu.findItem(R.id.menu_validate).setVisible(insert);
        menu.findItem(R.id.menu_delete_feed).setVisible(edit);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_validate: // only in insert mode
                final String name = mNameEditText.getText().toString().trim();
                final String urlOrSearch = mUrlEditText.getText().toString().trim();
                if (urlOrSearch.isEmpty()) {
                    UiUtils.showMessage(EditFeedActivity.this, R.string.error_feed_error);
                }

                if (!urlOrSearch.contains(".") || !urlOrSearch.contains("/") || urlOrSearch.contains(" ")) {
                    final ProgressDialog pd = new ProgressDialog(EditFeedActivity.this);
                    pd.setMessage(getString(R.string.loading));
                    pd.setCancelable(true);
                    pd.setIndeterminate(true);
                    pd.show();

                    getLoaderManager().restartLoader(1, null, new LoaderManager.LoaderCallbacks<ArrayList<HashMap<String, String>>>() {

                        @Override
                        public Loader<ArrayList<HashMap<String, String>>> onCreateLoader(int id, Bundle args) {
                            String encodedSearchText = urlOrSearch;
                            try {
                                encodedSearchText = URLEncoder.encode(urlOrSearch, Constants.UTF8);
                            } catch (UnsupportedEncodingException ignored) {
                            }

                            return new GetFeedSearchResultsLoader(EditFeedActivity.this, encodedSearchText);
                        }

                        @Override
                        public void onLoadFinished(Loader<ArrayList<HashMap<String, String>>> loader, final ArrayList<HashMap<String, String>> data) {
                            pd.cancel();

                            if (data == null) {
                                UiUtils.showMessage(EditFeedActivity.this, R.string.error);
                            } else if (data.isEmpty()) {
                                UiUtils.showMessage(EditFeedActivity.this, R.string.no_result);
                            } else {
                                AlertDialog.Builder builder = new AlertDialog.Builder(EditFeedActivity.this);
                                builder.setTitle(R.string.feed_search);

                                // create the grid item mapping
                                String[] from = new String[]{FEED_SEARCH_TITLE, FEED_SEARCH_DESC};
                                int[] to = new int[]{android.R.id.text1, android.R.id.text2};

                                // fill in the grid_item layout
                                SimpleAdapter adapter = new SimpleAdapter(EditFeedActivity.this, data, R.layout.item_search_result, from,
                                        to);
                                builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        FeedDataContentProvider.addFeed(EditFeedActivity.this,
                                                                        data.get(which).get(FEED_SEARCH_URL),
                                                                        name.isEmpty() ? data.get(which).get(FEED_SEARCH_TITLE) : name,
                                                                        mRetrieveFulltextCb.isChecked(),
                                                                        mShowTextInEntryListCb.isChecked());

                                        setResult(RESULT_OK);
                                        finish();
                                    }
                                });
                                builder.show();
                            }
                        }

                        @Override
                        public void onLoaderReset(Loader<ArrayList<HashMap<String, String>>> loader) {
                        }
                    });
                } else {
                    FeedDataContentProvider.addFeed(EditFeedActivity.this,
                                                    urlOrSearch,
                                                    name,
                                                    mRetrieveFulltextCb.isChecked(),
                                                    mShowTextInEntryListCb.isChecked());

                    setResult(RESULT_OK);
                    finish();
                }
                return true;
            case R.id.menu_add_filter: {
                final View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter_edit, null);

                new AlertDialog.Builder(this) //
                        .setTitle(R.string.filter_add_title) //
                        .setView(dialogView) //
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                String filterText = ((EditText) dialogView.findViewById(R.id.filterText)).getText().toString();
                                if (filterText.length() != 0) {
                                    String feedId = getIntent().getData().getLastPathSegment();

                                    ContentValues values = new ContentValues();
                                    values.put(FilterColumns.FILTER_TEXT, filterText);
                                    values.put(FilterColumns.IS_REGEX, ((CheckBox) dialogView.findViewById(R.id.regexCheckBox)).isChecked());
                                    values.put(FilterColumns.IS_APPLIED_TO_TITLE, ((RadioButton) dialogView.findViewById(R.id.applyTitleRadio)).isChecked());
                                    values.put(FilterColumns.IS_ACCEPT_RULE, ((RadioButton) dialogView.findViewById(R.id.acceptRadio)).isChecked());

                                    ContentResolver cr = getContentResolver();
                                    cr.insert(FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(feedId), values);
                                }
                            }
                        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                }).show();
                return true;
            }
            case R.id.menu_delete_feed:
                EditFeedsListFragment.DeleteFeed( this, getIntent().getData(), "" );
                return true;
            default:

                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader cursorLoader = new CursorLoader(this, FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(getIntent().getData().getLastPathSegment()),
                null, null, null, FilterColumns.IS_ACCEPT_RULE + Constants.DB_DESC);
        cursorLoader.setUpdateThrottle(Constants.UPDATE_THROTTLE_DELAY);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mFiltersCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mFiltersCursorAdapter.swapCursor(Constants.EMPTY_CURSOR);
    }
}

/**
 * A custom Loader that loads feed search results from the google WS.
 */
class GetFeedSearchResultsLoader extends BaseLoader<ArrayList<HashMap<String, String>>> {
    private final String mSearchText;

    public GetFeedSearchResultsLoader(Context context, String searchText) {
        super(context);
        mSearchText = searchText;
    }

    /**
     * This is where the bulk of our work is done. This function is called in a background thread and should generate a new set of data to be
     * published by the loader.
     */
    @Override
    public ArrayList<HashMap<String, String>> loadInBackground() {
        try {
        //    HttpURLConnection conn = NetworkUtils.setupConnection("https://ajax.googleapis.com/ajax/services/feed/find?v=1.0&q=" + mSearchText);
            HttpURLConnection conn = NetworkUtils.setupConnection("http://cloud.feedly.com/v3/search/feeds/?query=" + mSearchText);
            try {
                String jsonStr = new String(NetworkUtils.getBytes(conn.getInputStream()));
                //String jsonStr = "{\"results\":[{\"deliciousTags\":[\"история\",\"science\",\"education\",\"culture\"],\"feedId\":\"feed/http://arzamas.academy/feed_v1.rss\",\"title\":\"Arzamas | Всё\",\"language\":\"ru\",\"lastUpdated\":1493701920000,\"subscribers\":1947,\"velocity\":3.5,\"website\":\"http://arzamas.academy/?utm_campaign=main_rss&utm_medium=rss&utm_source=rss_link\",\"score\":1947.0,\"coverage\":0.0,\"coverageScore\":0.0,\"estimatedEngagement\":1507,\"scheme\":\"TEXT:BASELINE:ORGANIC_SEARCH\",\"contentType\":\"article\",\"description\":\"Новые курсы каждый четверг и дополнительные материалы в течение недели\",\"coverUrl\":\"https://storage.googleapis.com/site-assets/k-a2Rfu4lTUFERhdb4Iu4gHFsZMPdSLNGXIfDvO_Xio_cover-15b8675fdfa\",\"iconUrl\":\"http://storage.googleapis.com/site-assets/k-a2Rfu4lTUFERhdb4Iu4gHFsZMPdSLNGXIfDvO_Xio_icon-15414d05afc\",\"partial\":false,\"visualUrl\":\"http://storage.googleapis.com/site-assets/k-a2Rfu4lTUFERhdb4Iu4gHFsZMPdSLNGXIfDvO_Xio_visual-15414d05afc\",\"coverColor\":\"000000\",\"art\":0.0},{\"deliciousTags\":[\"magazines\",\"история\",\"журналы\",\"блоги\"],\"feedId\":\"feed/http://arzamas.academy/feed_v1/mag.rss\",\"title\":\"Arzamas | Журнал\",\"language\":\"ru\",\"lastUpdated\":1493701980000,\"subscribers\":249,\"velocity\":1.2,\"website\":\"http://arzamas.academy/mag\",\"score\":1871.93,\"coverage\":0.0,\"coverageScore\":0.0,\"estimatedEngagement\":1390,\"scheme\":\"TEXT:BASELINE:ORGANIC_SEARCH\",\"contentType\":\"article\",\"description\":\"??????? ???????? ? ?????, ??????? ? ???????????\",\"coverUrl\":\"https://storage.googleapis.com/site-assets/ji5fzOy4BO9f4oY16Qmlc3KH9RlyGhyWY94tNjhuGzM_cover-15b8e8b3d47\",\"iconUrl\":\"http://storage.googleapis.com/site-assets/ji5fzOy4BO9f4oY16Qmlc3KH9RlyGhyWY94tNjhuGzM_icon-1542d1e8523\",\"partial\":false,\"visualUrl\":\"http://storage.googleapis.com/site-assets/ji5fzOy4BO9f4oY16Qmlc3KH9RlyGhyWY94tNjhuGzM_visual-1542d1e8523\",\"coverColor\":\"000000\",\"art\":0.0},{\"deliciousTags\":[\"science\",\"история\",\"образование\",\"education\"],\"feedId\":\"feed/http://arzamas.academy/feed_v1/courses.rss\",\"title\":\"Arzamas | Курсы\",\"language\":\"ru\",\"lastUpdated\":1435214100000,\"subscribers\":245,\"velocity\":0.01,\"website\":\"http://arzamas.academy/courses?utm_campaign=courses_rss&utm_medium=rss&utm_source=rss_link\",\"score\":24.5,\"coverage\":0.0,\"coverageScore\":0.0,\"scheme\":\"TEXT:BASELINE:ORGANIC_SEARCH\",\"contentType\":\"article\",\"description\":\"Новые курсы каждый четверг\",\"coverUrl\":\"https://storage.googleapis.com/site-assets/oAIHb_UC11ZeKuhJ5EHbRqG5xBSgtfKokwxB22lC79M_cover-15b870310a2\",\"iconUrl\":\"https://storage.googleapis.com/site-assets/oAIHb_UC11ZeKuhJ5EHbRqG5xBSgtfKokwxB22lC79M_icon-15b870310a2\",\"partial\":false,\"visualUrl\":\"https://storage.googleapis.com/site-assets/oAIHb_UC11ZeKuhJ5EHbRqG5xBSgtfKokwxB22lC79M_visual-15b870310a2\",\"coverColor\":\"000000\",\"art\":0.0},{\"feedId\":\"feed/http://arzamas.academy/feed_v1/podcast.rss\",\"title\":\"История культуры | Курсы | Arzamas\",\"language\":\"ru\",\"lastUpdated\":1492671600000,\"subscribers\":17,\"velocity\":10.5,\"website\":\"http://arzamas.academy/courses?utm_campaign=episodes_podcast_rss&utm_medium=rss&utm_source=rss_link\",\"score\":17.0,\"coverage\":0.0,\"coverageScore\":0.0,\"estimatedEngagement\":366,\"scheme\":\"TEXT:BASELINE:ORGANIC_SEARCH\",\"contentType\":\"audio\",\"description\":\"Arzamas — это просветительский проект, посвященный гуманитарному знанию. В основе Arzamas лежат курсы, или «гуманитарные сериалы», — каждый на свою тему. Мы записываем выдающихся ученых-гуманитариев и снабжаем их лекции дополнительными материалами. Получается своебразный университет, каждую неделю, по четвергам, выпускающий новый курс по истории, литературе, искусству, антропологии, философии — о культуре и человеке. В этом подкасте представлены аудио-версии наших курсов. Полные версии доступы на нашем сайте (http://arzamas.academy).\",\"coverUrl\":\"https://storage.googleapis.com/site-assets/icWMybnKLWR_HUKv9xalz-t_5fWkFgn19OICVXWnYSA_cover-15b99c01c32\",\"iconUrl\":\"https://storage.googleapis.com/site-assets/icWMybnKLWR_HUKv9xalz-t_5fWkFgn19OICVXWnYSA_icon-15b99c01c32\",\"partial\":false,\"visualUrl\":\"https://storage.googleapis.com/site-assets/icWMybnKLWR_HUKv9xalz-t_5fWkFgn19OICVXWnYSA_visual-15b99c01c32\",\"coverColor\":\"000000\",\"art\":0.0},{\"deliciousTags\":[\"youtube\"],\"feedId\":\"feed/https://www.youtube.com/playlist?list=UUVgvnGSFU41kIhEc09aztEg\",\"title\":\"Arzamas (uploads) on YouTube\",\"language\":\"ru\",\"lastUpdated\":1491832800000,\"subscribers\":28,\"velocity\":1.2,\"website\":\"https://youtube.com/playlist?list=UUVgvnGSFU41kIhEc09aztEg\",\"score\":152.78,\"coverage\":0.0,\"coverageScore\":0.0,\"estimatedEngagement\":149,\"scheme\":\"TEXT:BASELINE:ORGANIC_SEARCH\",\"contentType\":\"video\",\"description\":\"Официальный канал Arzamas.academy\",\"iconUrl\":\"https://storage.googleapis.com/site-assets/jGwxZEqqmeCambMrgz-PZPVTczhBEEZ48IdO6thk3Ww_sicon-15b6534aa74\",\"partial\":false,\"visualUrl\":\"https://storage.googleapis.com/site-assets/jGwxZEqqmeCambMrgz-PZPVTczhBEEZ48IdO6thk3Ww_svisual-15b6534aa74\",\"art\":0.0}],\"queryType\":\"term\",\"related\":[\"история\"],\"scheme\":\"subs.0\"}";

                // Parse results
                final ArrayList<HashMap<String, String>> results = new ArrayList<>();
                //JSONObject response = new JSONObject(jsonStr).getJSONObject("responseData");
                JSONArray entries = new JSONObject(jsonStr).getJSONArray("results");
                for (int i = 0; i < entries.length(); i++) {
                    try {
                        JSONObject entry = (JSONObject) entries.get(i);
                        String url = entry.get(EditFeedActivity.FEED_SEARCH_URL).toString();
                        if (!url.isEmpty()) {
                            HashMap<String, String> map = new HashMap<>();
                            map.put(EditFeedActivity.FEED_SEARCH_TITLE, Html.fromHtml(entry.get(EditFeedActivity.FEED_SEARCH_TITLE).toString())
                                    .toString());
                            map.put(EditFeedActivity.FEED_SEARCH_URL, url);
                            map.put(EditFeedActivity.FEED_SEARCH_DESC, Html.fromHtml(entry.get(EditFeedActivity.FEED_SEARCH_DESC).toString()).toString());

                            results.add(map);
                        }
                    } catch (Exception ignored) {
                    }
                }

                return results;
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            Dog.e("Error", e);
            return null;
        }
    }
}
