/**
 * FeedEx
 *
 * Copyright (c) 2012-2013 Frederic Julian
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * Some parts of this software are based on "Sparse rss" under the MIT license (see
 * below). Please refers to the original project to identify which parts are under the
 * MIT license.
 *
 * Copyright (c) 2010-2012 Stefan Handschuh
 *
 *     Permission is hereby granted, free of charge, to any person obtaining a copy
 *     of this software and associated documentation files (the "Software"), to deal
 *     in the Software without restriction, including without limitation the rights
 *     to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *     copies of the Software, and to permit persons to whom the Software is
 *     furnished to do so, subject to the following conditions:
 *
 *     The above copyright notice and this permission notice shall be included in
 *     all copies or substantial portions of the Software.
 *
 *     THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *     IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *     FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *     AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *     LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *     OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *     THE SOFTWARE.
 */

package net.fred.feedex.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.AsyncTaskLoader;
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
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateFormat;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import net.fred.feedex.Constants;
import net.fred.feedex.MainApplication;
import net.fred.feedex.R;
import net.fred.feedex.activity.BaseActivity;
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

import java.util.Date;

public class EntryFragment extends Fragment implements LoaderManager.LoaderCallbacks<EntryLoaderResult> {

    private static final int LOADER_ID = 2;

    private static final String STATE_URI = "STATE_URI";
    private static final String STATE_SCROLL_PERCENTAGE = "STATE_SCROLL_PERCENTAGE";
    private static final String STATE_ENTRIES_IDS = "STATE_ENTRIES_IDS";

    private static final long ANIM_DURATION = 250;
    private static final TranslateAnimation SLIDE_IN_RIGHT = generateAnimation(1, 0);
    private static final TranslateAnimation SLIDE_IN_LEFT = generateAnimation(-1, 0);
    private static final TranslateAnimation SLIDE_OUT_RIGHT = generateAnimation(0, 1);
    private static final TranslateAnimation SLIDE_OUT_LEFT = generateAnimation(0, -1);

    private static TranslateAnimation generateAnimation(float fromX, float toX) {
        TranslateAnimation anim = new TranslateAnimation(Animation.RELATIVE_TO_SELF, fromX, Animation.RELATIVE_TO_SELF, toX, 0, 0, 0, 0);
        anim.setDuration(ANIM_DURATION);
        return anim;
    }

    private static final String TEXT_HTML = "text/html";
    private static final String HTML_IMG_REGEX = "(?i)<[/]?[ ]?img(.|\n)*?>";

    private static final String BACKGROUND_COLOR = PrefUtils.getBoolean(PrefUtils.LIGHT_THEME, true) ? "#f6f6f6" : "#181b1f";
    private static final String TEXT_COLOR = PrefUtils.getBoolean(PrefUtils.LIGHT_THEME, true) ? "#000000" : "#C0C0C0";
    private static final String BUTTON_COLOR = PrefUtils.getBoolean(PrefUtils.LIGHT_THEME, true) ? "#D0D0D0" : "#505050";

    private static final String CSS = "<head><style type='text/css'>body {background-color:"
            + BACKGROUND_COLOR
            + "; max-width: 100%; font-family: sans-serif-light}\nimg {max-width: 100%; height: auto;}\ndiv[style] {max-width: 100%;}\npre {white-space: pre-wrap;}</style></head>";
    private static final String BODY_START = CSS + "<body link='#97ACE5' text='" + TEXT_COLOR + "'>";
    private static final String FONT_SIZE_START = CSS + BODY_START + "<font size='+";
    private static final String FONT_SIZE_MIDDLE = "'>";
    private static final String BODY_END = "<br/><br/><br/><br/></body>";
    private static final String FONT_SIZE_END = "</font>" + BODY_END;
    private static final String TITLE_START = "<p style='margin-top:1cm; margin-bottom:0.6cm'><font size='+2'><a href='";
    private static final String TITLE_MIDDLE = "' style='text-decoration: none; color:inherit'>";
    private static final String TITLE_END = "</a></font></p>";
    private static final String SUBTITLE_START = "<font size='-1'>";
    private static final String SUBTITLE_END = "</font><div style='width:100%; border:0px; height:1px; margin-top:0.1cm; background:#33b5e5'/><br/><div align='justify'>";

    private static final String BUTTON_SEPARATION = "</div><br/>";

    private static final String BUTTON_START = "<div style='text-align: center'><input type='button' value='";
    private static final String BUTTON_MIDDLE = "' onclick='";
    private static final String BUTTON_END = "' style='background-color:" + BUTTON_COLOR + "; color:" + TEXT_COLOR
            + "; border: none; border-radius:0.2cm; padding: 0.3cm;'/></div>";

