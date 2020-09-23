package net.frju.flym.ui.discover

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.text.toSpannable
import androidx.fragment.app.Fragment
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import net.fred.feedex.R
import net.frju.flym.App
import net.frju.flym.GlideApp
import net.frju.flym.data.entities.Feed
import net.frju.flym.data.entities.SearchFeedResult
import net.frju.flym.service.FetcherService
import net.frju.flym.ui.entries.EntryAdapter
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.layoutInflater
import org.jetbrains.anko.uiThread
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList

class FeedSearchFragment : Fragment(), AdapterView.OnItemClickListener {

    companion object {
        const val TAG = "FeedSearchFragment"
        const val ARG_QUERY = "query"

        private const val FEED_SEARCH_TITLE = "title"
        private const val FEED_SEARCH_URL = "feedId"
        private const val FEED_SEARCH_DESC = "description"
        private const val FEED_SEARCH_ICON_URL = "iconUrl"
        private val FEED_SEARCH_BLACKLIST = arrayOf("http://syndication.lesechos.fr/rss/rss_finance.xml")

        @JvmStatic
        fun newInstance(query: String) =
                FeedSearchFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_QUERY, query)
                    }
                }
    }

    private var resultsListView: ListView? = null
    private lateinit var manageFeeds: FeedManagementInterface

    private var query: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            query = it.getString(ARG_QUERY)
            query?.let { query ->
                searchForFeed(query)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_feed_search, container, false)
        initSearchListView(view)
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        manageFeeds = context as FeedManagementInterface
    }

    fun search(query: String) {
        this.query = query
        searchForFeed(query)
    }

    private fun initSearchListView(view: View) {
        resultsListView = view.findViewById(R.id.lv_search_results)
        resultsListView?.adapter = SearchResultsAdapter(view.context, ArrayList())
        resultsListView?.onItemClickListener = this
    }

    private fun getFeedlySearchUrl(term: String): String {
        return Uri.Builder()
                .scheme("https")
                .authority("cloud.feedly.com")
                .path("/v3/search/feeds")
                .appendQueryParameter("count", "20")
                .appendQueryParameter("locale", resources.configuration.locale.language)
                .appendQueryParameter("query", term)
                .build()
                .toString()
    }

    private fun searchForFeed(term: String) {
        doAsync {
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
                                    val feedTitle = HtmlCompat.fromHtml(entry.get(FEED_SEARCH_TITLE).toString(), HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
                                    val feedDescription = HtmlCompat.fromHtml(entry.get(FEED_SEARCH_DESC).toString(), HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
                                    val feedIconUrl = if (entry.has(FEED_SEARCH_ICON_URL)) HtmlCompat.fromHtml(entry.get(FEED_SEARCH_ICON_URL).toString(), HtmlCompat.FROM_HTML_MODE_LEGACY).toString() else null
                                    val feedAdded = App.db.feedDao().findByLink(url) != null
                                    val feedResult = SearchFeedResult(feedIconUrl, url, feedTitle, feedDescription, feedAdded)
                                    Log.d(TAG, feedResult.toString())
                                    array.add(feedResult)
                                }
                                uiThread {
                                    (resultsListView?.adapter as SearchResultsAdapter).updateData(term, array)
                                }

                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        view?.let { vw ->
            val feedAdded = vw.findViewById<ImageView>(R.id.iv_feed_added)
            val item = parent?.getItemAtPosition(position) as SearchFeedResult
            if (item.isAdded) {
                AlertDialog.Builder(view.context)
                        .setTitle(item.name)
                        .setMessage(R.string.question_delete_feed)
                        .setPositiveButton(android.R.string.yes) { _, _ ->
                            manageFeeds.deleteFeed(vw, item)
                            item.isAdded = false
                            feedAdded?.setImageResource(R.drawable.ic_baseline_add_24)
                        }.setNegativeButton(android.R.string.no, null)
                        .show()
            } else {
                feedAdded?.setImageResource(R.drawable.ic_baseline_check_24)
                item.isAdded = true
                manageFeeds.addFeed(vw, item.name, item.link)
            }
        }
    }

    class SearchResultsAdapter(context: Context, items: ArrayList<SearchFeedResult>) :
            ArrayAdapter<SearchFeedResult>(context, R.layout.item_feed_search_result, items) {

        private var searchTerm: String? = null

        private class ItemViewHolder {
            var icon: ImageView? = null
            var feedAdded: ImageView? = null
            var title: TextView? = null
            var description: TextView? = null
        }

        fun updateData(searchTerm: String, items: ArrayList<SearchFeedResult>) {
            this.searchTerm = searchTerm
            this.clear()
            this.addAll(items)
            this.notifyDataSetChanged()
        }

        private fun setTitle(viewHolder: ItemViewHolder, item: SearchFeedResult) {
            searchTerm?.let { term ->
                val start = item.name.toLowerCase(Locale.ROOT).indexOf(term.toLowerCase(Locale.ROOT))
                if (start != -1) {
                    val end = start + term.length
                    val spannable = item.name.toSpannable()
                    val color = ContextCompat.getColor(context, R.color.colorAccent)
                    spannable.setSpan(ForegroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    viewHolder.title?.text = spannable
                    return
                }
            }
            viewHolder.title?.text = item.name
        }

        private fun setDescription(viewHolder: ItemViewHolder, item: SearchFeedResult) {
            viewHolder.description?.text = item.link
        }

        private fun setIcon(viewHolder: ItemViewHolder, item: SearchFeedResult) {
            val letterDrawable = Feed.getLetterDrawable(item.link.hashCode().toLong(), item.name)
            viewHolder.icon?.let { iv ->
                if (!item.iconUrl.isNullOrEmpty()) {
                    GlideApp.with(context)
                            .load(item.iconUrl)
                            .centerCrop()
                            .transition(DrawableTransitionOptions.withCrossFade(EntryAdapter.CROSS_FADE_FACTORY))
                            .placeholder(letterDrawable)
                            .error(letterDrawable)
                            .into(iv)
                } else {
                    GlideApp.with(context).clear(iv)
                    iv.setImageDrawable(letterDrawable)
                }
            }
        }

        private fun setFeedAdded(viewHolder: ItemViewHolder, item: SearchFeedResult) {
            if (item.isAdded) {
                viewHolder.feedAdded?.setImageResource(R.drawable.ic_baseline_check_24)
            } else {
                viewHolder.feedAdded?.setImageResource(R.drawable.ic_baseline_add_24)
            }
        }

        override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
            val viewHolder: ItemViewHolder
            var inflatedView: View? = view
            if (inflatedView == null) {
                inflatedView = context.layoutInflater.inflate(R.layout.item_feed_search_result, null)
                viewHolder = ItemViewHolder()
                inflatedView?.let { vw ->
                    viewHolder.icon = vw.findViewById(R.id.iv_icon) as ImageView
                    viewHolder.title = vw.findViewById(R.id.tv_title) as TextView
                    viewHolder.description = vw.findViewById(R.id.tv_description) as TextView
                    viewHolder.feedAdded = vw.findViewById(R.id.iv_feed_added) as ImageView
                }
            } else {
                viewHolder = inflatedView.tag as ItemViewHolder
            }
            getItem(i)?.let {
                setIcon(viewHolder, it)
                setTitle(viewHolder, it)
                setDescription(viewHolder, it)
                setFeedAdded(viewHolder, it)
            }
            inflatedView?.tag = viewHolder
            return inflatedView!!
        }
    }
}