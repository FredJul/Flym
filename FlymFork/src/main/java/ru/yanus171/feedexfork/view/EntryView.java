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

package ru.yanus171.feedexfork.view;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.activity.EntryActivity;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.FileUtils;
import ru.yanus171.feedexfork.utils.HtmlUtils;
import ru.yanus171.feedexfork.utils.PrefUtils;

public class EntryView extends WebView implements Observer {

    private static final String TEXT_HTML = "text/html";
    private static final String HTML_IMG_REGEX = "(?i)<[/]?[ ]?img(.|\n)*?>";
    private static final String BACKGROUND_COLOR = PrefUtils.getBoolean(PrefUtils.LIGHT_THEME, false) ? "#f6f6f6" : "#181b1f";
    private static final String QUOTE_BACKGROUND_COLOR = PrefUtils.getBoolean(PrefUtils.LIGHT_THEME, false) ? "#e6e6e6" : "#383b3f";
    private static final String QUOTE_LEFT_COLOR = PrefUtils.getBoolean(PrefUtils.LIGHT_THEME, false) ? "#a6a6a6" : "#686b6f";
    private long mEntryId = -1;

    //private static final String TEXT_COLOR_BRIGHTNESS = PrefUtils.getBoolean(PrefUtils.LIGHT_THEME, false) ? "#000000" : "#C0C0C0";
    private static String GetTextColor() {return PrefUtils.getBoolean(PrefUtils.LIGHT_THEME, false) ? "#000000" : getTextColorDarkTheme();}

    private static String getTextColorDarkTheme() {

        int b = 200;
        try {
            b = Integer.parseInt( PrefUtils.getString(PrefUtils.TEXT_COLOR_BRIGHTNESS, "200") );
        } catch (NumberFormatException e) {

        }
        return "#" + Integer.toHexString( Color.argb( 255, b, b, b ) ).substring( 2 );
    }

    private static final String BUTTON_COLOR = PrefUtils.getBoolean(PrefUtils.LIGHT_THEME, false) ? "#52A7DF" : "#1A5A81";
    private static final String SUBTITLE_COLOR = PrefUtils.getBoolean(PrefUtils.LIGHT_THEME, false) ? "#666666" : "#8c8c8c";
    private static final String SUBTITLE_BORDER_COLOR = PrefUtils.getBoolean(PrefUtils.LIGHT_THEME, false) ? "solid #ddd" : "solid #303030";
    public static String GetCSS() { return "<head><style type='text/css'> "
            + "body {max-width: 100%; margin: 0.1cm; text-align:justify; font-weight: " + getFontBold() + " color: " + GetTextColor() + "; background-color:" + BACKGROUND_COLOR + "; line-height: 120%} "
            + "* {max-width: 100%; word-break: break-word}"
            + "h1, h2 {font-weight: normal; line-height: 130%} "
            + "h1 {font-size: 170%; text-align:center; margin-bottom: 0.1em} "
            + "h2 {font-size: 140%} "
            + "a {color: #0099CC}"
            + "h1 a {color: inherit; text-decoration: none}"
            + "img {display: inline;max-width: 100%;height: auto} "
            + "iframe {allowfullscreen;position:relative;top:0;left:0;width:100%;height:100%;}"
            + "pre {white-space: pre-wrap;} "
            + "blockquote {border-left: thick solid " + QUOTE_LEFT_COLOR + "; background-color:" + QUOTE_BACKGROUND_COLOR + "; margin: 0.5em 0 0.5em 0em; padding: 0.5em} "
            + "p {margin: 0.8em 0 0.8em 0} "
            + "p.subtitle {color: " + SUBTITLE_COLOR + "; border-top:1px " + SUBTITLE_BORDER_COLOR + "; border-bottom:1px " + SUBTITLE_BORDER_COLOR + "; padding-top:2px; padding-bottom:2px; font-weight:800 } "
            + "ul, ol {margin: 0 0 0.8em 0.6em; padding: 0 0 0 1em} "
            + "ul li, ol li {margin: 0 0 0.8em 0; padding: 0} "
            + "div.button-section {padding: 0.4cm 0; margin: 0; text-align: center} "
            + ".button-section p {margin: 0.1cm 0 0.2cm 0}"
            + ".button-section p.marginfix {margin: 0.2cm 0 0.2cm 0}"
            + ".button-section input, .button-section a {font-family: sans-serif-light; font-size: 100%; color: #FFFFFF; background-color: " + BUTTON_COLOR + "; text-decoration: none; border: none; border-radius:0.2cm; padding: 0.3cm} "
            + "</style><meta name='viewport' content='width=device-width'/></head>"; }