    private static final String LINK_BUTTON_START = "<div style='text-align: center; margin-top:0.4cm'><a href='";
    private static final String LINK_BUTTON_MIDDLE = "' style='background-color:" + BUTTON_COLOR + "; color:" + TEXT_COLOR
            + "; text-decoration: none; border: none; border-radius:0.2cm; padding: 0.3cm;'>";
    private static final String LINK_BUTTON_END = "</a></div>";

    private static final String IMAGE_ENCLOSURE = "[@]image/";

    private int mTitlePosition = -1, mDatePosition, mMobilizedHtmlPosition, mAbstractPosition, mLinkPosition, mIsFavoritePosition,
            mEnclosurePosition, mAuthorPosition;

    private long mId = -1;
    private long mNextId = -1;
    private long mPreviousId = -1;
    private long[] mEntriesIds;
    private EntryLoaderResult mLoaderResult;

    private Uri mUri;
    private boolean mFavorite, mPreferFullText = true;

    private WebView mWebView;
    private WebView mWebView0; // only needed for the animation

    private EntryView mEntryView;

    private float mScrollPercentage = 0;

    private String mLink, mTitle, mEnclosure;
    private LayoutParams mLayoutParams;
    private View mCancelFullscreenBtn, mBackBtn, mForwardBtn;

    private GestureDetector mGestureDetector;

