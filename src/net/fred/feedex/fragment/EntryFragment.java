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
import net.fred.feedex.view.EntryView;

public class EntryFragment extends Fragment implements BaseActivity.OnFullScreenListener, LoaderManager.LoaderCallbacks<EntryLoaderResult>, EntryView.OnActionListener {

    private static final int LOADER_ID = 2;

    private static final String STATE_URI = "STATE_URI";
    private static final String STATE_ENTRIES_IDS = "STATE_ENTRIES_IDS";

    private int mTitlePosition = -1, mDatePosition, mMobilizedHtmlPosition, mAbstractPosition, mLinkPosition, mIsFavoritePosition,
            mEnclosurePosition, mAuthorPosition;

    private long mId = -1;
    private long mNextId = -1;
    private long mPreviousId = -1;
    private long[] mEntriesIds;
    private EntryLoaderResult mLoaderResult;

    private Uri mUri;
    private boolean mFavorite, mPreferFullText = true;

    private EntryView mEntryView;

    private String mLink, mTitle, mEnclosure;
    private View mCancelFullscreenBtn, mBackBtn, mForwardBtn;

    private final ThrottledContentObserver mTasksObserver = new ThrottledContentObserver(new Handler(), 2000) {
        @Override
        public void onChangeThrottled() {
            BaseActivity activity = (BaseActivity) getActivity();
            boolean isMobilizing = FetcherService.getMobilizingTaskId(mId) != -1;
            if (activity == null || (activity.getProgressBar().getVisibility() == View.VISIBLE && isMobilizing)
                    || (activity.getProgressBar().getVisibility() == View.GONE && !isMobilizing)) {
                return; // no change => no update
            }

            if (isMobilizing) { // We start a mobilization
                activity.getProgressBar().setVisibility(View.VISIBLE);
            } else { // We finished one
                mPreferFullText = true;
                getLoaderManager().restartLoader(LOADER_ID, null, EntryFragment.this);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);

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
        mBackBtn = rootView.findViewById(R.id.backBtn);
        mBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mEntryView.previousEntry();
            }
        });
        mForwardBtn = rootView.findViewById(R.id.forwardBtn);
        mForwardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mEntryView.nextEntry();
            }
        });

        mEntryView = (EntryView) rootView.findViewById(R.id.entry);
        mEntryView.setListener(this);

        if (savedInstanceState != null) {
            mEntriesIds = savedInstanceState.getLongArray(STATE_ENTRIES_IDS);
            setData((Uri) savedInstanceState.getParcelable(STATE_URI));
        }

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(STATE_URI, mUri);
        outState.putLongArray(STATE_ENTRIES_IDS, mEntriesIds);

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
        mEntryView.onResume();

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
        mEntryView.onPause();

        MainApplication.getContext().getContentResolver().unregisterContentObserver(mTasksObserver);
    }

    @Override
    public void onDestroyView() {
        mEntryView.onDestroy();
        super.onDestroyView();
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
        Activity activity = getActivity();

        switch (item.getItemId()) {
            case R.id.menu_star:
                mFavorite = !mFavorite;

                ContentValues values = new ContentValues();
                values.put(EntryColumns.IS_FAVORITE, mFavorite ? 1 : 0);
                ContentResolver cr = MainApplication.getContext().getContentResolver();
                if (cr.update(mUri, values, null, null) > 0) {
                    FeedDataContentProvider.notifyAllFromEntryUri(mUri, true);
                }

                if (mFavorite) {
                    item.setTitle(R.string.menu_unstar).setIcon(R.drawable.rating_important);
                } else {
                    item.setTitle(R.string.menu_star).setIcon(R.drawable.rating_not_important);
                }
                break;
            case R.id.menu_share:
                if (mLink != null) {
                    startActivity(Intent.createChooser(
                            new Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_SUBJECT, mTitle).putExtra(Intent.EXTRA_TEXT, mLink)
                                    .setType(Constants.MIMETYPE_TEXT_PLAIN), getString(R.string.menu_share)));
                }
                break;
            case R.id.menu_full_screen: {
                toggleFullScreen();
                break;
            }
            case R.id.menu_copy_clipboard: {
                ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Copied Text", mLink);
                clipboard.setPrimaryClip(clip);

                Toast.makeText(activity, R.string.copied_clipboard, Toast.LENGTH_SHORT).show();
                break;
            }
            case R.id.menu_mark_as_unread:
                new Thread() {
                    @Override
                    public void run() {
                        ContentResolver cr = MainApplication.getContext().getContentResolver();
                        if (cr.update(mUri, FeedData.getUnreadContentValues(), null, null) > 0) {
                            FeedDataContentProvider.notifyAllFromEntryUri(mUri, false);
                        }
                    }
                }.start();
                activity.finish();
                break;
        }

        return true;
    }

    public Uri getUri() {
        return mUri;
    }

    public long getEntryId() {
        return mId;
    }

    public void setData(Uri uri) {
        mUri = uri;
        if (mUri != null) {
            mId = Long.parseLong(mUri.getLastPathSegment());
        } else {
            mId = -1;
        }

        if (mEntryView != null) {
            mEntryView.reset();

            if (mUri != null) { // Just load the new entry
                getLoaderManager().restartLoader(LOADER_ID, null, this);
            } else {
                mEntriesIds = null;
                setupNavigationButton();
            }
        }
    }

    private void registerContentObserver() {
        ContentResolver cr = MainApplication.getContext().getContentResolver();
        BaseActivity activity = (BaseActivity) getActivity();

        long mobilizingTaskId = FetcherService.getMobilizingTaskId(mId);
        if (mobilizingTaskId != -1) {
            activity.getProgressBar().setVisibility(View.VISIBLE);
            cr.unregisterContentObserver(mTasksObserver);
            cr.registerContentObserver(TaskColumns.CONTENT_URI(mobilizingTaskId), false, mTasksObserver);
        } else {
            activity.getProgressBar().setVisibility(View.GONE);
            cr.unregisterContentObserver(mTasksObserver);
        }
    }

    private void reload(boolean forceUpdate) {
        if (mLoaderResult.entryCursor != null && mLoaderResult.entryCursor.moveToFirst()) {
            String contentText = mLoaderResult.entryCursor.getString(mMobilizedHtmlPosition);
            if (contentText == null || (forceUpdate && !mPreferFullText)) {
                mPreferFullText = false;
                contentText = mLoaderResult.entryCursor.getString(mAbstractPosition);
            } else {
                mPreferFullText = true;
            }
            if (contentText == null) {
                contentText = "";
            }

            // Need to be done before the "mark as read" action
            setupNavigationButton();

            BaseActivity activity = (BaseActivity) getActivity();
            activity.setTitle(mLoaderResult.feedTitle);

            if (mLoaderResult.iconBytes != null && mLoaderResult.iconBytes.length > 0) {
                int bitmapSizeInDip = UiUtils.dpToPixel(24);
                Bitmap bitmap = BitmapFactory.decodeByteArray(mLoaderResult.iconBytes, 0, mLoaderResult.iconBytes.length);
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

            mFavorite = mLoaderResult.entryCursor.getInt(mIsFavoritePosition) == 1;
            activity.invalidateOptionsMenu();

            String author = mLoaderResult.entryCursor.getString(mAuthorPosition);
            long timestamp = mLoaderResult.entryCursor.getLong(mDatePosition);
            mLink = mLoaderResult.entryCursor.getString(mLinkPosition);
            mTitle = mLoaderResult.entryCursor.getString(mTitlePosition);
            mEnclosure = mLoaderResult.entryCursor.getString(mEnclosurePosition);

            mEntryView.setHtml(mId, mNextId, mPreviousId, mTitle, mLink, contentText, mEnclosure, author, timestamp, mPreferFullText);

            // Listen the mobilizing task
            registerContentObserver();
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

    private void setupNavigationButton() {
        mPreviousId = -1;
        mBackBtn.setVisibility(View.GONE);
        mNextId = -1;
        mForwardBtn.setVisibility(View.GONE);

        if (mEntriesIds != null) {
            for (int i = 0; i < mEntriesIds.length; ++i) {
                if (mId == mEntriesIds[i]) {
                    if (i > 0) {
                        mPreviousId = mEntriesIds[i - 1];
                        mBackBtn.setVisibility(View.VISIBLE);
                    }

                    if (i < mEntriesIds.length - 1) {
                        mNextId = mEntriesIds[i + 1];
                        mForwardBtn.setVisibility(View.VISIBLE);
                    }

                    break;
                }
            }
        }
    }

    private void toggleFullScreen() {
        BaseActivity activity = (BaseActivity) getActivity();
        activity.toggleFullScreen();
    }

    @Override
    public void onEntrySwitched(long newEntryId) {
        setData(FeedData.EntryColumns.PARENT_URI(mUri.getPath()).buildUpon().appendPath(String.valueOf(newEntryId)).build());
    }

    @Override
    public void onClickOriginalText() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPreferFullText = false;
                reload(true);
            }
        });
    }

    @Override
    public void onClickFullText() {
        final BaseActivity activity = (BaseActivity) getActivity();

        if (activity.getProgressBar().getVisibility() != View.VISIBLE) {
            ContentResolver cr = MainApplication.getContext().getContentResolver();
            Cursor entryCursor = cr.query(mUri, null, null, null, null);
            final boolean alreadyMobilized = entryCursor.moveToFirst() && !entryCursor.isNull(mMobilizedHtmlPosition);
            entryCursor.close();

            if (alreadyMobilized) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPreferFullText = true;
                        reload(true);
                    }
                });
            } else {
                ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
                final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

                // since we have acquired the networkInfo, we use it for basic checks
                if (networkInfo != null && networkInfo.getState() == NetworkInfo.State.CONNECTED) {
                    FetcherService.addEntriesToMobilize(new long[]{mId});
                    long mobilizingTaskId = FetcherService.getMobilizingTaskId(mId);
                    if (mobilizingTaskId != -1) {
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
                final int position1 = mEnclosure.indexOf(Constants.ENCLOSURE_SEPARATOR);
                final int position2 = mEnclosure.indexOf(Constants.ENCLOSURE_SEPARATOR, position1 + 3);

                Uri uri = Uri.parse(mEnclosure.substring(0, position1));
                showEnclosure(uri, mEnclosure, position1, position2);
            }
        });
    }

    @Override
    public Loader<EntryLoaderResult> onCreateLoader(int id, Bundle args) {
        return new EntryLoader(getActivity(), mUri, mEntriesIds == null);
    }

    @Override
    public void onLoadFinished(Loader<EntryLoaderResult> loader, EntryLoaderResult result) {
        if (mLoaderResult != null && mLoaderResult.entryCursor != null) {
            mLoaderResult.entryCursor.close();
        }

        if (mUri != null) { // can be null if we do a setData(null) before
            mLoaderResult = result;
            if (mLoaderResult.entriesIds != null) {
                mEntriesIds = mLoaderResult.entriesIds;
            }

            if (mLoaderResult.entryCursor != null && mTitlePosition == -1) {
                mTitlePosition = mLoaderResult.entryCursor.getColumnIndex(EntryColumns.TITLE);
                mDatePosition = mLoaderResult.entryCursor.getColumnIndex(EntryColumns.DATE);
                mAbstractPosition = mLoaderResult.entryCursor.getColumnIndex(EntryColumns.ABSTRACT);
                mMobilizedHtmlPosition = mLoaderResult.entryCursor.getColumnIndex(EntryColumns.MOBILIZED_HTML);
                mLinkPosition = mLoaderResult.entryCursor.getColumnIndex(EntryColumns.LINK);
                mIsFavoritePosition = mLoaderResult.entryCursor.getColumnIndex(EntryColumns.IS_FAVORITE);
                mEnclosurePosition = mLoaderResult.entryCursor.getColumnIndex(EntryColumns.ENCLOSURE);
                mAuthorPosition = mLoaderResult.entryCursor.getColumnIndex(EntryColumns.AUTHOR);
            }

            reload(false);
        }
    }

    @Override
    public void onLoaderReset(Loader<EntryLoaderResult> loader) {
        if (mLoaderResult != null && mLoaderResult.entryCursor != null) {
            mLoaderResult.entryCursor.close();
        }
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
    long feedId;
    long[] entriesIds;
    String feedTitle;
    byte[] iconBytes;
}

