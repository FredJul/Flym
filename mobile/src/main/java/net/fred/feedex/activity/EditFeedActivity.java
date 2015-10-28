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

package net.fred.feedex.activity;

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
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SimpleAdapter;
import android.widget.TabHost;
import android.widget.Toast;

import net.fred.feedex.Constants;
import net.fred.feedex.R;
import net.fred.feedex.adapter.FiltersCursorAdapter;
import net.fred.feedex.loader.BaseLoader;
import net.fred.feedex.provider.FeedData.FeedColumns;
import net.fred.feedex.provider.FeedData.FilterColumns;
import net.fred.feedex.provider.FeedDataContentProvider;
import net.fred.feedex.utils.Dog;
import net.fred.feedex.utils.NetworkUtils;
import net.fred.feedex.utils.UiUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

public class EditFeedActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    static final String FEED_SEARCH_TITLE = "title";
    static final String FEED_SEARCH_URL = "url";
    static final String FEED_SEARCH_DESC = "contentSnippet";
    private static final String STATE_CURRENT_TAB = "STATE_CURRENT_TAB";
    private static final String[] FEED_PROJECTION = new String[]{FeedColumns.NAME, FeedColumns.URL, FeedColumns.RETRIEVE_FULLTEXT};
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
    private ListView mFiltersListView;
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
        mFiltersListView = (ListView) findViewById(android.R.id.list);
        View tabWidget = findViewById(android.R.id.tabs);
        View buttonLayout = findViewById(R.id.button_layout);

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

        if (intent.getAction().equals(Intent.ACTION_INSERT) || intent.getAction().equals(Intent.ACTION_SEND)) {
            setTitle(R.string.new_feed_title);

            tabWidget.setVisibility(View.GONE);

            if (intent.hasExtra(Intent.EXTRA_TEXT)) {
                mUrlEditText.setText(intent.getStringExtra(Intent.EXTRA_TEXT));
            }
        } else if (intent.getAction().equals(Intent.ACTION_EDIT)) {
            setTitle(R.string.edit_feed_title);

            buttonLayout.setVisibility(View.GONE);

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

                if (cursor.moveToNext()) {
                    mNameEditText.setText(cursor.getString(0));
                    mUrlEditText.setText(cursor.getString(1));
                    mRetrieveFulltextCb.setChecked(cursor.getInt(2) == 1);
                    cursor.close();
                } else {
                    cursor.close();
                    Toast.makeText(EditFeedActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_CURRENT_TAB, mTabHost.getCurrentTab());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (getIntent().getAction().equals(Intent.ACTION_EDIT)) {
            String url = mUrlEditText.getText().toString();
            ContentResolver cr = getContentResolver();

            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(FeedColumns.CONTENT_URI, FeedColumns.PROJECTION_ID,
                        FeedColumns.URL + Constants.DB_ARG, new String[]{url}, null);

                if (cursor != null && cursor.moveToFirst() && !getIntent().getData().getLastPathSegment().equals(cursor.getString(0))) {
                    Toast.makeText(EditFeedActivity.this, R.string.error_feed_url_exists, Toast.LENGTH_LONG).show();
                } else {
                    ContentValues values = new ContentValues();

                    if (!url.startsWith(Constants.HTTP_SCHEME) && !url.startsWith(Constants.HTTPS_SCHEME)) {
                        url = Constants.HTTP_SCHEME + url;
                    }
                    values.put(FeedColumns.URL, url);

                    String name = mNameEditText.getText().toString();

                    values.put(FeedColumns.NAME, name.trim().length() > 0 ? name : null);
                    values.put(FeedColumns.RETRIEVE_FULLTEXT, mRetrieveFulltextCb.isChecked() ? 1 : null);
                    values.put(FeedColumns.FETCH_MODE, 0);
                    values.putNull(FeedColumns.ERROR);

                    cr.update(getIntent().getData(), values, null, null);
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

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onClickOk(View view) {
        // only in insert mode

        final String name = mNameEditText.getText().toString().trim();
        final String urlOrSearch = mUrlEditText.getText().toString().trim();
        if (urlOrSearch.isEmpty()) {
            Toast.makeText(this, R.string.error_feed_error, Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(EditFeedActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                    } else if (data.isEmpty()) {
                        Toast.makeText(EditFeedActivity.this, R.string.no_result, Toast.LENGTH_SHORT).show();
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
                                FeedDataContentProvider.addFeed(EditFeedActivity.this, data.get(which).get(FEED_SEARCH_URL), name.isEmpty() ? data.get(which).get(FEED_SEARCH_TITLE) : name, mRetrieveFulltextCb.isChecked());

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
            FeedDataContentProvider.addFeed(EditFeedActivity.this, urlOrSearch, name, mRetrieveFulltextCb.isChecked());

            setResult(RESULT_OK);
            finish();
        }
    }

    public void onClickCancel(View view) {
        finish();
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
            HttpURLConnection conn = NetworkUtils.setupConnection("https://ajax.googleapis.com/ajax/services/feed/find?v=1.0&q=" + mSearchText);
            try {
                String jsonStr = new String(NetworkUtils.getBytes(conn.getInputStream()));

                // Parse results
                final ArrayList<HashMap<String, String>> results = new ArrayList<>();
                JSONObject response = new JSONObject(jsonStr).getJSONObject("responseData");
                JSONArray entries = response.getJSONArray("entries");
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