    private static String getFontBold() {
        if ( PrefUtils.getBoolean( PrefUtils.ENTRY_FONT_BOLD, false ) )
            return "bold;";
        else
            return "normal;";
    }

    private static final String BODY_START = "<body>";
    private static final String BODY_END = "</body>";
    private static final String TITLE_START = "<h1><a href='";
    private static final String TITLE_MIDDLE = "'>";
    private static final String TITLE_END = "</a></h1>";
    private static final String SUBTITLE_START = "<p class='subtitle'>";
    private static final String SUBTITLE_END = "</p>";
    static final String BUTTON_SECTION_START = "<div class='button-section'>";
    static final String BUTTON_SECTION_END = "</div>";
    private static final String BUTTON_START = "<p><input type='button' value='";
    private static final String BUTTON_MIDDLE = "' onclick='";
    private static final String BUTTON_END = "'/></p>";
    // the separate 'marginfix' selector in the following is only needed because the CSS box model treats <input> and <a> elements differently
    private static final String LINK_BUTTON_START = "<p class='marginfix'><a href='";
    private static final String LINK_BUTTON_MIDDLE = "'>";
    private static final String LINK_BUTTON_END = "</a></p>";
    private static final String IMAGE_ENCLOSURE = "[@]image/";

    private final JavaScriptObject mInjectedJSObject = new JavaScriptObject();
    private final ImageDownloadJavaScriptObject mImageDownloadObject = new ImageDownloadJavaScriptObject();
    public static final ImageDownloadObservable mImageDownloadObservable = new ImageDownloadObservable();
    private EntryViewManager mEntryViewMgr;
    public static Handler mHandler = null;
    public String mData = "";
    public int mScrollY = 0;

    private EntryActivity mActivity;


    public EntryView(Context context) {
        super(context);
        init();
    }

    public EntryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EntryView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void setListener(EntryViewManager manager) {
        mEntryViewMgr = manager;
    }

    public void setHtml(long entryId,
                        String title,
                        String link,
                        String contentText,
                        String enclosure,
                        String author,
                        long timestamp,
                        boolean preferFullText,
                        EntryActivity activity) {
        mActivity = activity;
        mEntryId = entryId;
        //getSettings().setBlockNetworkLoads(true);
        getSettings().setUseWideViewPort( true );
        getSettings().setSupportZoom( false );
        getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        if (PrefUtils.getBoolean(PrefUtils.DISPLAY_IMAGES, true)) {
            contentText = HtmlUtils.replaceImageURLs(contentText, entryId);
            if (getSettings().getBlockNetworkImage()) {
                // setBlockNetworkImage(false) calls postSync, which takes time, so we clean up the html first and change the value afterwards
                loadData("", TEXT_HTML, Constants.UTF8);
                getSettings().setBlockNetworkImage(false);
            }
        } else {
            contentText = contentText.replaceAll(HTML_IMG_REGEX, "");
            getSettings().setBlockNetworkImage(true);
        }


        // String baseUrl = "";
        // try {
        // URL url = new URL(mLink);
        // baseUrl = url.getProtocol() + "://" + url.getHost();
        // } catch (MalformedURLException ignored) {
        // }
        // do not put 'null' to the base url...
        mData = generateHtmlContent(title, link, contentText, enclosure, author, timestamp, preferFullText);
        if ( getScrollY() != 0 )
            mScrollY = getScrollY();
        loadDataWithBaseURL("", mData, TEXT_HTML, Constants.UTF8, null);
    }

