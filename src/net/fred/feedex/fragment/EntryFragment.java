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
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import net.fred.feedex.Constants;
import net.fred.feedex.MainApplication;
import net.fred.feedex.R;
import net.fred.feedex.activity.BaseActivity;
import net.fred.feedex.loader.BaseLoader;
import net.fred.feedex.provider.FeedData;
import net.fred.feedex.provider.FeedData.EntryColumns;
import net.fred.feedex.provider.FeedData.FeedColumns;
import net.fred.feedex.provider.FeedData.TaskColumns;
import net.fred.feedex.provider.FeedDataContentProvider;
import net.fred.feedex.service.FetcherService;
import net.fred.feedex.utils.PrefUtils;
import net.fred.feedex.utils.ThrottledContentObserver;
import net.fred.feedex.utils.UiUtils;
import net.fred.feedex.view.DepthPageTransformer;
import net.fred.feedex.view.EntryView;

public class EntryFragment extends Fragment implements BaseActivity.OnFullScreenListener, LoaderManager.LoaderCallbacks<EntryLoaderResult>, EntryView.OnActionListener {

    private static final String STATE_BASE_URI = "STATE_BASE_URI";
    private static final String STATE_CURRENT_PAGER_POS = "STATE_CURRENT_PAGER_POS";
    private static final String STATE_ENTRIES_IDS = "STATE_ENTRIES_IDS";
    private static final String STATE_INITIAL_ENTRY_ID = "STATE_INITIAL_ENTRY_ID";

    private int mTitlePos = -1, mDatePos, mMobilizedHtmlPos, mAbstractPos, mLinkPos, mIsFavoritePos, mIsReadPos, mEnclosurePos, mAuthorPos, mFeedNamePos, mFeedUrlPos, mFeedIconPos;

    private int mCurrentPagerPos = -1;
    private Uri mBaseUri;
    private long mInitialEntryId = -1;
    private long[] mEntriesIds;

    private boolean mFavorite, mPreferFullText = true;

    private ViewPager mEntryPager;
    private EntryPagerAdapter mEntryPagerAdapter;

    private View mCancelFullscreenBtn;

    private final ThrottledContentObserver mTasksObserver = new ThrottledContentObserver(new Handler(), 2000) {
        @Override
        public void onChangeThrottled() {
            BaseActivity activity = (BaseActivity) getActivity();
            boolean isMobilizing = FetcherService.getMobilizingTaskId(mEntriesIds[mCurrentPagerPos]) != -1;
            if (activity == null || (activity.getProgressBar().getVisibility() == View.VISIBLE && isMobilizing)
                    || (activity.getProgressBar().getVisibility() == View.GONE && !isMobilizing)) {
                return; // no change => no update
            }

            if (isMobilizing) { // We start a mobilization
                activity.getProgressBar().setVisibility(View.VISIBLE);
            } else { // We finished one
                mPreferFullText = true;
                getLoaderManager().restartLoader(mEntryPager.getCurrentItem(), null, EntryFragment.this);
            }
        }
    };

    private class EntryPagerAdapter extends PagerAdapter {

        private SparseArray<EntryView> mEntryViews = new SparseArray<EntryView>();

        public EntryPagerAdapter() {
        }

        @Override
        public int getCount() {
            return mEntriesIds != null ? mEntriesIds.length : 0;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            EntryView view = new EntryView(getActivity());
            mEntryViews.put(position, view);
            container.addView(view);
            view.setListener(EntryFragment.this);
            getLoaderManager().restartLoader(position, null, EntryFragment.this);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            EntryView view = (EntryView) object;
            Cursor cursor = (Cursor) view.getTag();
            if (cursor != null) {
                cursor.close();
            }
            ((ViewPager) container).removeView((View) object);
            mEntryViews.delete(position);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == ((View) object);
        }

