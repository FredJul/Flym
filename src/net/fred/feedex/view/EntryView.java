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

package net.fred.feedex.view;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.widget.ViewFlipper;

import net.fred.feedex.Constants;
import net.fred.feedex.R;
import net.fred.feedex.utils.PrefUtils;

import java.util.Date;

public class EntryView extends ViewFlipper {

    public interface OnActionListener {
        public void onEntrySwitched(long newEntryId);

        public void onClickOriginalText();

        public void onClickFullText();

        public void onClickEnclosure();
    }

    private static final String STATE_SCROLL_PERCENTAGE = "STATE_SCROLL_PERCENTAGE";
    private static final String STATE_PREVIOUS_ENTRY_ID = "STATE_PREVIOUS_ENTRY_ID";
    private static final String STATE_NEXT_ENTRY_ID = "STATE_NEXT_ENTRY_ID";

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
    private static final String BUTTON_COLOR = PrefUtils.getBoolean(PrefUtils.LIGHT_THEME, true) ? "#ddecf1" : "#475773";

    private static final String CSS = "<head><meta name='viewport' content='width=device-width, initial-scale=1'><style type='text/css'>body {background-color:"
            + BACKGROUND_COLOR
            + "; max-width: 100%; font-family: sans-serif-light}\nimg {max-width: 100%; height: auto;}\ndiv[style] {max-width: 100%;}\npre {white-space: pre-wrap;}</style></head>";
    private static final String BODY_START = CSS + "<body link='#97ACE5' text='" + TEXT_COLOR + "'>";
    private static final String FONT_SIZE_START = CSS + BODY_START + "<font size='";
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
            + "; text-decoration: none; font-family: sans-serif-light; font-size: 100%; border: none; border-radius:0.2cm; padding: 0.3cm;'/></div>";

    private static final String LINK_BUTTON_START = "<div style='text-align: center; margin-top:0.4cm'><a href='";
    private static final String LINK_BUTTON_MIDDLE = "' style='background-color:" + BUTTON_COLOR + "; color:" + TEXT_COLOR
            + "; text-decoration: none; border: none; border-radius:0.2cm; padding: 0.3cm;'>";
    private static final String LINK_BUTTON_END = "</a></div>";

    private static final String IMAGE_ENCLOSURE = "[@]image/";


    private ViewGroup.LayoutParams mLayoutParams;
    private WebView mWebView;
    private WebView mWebView0; // only needed for the animation

    private long mNextId = -1;
    private long mPreviousId = -1;

    private float mScrollPercentage = 0;

    private GestureDetector mGestureDetector;

