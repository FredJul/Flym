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
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
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
import ru.yanus171.feedexfork.provider.FeedDataContentProvider;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.ArticleTextExtractor;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.NetworkUtils;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.Timer;
import ru.yanus171.feedexfork.utils.UiUtils;
import ru.yanus171.feedexfork.view.EntryView;
import ru.yanus171.feedexfork.view.StatusText;
import ru.yanus171.feedexfork.view.TapZonePreviewPreference;

import static ru.yanus171.feedexfork.Constants.VIBRATE_DURATION;
import static ru.yanus171.feedexfork.service.FetcherService.CancelStarNotification;
import static ru.yanus171.feedexfork.utils.PrefUtils.DISPLAY_ENTRIES_FULLSCREEN;
import static ru.yanus171.feedexfork.utils.PrefUtils.VIBRATE_ON_ARTICLE_LIST_ENTRY_SWYPE;
import static ru.yanus171.feedexfork.utils.PrefUtils.getBoolean;


public class EntryFragment extends /*SwipeRefresh*/Fragment implements LoaderManager.LoaderCallbacks<Cursor>, EntryView.EntryViewManager {

    private static final String STATE_BASE_URI = "STATE_BASE_URI";
    private static final String STATE_CURRENT_PAGER_POS = "STATE_CURRENT_PAGER_POS";
    private static final String STATE_ENTRIES_IDS = "STATE_ENTRIES_IDS";
    private static final String STATE_INITIAL_ENTRY_ID = "STATE_INITIAL_ENTRY_ID";
    private static final String STATE_LOCK_LAND_ORIENTATION = "STATE_LOCK_LAND_ORIENTATION";


    private int mTitlePos = -1, mDatePos, mMobilizedHtmlPos, mAbstractPos, mLinkPos, mIsFavoritePos, mIsReadPos, mIsNewPos, mEnclosurePos, mAuthorPos, mFeedNamePos, mFeedUrlPos, mFeedIconPos, mScrollPosPos, mRetrieveFullTextPos;


    private int mCurrentPagerPos = -1, mLastPagerPos = -1;
    private Uri mBaseUri;
    private long mInitialEntryId = -1;
    private long[] mEntriesIds;

    private boolean mFavorite, mIsFullTextShown = true;

    private ViewPager mEntryPager;
    public EntryPagerAdapter mEntryPagerAdapter;

    private View mToggleFullscreenBtn;
    private View mToggleStatusBarVisbleBtn;
    private View mDimFrame;
    private View mStarFrame;
    public ProgressBar mProgressBar;
    TextView mLabelClock;

    private StatusText mStatusText = null;

    private boolean mLockLandOrientation = false;

    public boolean mMarkAsUnreadOnFinish = false;
    private boolean mRetrieveFullText = false;

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
        TapZonePreviewPreference.SetupZoneSizes( rootView );