        public void displayEntry(int pagerPos, Cursor cursor, boolean forceUpdate) {
            EntryView view = mEntryViews.get(pagerPos);
            if (view != null) {
                if (cursor == null) {
                    cursor = (Cursor) view.getTag();
                }

                if (cursor != null && cursor.moveToFirst()) {
                    String contentText = cursor.getString(mMobilizedHtmlPos);
                    if (contentText == null || (forceUpdate && !mPreferFullText)) {
                        mPreferFullText = false;
                        contentText = cursor.getString(mAbstractPos);
                    } else {
                        mPreferFullText = true;
                    }
                    if (contentText == null) {
                        contentText = "";
                    }

                    String author = cursor.getString(mAuthorPos);
                    long timestamp = cursor.getLong(mDatePos);
                    String link = cursor.getString(mLinkPos);
                    String title = cursor.getString(mTitlePos);
                    String enclosure = cursor.getString(mEnclosurePos);

                    view.setHtml(mEntriesIds[pagerPos], title, link, contentText, enclosure, author, timestamp, mPreferFullText);
                    view.setTag(cursor);
                }
            }
        }

        public Cursor getCursor(int pagerPos) {
            return (Cursor) mEntryViews.get(pagerPos).getTag();
        }

        public void onResume() {
            if (mEntriesIds != null) {
                EntryView view = mEntryViews.get(mCurrentPagerPos);
                if (view != null) {
                    view.onResume();
                }
            }
        }