    final private OnTouchListener mOnTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return mGestureDetector.onTouchEvent(event);
        }
    };

    private final ThrottledContentObserver mTasksObserver = new ThrottledContentObserver(new Handler(), 2000) {
        @Override
        public void onChangeThrottled() {
            BaseActivity activity = (BaseActivity) getActivity();
            boolean isMobilizing = FetcherService.getMobilizingTaskId(mId) != -1;
            if ((activity.getProgressBar().getVisibility() == View.VISIBLE && isMobilizing)
                    || (activity.getProgressBar().getVisibility() == View.GONE && !isMobilizing)) {
                return; // no change => no update
            }

            if (isMobilizing) { // We start a mobilization
                activity.getProgressBar().setVisibility(View.VISIBLE);
            } else { // We finished one
                mPreferFullText = true;
                getLoaderManager().restartLoader(LOADER_ID, null, EntryFragment.this).forceLoad();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        if (savedInstanceState != null) {
            mUri = savedInstanceState.getParcelable(STATE_URI);
            mEntriesIds = savedInstanceState.getLongArray(STATE_ENTRIES_IDS);
            mScrollPercentage = savedInstanceState.getFloat(STATE_SCROLL_PERCENTAGE);
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_entry, container, false);

        mGestureDetector = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (Math.abs(velocityY) * 1.5 < Math.abs(velocityX)) {
                    if (velocityX > 800) {
                        if (mPreviousId != -1 && mWebView.getScrollX() == 0) {
                            previousEntry();
                        }
                    } else if (velocityX < -800) {
                        if (mNextId != -1) {
                            nextEntry();
                        }
                    }
                }

                return false;
            }
        });

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
                previousEntry();
            }
        });
        mForwardBtn = rootView.findViewById(R.id.forwardBtn);
        mForwardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextEntry();
            }
        });

        mEntryView = (EntryView) rootView.findViewById(R.id.entry);

        mLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        mWebView = new WebView(getActivity());
        setupWebview(mWebView);
        mEntryView.addView(mWebView, mLayoutParams);

        mWebView0 = new WebView(getActivity());
        setupWebview(mWebView0);

        if (mUri != null) {
            getLoaderManager().restartLoader(LOADER_ID, null, EntryFragment.this).forceLoad();
        }

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(STATE_URI, mUri);
        outState.putLongArray(STATE_ENTRIES_IDS, mEntriesIds);

        float positionTopView = mWebView.getTop();
        float contentHeight = mWebView.getContentHeight();
        float currentScrollPosition = mWebView.getScrollY();

        outState.putFloat(STATE_SCROLL_PERCENTAGE, (currentScrollPosition - positionTopView) / contentHeight);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();

        try {
            mWebView.onResume();
        } catch (Exception unused) { // Seems possible to have an NPE here on some phones...
        }

        if (getActivity().getActionBar().isShowing()) {
            mCancelFullscreenBtn.setVisibility(View.GONE);
        } else {
            mCancelFullscreenBtn.setVisibility(View.VISIBLE);
        }

        registerContentObserver();
    }

    @Override
    public void onPause() {
        super.onPause();

        try {
            mWebView.onPause();
        } catch (Exception unused) { // Seems possible to have an NPE here on some phones...
        }

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

    public void setData(Uri uri) {
        mUri = uri;
        if (mUri != null) {
            if (mWebView != null) { // If the view is already created, just load the new entry
                getLoaderManager().restartLoader(LOADER_ID, null, this).forceLoad();
            }
        } else if (mWebView != null) {
            mWebView.loadUrl("about:blank");
            mEntriesIds = null;
            mScrollPercentage = 0;
            setupNavigationButton();
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
        mId = Long.parseLong(mUri.getLastPathSegment());

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

            // loadData does not recognize the encoding without correct html-header
            boolean localPictures = contentText.contains(Constants.IMAGEID_REPLACEMENT);

            if (localPictures) {
                contentText = contentText.replace(Constants.IMAGEID_REPLACEMENT, mId + Constants.IMAGEFILE_IDSEPARATOR);
            }

            if (PrefUtils.getBoolean(PrefUtils.DISABLE_PICTURES, false)) {
                contentText = contentText.replaceAll(HTML_IMG_REGEX, "");
                mWebView.getSettings().setBlockNetworkImage(true);
            } else {
                if (mWebView.getSettings().getBlockNetworkImage()) {
                    // setBlockNetwortImage(false) calls postSync, which takes time, so we clean up the html first and change the value afterwards
                    mWebView.loadData("", TEXT_HTML, Constants.UTF8);
                    mWebView.getSettings().setBlockNetworkImage(false);
                }
            }

            String author = mLoaderResult.entryCursor.getString(mAuthorPosition);
            long timestamp = mLoaderResult.entryCursor.getLong(mDatePosition);
            mLink = mLoaderResult.entryCursor.getString(mLinkPosition);
            mTitle = mLoaderResult.entryCursor.getString(mTitlePosition);
            mEnclosure = mLoaderResult.entryCursor.getString(mEnclosurePosition);

            // String baseUrl = "";
            // try {
            // URL url = new URL(mLink);
            // baseUrl = url.getProtocol() + "://" + url.getHost();
            // } catch (MalformedURLException ignored) {
            // }
            mWebView.loadDataWithBaseURL("", generateHtmlContent(mTitle, mLink, contentText, mEnclosure, author, timestamp), TEXT_HTML, Constants.UTF8,
                    null); // do not put 'null' to the base url...

            // Listen the mobilizing task
            registerContentObserver();
        }
    }

    private String generateHtmlContent(String title, String link, String abstractText, String enclosure, String author, long timestamp) {
        StringBuilder content = new StringBuilder();

        int fontSize = Integer.parseInt(PrefUtils.getString(PrefUtils.FONT_SIZE, "0"));
        if (fontSize > 0) {
            content.append(FONT_SIZE_START).append(fontSize).append(FONT_SIZE_MIDDLE);
        } else {
            content.append(BODY_START);
        }

        if (link == null) {
            link = "";
        }
        content.append(TITLE_START).append(link).append(TITLE_MIDDLE).append(title).append(TITLE_END).append(SUBTITLE_START);
        Date date = new Date(timestamp);
        StringBuilder dateStringBuilder = new StringBuilder(DateFormat.getDateFormat(getActivity()).format(date)).append(' ').append(
                DateFormat.getTimeFormat(getActivity()).format(date));

        if (author != null && !author.isEmpty()) {
            dateStringBuilder.append(" &mdash; ").append(author);
        }
        content.append(dateStringBuilder).append(SUBTITLE_END).append(abstractText).append(BUTTON_SEPARATION).append(BUTTON_START);

        if (!mPreferFullText) {
            content.append(getString(R.string.get_full_text)).append(BUTTON_MIDDLE).append("injectedJSObject.onClickFullText();");
        } else {
            content.append(getString(R.string.original_text)).append(BUTTON_MIDDLE).append("injectedJSObject.onClickOriginalText();");
        }
        content.append(BUTTON_END);

        if (enclosure != null && enclosure.length() > 6 && !enclosure.contains(IMAGE_ENCLOSURE)) {
            content.append(BUTTON_START).append(getString(R.string.see_enclosure)).append(BUTTON_MIDDLE)
                    .append("injectedJSObject.onClickEnclosure();").append(BUTTON_END);
        }

        if (link.length() > 0) {
            content.append(LINK_BUTTON_START).append(link).append(LINK_BUTTON_MIDDLE).append(getString(R.string.see_link)).append(LINK_BUTTON_END);
        }

        if (fontSize > 0) {
            content.append(FONT_SIZE_END);
        } else {
            content.append(BODY_END);
        }

        return content.toString();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebview(final WebView wv) {
        // For color
        wv.setBackgroundColor(Color.parseColor(BACKGROUND_COLOR));

        // For gesture
        wv.setOnTouchListener(mOnTouchListener);

        // For javascript
        wv.getSettings().setJavaScriptEnabled(true);
        wv.addJavascriptInterface(injectedJSObject, "injectedJSObject");

        // For HTML5 video
        wv.setWebChromeClient(new WebChromeClient());

        wv.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (mScrollPercentage != 0) {
                    view.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            float webviewSize = wv.getContentHeight() - wv.getTop();
                            float positionInWV = webviewSize * mScrollPercentage;
                            int positionY = Math.round(wv.getTop() + positionInWV);
                            wv.scrollTo(0, positionY);
                        }
                        // Delay the scrollTo to make it work
                    }, 150);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                try {
                    // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getActivity(), R.string.cant_open_link, Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
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

    private void switchEntry(long id, Animation inAnimation, Animation outAnimation) {
        mUri = EntryColumns.PARENT_URI(mUri.getPath()).buildUpon().appendPath(String.valueOf(id)).build();
        mScrollPercentage = 0;

        WebView tmp = mWebView; // switch reference

        mWebView = mWebView0;
        mWebView0 = tmp;

        getLoaderManager().restartLoader(LOADER_ID, null, this).forceLoad();

        mEntryView.setInAnimation(inAnimation);
        mEntryView.setOutAnimation(outAnimation);
        mEntryView.addView(mWebView, mLayoutParams);
        mEntryView.showNext();
        mEntryView.removeViewAt(0);

        // To clear memory and avoid possible glitches
        mEntryView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mWebView0.loadUrl("about:blank");
            }
        }, ANIM_DURATION);
    }

    private void nextEntry() {
        switchEntry(mNextId, SLIDE_IN_RIGHT, SLIDE_OUT_LEFT);
    }

    private void previousEntry() {
        switchEntry(mPreviousId, SLIDE_IN_LEFT, SLIDE_OUT_RIGHT);
    }

    private void toggleFullScreen() {
        BaseActivity activity = (BaseActivity) getActivity();
        if (activity.getActionBar().isShowing()) {
            mCancelFullscreenBtn.setVisibility(View.VISIBLE);
        } else {
            mCancelFullscreenBtn.setVisibility(View.GONE);
        }

        activity.toggleFullScreen();
    }

    private class JavaScriptObject {
        @Override
        @JavascriptInterface
        public String toString() {
            return "injectedJSObject";
        }

        @JavascriptInterface
        public void onClickOriginalText() {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mPreferFullText = false;
                    reload(true);
                }
            });
        }

        @JavascriptInterface
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
                            cr.registerContentObserver(TaskColumns.CONTENT_URI(mobilizingTaskId), false, mTasksObserver);
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

        @JavascriptInterface
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
    }

    private final JavaScriptObject injectedJSObject = new JavaScriptObject();

    @Override
    public Loader<EntryLoaderResult> onCreateLoader(int id, Bundle args) {
        return new EntryLoader(getActivity(), mUri);
    }

    @Override
    public void onLoadFinished(Loader<EntryLoaderResult> loader, EntryLoaderResult result) {
        if (mLoaderResult != null && mLoaderResult.entryCursor != null) {
            mLoaderResult.entryCursor.close();
        }
        mLoaderResult = result;
        if (mEntriesIds == null) {
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

    @Override
    public void onLoaderReset(Loader<EntryLoaderResult> loader) {
        if (mLoaderResult != null && mLoaderResult.entryCursor != null) {
            mLoaderResult.entryCursor.close();
        }
    }
}

class EntryLoaderResult {
    Cursor entryCursor;
    long feedId;
    long[] entriesIds;
    String feedTitle;
    byte[] iconBytes;
}

class EntryLoader extends AsyncTaskLoader<EntryLoaderResult> {

    private final Uri mUri;

    public EntryLoader(Context context, Uri uri) {
        super(context);
        mUri = uri;
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

            // Mark the article as read
            if (result.entryCursor.getInt(result.entryCursor.getColumnIndex(EntryColumns.IS_READ)) != 1) {
                if (cr.update(mUri, FeedData.getReadContentValues(), null, null) > 0) {
                    FeedDataContentProvider.notifyAllFromEntryUri(mUri, false);
                }
            }

            // Get all the other entry ids (for navigation)
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

