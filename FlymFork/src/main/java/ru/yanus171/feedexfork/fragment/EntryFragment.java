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

package ru.yanus171.feedexfork.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.activity.BaseActivity;
import ru.yanus171.feedexfork.activity.EntryActivity;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns;
import ru.yanus171.feedexfork.provider.FeedData.FeedColumns;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.NetworkUtils;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.UiUtils;
import ru.yanus171.feedexfork.view.EntryView;
import ru.yanus171.feedexfork.view.StatusText;

import static ru.yanus171.feedexfork.utils.PrefUtils.DISPLAY_ENTRIES_FULLSCREEN;
import static ru.yanus171.feedexfork.utils.PrefUtils.getBoolean;


public class EntryFragment extends SwipeRefreshFragment implements LoaderManager.LoaderCallbacks<Cursor>, EntryView.EntryViewManager {

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

    private View mToggleFullscreenBtn;
    private View mToggleStatusBarVisbleBtn;
    public ProgressBar mProgressBar;
    TextView mLabelClock;

    private StatusText mStatusText = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        mEntryPagerAdapter = new EntryPagerAdapter();

        super.onCreate(savedInstanceState);
    }

    @Override
    public View inflateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_entry, container, true);

        mStatusText = new StatusText( (TextView)rootView.findViewById( R.id.statusText ),
                                      FetcherService.getObservable()/*,
                                      this*/);
        mToggleFullscreenBtn = rootView.findViewById(R.id.toggleFullscreenBtn);
        mToggleFullscreenBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EntryActivity activity = (EntryActivity) getActivity();
                activity.setFullScreen( EntryActivity.GetIsStatusBarHidden(), !EntryActivity.GetIsActionBarHidden() );
            }
        });

        mProgressBar = (ProgressBar)rootView.findViewById(R.id.progressBar);

        mLabelClock = (TextView)rootView.findViewById(R.id.textClock);

        mToggleStatusBarVisbleBtn =  rootView.findViewById(R.id.toggleFullScreenStatusBarBtn);
        mToggleStatusBarVisbleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            EntryActivity activity = (EntryActivity) getActivity();
            activity.setFullScreen(!EntryActivity.GetIsStatusBarHidden(), EntryActivity.GetIsActionBarHidden());
            }
        });

        mEntryPager = (ViewPager) rootView.findViewById(R.id.pager);
        //mEntryPager.setPageTransformer(true, new DepthPageTransformer());
        mEntryPager.setAdapter(mEntryPagerAdapter);

        if (savedInstanceState != null) {
            mBaseUri = savedInstanceState.getParcelable(STATE_BASE_URI);
            mEntriesIds = savedInstanceState.getLongArray(STATE_ENTRIES_IDS);
            mInitialEntryId = savedInstanceState.getLong(STATE_INITIAL_ENTRY_ID);
            mCurrentPagerPos = savedInstanceState.getInt(STATE_CURRENT_PAGER_POS);
            mEntryPager.getAdapter().notifyDataSetChanged();
            mEntryPager.setCurrentItem(mCurrentPagerPos);
        }

        mEntryPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {
            }

            @Override
            public void onPageSelected(int i) {
                mCurrentPagerPos = i;
                mEntryPagerAdapter.onPause(); // pause all webviews
                mEntryPagerAdapter.onResume(); // resume the current webview

                PrefUtils.putString(PrefUtils.LAST_ENTRY_URI, ContentUris.withAppendedId(mBaseUri, getCurrentEntryID()).toString());

                refreshUI(mEntryPagerAdapter.getCursor(i));
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });

        TextView.OnClickListener listener = new TextView.OnClickListener() {
            @Override
            public void onClick(View view) {
                //EntryView entryView = (EntryView) mEntryPager.findViewWithTag("EntryView" + mEntryPager.getCurrentItem());
                EntryView entryView = mEntryPagerAdapter.mEntryViews.get(mEntryPager.getCurrentItem());
                //Toast.makeText(getContext(), "pageDown onclick", Toast.LENGTH_LONG ).show();
                if (entryView != null) {
                    //Toast.makeText(getContext(), "pageDown onclick not null", Toast.LENGTH_LONG ).show();
                    entryView.pageDown(false);
                }
            }
        };

        rootView.findViewById(R.id.pageDownBtn).setOnClickListener(listener);
        rootView.findViewById(R.id.pageDownBtnVert).setOnClickListener(listener);

        rootView.findViewById(R.id.pageUpBtn).setOnClickListener(new TextView.OnClickListener() {
            @Override
            public void onClick(View view) {
                EntryView entryView = mEntryPagerAdapter.mEntryViews.get(mEntryPager.getCurrentItem());
                if (entryView != null) {
                    entryView.pageUp(false);
                }
            }
        });

        disableSwipe();

        HideButtonText(rootView, R.id.pageDownBtnVert);
        HideButtonText(rootView, R.id.pageDownBtn);
        HideButtonText(rootView, R.id.pageUpBtn);
        HideButtonText(rootView, R.id.toggleFullScreenStatusBarBtn);

        rootView.findViewById(R.id.layoutBottom).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.statusText).setVisibility(View.VISIBLE);

        return rootView;
    }

    void HideButtonText(View rootView, int ID) {
        TextView btn = (TextView)rootView.findViewById(ID);
        btn.setBackgroundColor(Color.TRANSPARENT);
        btn.setText("");
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

        ( (EntryActivity) getActivity() ).setFullScreen();
    }

    @Override
    public void onDetach() {
        ( (EntryActivity) getActivity() ).setFullScreen();

        super.onDetach();
    }

    @Override
    public void onDestroy() {
        FetcherService.getObservable().deleteObserver(mStatusText);
        super.onDestroy();
    }
    @Override
    public void onResume() {
        super.onResume();
        mEntryPagerAdapter.onResume();

//        if (((BaseActivity) getActivity()).isFullScreen()) {
//            mToggleFullscreenBtn.setVisibility(View.VISIBLE);
//        } else {
//            mToggleFullscreenBtn.setVisibility(View.GONE);
//        }


    }

    @Override
    public void onPause() {
        super.onPause();
        EntryView entryView = mEntryPagerAdapter.mEntryViews.get(mEntryPager.getCurrentItem());
        if (entryView != null) {
            PrefUtils.putInt(PrefUtils.LAST_ENTRY_SCROLL_Y, entryView.getScrollY());
            PrefUtils.putLong(PrefUtils.LAST_ENTRY_ID, getCurrentEntryID());

        }
        mEntryPagerAdapter.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.entry, menu);

        if (mFavorite) {
            MenuItem item = menu.findItem(R.id.menu_star);
            item.setTitle(R.string.menu_unstar).setIcon(R.drawable.rating_important);
        }

        menu.findItem(R.id.menu_mark_as_favorite).setVisible( !mFavorite );
        menu.findItem(R.id.menu_mark_as_unfavorite).setVisible(mFavorite);

            super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mEntriesIds != null) {
            Activity activity = getActivity();

            switch (item.getItemId()) {
                case R.id.menu_star: {
                    mFavorite = !mFavorite;

                    if (mFavorite) {
                        item.setTitle(R.string.menu_unstar).setIcon(R.drawable.rating_important);
                    } else {
                        item.setTitle(R.string.menu_star).setIcon(R.drawable.rating_not_important);
                    }

                    final Uri uri = ContentUris.withAppendedId(mBaseUri, getCurrentEntryID());
                    new Thread() {
                        @Override
                        public void run() {
                            ContentValues values = new ContentValues();
                            values.put(EntryColumns.IS_FAVORITE, mFavorite ? 1 : 0);
                            ContentResolver cr = MainApplication.getContext().getContentResolver();
                            cr.update(uri, values, null, null);

                            // Update the cursor
                            Cursor updatedCursor = cr.query(uri, null, null, null, null);
                            updatedCursor.moveToFirst();
                            mEntryPagerAdapter.setUpdatedCursor(mCurrentPagerPos, updatedCursor);
                        }
                    }.start();
                    break;
                }
                case R.id.menu_share: {
                    Cursor cursor = mEntryPagerAdapter.getCursor(mCurrentPagerPos);
                    if (cursor != null) {
                        String link = cursor.getString(mLinkPos);
                        if (link != null) {
                            String title = cursor.getString(mTitlePos);
                            startActivity(Intent.createChooser(
                                    new Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_SUBJECT, title).putExtra(Intent.EXTRA_TEXT, link)
                                            .setType(Constants.MIMETYPE_TEXT_PLAIN), getString(R.string.menu_share)
                            ));
                        }
                    }
                    break;
                }
                case R.id.menu_full_screen: {
                    EntryActivity activity1 = (EntryActivity) getActivity();
                    activity1.setFullScreen( true, true );
                    break;
                }

                case R.id.menu_copy_clipboard: {
                    Cursor cursor = mEntryPagerAdapter.getCursor(mCurrentPagerPos);
                    String link = cursor.getString(mLinkPos);
                    ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Copied Text 1", link);
                    clipboard.setPrimaryClip(clip);

                    UiUtils.showMessage(getActivity(), R.string.copied_clipboard);
                    break;
                }
                case R.id.menu_mark_as_unread: {
                    final Uri uri = ContentUris.withAppendedId(mBaseUri, getCurrentEntryID());
                    new Thread() {
                        @Override
                        public void run() {
                            ContentResolver cr = MainApplication.getContext().getContentResolver();
                            cr.update(uri, FeedData.getUnreadContentValues(), null, null);
                        }
                    }.start();
                    CloseEntry();
                    break;
                }
                case R.id.menu_mark_as_favorite: {
                    final Uri uri = ContentUris.withAppendedId(mBaseUri, getCurrentEntryID());
                    new Thread() {
                        @Override
                        public void run() {
                            ContentResolver cr = MainApplication.getContext().getContentResolver();
                            cr.update(uri, FeedData.getFavoriteContentValues(true), null, null);
                            //cr.update(uri, FeedData.getUnreadContentValues(), null, null);
                        }
                    }.start();
                    //activity.finish();
                    break;
                }
                case R.id.menu_mark_as_unfavorite: {
                    final Uri uri = ContentUris.withAppendedId(mBaseUri, getCurrentEntryID());
                    new Thread() {
                        @Override
                        public void run() {
                            ContentResolver cr = MainApplication.getContext().getContentResolver();
                            cr.update(uri, FeedData.getFavoriteContentValues(false), null, null);
                        }
                    }.start();

                    CloseEntry();

                    break;
                }
                case R.id.menu_font_bold: {
                    PrefUtils.putBoolean(PrefUtils.ENTRY_FONT_BOLD,
                            !PrefUtils.getBoolean(PrefUtils.ENTRY_FONT_BOLD, false));
                    mEntryPagerAdapter.displayEntry(mCurrentPagerPos, null, true);
                    break;
                }
                case R.id.menu_load_all_images: {
                    FetcherService.mMaxImageDownloadCount = 0;
                    mEntryPagerAdapter.displayEntry(mCurrentPagerPos, null, true);
                    break;
                }
                case R.id.menu_cancel_refresh: {
                    FetcherService.cancelRefresh();
                    break;
                }
                case R.id.menu_reload_full_text: {

                    int status = FetcherService.getObservable().Start("Reload fulltext"); try {

                        /*final Uri uri = ContentUris.withAppendedId(mBaseUri, mEntriesIds[mCurrentPagerPos]);
                        new Thread() {
                            @Override
                            public void run() {

                            }
                        }.start();*/


                        ContentValues values = new ContentValues();
                        values.putNull(EntryColumns.MOBILIZED_HTML);
                        ContentResolver cr = MainApplication.getContext().getContentResolver();
                        final Uri uri = ContentUris.withAppendedId(mBaseUri, getCurrentEntryID());
                        cr.update(uri, values, null, null);

                        //if (!isRefreshing()) {
                            LoadFullText();
                        //}

                    } finally { FetcherService.getObservable().End( status ); }
                    break;
                }
            }
        }

        return true;
    }

    void CloseEntry() {
        PrefUtils.putLong(PrefUtils.LAST_ENTRY_ID, 0);
        PrefUtils.putString(PrefUtils.LAST_ENTRY_URI, "");
        getActivity().finish();
    }

    public long getCurrentEntryID() {
        if ( mCurrentPagerPos >= 0 && mCurrentPagerPos < mEntriesIds.length )
            return mEntriesIds[mCurrentPagerPos];
        else
            return -1;
    }

    public void setData(Uri uri) {
        mCurrentPagerPos = -1;

        //PrefUtils.putString( PrefUtils.LAST_URI, uri.toString() );

        mBaseUri = FeedData.EntryColumns.PARENT_URI(uri.getPath());
        try {
            mInitialEntryId = Long.parseLong(uri.getLastPathSegment());
        } catch (Exception unused) {
            mInitialEntryId = -1;
        }

        if (mBaseUri != null) {
            String entriesOrder = PrefUtils.getBoolean(PrefUtils.DISPLAY_OLDEST_FIRST, false) ? Constants.DB_ASC : Constants.DB_DESC;

            // Load the entriesIds list. Should be in a loader... but I was too lazy to do so
            Cursor entriesCursor = MainApplication.getContext().getContentResolver().query(mBaseUri, EntryColumns.PROJECTION_ID,
                    null, null, EntryColumns.DATE + entriesOrder);

            if (entriesCursor != null && entriesCursor.getCount() > 0) {
                mEntriesIds = new long[entriesCursor.getCount()];
                int i = 0;
                while (entriesCursor.moveToNext()) {
                    mEntriesIds[i] = entriesCursor.getLong(0);
                    if (mEntriesIds[i] == mInitialEntryId) {
                        mCurrentPagerPos = i; // To immediately display the good entry
                    }
                    i++;
                }

                entriesCursor.close();
            }
        } else {
            mEntriesIds = null;
        }

        mEntryPagerAdapter.notifyDataSetChanged();
        if (mCurrentPagerPos != -1) {
            mEntryPager.setCurrentItem(mCurrentPagerPos);
        }
    }

    private void refreshUI(Cursor entryCursor) {
        if (entryCursor != null) {
            String feedTitle = entryCursor.isNull(mFeedNamePos) ? entryCursor.getString(mFeedUrlPos) : entryCursor.getString(mFeedNamePos);
            EntryActivity activity = (EntryActivity) getActivity();
            activity.setTitle("");//activity.setTitle(feedTitle);

            mFavorite = entryCursor.getInt(mIsFavoritePos) == 1;
            activity.invalidateOptionsMenu();

            // Listen the mobilizing task

            if (FetcherService.hasMobilizationTask(getCurrentEntryID())) {
                //--showSwipeProgress();

                // If the service is not started, start it here to avoid an infinite loading
                if (!PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false)) {
                    MainApplication.getContext().startService(new Intent(MainApplication.getContext(),
                                                                         FetcherService.class)
                                                              .setAction(FetcherService.ACTION_MOBILIZE_FEEDS));
                }
            } else {
                //--hideSwipeProgress();
            }
            refreshSwipeProgress();

            // Mark the article as read
            if (entryCursor.getInt(mIsReadPos) != 1) {
                final Uri uri = ContentUris.withAppendedId(mBaseUri, getCurrentEntryID());
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ContentResolver cr = MainApplication.getContext().getContentResolver();
                        cr.update(uri, FeedData.getReadContentValues(), null, null);

                        // Update the cursor
                        Cursor updatedCursor = cr.query(uri, null, null, null, null);
                        updatedCursor.moveToFirst();
                        mEntryPagerAdapter.setUpdatedCursor(mCurrentPagerPos, updatedCursor);
                    }
                }).start();
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
                UiUtils.showMessage(getActivity(), t.getMessage());
            }
        }
    }

    /*private void setImmersiveFullScreen(boolean fullScreen) {
        BaseActivity activity = (BaseActivity) getActivity();
        if ( fullScreen )
            mToggleStatusBarVisbleBtn.setVisibility(View.VISIBLE);
        else
            mToggleStatusBarVisbleBtn.setVisibility(View.GONE);
        activity.setImmersiveFullScreen(fullScreen);
    }*/

    public void UpdateClock() {
        mLabelClock.setVisibility( ( ( EntryActivity ) getActivity() ).GetIsStatusBarHidden() ? View.VISIBLE : View.GONE );
        mLabelClock.setText( new SimpleDateFormat("HH:mm").format(new Date()) );
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
        } else /*--if (!isRefreshing())*/ {
            LoadFullText();
        }
    }

    private void LoadFullText() {
        final BaseActivity activity = (BaseActivity) getActivity();
        ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        // since we have acquired the networkInfo, we use it for basic checks
        if (networkInfo != null && networkInfo.getState() == NetworkInfo.State.CONNECTED) {
            //FetcherService.addEntriesToMobilize(new Long[]{mEntriesIds[mCurrentPagerPos]});
            //activity.startService(new Intent(activity, FetcherService.class)
            //                      .setAction(FetcherService.ACTION_MOBILIZE_FEEDS));
            new Thread() {
                @Override
                public void run() {
                    int status = FetcherService.getObservable().Start(getActivity().getString(R.string.loadFullText));
                    FetcherService.mobilizeEntry(getContext().getContentResolver(), getCurrentEntryID());
                    FetcherService.getObservable().End( status );
                }
            }.start();



            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshSwipeProgress();
                }
            });
        } else {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    UiUtils.showMessage(getActivity(), R.string.network_error);
                }
            });
        }
    }

    @Override
    public void onClickEnclosure() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final String enclosure = mEntryPagerAdapter.getCursor(mCurrentPagerPos).getString(mEnclosurePos);

                final int position1 = enclosure.indexOf(Constants.ENCLOSURE_SEPARATOR);
                final int position2 = enclosure.indexOf(Constants.ENCLOSURE_SEPARATOR, position1 + 3);

                final Uri uri = Uri.parse(enclosure.substring(0, position1));
                final String filename = uri.getLastPathSegment();

                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.open_enclosure)
                        .setMessage(getString(R.string.file) + ": " + filename)
                        .setPositiveButton(R.string.open_link, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                showEnclosure(uri, enclosure, position1, position2);
                            }
                        }).setNegativeButton(R.string.download_and_save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            DownloadManager.Request r = new DownloadManager.Request(uri);
                            r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                            r.allowScanningByMediaScanner();
                            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                            DownloadManager dm = (DownloadManager) MainApplication.getContext().getSystemService(Context.DOWNLOAD_SERVICE);
                            dm.enqueue(r);
                        } catch (Exception e) {
                            UiUtils.showMessage(getActivity(), R.string.error);
                        }
                    }
                }).show();
            }
        });
    }

    @Override
    public void onStartVideoFullScreen() {
        BaseActivity activity = (BaseActivity) getActivity();
        //activity.setNormalFullScreen(true);
    }

    @Override
    public void onEndVideoFullScreen() {
        BaseActivity activity = (BaseActivity) getActivity();
        //activity.setNormalFullScreen(false);
    }

    @Override
    public FrameLayout getVideoLayout() {
        View layout = getView();
        return (layout == null ? null : (FrameLayout) layout.findViewById(R.id.videoLayout));
    }

    @Override
    public void downloadImage(final String url) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        FetcherService.mCancelRefresh = false;
                        int status = FetcherService.getObservable().Start( getString(R.string.downloadImage) );
                        NetworkUtils.downloadImage(getCurrentEntryID(), url/*, true*/ );
                        FetcherService.getObservable().End( status );
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }).start();
            //mEntryPagerAdapter.displayEntry(mCurrentPagerPos, null, true);
    }

    @Override
    public void downloadNextImages() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                FetcherService.mMaxImageDownloadCount += PrefUtils.getImageDownloadCount();
                mEntryPagerAdapter.displayEntry(mCurrentPagerPos, null, true);
            }
        });

    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader cursorLoader = new CursorLoader(getActivity(), EntryColumns.CONTENT_URI(mEntriesIds[id]), null, null, null, null);
        cursorLoader.setUpdateThrottle(100);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (mBaseUri != null && cursor != null) { // can be null if we do a setData(null) before
            cursor.moveToFirst();

            if (mTitlePos == -1) {
                mTitlePos = cursor.getColumnIndex(EntryColumns.TITLE);
                mDatePos = cursor.getColumnIndex(EntryColumns.DATE);
                mAbstractPos = cursor.getColumnIndex(EntryColumns.ABSTRACT);
                mMobilizedHtmlPos = cursor.getColumnIndex(EntryColumns.MOBILIZED_HTML);
                mLinkPos = cursor.getColumnIndex(EntryColumns.LINK);
                mIsFavoritePos = cursor.getColumnIndex(EntryColumns.IS_FAVORITE);
                mIsReadPos = cursor.getColumnIndex(EntryColumns.IS_READ);
                mEnclosurePos = cursor.getColumnIndex(EntryColumns.ENCLOSURE);
                mAuthorPos = cursor.getColumnIndex(EntryColumns.AUTHOR);
                mFeedNamePos = cursor.getColumnIndex(FeedColumns.NAME);
                mFeedUrlPos = cursor.getColumnIndex(FeedColumns.URL);
                mFeedIconPos = cursor.getColumnIndex(FeedColumns.ICON);
            }

            int position = loader.getId();
            if (position != -1) {
                FetcherService.mMaxImageDownloadCount = PrefUtils.getImageDownloadCount();
                mEntryPagerAdapter.displayEntry(position, cursor, false);

                EntryActivity activity = (EntryActivity) getActivity();
                if (getBoolean(DISPLAY_ENTRIES_FULLSCREEN, false))
                    activity.setFullScreen(true, true);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mEntryPagerAdapter.setUpdatedCursor(loader.getId(), null);
    }



    @Override
    public void onRefresh() {
        // Nothing to do
    }

    public class EntryPagerAdapter extends PagerAdapter {

        private final SparseArray<EntryView> mEntryViews = new SparseArray<>();

        public EntryPagerAdapter() {
        }

        @Override
        public int getCount() {
            return mEntriesIds != null ? mEntriesIds.length : 0;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            getLoaderManager().destroyLoader(position);
            container.removeView((View) object);
            EntryView.mImageDownloadObservable.deleteObserver(mEntryViews.get(position));
            mEntryViews.delete(position);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            final EntryView view = new EntryView(getActivity());
            mEntryViews.put(position, view);
            container.addView(view);
            view.setListener(EntryFragment.this);
            getLoaderManager().restartLoader(position, null, EntryFragment.this);
            view.setTag("EntryView" + position);

            /*view.setOnScrollChangeListener( new View.OnScrollChangeListener() {
                @Override
                public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                    mProgressBar.setProgress( scrollY );
                }

            });*/
            //mProgressBar.setMax( view.getContentHeight() );
            //mProgressBar.setProgress( view.getScrollY() );

            return view;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        public void displayEntry(int pagerPos, Cursor newCursor, boolean forceUpdate) {
            EntryView view = mEntryViews.get(pagerPos);
            if (view != null) {
                if (newCursor == null) {
                    newCursor = (Cursor) view.getTag(); // get the old one
                }

                if (newCursor != null && newCursor.moveToFirst()) {
                    String contentText = newCursor.getString(mMobilizedHtmlPos);
                    if (contentText == null || (forceUpdate && !mPreferFullText)) {
                        mPreferFullText = false;
                        contentText = newCursor.getString(mAbstractPos);
                    } else {
                        mPreferFullText = true;
                    }
                    if (contentText == null) {
                        contentText = "";
                    }

                    String author = newCursor.getString(mAuthorPos);
                    long timestamp = newCursor.getLong(mDatePos);
                    String link = newCursor.getString(mLinkPos);
                    String title = newCursor.getString(mTitlePos);
                    String enclosure = newCursor.getString(mEnclosurePos);

                    FetcherService.setCurrentEntryID( getCurrentEntryID() );
                    view.setHtml(mEntriesIds[pagerPos],
                                 title,
                                 link,
                                    contentText,
                                 enclosure,
                                 author,
                                 timestamp,
                                 mPreferFullText,
                                 ( EntryActivity )getActivity() );
                    view.setTag(newCursor);

                    if (pagerPos == mCurrentPagerPos) {
                        refreshUI(newCursor);

                        //PrefUtils.putString(PrefUtils.LAST_URI, uri.toString());

                        if ( PrefUtils.getLong( PrefUtils.LAST_ENTRY_ID, 0 ) == mEntriesIds[pagerPos] ) {
                            int dy = PrefUtils.getInt( PrefUtils.LAST_ENTRY_SCROLL_Y, 0 );
                            if ( dy > view.getScrollY() )
                                view.mScrollY = dy;
                        }

                    }

                    UpdateProgress();
                    UpdateClock();
                }
            }
        }

        public Cursor getCursor(int pagerPos) {
            EntryView view = mEntryViews.get(pagerPos);
            if (view != null) {
                return (Cursor) view.getTag();
            }
            return null;
        }

        public void setUpdatedCursor(int pagerPos, Cursor newCursor) {
            EntryView view = mEntryViews.get(pagerPos);
            if (view != null) {
                Cursor previousUpdatedOne = (Cursor) view.getTag(R.id.updated_cursor);
                if (previousUpdatedOne != null) {
                    previousUpdatedOne.close();
                }
                view.setTag(newCursor);
                view.setTag(R.id.updated_cursor, newCursor);
            }
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

    public void UpdateProgress() {
        EntryView entryView = mEntryPagerAdapter.mEntryViews.get(mEntryPager.getCurrentItem());
        if (entryView != null) {
            ProgressBar progressBar = mProgressBar;
            int webViewHeight = entryView.getMeasuredHeight();
            int height = (int) Math.floor(entryView.getContentHeight() * entryView.getScale());
            progressBar.setMax(height - webViewHeight);
            progressBar.setProgress(entryView.getScrollY());
        }
    }

}