class EntryLoader extends BaseLoader<EntryLoaderResult> {

    private final Uri mUri;
    private final boolean mNeedEntriesIds;

    public EntryLoader(Context context, Uri uri, boolean needEntriesIds) {
        super(context);
        mUri = uri;
        mNeedEntriesIds = needEntriesIds;
    }

    @Override
    public EntryLoaderResult loadInBackground() {
        EntryLoaderResult result = new EntryLoaderResult();

        // Get the entry cursor
        Context context = MainApplication.getContext();
        ContentResolver cr = context.getContentResolver();
        result.entryCursor = cr.query(mUri, null, null, null, null);

        if (result.entryCursor != null) {
            result.entryCursor.moveToFirst();

            // Get all the other entry ids (for navigation)
            if (mNeedEntriesIds) {
                Uri parentUri = EntryColumns.PARENT_URI(mUri.getPath());
                Cursor entriesCursor = context.getContentResolver().query(parentUri, EntryColumns.PROJECTION_ID,
                        PrefUtils.getBoolean(PrefUtils.SHOW_READ, true) || EntryColumns.FAVORITES_CONTENT_URI.equals(parentUri) ? null
                                : EntryColumns.WHERE_UNREAD, null, EntryColumns.DATE + Constants.DB_DESC);

                if (entriesCursor != null && entriesCursor.getCount() > 0) {
                    result.entriesIds = new long[entriesCursor.getCount()];
                    int i = 0;
                    while (entriesCursor.moveToNext()) {
                        result.entriesIds[i++] = entriesCursor.getLong(0);
                    }

                    entriesCursor.close();
                }
            }

            // Mark the article as read
            if (result.entryCursor.getInt(result.entryCursor.getColumnIndex(EntryColumns.IS_READ)) != 1) {
                if (cr.update(mUri, FeedData.getReadContentValues(), null, null) > 0) {
                    FeedDataContentProvider.notifyAllFromEntryUri(mUri, false);
                }
            }

            // Get the feedId & title & icon
            result.feedId = result.entryCursor.getInt(result.entryCursor.getColumnIndex(EntryColumns.FEED_ID));
            Cursor feedCursor = cr.query(FeedColumns.CONTENT_URI(result.feedId), new String[]{FeedColumns.NAME, FeedColumns.URL, FeedColumns.ICON}, null, null, null);
            if (feedCursor.moveToFirst()) {
                result.feedTitle = feedCursor.isNull(0) ? feedCursor.getString(1) : feedCursor.getString(0);
                result.iconBytes = feedCursor.getBlob(2);
            }
            feedCursor.close();
        }

        return result;
    }
}