    private String generateHtmlContent(String title, String link, String contentText, String enclosure, String author, long timestamp, boolean preferFullText) {
        StringBuilder content = new StringBuilder(GetCSS()).append(BODY_START);

        if (link == null) {
            link = "";
        }
        content.append(TITLE_START).append(link).append(TITLE_MIDDLE).append(title).append(TITLE_END).append(SUBTITLE_START);
        Date date = new Date(timestamp);
        Context context = getContext();
        StringBuilder dateStringBuilder = new StringBuilder(DateFormat.getLongDateFormat(context).format(date)).append(' ').append(
                DateFormat.getTimeFormat(context).format(date));

        if (author != null && !author.isEmpty()) {
            dateStringBuilder.append(" &mdash; ").append(author);
        }

        content.append(dateStringBuilder).append(SUBTITLE_END).append(contentText).append(BUTTON_SECTION_START).append(BUTTON_START);

        if (!preferFullText) {
            content.append(context.getString(R.string.get_full_text)).append(BUTTON_MIDDLE).append("injectedJSObject.onClickFullText();");
        } else {
            content.append(context.getString(R.string.original_text)).append(BUTTON_MIDDLE).append("injectedJSObject.onClickOriginalText();");
        }
        content.append(BUTTON_END);

        if (enclosure != null && enclosure.length() > 6 && !enclosure.contains(IMAGE_ENCLOSURE)) {
            content.append(BUTTON_START).append(context.getString(R.string.see_enclosure)).append(BUTTON_MIDDLE)
                    .append("injectedJSObject.onClickEnclosure();").append(BUTTON_END);
        }

        /*if (link.length() > 0) {
            content.append(LINK_BUTTON_START).append(link).append(LINK_BUTTON_MIDDLE).append(context.getString(R.string.see_link)).append(LINK_BUTTON_END);
        }*/

        content.append(BUTTON_SECTION_END).append(BODY_END);

        return content.toString();
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void init() {
        if ( mHandler == null )
            mHandler = new Handler();
        mImageDownloadObservable.addObserver(this);
        // For scrolling
        setHorizontalScrollBarEnabled(false);
        getSettings().setUseWideViewPort(true);

        // For color
        setBackgroundColor(Color.parseColor(BACKGROUND_COLOR));

        // Text zoom level from preferences
        int fontSize = PrefUtils.getFontSize();
        if (fontSize != 0) {
            getSettings().setTextZoom(100 + (fontSize * 20));
        }

        // For javascript
        getSettings().setJavaScriptEnabled(true);
        addJavascriptInterface(mInjectedJSObject, mInjectedJSObject.toString());
        addJavascriptInterface(mImageDownloadObject, mImageDownloadObject.toString());

        // For HTML5 video
        setWebChromeClient(new WebChromeClient() {
            private View mCustomView;
            private WebChromeClient.CustomViewCallback mCustomViewCallback;

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                // if a view already exists then immediately terminate the new one
                if (mCustomView != null) {
                    callback.onCustomViewHidden();
                    return;
                }

                FrameLayout videoLayout = mEntryViewMgr.getVideoLayout();
                if (videoLayout != null) {
                    mCustomView = view;

                    setVisibility(View.GONE);
                    videoLayout.setVisibility(View.VISIBLE);
                    videoLayout.addView(view);
                    mCustomViewCallback = callback;

                    mEntryViewMgr.onStartVideoFullScreen();
                }
            }

            @Override
            public void onHideCustomView() {
                super.onHideCustomView();

                if (mCustomView == null) {
                    return;
                }

                FrameLayout videoLayout = mEntryViewMgr.getVideoLayout();
                if (videoLayout != null) {
                    setVisibility(View.VISIBLE);
                    videoLayout.setVisibility(View.GONE);

                    // HideByScroll the custom view.
                    mCustomView.setVisibility(View.GONE);

                    // Remove the custom view from its container.
                    videoLayout.removeView(mCustomView);
                    mCustomViewCallback.onCustomViewHidden();

                    mCustomView = null;

                    mEntryViewMgr.onEndVideoFullScreen();
                }
            }
        });


        setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Context context = getContext();
                try {
                    if (url.startsWith(Constants.FILE_SCHEME)) {
                        File file = new File(url.replace(Constants.FILE_SCHEME, ""));
                        File extTmpFile = new File(context.getExternalCacheDir(), "tmp_img.jpg");
                        FileUtils.copy(file, extTmpFile);
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.fromFile(extTmpFile), "image/jpeg");
                        context.startActivity(intent);
                    } else {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        context.startActivity(intent);
                    }
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(context, R.string.cant_open_link, Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.v("main", "EntryView.this.scrollTo " + mScrollY);
                if (mScrollY != 0) {
                    //EntryView.this.scrollTo(0, mScrollY);
                    view.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //float webviewsize = mWebView.getContentHeight() - mWebView.getTop();
                            //float positionInWV = webviewsize * mProgressToRestore;
                            //int positionY = Math.round(mWebView.getTop() + positionInWV);
                            EntryView.this.scrollTo(0, mScrollY);
                        }
                        // Delay the scrollTo to make it work
                    }, 150);
                }
            }
        });


    }


    /*@Override
    public void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
        if ( scrollY > 50 && clampedY ) {
            mActivity.setFullScreen(false, EntryActivity.GetIsActionBarHidden());
        }

    }*/

    @Override
    protected void onScrollChanged (int l, int t, int oldl, int oldt) {
        FetcherService.getStatusText().HideByScroll();
        int height = (int) Math.floor(getContentHeight() * getScale());
        int webViewHeight = getMeasuredHeight();
        if(getScrollY() + webViewHeight >= height){
            if ( EntryActivity.GetIsStatusBarHidden() )
                mActivity.setFullScreenWithNavBar();//setFullScreen(false, EntryActivity.GetIsActionBarHidden());
        }
        mActivity.mEntryFragment.UpdateProgress();
        mActivity.mEntryFragment.UpdateClock();
    }


    @Override
    public void update(Observable observable, Object data) {
        if ( ( data != null ) && ( (Long)data == mEntryId ) )  {
            if (getScrollY() != 0)
                mScrollY = getScrollY();
            mData = HtmlUtils.replaceImageURLs(mData, mEntryId);
            loadDataWithBaseURL("", mData, TEXT_HTML, Constants.UTF8, null);
        //setScrollY( y );
        }
    }

    static volatile boolean mNotifyInProcess = false;
    public static void NotifyToUpdate( final long entryId) {
        synchronized ( mImageDownloadObservable ) {
            if (mHandler != null && !mNotifyInProcess) {
                mNotifyInProcess = true;
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        synchronized ( mImageDownloadObservable ) {
                            mNotifyInProcess = false;
                            mImageDownloadObservable.notifyObservers(entryId);
                        }
                    }
                }, 1000);
            }
        }
    }

    public interface EntryViewManager {
        void onClickOriginalText();

        void onClickFullText();

        void onClickEnclosure();

        void onStartVideoFullScreen();

        void onEndVideoFullScreen();

        FrameLayout getVideoLayout();

        void downloadImage(String url);

        void downloadNextImages();
    }

    private class JavaScriptObject {
        @Override
        @JavascriptInterface
        public String toString() {
            return "injectedJSObject";
        }

        @JavascriptInterface
        public void onClickOriginalText() {
            mEntryViewMgr.onClickOriginalText();
        }

        @JavascriptInterface
        public void onClickFullText() {
            mEntryViewMgr.onClickFullText();
        }

        @JavascriptInterface
        public void onClickEnclosure() {
            mEntryViewMgr.onClickEnclosure();
        }
    }

    private class ImageDownloadJavaScriptObject {
        @Override
        @JavascriptInterface
        public String toString() {
            return "ImageDownloadJavaScriptObject";
        }
        @JavascriptInterface
        public void downloadImage( String url ) {
            mEntryViewMgr.downloadImage(url);
        }
        @JavascriptInterface
        public void downloadNextImages() {
            mEntryViewMgr.downloadNextImages();
        }
    }

    public static class ImageDownloadObservable extends Observable {
        @Override
        public boolean hasChanged () {
            return true;
        }
    }



}