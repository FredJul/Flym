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
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
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
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.activity.BaseActivity;
import ru.yanus171.feedexfork.activity.EntryActivity;
import ru.yanus171.feedexfork.adapter.DrawerAdapter;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns;
import ru.yanus171.feedexfork.provider.FeedData.FeedColumns;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.ArticleTextExtractor;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.NetworkUtils;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.UiUtils;
import ru.yanus171.feedexfork.view.EntryView;
import ru.yanus171.feedexfork.view.StatusText;

import static ru.yanus171.feedexfork.utils.PrefUtils.DISPLAY_ENTRIES_FULLSCREEN;
import static ru.yanus171.feedexfork.utils.PrefUtils.getBoolean;


public class EntryFragment extends /*SwipeRefresh*/Fragment implements LoaderManager.LoaderCallbacks<Cursor>, EntryView.EntryViewManager {

    private static final String STATE_BASE_URI = "STATE_BASE_URI";
    private static final String STATE_CURRENT_PAGER_POS = "STATE_CURRENT_PAGER_POS";
    private static final String STATE_ENTRIES_IDS = "STATE_ENTRIES_IDS";
    private static final String STATE_INITIAL_ENTRY_ID = "STATE_INITIAL_ENTRY_ID";

    private int mTitlePos = -1, mDatePos, mMobilizedHtmlPos, mAbstractPos, mLinkPos, mIsFavoritePos, mIsReadPos, mEnclosurePos, mAuthorPos, mFeedNamePos, mFeedUrlPos, mFeedIconPos, mScrollPosPos;

    private int mCurrentPagerPos = -1, mLastPagerPos = -1;
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

    public boolean mMarkAsUnreadOnFinish = false;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        mEntryPagerAdapter = new EntryPagerAdapter();


