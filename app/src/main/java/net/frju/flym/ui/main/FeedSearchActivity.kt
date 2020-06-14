package net.frju.flym.ui.main

import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.webkit.URLUtil
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import net.fred.feedex.R
import net.frju.flym.data.entities.SearchFeedResult
import net.frju.flym.service.FetcherService
import org.jetbrains.anko.sdk21.listeners.textChangedListener
import org.json.JSONException
import org.json.JSONObject
import java.net.URLEncoder
import java.util.*


class FeedSearchActivity : AppCompatActivity() {

    private val TAG = "FeedSearchActivity"

    private val FEED_SEARCH_TITLE = "title"
    private val FEED_SEARCH_URL = "feedId"
    private val FEED_SEARCH_DESC = "description"
    private val FEED_SEARCH_BLACKLIST = arrayOf("http://syndication.lesechos.fr/rss/rss_finance.xml")

    private val GNEWS_TOPIC_NAME = intArrayOf(R.string.google_news_top_stories, R.string.google_news_world, R.string.google_news_business, R.string.google_news_science_technology, R.string.google_news_entertainment, R.string.google_news_sports, R.string.google_news_health)
    private val GNEWS_TOPIC_CODE = arrayOf("", "WORLD", "BUSINESS", "SCITECH", "ENTERTAINMENT", "SPORTS", "HEALTH")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feed_search)

        this.watchSearchInput()
    }

    private fun watchSearchInput(){
        val searchInput = this.findViewById<EditText>(R.id.et_search_input)
        var timer: Timer = Timer()

        searchInput.textChangedListener {
            afterTextChanged {
                val term = searchInput.text.toString().trim()
                if (term.isNotEmpty()) {
                    timer.cancel()
                    timer = Timer()
                    timer.schedule(object: TimerTask() {
                        override fun run() {
                            searchForFeed(term)
                        }
                    }, 2000)

                }

            }
        }
    }

    private fun getFeedlySearchUrl(term:String) : String {
        return Uri.Builder()
                .scheme("https")
                .authority("cloud.feedly.com")
                .path("/v3/search/feeds")
                .appendQueryParameter("count", "20")
                .appendQueryParameter("locale", resources.configuration.locale.language)
                .appendQueryParameter("query", URLEncoder.encode(term, "UTF-8"))
                .build()
                .toString()
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

    private fun searchForFeed(term:String){

        val array = ArrayList<SearchFeedResult>()
        if (URLUtil.isNetworkUrl(term)) {
            FetcherService.createCall(term).execute().use { response ->
                val feed = SyndFeedInput().build(XmlReader(response.body!!.byteStream()))
                val feedTitle = feed.title?: term
                val feedDescription = feed.description?: ""
                val feedResult = SearchFeedResult(term, feedTitle, feedDescription)
                Log.d(TAG, feedResult.toString())
                array.add(feedResult)
            }

        } else {
            @Suppress("DEPRECATION")
            FetcherService.createCall(getFeedlySearchUrl(term)).execute().use {
                it.body?.let { body ->
                    val json = JSONObject(body.string())
                    val entries = json.getJSONArray("results")

                    for (i in 0 until entries.length()) {
                        try {
                            val entry = entries.get(i) as JSONObject
                            val url = entry.get(FEED_SEARCH_URL).toString().replace("feed/", "")
                            if (url.isNotEmpty() && !FEED_SEARCH_BLACKLIST.contains(url)) {
                                @Suppress("DEPRECATION")
                                val feedTitle = Html.fromHtml(entry.get(FEED_SEARCH_TITLE).toString()).toString()
                                val feedDescription = Html.fromHtml(entry.get(FEED_SEARCH_DESC).toString()).toString()
                                val feedResult = SearchFeedResult(url, feedTitle, feedDescription)
                                Log.d(TAG, feedResult.toString())
                                array.add(feedResult)
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

    }
}