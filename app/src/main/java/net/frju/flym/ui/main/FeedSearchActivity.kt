package net.frju.flym.ui.main

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.webkit.URLUtil
import android.widget.EditText
import net.fred.feedex.R
import org.jetbrains.anko.sdk21.listeners.textChangedListener
import java.net.URLEncoder


class FeedSearchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feed_search)

        this.watchSearchInput()
    }

    private fun watchSearchInput(){
        val searchInput = this.findViewById<EditText>(R.id.et_search_input)

        searchInput.textChangedListener {
            afterTextChanged {
                val term = searchInput.text.toString().trim()
                if (term.isNotEmpty()) {
                    val searchUrl = getSearchUrl(term)
                    Log.d("FeedSearchActivity", searchUrl)
                }

            }
        }
    }

    private fun getSearchUrl(term:String) : String {
        return if (URLUtil.isNetworkUrl(term)) {
            term
        }else{
            Uri.Builder()
                    .scheme("https")
                    .authority("cloud.feedly.com")
                    .path("/v3/search/feeds")
                    .appendQueryParameter("count", "20")
                    .appendQueryParameter("locale", resources.configuration.locale.language)
                    .appendQueryParameter("query", URLEncoder.encode(term, "UTF-8"))
                    .build()
                    .toString()
        }
    }

    private fun getGoogleNewsRssUrl(topic:String) : String {
        return Uri.Builder()
                .scheme("https")
                .authority("news.google.com")
                .path(if (topic.isNullOrEmpty()) "/news/rss/" else "/news/rss/headlines/section/topic/")
                .appendQueryParameter("ned", this.resources.configuration.locale.language)
                .build()
                .toString()
    }

}