        super.onCreate(savedInstanceState);
    }

    //@Override
    //public View inflateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_entry, container, true);

        mStatusText = new StatusText( (TextView)rootView.findViewById( R.id.statusText ),
                                      FetcherService.getStatusText()/*,
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
        mProgressBar.setProgress( 0 );

        mLabelClock = (TextView)rootView.findViewById(R.id.textClock);
        mLabelClock.setText("");

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
            mLastPagerPos = mCurrentPagerPos;
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
                mLastPagerPos = i;
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });

        TextView.OnClickListener listener = new TextView.OnClickListener() {
            @Override
            public void onClick(View view) {
                //EntryView entryView = (EntryView) mEntryPager.findViewWithTag("EntryView" + mEntryPager.getCurrentItem());
                PageDown();
            }
        };

        rootView.findViewById(R.id.pageDownBtn).setOnClickListener(listener);
        rootView.findViewById(R.id.pageDownBtnVert).setOnClickListener(listener);

        rootView.findViewById(R.id.pageUpBtn).setOnClickListener(new TextView.OnClickListener() {
            @Override
            public void onClick(View view) {
                PageUp();
            }
        });

        //disableSwipe();

        HideButtonText(rootView, R.id.pageDownBtnVert, true);
        HideButtonText(rootView, R.id.pageDownBtn, true);
        HideButtonText(rootView, R.id.pageUpBtn, true);
        HideButtonText(rootView, R.id.toggleFullScreenStatusBarBtn, !PrefUtils.getBoolean( PrefUtils.TAP_ZONES_VISIBLE, true ));
        HideButtonText(rootView, R.id.toggleFullscreenBtn, !PrefUtils.getBoolean( PrefUtils.TAP_ZONES_VISIBLE, true ));


        rootView.findViewById(R.id.layoutBottom).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.statusText).setVisibility(View.GONE);

        return rootView;
    }

    public void PageUp() {
        EntryView entryView = mEntryPagerAdapter.mEntryViews.get(mEntryPager.getCurrentItem());
        if (entryView != null) {
            entryView.pageUp(false);
        }
    }

    public void PageDown() {
        EntryView entryView = mEntryPagerAdapter.mEntryViews.get(mEntryPager.getCurrentItem());
        //Toast.makeText(getContext(), "pageDown onclick", Toast.LENGTH_LONG ).show();
        if (entryView != null) {
            //Toast.makeText(getContext(), "pageDown onclick not null", Toast.LENGTH_LONG ).show();
            entryView.pageDown(false);
        }
    }

    public void NextEntry() {
        if ( mEntryPager.getCurrentItem() < mEntryPager.getAdapter().getCount() - 1  )
            mEntryPager.setCurrentItem( mEntryPager.getCurrentItem() + 1 );
    }
    public void PreviousEntry() {
        if ( mEntryPager.getCurrentItem() > 0  )
            mEntryPager.setCurrentItem( mEntryPager.getCurrentItem() - 1 );
    }

    void HideButtonText(View rootView, int ID, boolean transparent) {
        TextView btn = (TextView)rootView.findViewById(ID);
        if ( transparent )
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
        FetcherService.getStatusText().deleteObserver(mStatusText);
        super.onDestroy();
    }
    @Override
    public void onResume() {
        super.onResume();
        mEntryPagerAdapter.onResume();
        mMarkAsUnreadOnFinish = false;

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
            //PrefUtils.putInt(PrefUtils.LAST_ENTRY_SCROLL_Y, entryView.getScrollY());
            mEntryPagerAdapter.SaveScrollPos();
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
                        Toast.makeText( getContext(), R.string.entry_marked_favourite, Toast.LENGTH_LONG ).show();
                    } else {
                        item.setTitle(R.string.menu_star).setIcon(R.drawable.rating_not_important);
                        Toast.makeText( getContext(), R.string.entry_marked_unfavourite, Toast.LENGTH_LONG ).show();
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

                    if ( EntryActivity.GetIsStatusBarHidden() )
                        ( (EntryActivity)getActivity() ).setFullScreen( true, true );
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

                    UiUtils.toast( getActivity(), R.string.copied_clipboard);
                    break;
                }
                case R.id.menu_mark_as_unread: {
                    mMarkAsUnreadOnFinish = true;
                    CloseEntry();
                    final Uri uri = ContentUris.withAppendedId(mBaseUri, getCurrentEntryID());
                    new Thread() {
                        @Override
                        public void run() {
                            ContentResolver cr = MainApplication.getContext().getContentResolver();
                            cr.update(uri, FeedData.getUnreadContentValues(), null, null);
                        }
                    }.start();
                    UiUtils.toast( getActivity(), R.string.entry_marked_unread );
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
                    UiUtils.toast( getActivity(), R.string.entry_marked_favourite );
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

                    UiUtils.toast( getActivity(), R.string.entry_marked_unfavourite );

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
                case R.id.menu_share_all_text: {
                    if ( mCurrentPagerPos != -1 ) {
                        Spanned spanned = Html.fromHtml(mEntryPagerAdapter.mEntryViews.get(mCurrentPagerPos).mData);
                        char[] chars = new char[spanned.length()];
                        TextUtils.getChars(spanned, 0, spanned.length(), chars, 0);
                        String plainText = new String(chars);
                        plainText = plainText.replaceAll( "body(.)*", "" );
                        startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND)
                                        .putExtra(Intent.EXTRA_TEXT, plainText)
                                        .setType(Constants.MIMETYPE_TEXT_PLAIN),
                                getString(R.string.menu_share)));
                    }
                    break;
                }
                case R.id.menu_cancel_refresh: {
                    FetcherService.cancelRefresh();
                    break;
                }
                case R.id.menu_open_link: {
                    Uri uri = Uri.parse( mEntryPagerAdapter.getCursor(mCurrentPagerPos).getString(mLinkPos) );
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri );
                    getActivity().startActivity(intent);
                }

                case R.id.menu_reload_full_text: {

                    int status = FetcherService.getStatusText().Start("Reload fulltext"); try {
                        DeleteMobilized();
                        LoadFullText( ArticleTextExtractor.Mobilize.Yes );
                    } finally { FetcherService.getStatusText().End( status ); }
                    break;
                }

                case R.id.menu_reload_full_text_without_mobilizer: {

                    int status = FetcherService.getStatusText().Start("Reload fulltext"); try {
                        DeleteMobilized();
                        LoadFullText( ArticleTextExtractor.Mobilize.No );
                    } finally { FetcherService.getStatusText().End( status ); }
                    break;
                }
            }
        }

        return true;
    }

    void DeleteMobilized() {
        ContentValues values = new ContentValues();
        values.putNull(EntryColumns.MOBILIZED_HTML);
        ContentResolver cr = MainApplication.getContext().getContentResolver();
        final Uri uri = ContentUris.withAppendedId(mBaseUri, getCurrentEntryID());
        cr.update(uri, values, null, null);
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
        //Dog.v( String.format( "EntryFragment.setData( %s )", uri.toString() ) );

        mCurrentPagerPos = -1;

        //PrefUtils.putString( PrefUtils.LAST_URI, uri.toString() );

        mBaseUri = FeedData.EntryColumns.PARENT_URI(uri.getPath());
        Dog.v( String.format( "EntryFragment.setData( %s ) baseUri = %s", uri.toString(), mBaseUri ) );
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
                        mLastPagerPos = i;
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
            //refreshSwipeProgress();

            // Mark the previous opened article as read
            //if (entryCursor.getInt(mIsReadPos) != 1) {
            if ( !mMarkAsUnreadOnFinish && mLastPagerPos != -1 && mEntryPagerAdapter.getCursor(mLastPagerPos).getInt(mIsReadPos) != 1 ) {
                final Uri uri = ContentUris.withAppendedId(mBaseUri, mEntriesIds[mLastPagerPos]);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ContentResolver cr = MainApplication.getContext().getContentResolver();
                        cr.update(uri, FeedData.getReadContentValues(), null, null);

                        // Update the cursor
                        Cursor updatedCursor = cr.query(uri, null, null, null, null);
                        updatedCursor.moveToFirst();
                        mEntryPagerAdapter.setUpdatedCursor(mLastPagerPos, updatedCursor);
                    }
                }).start();
            }
        }
    }

    private void showEnclosure(Uri uri, String enclosure, int position1, int position2) {
        try {
            startActivityForResult(new Intent(Intent.ACTION_VIEW).setDataAndType(uri, enclosure.substring(position1 + DrawerAdapter.FIRST_ENTRY_POS, position2)), 0);
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
            LoadFullText( ArticleTextExtractor.Mobilize.Yes );
        }
    }

    private void LoadFullText(final ArticleTextExtractor.Mobilize mobilize ) {
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
                    int status = FetcherService.getStatusText().Start(getActivity().getString(R.string.loadFullText)); try {
                        FetcherService.mobilizeEntry(getContext().getContentResolver(), getCurrentEntryID(), mobilize, FetcherService.AutoDownloadEntryImages.Yes);
                    } finally { FetcherService.getStatusText().End( status ); }
                }
            }.start();



            /*activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshSwipeProgress();
                }
            });*/
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
                final int position2 = enclosure.indexOf(Constants.ENCLOSURE_SEPARATOR, position1 + DrawerAdapter.FIRST_ENTRY_POS);

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
                    FetcherService.mCancelRefresh = false;
                    int status = FetcherService.getStatusText().Start( getString(R.string.downloadImage) ); try {
                        NetworkUtils.downloadImage(getCurrentEntryID(), url, false);
                    } catch (IOException e) {
                        //FetcherService.getStatusText().End( status );
                        e.printStackTrace();
                    } finally {
                        FetcherService.getStatusText().End( status );
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
                mScrollPosPos = cursor.getColumnIndex(EntryColumns.SCROLL_POS);
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



    /*@Override
    public void onRefresh() {
        // Nothing to do
    }*/

    public class EntryPagerAdapter extends PagerAdapter {

        private final SparseArray<EntryView> mEntryViews = new SparseArray<>();

        public EntryPagerAdapter() {
        }

        @Override
        public int getCount() {
            return mEntriesIds != null ? mEntriesIds.length : 0;
        }

        @Override
        public void destroyItem(ViewGroup container, final int position, Object object) {
            Dog.d( "EntryPagerAdapter.destroyItem " + position );
            FetcherService.removeActiveEntryID( mEntriesIds[position] );
            getLoaderManager().destroyLoader(position);
            container.removeView((View) object);
            EntryView.mImageDownloadObservable.deleteObserver(mEntryViews.get(position));
            SaveScrollPos();
            mEntryViews.delete(position);
        }

        private void SaveScrollPos() {
            final long entryID = getCurrentEntryID();
            new Thread() {
                @Override
                public void run() {
                    ContentValues values = new ContentValues();
                    EntryView view = mEntryViews.get(mCurrentPagerPos);
                    if ( view != null ) {
                        final int scroll = view.getScrollY();
                        values.put(EntryColumns.SCROLL_POS, scroll);
                        ContentResolver cr = MainApplication.getContext().getContentResolver();
                        //cr.update(EntryColumns.CONTENT_URI(entryID), values, EntryColumns.SCROLL_POS + Constants.DB_IS_NULL + Constants.DB_OR + EntryColumns.SCROLL_POS + " < " + scroll, null);
                        cr.update(EntryColumns.CONTENT_URI(entryID), values, null, null);
                        Dog.v(String.format("EntryPagerAdapter.SaveScrollPos (entry %d) update scrollPos = %d", entryID, scroll));
                    }
                }
            }.start();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Dog.d( "EntryPagerAdapter.instantiateItem" + position );
            FetcherService.addActiveEntryID( mEntriesIds[position] );
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
            Dog.d( "EntryPagerAdapter.displayEntry" + pagerPos);

            EntryView view = mEntryViews.get(pagerPos);
            if (view != null ) {
                if (newCursor == null) {
                    newCursor = (Cursor) view.getTag(); // get the old one
                }

                if (newCursor != null && newCursor.moveToFirst()  ) {
                    String contentText = "";
                    String author = "";
                    long timestamp = 0;
                    String link = "";
                    String title = "";
                    String enclosure = "";
                    int scrollPos = 0;
                    try {
                        contentText = newCursor.getString(mMobilizedHtmlPos);
                        if (contentText == null || (forceUpdate && !mPreferFullText)) {
                            mPreferFullText = false;
                            contentText = newCursor.getString(mAbstractPos);
                        } else {
                            mPreferFullText = true;
                        }
                        if (contentText == null) {
                            contentText = "";
                        }

                        author = newCursor.getString(mAuthorPos);
                        timestamp = newCursor.getLong(mDatePos);
                        link = newCursor.getString(mLinkPos);
                        title = newCursor.getString(mTitlePos);
                        enclosure = newCursor.getString(mEnclosurePos);
                        if ( !newCursor.isNull(mScrollPosPos) )
                            scrollPos = newCursor.getInt(mScrollPosPos);
                    } catch ( IllegalStateException e ) {
                        contentText = "Context too large";
                    }

                    //FetcherService.setCurrentEntryID( getCurrentEntryID() );
                    view.setHtml(mEntriesIds[pagerPos],
                            title,
                            link,
                            contentText,
                            enclosure,
                            author,
                            timestamp,
                            mPreferFullText,
                            (EntryActivity) getActivity());
                    view.setTag(newCursor);

                    if (pagerPos == mCurrentPagerPos) {
                        refreshUI(newCursor);

                        //if (PrefUtils.getLong(PrefUtils.LAST_ENTRY_ID, 0) == mEntriesIds[pagerPos]) {
                            //int dy = mScrollPosPos;
                            //if (dy > view.getScrollY())
                        view.mScrollY = view.getScrollY() >  scrollPos ? view.getScrollY() : scrollPos;
                        Dog.v( String.format( "displayEntry view.mScrollY  (entry %s) view.mScrollY = %d", getCurrentEntryID(),  view.mScrollY ) );
                        //Dog.v( "displayEntry view.mScrollY = " + view.mScrollY );
                        //}


                    }

                    UpdateProgress();
                    UpdateClock();

                }
            }
        }

        public Cursor getCursor(int pagerPos) {
            EntryView view = mEntryViews.get(pagerPos);
            if (view != null && ( view.getTag() instanceof Cursor ) ) {
                return (Cursor) view.getTag();
            }
            return null;
        }

        public void setUpdatedCursor(int pagerPos, Cursor newCursor) {
            EntryView view = mEntryViews.get(pagerPos);
            if (view != null && ( view.getTag(R.id.updated_cursor) instanceof Cursor ) ) {
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