        public void onPause() {
            for (int i = 0; i < mEntryViews.size(); i++) {
                mEntryViews.valueAt(i).onPause();
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        mEntryPagerAdapter = new EntryPagerAdapter();

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_entry, container, false);

        mCancelFullscreenBtn = rootView.findViewById(R.id.cancelFullscreenBtn);
        mCancelFullscreenBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleFullScreen();
            }
        });

        mEntryPager = (ViewPager) rootView.findViewById(R.id.pager);
        mEntryPager.setPageTransformer(true, new DepthPageTransformer());
        mEntryPager.setAdapter(mEntryPagerAdapter);

        if (savedInstanceState != null) {
            mBaseUri = savedInstanceState.getParcelable(STATE_BASE_URI);
            mEntriesIds = savedInstanceState.getLongArray(STATE_ENTRIES_IDS);
            mInitialEntryId = savedInstanceState.getLong(STATE_INITIAL_ENTRY_ID);
            mCurrentPagerPos = savedInstanceState.getInt(STATE_CURRENT_PAGER_POS);
            mEntryPager.getAdapter().notifyDataSetChanged();
            mEntryPager.setCurrentItem(mCurrentPagerPos);
        }

        mEntryPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {
            }

            @Override
            public void onPageSelected(int i) {
                mCurrentPagerPos = i;
                mEntryPagerAdapter.onPause(); // pause all webviews
                mEntryPagerAdapter.onResume(); // resume the current webview

                refreshUI(mEntryPagerAdapter.getCursor(i));
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(STATE_BASE_URI, mBaseUri);
        outState.putLongArray(STATE_ENTRIES_IDS, mEntriesIds);
        outState.putLong(STATE_INITIAL_ENTRY_ID, mInitialEntryId);
        outState.putInt(STATE_CURRENT_PAGER_POS, mCurrentPagerPos);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        ((BaseActivity) activity).setOnFullscreenListener(this);
    }

    @Override
    public void onDetach() {
        ((BaseActivity) getActivity()).setOnFullscreenListener(null);

        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        mEntryPagerAdapter.onResume();

        if (((BaseActivity) getActivity()).isFullScreen()) {
            mCancelFullscreenBtn.setVisibility(View.VISIBLE);
        } else {
            mCancelFullscreenBtn.setVisibility(View.GONE);
        }

        registerContentObserver();
    }

    @Override
    public void onPause() {
        super.onPause();
        mEntryPagerAdapter.onPause();

        MainApplication.getContext().getContentResolver().unregisterContentObserver(mTasksObserver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.entry, menu);

        if (mFavorite) {
            MenuItem item = menu.findItem(R.id.menu_star);
            item.setTitle(R.string.menu_unstar).setIcon(R.drawable.rating_important);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mEntriesIds != null) {
            Activity activity = getActivity();

            switch (item.getItemId()) {
                case R.id.menu_star: {
                    mFavorite = !mFavorite;

                    Uri uri = buildUri(mEntriesIds[mCurrentPagerPos]);
                    ContentValues values = new ContentValues();
                    values.put(EntryColumns.IS_FAVORITE, mFavorite ? 1 : 0);
                    ContentResolver cr = MainApplication.getContext().getContentResolver();
                    if (cr.update(uri, values, null, null) > 0) {
                        FeedDataContentProvider.notifyAllFromEntryUri(uri, true);
                    }

                    if (mFavorite) {
                        item.setTitle(R.string.menu_unstar).setIcon(R.drawable.rating_important);
                    } else {
                        item.setTitle(R.string.menu_star).setIcon(R.drawable.rating_not_important);
                    }
                    break;
                }
                case R.id.menu_share: {
                    Cursor cursor = mEntryPagerAdapter.getCursor(mCurrentPagerPos);
                    String link = cursor.getString(mLinkPos);
                    if (link != null) {
                        String title = cursor.getString(mTitlePos);
                        startActivity(Intent.createChooser(
                                new Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_SUBJECT, title).putExtra(Intent.EXTRA_TEXT, link)
                                        .setType(Constants.MIMETYPE_TEXT_PLAIN), getString(R.string.menu_share)));
                    }
                    break;
                }
                case R.id.menu_full_screen: {
                    toggleFullScreen();
                    break;
                }
                case R.id.menu_copy_clipboard: {
                    Cursor cursor = mEntryPagerAdapter.getCursor(mCurrentPagerPos);
                    String link = cursor.getString(mLinkPos);
                    ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Copied Text", link);
                    clipboard.setPrimaryClip(clip);

                    Toast.makeText(activity, R.string.copied_clipboard, Toast.LENGTH_SHORT).show();
                    break;
                }
                case R.id.menu_mark_as_unread: {
                    final Uri uri = buildUri(mEntriesIds[mCurrentPagerPos]);
                    new Thread() {
                        @Override
                        public void run() {
                            ContentResolver cr = MainApplication.getContext().getContentResolver();
                            if (cr.update(uri, FeedData.getUnreadContentValues(), null, null) > 0) {
                                FeedDataContentProvider.notifyAllFromEntryUri(uri, false);
                            }
                        }
                    }.start();
                    activity.finish();
                    break;
                }
            }
        }

        return true;
    }

    public void setData(Uri uri) {
        mBaseUri = FeedData.EntryColumns.PARENT_URI(uri.getPath());
        if (uri != null) {
            mInitialEntryId = Long.parseLong(uri.getLastPathSegment());
        } else {
            mInitialEntryId = -1;
        }

        if (mEntryPagerAdapter != null) {
            //TODO mEntryView.reset();

            if (mBaseUri != null) { // Just load the new entry
                getLoaderManager().restartLoader(-1, null, this);
            } else {
                mEntriesIds = null;
            }
        }
    }

    private void refreshUI(Cursor entryCursor) {
        if (entryCursor != null) {
            String feedTitle = entryCursor.isNull(mFeedNamePos) ? entryCursor.getString(mFeedUrlPos) : entryCursor.getString(mFeedNamePos);
            BaseActivity activity = (BaseActivity) getActivity();
            activity.setTitle(feedTitle);

            byte[] iconBytes = entryCursor.getBlob(mFeedIconPos);
            if (iconBytes != null && iconBytes.length > 0) {
                int bitmapSizeInDip = UiUtils.dpToPixel(24);
                Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
                if (bitmap != null) {
                    if (bitmap.getHeight() != bitmapSizeInDip) {
                        bitmap = Bitmap.createScaledBitmap(bitmap, bitmapSizeInDip, bitmapSizeInDip, false);
                    }

                    activity.getActionBar().setIcon(new BitmapDrawable(getResources(), bitmap));
                } else {
                    activity.getActionBar().setIcon(R.drawable.icon);
                }
            } else {
                activity.getActionBar().setIcon(R.drawable.icon);
            }

            mFavorite = entryCursor.getInt(mIsFavoritePos) == 1;
            activity.invalidateOptionsMenu();

            // Listen the mobilizing task
            registerContentObserver();

            // Mark the article as read
            if (entryCursor.getInt(mIsReadPos) != 1) {
                final Uri uri = buildUri(mEntriesIds[mCurrentPagerPos]);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ContentResolver cr = MainApplication.getContext().getContentResolver();
                        if (cr.update(uri, FeedData.getReadContentValues(), null, null) > 0) {
                            FeedDataContentProvider.notifyAllFromEntryUri(uri, false);
                        }
                    }
                }).start();
            }
        }
    }

    private void registerContentObserver() {
        if (mEntriesIds != null) {
            ContentResolver cr = MainApplication.getContext().getContentResolver();
            BaseActivity activity = (BaseActivity) getActivity();

            long mobilizingTaskId = FetcherService.getMobilizingTaskId(mEntriesIds[mCurrentPagerPos]);
            if (mobilizingTaskId != -1) {
                activity.getProgressBar().setVisibility(View.VISIBLE);
                cr.unregisterContentObserver(mTasksObserver);
                cr.registerContentObserver(TaskColumns.CONTENT_URI(mobilizingTaskId), false, mTasksObserver);
            } else {
                activity.getProgressBar().setVisibility(View.GONE);
                cr.unregisterContentObserver(mTasksObserver);
            }
        }
    }

    private void showEnclosure(Uri uri, String enclosure, int position1, int position2) {
        try {
            startActivityForResult(new Intent(Intent.ACTION_VIEW).setDataAndType(uri, enclosure.substring(position1 + 3, position2)), 0);
        } catch (Exception e) {
            try {
                startActivityForResult(new Intent(Intent.ACTION_VIEW, uri), 0); // fallbackmode - let the browser handle this
            } catch (Throwable t) {
                Toast.makeText(getActivity(), t.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void toggleFullScreen() {
        BaseActivity activity = (BaseActivity) getActivity();
        activity.toggleFullScreen();
    }

    private Uri buildUri(long entryId) {
        return mBaseUri.buildUpon().appendPath(String.valueOf(entryId)).build();
    }

    @Override
    public void onClickOriginalText() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPreferFullText = false;
                mEntryPagerAdapter.displayEntry(mCurrentPagerPos, null, true);
            }
        });
    }

    @Override
    public void onClickFullText() {
        final BaseActivity activity = (BaseActivity) getActivity();

        if (activity.getProgressBar().getVisibility() != View.VISIBLE) {
            Cursor cursor = mEntryPagerAdapter.getCursor(mCurrentPagerPos);
            final boolean alreadyMobilized = !cursor.isNull(mMobilizedHtmlPos);

            if (alreadyMobilized) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPreferFullText = true;
                        mEntryPagerAdapter.displayEntry(mCurrentPagerPos, null, true);
                    }
                });
            } else {
                ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
                final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

                // since we have acquired the networkInfo, we use it for basic checks
                if (networkInfo != null && networkInfo.getState() == NetworkInfo.State.CONNECTED) {
                    FetcherService.addEntriesToMobilize(new long[]{mEntriesIds[mCurrentPagerPos]});
                    long mobilizingTaskId = FetcherService.getMobilizingTaskId(mEntriesIds[mCurrentPagerPos]);
                    if (mobilizingTaskId != -1) {
                        ContentResolver cr = MainApplication.getContext().getContentResolver();
                        cr.unregisterContentObserver(mTasksObserver);
                        cr.registerContentObserver(FeedData.TaskColumns.CONTENT_URI(mobilizingTaskId), false, mTasksObserver);
                        activity.startService(new Intent(activity, FetcherService.class).setAction(FetcherService.ACTION_MOBILIZE_FEEDS));
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                activity.getProgressBar().setVisibility(View.VISIBLE);
                            }
                        });
                    }
                } else {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity, R.string.network_error, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }
    }

    @Override
    public void onClickEnclosure() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String enclosure = mEntryPagerAdapter.getCursor(mCurrentPagerPos).getString(mEnclosurePos);

                final int position1 = enclosure.indexOf(Constants.ENCLOSURE_SEPARATOR);
                final int position2 = enclosure.indexOf(Constants.ENCLOSURE_SEPARATOR, position1 + 3);

                Uri uri = Uri.parse(enclosure.substring(0, position1));
                showEnclosure(uri, enclosure, position1, position2);
            }
        });
    }

    @Override
    public Loader<EntryLoaderResult> onCreateLoader(int id, Bundle args) {
        return new EntryLoader(getActivity(), id == -1 ? mBaseUri : buildUri(mEntriesIds[id]), id == -1);
    }

    @Override
    public void onLoadFinished(Loader<EntryLoaderResult> loader, EntryLoaderResult result) {
        if (mBaseUri != null) { // can be null if we do a setData(null) before
            if (result.entriesIds != null) {
                mEntriesIds = result.entriesIds;
                for (int i = 0; i < mEntriesIds.length; i++) {
                    if (mEntriesIds[i] == mInitialEntryId) {
                        mCurrentPagerPos = i; // To immediately display the good entry
                        break;
                    }
                }
                mEntryPagerAdapter.notifyDataSetChanged();
                mEntryPager.setCurrentItem(mCurrentPagerPos);
            }

            if (result.entryCursor != null && mTitlePos == -1) {
                mTitlePos = result.entryCursor.getColumnIndex(EntryColumns.TITLE);
                mDatePos = result.entryCursor.getColumnIndex(EntryColumns.DATE);
                mAbstractPos = result.entryCursor.getColumnIndex(EntryColumns.ABSTRACT);
                mMobilizedHtmlPos = result.entryCursor.getColumnIndex(EntryColumns.MOBILIZED_HTML);
                mLinkPos = result.entryCursor.getColumnIndex(EntryColumns.LINK);
                mIsFavoritePos = result.entryCursor.getColumnIndex(EntryColumns.IS_FAVORITE);
                mIsReadPos = result.entryCursor.getColumnIndex(EntryColumns.IS_READ);
                mEnclosurePos = result.entryCursor.getColumnIndex(EntryColumns.ENCLOSURE);
                mAuthorPos = result.entryCursor.getColumnIndex(EntryColumns.AUTHOR);
                mFeedNamePos = result.entryCursor.getColumnIndex(FeedColumns.NAME);
                mFeedUrlPos = result.entryCursor.getColumnIndex(FeedColumns.URL);
                mFeedIconPos = result.entryCursor.getColumnIndex(FeedColumns.ICON);
            }

            int position = loader.getId();
            if (position != -1) {
                if (position == mCurrentPagerPos) {
                    refreshUI(result.entryCursor);
                }
                mEntryPagerAdapter.displayEntry(position, result.entryCursor, false);
            }

//TODO            if (result.entryCursor != null) {
//                result.entryCursor.close();
//            }
        }
    }

    @Override
    public void onLoaderReset(Loader<EntryLoaderResult> loader) {
//TODO        if (mLoaderResult != null && mLoaderResult.entryCursor != null) {
//            mLoaderResult.entryCursor.close();
//        }
    }

    @Override
    public void onFullScreenEnabled(boolean isImmersive) {
        if (!isImmersive) {
            mCancelFullscreenBtn.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onFullScreenDisabled() {
        mCancelFullscreenBtn.setVisibility(View.GONE);
    }
}

class EntryLoaderResult {
    Cursor entryCursor;
    long[] entriesIds;
}

class EntryLoader extends BaseLoader<EntryLoaderResult> {

    private final Uri mUri;
    private final boolean mIsIdsQuery;

    public EntryLoader(Context context, Uri uri, boolean isIdsQuery) {
        super(context);
        mUri = uri;
        mIsIdsQuery = isIdsQuery;
    }

    @Override
    public EntryLoaderResult loadInBackground() {
        EntryLoaderResult result = new EntryLoaderResult();
        Context context = MainApplication.getContext();
        ContentResolver cr = context.getContentResolver();

        // Get all the other entry ids (for navigation)
        if (mIsIdsQuery) {
            Cursor entriesCursor = context.getContentResolver().query(mUri, EntryColumns.PROJECTION_ID,
                    PrefUtils.getBoolean(PrefUtils.SHOW_READ, true) || EntryColumns.FAVORITES_CONTENT_URI.equals(mUri) ? null
                            : EntryColumns.WHERE_UNREAD, null, EntryColumns.DATE + Constants.DB_DESC);

            if (entriesCursor != null && entriesCursor.getCount() > 0) {
                result.entriesIds = new long[entriesCursor.getCount()];
                int i = 0;
                while (entriesCursor.moveToNext()) {
                    result.entriesIds[i++] = entriesCursor.getLong(0);
                }

                entriesCursor.close();
            }
        } else {
            // Get the entry cursor
            result.entryCursor = cr.query(mUri, null, null, null, null);

            if (result.entryCursor != null) {
                result.entryCursor.moveToFirst();
            }
        }

        return result;
    }
}

