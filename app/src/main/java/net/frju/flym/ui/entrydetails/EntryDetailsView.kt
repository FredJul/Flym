/**
 * Flym
 *
 * Copyright (c) 2012-2015 Frederic Julian
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
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package net.frju.flym.ui.entrydetails

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.util.AttributeSet
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import net.fred.feedex.R
import net.frju.flym.data.entities.EntryWithFeed
import net.frju.flym.utils.FILE_SCHEME
import net.frju.flym.utils.HtmlUtils
import net.frju.flym.utils.UTF8
import net.frju.parentalcontrol.utils.PrefUtils
import java.io.File
import java.io.IOException

class EntryDetailsView : WebView {

    private val TEXT_HTML = "text/html"
    private val HTML_IMG_REGEX = "(?i)<[/]?[ ]?img(.|\n)*?>"
    private val BACKGROUND_COLOR = "#FF2B2B2B"
    private val QUOTE_BACKGROUND_COLOR = "#383b3f"
    private val QUOTE_LEFT_COLOR = "#686b6f"
    private val TEXT_COLOR = "#C0C0C0"
    private val SUBTITLE_COLOR = "#8c8c8c"
    private val SUBTITLE_BORDER_COLOR = "solid #303030"
    private val CSS = "<head><style type='text/css'> " +
            "body {max-width: 100%; margin: 0.3cm; font-family: sans-serif-light; color: " + TEXT_COLOR + "; background-color:" + BACKGROUND_COLOR + "; line-height: 150%} " +
            "* {max-width: 100%; word-break: break-word}" +
            "h1, h2 {font-weight: normal; line-height: 130%} " +
            "h1 {font-size: 170%; margin-bottom: 0.1em} " +
            "h2 {font-size: 140%} " +
            "a {color: #0099CC}" +
            "h1 a {color: inherit; text-decoration: none}" +
            "img {height: auto} " +
            "pre {white-space: pre-wrap;} " +
            "blockquote {border-left: thick solid " + QUOTE_LEFT_COLOR + "; background-color:" + QUOTE_BACKGROUND_COLOR + "; margin: 0.5em 0 0.5em 0em; padding: 0.5em} " +
            "p {margin: 0.8em 0 0.8em 0} " +
            "p.subtitle {color: " + SUBTITLE_COLOR + "; border-top:1px " + SUBTITLE_BORDER_COLOR + "; border-bottom:1px " + SUBTITLE_BORDER_COLOR + "; padding-top:2px; padding-bottom:2px; font-weight:800 } " +
            "ul, ol {margin: 0 0 0.8em 0.6em; padding: 0 0 0 1em} " +
            "ul li, ol li {margin: 0 0 0.8em 0; padding: 0} " +
            "</style><meta name='viewport' content='width=device-width'/></head>"
    private val BODY_START = "<body>"
    private val BODY_END = "</body>"

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun init() {
        // For scrolling
        isHorizontalScrollBarEnabled = false
        settings.useWideViewPort = false
        settings.cacheMode = WebSettings.LOAD_NO_CACHE
        settings.javaScriptEnabled = true

        // For color
        setBackgroundColor(Color.parseColor(BACKGROUND_COLOR))

        // Text zoom level from preferences
        val fontSize = Integer.parseInt(PrefUtils.getString(PrefUtils.FONT_SIZE, "0"))
        if (fontSize != 0) {
            settings.textZoom = 100 + fontSize * 20
        }

        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                val context = context
                try {
                    //TODO
                    if (url.startsWith(FILE_SCHEME)) {
                        val file = File(url.replace(FILE_SCHEME, ""))
                        val extTmpFile = File(context.externalCacheDir, "tmp_img.jpg")
                        file.copyTo(extTmpFile, true)
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.setDataAndType(Uri.fromFile(extTmpFile), "image/jpeg")
                        context.startActivity(intent)
                    } else {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, R.string.cant_open_link, Toast.LENGTH_SHORT).show()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                return true
            }
        }
    }

    fun setEntry(entry: EntryWithFeed?) {
        if (entry == null) {
            loadDataWithBaseURL("", "", TEXT_HTML, UTF8, null)
        } else {
            // TODO dynamic switch
            var contentText = entry.mobilizedContent ?: entry.description ?: ""
            if (PrefUtils.getBoolean(PrefUtils.DISPLAY_IMAGES, true)) {
                contentText = HtmlUtils.replaceImageURLs(contentText, entry.id)
                if (settings.blockNetworkImage) {
                    // setBlockNetworkImage(false) calls postSync, which takes time, so we clean up the html first and change the value afterwards
                    loadData("", TEXT_HTML, UTF8)
                    settings.blockNetworkImage = false
                }
            } else {
                contentText = contentText.replace(HTML_IMG_REGEX.toRegex(), "")
                settings.blockNetworkImage = true
            }

            val html = StringBuilder(CSS)
                    .append(BODY_START)
                    .append(contentText)
                    .append(BODY_END)
                    .toString()

            // do not put 'null' to the base url...
            loadDataWithBaseURL("", html, TEXT_HTML, UTF8, null)
        }
    }
}