        mStatusText = new StatusText( (TextView)rootView.findViewById( R.id.statusText ),
                                      FetcherService.Status()/*,
                                      this*/);
        mToggleFullscreenBtn = rootView.findViewById(R.id.toggleFullscreenBtn);
        mToggleFullscreenBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EntryActivity activity = (EntryActivity) getActivity();
                activity.setFullScreen( EntryActivity.GetIsStatusBarHidden(), !EntryActivity.GetIsActionBarHidden() );
            }
        });

        mProgressBar = rootView.findViewById(R.id.progressBar);
        mProgressBar.setProgress( 0 );

        mLabelClock = rootView.findViewById(R.id.textClock);
        mLabelClock.setText("");

        mToggleStatusBarVisbleBtn =  rootView.findViewById(R.id.toggleFullScreenStatusBarBtn);
        mToggleStatusBarVisbleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            EntryActivity activity = (EntryActivity) getActivity();
            activity.setFullScreen(!EntryActivity.GetIsStatusBarHidden(), EntryActivity.GetIsActionBarHidden());
            }
        });

        mEntryPager = rootView.findViewById(R.id.pager);
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

                CancelStarNotification( getCurrentEntryID() );

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
        HideButtonText(rootView, R.id.brightnessSlider, true);
        HideButtonText(rootView, R.id.toggleFullScreenStatusBarBtn, !PrefUtils.getBoolean( PrefUtils.TAP_ZONES_VISIBLE, true ));
        HideButtonText(rootView, R.id.toggleFullscreenBtn, !PrefUtils.getBoolean( PrefUtils.TAP_ZONES_VISIBLE, true ));


        rootView.findViewById(R.id.layoutBottom).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.statusText).setVisibility(View.GONE);

        mLockLandOrientation = PrefUtils.getBoolean(STATE_LOCK_LAND_ORIENTATION, false );
        mDimFrame = rootView.findViewById( R.id.dimFrame );
        rootView.findViewById(R.id.brightnessSlider).setOnTouchListener(new View.OnTouchListener() {
            private int paddingX = 0;
            private int paddingY = 0;
            private int initialx = 0;
            private int initialy = 0;
            private int currentx = 0;
            private int currenty = 0;
            private int mInitialAlpha = 0;

            @Override
            public boolean onTouch(View view1, MotionEvent event) {

                if ( event.getAction() == MotionEvent.ACTION_DOWN) {
                    paddingX = 0;
                    paddingY = 0;
                    initialx = (int) event.getX();
                    initialy = (int) event.getY();
                    currentx = (int) event.getX();
                    currenty = (int) event.getY();
                    mInitialAlpha = GetBrightness();
                    Dog.v( "onTouch ACTION_DOWN" );
                    return true;
                } else  if ( event.getAction() == MotionEvent.ACTION_MOVE ) {

                    currentx = (int) event.getX();
                    currenty = (int) event.getY();
                    paddingX = currentx - initialx;
                    paddingY = currenty - initialy;

                    if ( Math.abs( paddingY ) > Math.abs( paddingX ) &&
                            Math.abs( initialy - event.getY() ) > view1.getWidth()  ) {
                        Dog.v( "onTouch ACTION_MOVE " + paddingX + ", " + paddingY );
                        int currentAlpha = mInitialAlpha + 255 / 1 * paddingY / mDimFrame.getHeight();
                        if ( currentAlpha > 255 )
                            currentAlpha = 255;
                        else if ( currentAlpha < 1 )
                            currentAlpha = 1;
                        SetBrightness(currentAlpha);
                    }
                    return true;
                } else  if ( event.getAction() == MotionEvent.ACTION_UP) {
                    return false;
                }

                return false;
            }
        });

        final Vibrator vibrator = (Vibrator) getContext().getSystemService( Context.VIBRATOR_SERVICE );
        mStarFrame = rootView.findViewById(R.id.frameStar);
        final ImageView frameStarImage  = rootView.findViewById(R.id.frameStarImage);
        final boolean prefVibrate = PrefUtils.getBoolean(VIBRATE_ON_ARTICLE_LIST_ENTRY_SWYPE, true);
        rootView.findViewById(R.id.pageUpBtn).setOnTouchListener(new View.OnTouchListener() {
            private int initialy = 0;
            private boolean mWasVibrate = false;
            private boolean mWasSwipe = false;
            private final int MAX_HEIGHT = UiUtils.mmToPixel( 12 );
            private final int MIN_HEIGHT = UiUtils.mmToPixel( 1 );
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if ( event.getAction() == MotionEvent.ACTION_DOWN) {
                    Dog.v( "onTouch ACTION_DOWN " );
                    //initialx = (int) event.getX();
                    initialy = (int) event.getY();
                    mWasVibrate = false;
                    mWasSwipe = false;
                    return true;
                } else if ( event.getAction() == MotionEvent.ACTION_MOVE) {
                    Dog.v( "onTouch ACTION_MOVE " + ( event.getY() - initialy ) );
                    int w = Math.max( 0, (int) (event.getY() - initialy) );
                    SetStarFrameWidth( Math.min( w, MAX_HEIGHT ) );
                    if ( prefVibrate && w >= MAX_HEIGHT && !mWasVibrate ) {
                        mWasVibrate = true;
                        vibrator.vibrate(VIBRATE_DURATION);
                    } else if ( w < MAX_HEIGHT )
                        mWasVibrate = false;
                    if ( w >= MIN_HEIGHT )
                        mWasSwipe = true;

                    frameStarImage.setImageResource( ( w >= MAX_HEIGHT ) == mFavorite ? R.drawable.star_empty_gray : R.drawable.star_yellow );
                    return true;
                } else if ( event.getAction() == MotionEvent.ACTION_UP) {
                    Dog.v( "onTouch ACTION_UP " );
                    if ( !mWasSwipe ) {
                        PageUp();
                    } else if ( event.getY() - initialy >= MAX_HEIGHT ) {
                        SetIsFavourite(!mFavorite);
                    }
                    SetStarFrameWidth(0);
                    return true;
                } else
                    SetStarFrameWidth(0);
                return false;
            }


        });
        SetStarFrameWidth(0);

        SetOrientation();

        return rootView;
    }

    private void SetStarFrameWidth(int w) {
        mStarFrame.setLayoutParams( new FrameLayout.LayoutParams( FrameLayout.LayoutParams.FILL_PARENT, w));
    }

    private void SetBrightness(int currentAlpha) {
        int newColor = Color.argb( currentAlpha, 0,  0,  0 );
        mDimFrame.setBackgroundColor( newColor );
    }


    private void SetOrientation() {
		int or = mLockLandOrientation ?
			   ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE :
			   ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
		if ( mLockLandOrientation && or != getActivity().getRequestedOrientation() )
			getActivity().setRequestedOrientation( or );
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
        TextView btn = rootView.findViewById(ID);
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
        FetcherService.Status().deleteObserver(mStatusText);
        super.onDestroy();
    }
    @Override
    public void onResume() {
        super.onResume();
        mEntryPagerAdapter.onResume();
        mMarkAsUnreadOnFinish = false;
        SetBrightness( PrefUtils.getInt( PrefUtils.LAST_BRIGHTNESS, 0 ) );
    }

    @Override
    public void onPause() {
        super.onPause();
        EntryView entryView = mEntryPagerAdapter.mEntryViews.get(mEntryPager.getCurrentItem());
        if (entryView != null) {
            //PrefUtils.putInt(PrefUtils.LAST_ENTRY_SCROLL_Y, entryView.getScrollY());
            mEntryPagerAdapter.SaveScrollPos( false );
            PrefUtils.putLong(PrefUtils.LAST_ENTRY_ID, getCurrentEntryID());
            PrefUtils.putBoolean(STATE_LOCK_LAND_ORIENTATION, mLockLandOrientation);
        }
        PrefUtils.putInt( PrefUtils.LAST_BRIGHTNESS, GetBrightness());

        mEntryPagerAdapter.onPause();
    }

    private int GetBrightness() {
        return Color.alpha( ( (ColorDrawable)mDimFrame.getBackground() ).getColor() );
    }

    /**
     * Updates a menu item in the dropdown to show it's icon that was declared in XML.
     *
     * @param item
     *         the item to update
     */
    private static void updateMenuWithIcon(@NonNull final MenuItem item) {
        SpannableStringBuilder builder = new SpannableStringBuilder()
                .append("*") // the * will be replaced with the icon via ImageSpan
                .append("    ") // This extra space acts as padding. Adjust as you wish
                .append(item.getTitle());



        // Retrieve the icon that was declared in XML and assigned during inflation
        if (item.getIcon() != null && item.getIcon().getConstantState() != null) {
            Drawable drawable = item.getIcon().getConstantState().newDrawable();

            // Mutate this drawable so the tint only applies here
            // drawable.mutate().setTint(color);

            // Needs bounds, or else it won't show up (doesn't know how big to be)
            //drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            drawable.setBounds(0, 0, DpToPx( 30 ), DpToPx( 30 ) );
            ImageSpan imageSpan = new ImageSpan(drawable);
            builder.setSpan(imageSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            item.setTitle(builder);
        }
    }

    // -------------------------------------------------------------------------
    public static int DpToPx( float dp) {
        Resources r = MainApplication.getContext().getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
        return (int) px;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.entry, menu);

        //int color = ContextCompat.getColor(getContext(), R.color.common_google_signin_btn_text_dark);
        //updateMenuWithIcon(menu.findItem(R.id.menu_mark_as_favorite));
        //updateMenuWithIcon(menu.findItem(R.id.menu_mark_as_unfavorite));
        updateMenuWithIcon(menu.findItem(R.id.menu_reload_full_text));
        //updateMenuWithIcon(menu.findItem(R.id.menu_reload_full_text_without_mobilizer));
        //updateMenuWithIcon(menu.findItem(R.id.menu_reload_full_text_with_tags));
        updateMenuWithIcon(menu.findItem(R.id.menu_load_all_images));
        updateMenuWithIcon(menu.findItem(R.id.menu_share_all_text));
        updateMenuWithIcon(menu.findItem(R.id.menu_open_link));
        updateMenuWithIcon(menu.findItem(R.id.menu_cancel_refresh));

        EntryActivity activity = (EntryActivity) getActivity();
        menu.findItem(R.id.menu_star).setShowAsAction( EntryActivity.GetIsActionBarHidden() ? MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW : MenuItem.SHOW_AS_ACTION_IF_ROOM );

        {
            MenuItem item = menu.findItem(R.id.menu_star);
            if (mFavorite)
                item.setTitle(R.string.menu_unstar).setIcon(R.drawable.rating_important);
            else
                item.setTitle(R.string.menu_star).setIcon(R.drawable.rating_not_important);
            updateMenuWithIcon(item);
        }
        //menu.findItem(R.id.menu_mark_as_favorite).setVisible( !mFavorite );
        //menu.findItem(R.id.menu_mark_as_unfavorite).setVisible(mFavorite);

        menu.findItem(R.id.menu_lock_land_orientation).setChecked(mLockLandOrientation);

//        if (mFavorite)
//            menu.findItem(R.id.menu_mark_as_favorite ).setTitle(R.string.menu_unstar).setIcon(R.drawable.rating_important);
//        else
//            menu.findItem(R.id.menu_mark_as_unfavorite).setTitle(R.string.menu_star).setIcon(R.drawable.rating_not_important);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mEntriesIds != null) {
            Activity activity = getActivity();

            switch (item.getItemId()) {
                case R.id.menu_star: {

                    SetIsFavourite( !mFavorite );

                    if ( !mFavorite )
                        CloseEntry();
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

                case R.id.menu_toggle_theme: {
                    mEntryPagerAdapter.SaveScrollPos( false );
                    PrefUtils.ToogleTheme(new Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(mBaseUri, getCurrentEntryID())));
                    return true;
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
                /*case R.id.menu_mark_as_favorite: {
                    final Uri uri = ContentUris.withAppendedId(mBaseUri, getCurrentEntryID());
                    new Thread() {
                        @Override
                        public void run() {
                            ContentResolver cr = MainApplication.getContext().getContentResolver();
                            cr.update(uri, FeedData.getFavoriteContentValues(true), null, null);
                        }
                    }.start();
                    UiUtils.toast( getActivity(), R.string.entry_marked_favourite );
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
                }*/
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

                    int status = FetcherService.Status().Start("Reload fulltext"); try {
                        DeleteMobilized();
                        LoadFullText( ArticleTextExtractor.MobilizeType.Yes );
                    } finally { FetcherService.Status().End( status ); }
                    break;
                }

                case R.id.menu_reload_full_text_without_mobilizer: {

                    int status = FetcherService.Status().Start("Reload fulltext"); try {
                        DeleteMobilized();
                        LoadFullText( ArticleTextExtractor.MobilizeType.No );
                    } finally { FetcherService.Status().End( status ); }
                    break;
                }

                case R.id.menu_reload_full_text_with_tags: {

                    int status = FetcherService.Status().Start("Reload fulltext"); try {
                        DeleteMobilized();
                        LoadFullText( ArticleTextExtractor.MobilizeType.Tags );
                    } finally { FetcherService.Status().End( status ); }
                    break;
                }

                case R.id.menu_lock_land_orientation: {
                    mLockLandOrientation = !mLockLandOrientation;
                    item.setChecked(mLockLandOrientation);
                    SetOrientation();
                }
            }
        }

        return true;
    }

    public void SetIsFavourite(final boolean favorite) {
        if ( mFavorite == favorite )
            return;
        mFavorite = favorite;
        final Uri uri = ContentUris.withAppendedId(mBaseUri, getCurrentEntryID());
        new Thread() {
            @Override
            public void run() {
                ContentValues values = new ContentValues();
                values.put(EntryColumns.IS_FAVORITE, mFavorite ? 1 : 0);
                ContentResolver cr = MainApplication.getContext().getContentResolver();
                cr.update(uri, values, null, null);

                /*// Update the cursor
                Cursor updatedCursor = cr.query(uri, null, null, null, null);
                updatedCursor.moveToFirst();
                mEntryPagerAdapter.setUpdatedCursor(mCurrentPagerPos, updatedCursor);*/

            }
        }.start();
        getActivity().invalidateOptionsMenu();
        Toast.makeText( getContext(), mFavorite ? R.string.entry_marked_favourite : R.string.entry_marked_unfavourite, Toast.LENGTH_LONG ).show();
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

    public void setData(final Uri uri) {
        Timer timer = new Timer( "EntryFr.setData" );
        Dog.v( String.format( "EntryFragment.setData( %s )", uri.toString() ) );

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
                        CancelStarNotification( getCurrentEntryID() );
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
		if ( mBaseUri.getPathSegments().size() > 1 ) {
			Dog.v( "EntryFragment.setData() mBaseUri.getPathSegments[1] = " + mBaseUri.getPathSegments().get(1) );
			if ( mBaseUri.getPathSegments().get(1).equals( FetcherService.GetExtrenalLinkFeedID() ) ) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						Dog.v( "EntryFragment.setData() update time to current" );
						ContentResolver cr = MainApplication.getContext().getContentResolver();
						ContentValues values = new ContentValues();
						values.put(EntryColumns.DATE, (new Date()).getTime());
						cr.update(uri, values, null, null);
					}
				}).start();
			}
		}
        timer.End();
    }

    private void refreshUI(Cursor entryCursor) {
		try {
			if (entryCursor != null ) {
				String feedTitle = entryCursor.isNull(mFeedNamePos) ? entryCursor.getString(mFeedUrlPos) : entryCursor.getString(mFeedNamePos);
				EntryActivity activity = (EntryActivity) getActivity();
				activity.setTitle("");//activity.setTitle(feedTitle);

				mFavorite = entryCursor.getInt(mIsFavoritePos) == 1;
				//mRetrieveFullText = entryCursor.getInt( mRetrieveFullTextPos ) == 1;

				activity.invalidateOptionsMenu();

				// Listen the mobilizing task

				if (FetcherService.hasMobilizationTask(getCurrentEntryID())) {
					//--showSwipeProgress();

					// If the service is not started, start it here to avoid an infinite loading
					if (!PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false)) {
						FetcherService.StartService(new Intent(MainApplication.getContext(),
																			 FetcherService.class)
																  .setAction(FetcherService.ACTION_MOBILIZE_FEEDS));
					}
				} else {
					//--hideSwipeProgress();
				}
				//refreshSwipeProgress();

				// Mark the previous opened article as read
				//if (entryCursor.getInt(mIsReadPos) != 1) {
				if ( !mMarkAsUnreadOnFinish && mLastPagerPos != -1 ) {
                    class ReadAndOldWriter implements Runnable {
                        private final boolean mSetAsRead;
                        private final int mPagerPos;
                        public ReadAndOldWriter(int pagerPos, boolean setAsRead ){
                            mPagerPos = pagerPos;
                            mSetAsRead = setAsRead;
                        }
					    @Override
                        public void run() {
                            final Uri uri = ContentUris.withAppendedId(mBaseUri, mEntriesIds[mPagerPos]);
                            ContentResolver cr = MainApplication.getContext().getContentResolver();
                            if ( mSetAsRead )
                                cr.update(uri, FeedData.getReadContentValues(), EntryColumns.WHERE_UNREAD, null);
                            cr.update(uri, FeedData.getOldContentValues(), EntryColumns.WHERE_NEW, null);
                            /*// Update the cursor
                            Cursor updatedCursor = cr.query(uri, null, null, null, null);
                            updatedCursor.moveToFirst();
                            mEntryPagerAdapter.setUpdatedCursor(mPagerPos, updatedCursor);*/
                        }
                    }
                    new Thread(new ReadAndOldWriter( mLastPagerPos, mEntryPagerAdapter.getCursor(mLastPagerPos).getInt(mIsReadPos) != 1 )).start();
                }
            }
		} catch ( IllegalStateException e ) {
			e.printStackTrace();
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
                mIsFullTextShown = false;
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
                    mIsFullTextShown = true;
                    mEntryPagerAdapter.displayEntry(mCurrentPagerPos, null, true);
                }
            });
        } else /*--if (!isRefreshing())*/ {
            LoadFullText( ArticleTextExtractor.MobilizeType.Yes );
        }
    }

    private void LoadFullText(final ArticleTextExtractor.MobilizeType mobilize ) {
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
                    int status = FetcherService.Status().Start(getActivity().getString(R.string.loadFullText)); try {
                        FetcherService.mobilizeEntry(getContext().getContentResolver(), getCurrentEntryID(), mobilize, FetcherService.AutoDownloadEntryImages.Yes, true, true);
                    } finally { FetcherService.Status().End( status ); }
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
                    int status = FetcherService.Status().Start( getString(R.string.downloadImage) ); try {
                        NetworkUtils.downloadImage(getCurrentEntryID(), url, false);
                    } catch (IOException e) {
                        //FetcherService.Status().End( status );
                        e.printStackTrace();
                    } finally {
                        FetcherService.Status().End( status );
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
        Timer.Start( id, "EntryFr.onCreateLoader" );
        CursorLoader cursorLoader = new CursorLoader(getActivity(), EntryColumns.CONTENT_URI(mEntriesIds[id]), null, null, null, null);
        cursorLoader.setUpdateThrottle(100);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Timer.End( loader.getId() );
        if (mBaseUri != null && cursor != null) { // can be null if we do a setData(null) before
            try {
                if ( cursor.moveToFirst() ) {

                    if (mTitlePos == -1) {
                        mTitlePos = cursor.getColumnIndex(EntryColumns.TITLE);
                        mDatePos = cursor.getColumnIndex(EntryColumns.DATE);
                        mAbstractPos = cursor.getColumnIndex(EntryColumns.ABSTRACT);
                        mMobilizedHtmlPos = cursor.getColumnIndex(EntryColumns.MOBILIZED_HTML);
                        mLinkPos = cursor.getColumnIndex(EntryColumns.LINK);
                        mIsFavoritePos = cursor.getColumnIndex(EntryColumns.IS_FAVORITE);
                        mIsReadPos = cursor.getColumnIndex(EntryColumns.IS_READ);
                        mIsNewPos = cursor.getColumnIndex(EntryColumns.IS_NEW);
                        mEnclosurePos = cursor.getColumnIndex(EntryColumns.ENCLOSURE);
                        mAuthorPos = cursor.getColumnIndex(EntryColumns.AUTHOR);
                        mScrollPosPos = cursor.getColumnIndex(EntryColumns.SCROLL_POS);
                        mFeedNamePos = cursor.getColumnIndex(FeedColumns.NAME);
                        mFeedUrlPos = cursor.getColumnIndex(FeedColumns.URL);
                        mFeedIconPos = cursor.getColumnIndex(FeedColumns.ICON);
                        mRetrieveFullTextPos = cursor.getColumnIndex(FeedColumns.RETRIEVE_FULLTEXT);
                    }

                    int position = loader.getId();
                    if (position != -1) {
                        FetcherService.mMaxImageDownloadCount = PrefUtils.getImageDownloadCount();
                        mEntryPagerAdapter.displayEntry(position, cursor, false);
                        mRetrieveFullText = cursor.getInt(mRetrieveFullTextPos) == 1;
                        EntryActivity activity = (EntryActivity) getActivity();
                        if (getBoolean(DISPLAY_ENTRIES_FULLSCREEN, false))
                            activity.setFullScreen(true, true);

                    }
                }
            } catch ( IllegalStateException e ) {
                FetcherService.Status().SetError( e.getMessage(), e );
                Dog.e("Error", e);
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
            SaveScrollPos( false );
            mEntryViews.delete(position);
        }

        public void SaveScrollPos(final boolean force ) {
            final long entryID = getCurrentEntryID();
            final EntryView view = mEntryViews.get(mCurrentPagerPos);
            if ( view != null ) {
                final float scrollPart = view.GetViewScrollPartY();
                Dog.v(String.format("EntryPagerAdapter.SaveScrollPos (entry %d) getScrollY() = %d, view.getContentHeight() = %f", entryID, view.getScrollY(), view.GetContentHeight() ));
                new Thread() {
                    @Override
                    public void run() {
                        ContentValues values = new ContentValues();
                        values.put(EntryColumns.SCROLL_POS, scrollPart);
                        ContentResolver cr = MainApplication.getContext().getContentResolver();
                        //cr.update(EntryColumns.CONTENT_URI(entryID), values, EntryColumns.SCROLL_POS + Constants.DB_IS_NULL + Constants.DB_OR + EntryColumns.SCROLL_POS + " < " + scroll, null);
                        FeedDataContentProvider.mNotifyEnabled = false;
                        String where = EntryColumns.SCROLL_POS + " < " + scrollPart + Constants.DB_OR + EntryColumns.SCROLL_POS + Constants.DB_IS_NULL;
                        cr.update(EntryColumns.CONTENT_URI(entryID), values, force ? "" : where, null);
                        FeedDataContentProvider.mNotifyEnabled = true;
                        Dog.v(String.format("EntryPagerAdapter.SaveScrollPos (entry %d) update scrollPos = %f", entryID, scrollPart));
                    }
                }.start();
            }
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
            view.setTag(null);

            view.mScrollChangeListener = new Runnable(){
                @Override
                public void run() {
                    if ( !mFavorite )
                        return;
                    if ( mRetrieveFullText && !mIsFullTextShown )
                        return;
                    if ( !PrefUtils.getBoolean("entry_auto_unstart_at_bottom", true) )
                        return;
                    if ( view.IsScrolAtBottom() )
                        SetIsFavourite(false);
                }
            };
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
                    float scrollPart = 0;
                    try {
                        contentText = newCursor.getString(mMobilizedHtmlPos);
                        if (contentText == null || (forceUpdate && !mIsFullTextShown)) {
                            mIsFullTextShown = false;
                            contentText = newCursor.getString(mAbstractPos);
                        } else {
                            mIsFullTextShown = true;
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
                            scrollPart = newCursor.getFloat(mScrollPosPos);

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
                            mIsFullTextShown,
                            (EntryActivity) getActivity());
                    view.setTag(newCursor);

                    if (pagerPos == mCurrentPagerPos) {
                        refreshUI(newCursor);

                        //if (PrefUtils.getLong(PrefUtils.LAST_ENTRY_ID, 0) == mEntriesIds[pagerPos]) {
                            //int dy = mScrollPosPos;
                            //if (dy > view.getScrollY())
                        if ( view.GetViewScrollPartY() < scrollPart )
                            view.mScrollPartY = scrollPart;
                        Dog.v( String.format( "displayEntry view.mScrollY  (entry %s) view.mScrollY = %f", getCurrentEntryID(),  view.mScrollPartY ) );
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
            if (view != null ) {
                return (Cursor) view.getTag();
            }
            return null;
        }

        public void setUpdatedCursor(int pagerPos, Cursor newCursor) {
            EntryView view = mEntryViews.get(pagerPos);
            if (view != null ) {
                    Cursor previousUpdatedOne = (Cursor) view.getTag();
                    if (previousUpdatedOne != null) {
                        previousUpdatedOne.close();
                    }
                view.setTag(newCursor);
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

