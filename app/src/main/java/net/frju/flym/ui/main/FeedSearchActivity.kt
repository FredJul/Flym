package net.frju.flym.ui.main

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.URLUtil
import android.widget.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.toSpannable
import net.fred.feedex.R
import net.frju.flym.App
import net.frju.flym.data.entities.Feed
import net.frju.flym.data.entities.SearchFeedResult
import net.frju.flym.service.FetcherService
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.layoutInflater
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.sdk21.listeners.onClick
import org.jetbrains.anko.sdk21.listeners.onEditorAction
import org.jetbrains.anko.sdk21.listeners.textChangedListener
import org.json.JSONException
import org.json.JSONObject
import java.net.URLEncoder
import java.util.*
import kotlin.collections.ArrayList


class FeedSearchActivity : AppCompatActivity(), AdapterView.OnItemClickListener {

    private val TAG = "FeedSearchActivity"

    private val FEED_SEARCH_TITLE = "title"
    private val FEED_SEARCH_URL = "feedId"
    private val FEED_SEARCH_DESC = "description"
    private val FEED_SEARCH_BLACKLIST = arrayOf("http://syndication.lesechos.fr/rss/rss_finance.xml")

    private var mResultsListView: ListView? = null
    private var mSearchInput: AutoCompleteTextView? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feed_search)

        this.initSearchListView()
        this.initSearchInputs()
    }

    private fun initSearchListView(){
        mResultsListView = this.findViewById(R.id.lv_search_results)
        mResultsListView?.adapter = SearchResultsAdapter(this, ArrayList<SearchFeedResult>())
        mResultsListView?.onItemClickListener = this
    }

    private fun initSearchInputs(){
        var timer = Timer()
        mSearchInput = this.findViewById(R.id.et_search_input)
        mSearchInput?.let { searchInput ->

            // Handle IME Options Search Action
            searchInput.onEditorAction { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    val term = searchInput.text.toString().trim()
                    searchForFeed(term)
                    true
                }
                false
            }

            // Handle search after N ms pause after input
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
                        }, 400)
                    }
                }
            }

            // Handle manually adding URL
            this.findViewById<Button>(R.id.btn_add_feed).onClick {
                val text = searchInput.text.toString()
                if (URLUtil.isNetworkUrl(text)) {
                    addFeed(searchInput, text, text)
                }else{
                    // Todo: Swap to string resource
                    searchInput.snackbar("Please select from the results or provide a valid URL")
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

    @Suppress("DEPRECATION")
    private fun searchForFeed(term:String){

        val array = ArrayList<SearchFeedResult>()
        try {
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
                                val feedAdded = App.db.feedDao().findByLink(url) != null
                                val feedResult = SearchFeedResult(url, feedTitle, feedDescription, feedAdded)
                                Log.d(TAG, feedResult.toString())
                                array.add(feedResult)
                            }
                            this.runOnUiThread(Runnable {
                                (mResultsListView?.adapter as SearchResultsAdapter).updateData(term, array)
                            })

                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun addFeed(view:View, title:String, link:String){
        doAsync {
            if (App.db.feedDao().findByLink(link) != null) {
                runOnUiThread {
                    view.snackbar(getString(R.string.feed_already_added, link, title))
                }
            }else{
                val feedToAdd = Feed(link = link, title = title)
                App.db.feedDao().insert(feedToAdd)
                runOnUiThread {
                    mSearchInput?.setText("")
                    view.snackbar(getString(R.string.feed_added))
                }
            }
        }
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        view?.let {vw ->
            val feedAdded = vw.findViewById<ImageView>(R.id.iv_feed_added)
            val item = parent?.getItemAtPosition(position) as SearchFeedResult
            if (item.isAdded){
                // Remove
                AlertDialog.Builder(this)
                        .setTitle(item.title)
                        .setMessage(R.string.question_delete_feed)
                        .setPositiveButton(android.R.string.yes) { _, _ ->
                            doAsync {
                                App.db.feedDao().deleteByLink(item.link)
                                item.isAdded = false
                            }
                            feedAdded?.visibility = View.INVISIBLE
                        }.setNegativeButton(android.R.string.no, null)
                        .show()
            }else{
                // Add
                feedAdded?.visibility = View.VISIBLE
                item.isAdded = true
                addFeed(vw, item.title, item.link)
            }
        }
    }

    class SearchResultsAdapter(context: Context, items: ArrayList<SearchFeedResult>) :
            ArrayAdapter<SearchFeedResult>(context, R.layout.item_feed_search_result, items) {

        private var mSearchTerm: String? = null

        private class ItemViewHolder {
            internal var feedAdded: ImageView? = null
            internal var title: TextView? = null
            internal var description: TextView? = null
        }

        fun updateData(searchTerm: String, items: ArrayList<SearchFeedResult>){
            this.mSearchTerm = searchTerm
            this.clear()
            this.addAll(items)
            this.notifyDataSetChanged()
        }

        private fun setTitle(viewHolder: ItemViewHolder, item: SearchFeedResult){
            mSearchTerm?.let { term ->
                val start = item.title.toLowerCase(Locale.ROOT).indexOf(term.toLowerCase(Locale.ROOT))
                if (start != -1) {
                    val end = start + term.length
                    val spannable = item.title.toSpannable()
                    val color = ContextCompat.getColor(context, R.color.color_highlight)
                    spannable.setSpan(ForegroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    viewHolder.title?.text = spannable
                    return
                }
            }
            viewHolder.title?.text = item?.title
        }

        private fun setDescription(viewHolder: ItemViewHolder, item: SearchFeedResult){
            viewHolder.description?.text = item.desc
        }

        private fun setFeedAdded(viewHolder: ItemViewHolder, item: SearchFeedResult){
            viewHolder.feedAdded?.visibility = if (item.isAdded) View.VISIBLE else View.INVISIBLE
        }

        override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
            val viewHolder: ItemViewHolder
            var inflatedView: View? = view
            if (inflatedView == null) {
                inflatedView = context.layoutInflater.inflate(R.layout.item_feed_search_result, null)
                viewHolder = ItemViewHolder()
                inflatedView?.let { vw ->
                    viewHolder.title = vw.findViewById<View>(R.id.tv_title ) as TextView
                    viewHolder.description = vw.findViewById<View>(R.id.tv_description ) as TextView
                    viewHolder.feedAdded = vw.findViewById(R.id.iv_feed_added) as ImageView
                }
            } else {
                viewHolder = inflatedView.tag as ItemViewHolder
            }
            val item = getItem(i)
            item?.let { it ->
                setFeedAdded(viewHolder, it)
                setTitle(viewHolder, it)
                setDescription(viewHolder, it)
            }
            inflatedView?.tag = viewHolder
            return inflatedView!!
        }
    }
}