    final private OnTouchListener mOnTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return mGestureDetector.onTouchEvent(event);
        }
    };

    private OnActionListener mListener;

    private final JavaScriptObject mInjectedJSObject = new JavaScriptObject();

    public EntryView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mLayoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        mWebView = new WebView(context);
        setupWebview(mWebView);
        addView(mWebView, mLayoutParams);

        mWebView0 = new WebView(context);
        setupWebview(mWebView0);

        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
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
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("superInstanceState", super.onSaveInstanceState());

        float positionTopView = mWebView.getTop();
        float contentHeight = mWebView.getContentHeight();
        float currentScrollPosition = mWebView.getScrollY();

        bundle.putFloat(STATE_SCROLL_PERCENTAGE, (currentScrollPosition - positionTopView) / contentHeight);
        bundle.putLong(STATE_NEXT_ENTRY_ID, mNextId);
        bundle.putLong(STATE_PREVIOUS_ENTRY_ID, mPreviousId);

        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle bundle = (Bundle) state;
        mScrollPercentage = bundle.getFloat(STATE_SCROLL_PERCENTAGE);
        mNextId = bundle.getLong(STATE_NEXT_ENTRY_ID);
        mPreviousId = bundle.getLong(STATE_PREVIOUS_ENTRY_ID);
        super.onRestoreInstanceState(bundle.getParcelable("superInstanceState"));
    }

    public void setListener(OnActionListener listener) {
        mListener = listener;
    }

    public void onResume() {
        mWebView.onResume();
        mWebView0.onResume();
    }

    public void onPause() {
        mWebView.onPause();
        mWebView0.onPause();
    }

    public void onDestroy() {
        mWebView.loadUrl("about:blank");
    }

    public void reset() {
        mWebView.loadUrl("about:blank");
        mScrollPercentage = 0;
    }

    public void setHtml(long entryId, long nextId, long previousId, String title, String link, String contentText, String enclosure, String author, long timestamp, boolean preferFullText) {
        mNextId = nextId;
        mPreviousId = previousId;

        // loadData does not recognize the encoding without correct html-header
        boolean localPictures = contentText.contains(Constants.IMAGEID_REPLACEMENT);

        if (localPictures) {
            contentText = contentText.replace(Constants.IMAGEID_REPLACEMENT, entryId + Constants.IMAGEFILE_IDSEPARATOR);
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

        // String baseUrl = "";
        // try {
        // URL url = new URL(mLink);
        // baseUrl = url.getProtocol() + "://" + url.getHost();
        // } catch (MalformedURLException ignored) {
        // }

        // do not put 'null' to the base url...
        mWebView.loadDataWithBaseURL("", generateHtmlContent(title, link, contentText, enclosure, author, timestamp, preferFullText), TEXT_HTML, Constants.UTF8, null);
    }

    public void nextEntry() {
        switchEntry(mNextId, SLIDE_IN_RIGHT, SLIDE_OUT_LEFT);
    }

    public void previousEntry() {
        switchEntry(mPreviousId, SLIDE_IN_LEFT, SLIDE_OUT_RIGHT);
    }

    private String generateHtmlContent(String title, String link, String contentText, String enclosure, String author, long timestamp, boolean preferFullText) {
        StringBuilder content = new StringBuilder();

        int fontSize = Integer.parseInt(PrefUtils.getString(PrefUtils.FONT_SIZE, "0"));
        if (fontSize != 0) {
            content.append(FONT_SIZE_START).append(fontSize > 0 ? "+" : "").append(fontSize).append(FONT_SIZE_MIDDLE);
        } else {
            content.append(BODY_START);
        }

        if (link == null) {
            link = "";
        }
        content.append(TITLE_START).append(link).append(TITLE_MIDDLE).append(title).append(TITLE_END).append(SUBTITLE_START);
        Date date = new Date(timestamp);
        Context context = getContext();
        StringBuilder dateStringBuilder = new StringBuilder(DateFormat.getDateFormat(context).format(date)).append(' ').append(
                DateFormat.getTimeFormat(context).format(date));

        if (author != null && !author.isEmpty()) {
            dateStringBuilder.append(" &mdash; ").append(author);
        }
        content.append(dateStringBuilder).append(SUBTITLE_END).append(contentText).append(BUTTON_SEPARATION).append(BUTTON_START);

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

        if (link.length() > 0) {
            content.append(LINK_BUTTON_START).append(link).append(LINK_BUTTON_MIDDLE).append(context.getString(R.string.see_link)).append(LINK_BUTTON_END);
        }

        if (fontSize != 0) {
            content.append(FONT_SIZE_END);
        } else {
            content.append(BODY_END);
        }

        return content.toString();
    }

    private void switchEntry(long newEntryId, Animation inAnimation, Animation outAnimation) {
        mScrollPercentage = 0;

        WebView tmp = mWebView; // switch reference

        mWebView = mWebView0;
        mWebView0 = tmp;

        setInAnimation(inAnimation);
        setOutAnimation(outAnimation);
        addView(mWebView, mLayoutParams);
        showNext();
        removeViewAt(0);

        // To clear memory and avoid possible glitches
        postDelayed(new Runnable() {
            @Override
            public void run() {
                mWebView0.loadUrl("about:blank");
            }
        }, ANIM_DURATION);

        mListener.onEntrySwitched(newEntryId);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebview(final WebView wv) {
        // For zoom
        wv.getSettings().setSupportZoom(true);
        wv.getSettings().setBuiltInZoomControls(true);
        wv.getSettings().setDisplayZoomControls(false);
        // For color
        wv.setBackgroundColor(Color.parseColor(BACKGROUND_COLOR));
        // For gesture
        wv.setOnTouchListener(mOnTouchListener);

        // For javascript
        wv.getSettings().setJavaScriptEnabled(true);
        wv.addJavascriptInterface(mInjectedJSObject, mInjectedJSObject.toString());

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
                Context context = getContext();
                try {
                    // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    context.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(context, R.string.cant_open_link, Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
    }

    private class JavaScriptObject {
        @Override
        @JavascriptInterface
        public String toString() {
            return "injectedJSObject";
        }

        @JavascriptInterface
        public void onClickOriginalText() {
            mListener.onClickOriginalText();
        }

        @JavascriptInterface
        public void onClickFullText() {
            mListener.onClickFullText();
        }

        @JavascriptInterface
        public void onClickEnclosure() {
            mListener.onClickEnclosure();
        }
    }
}
