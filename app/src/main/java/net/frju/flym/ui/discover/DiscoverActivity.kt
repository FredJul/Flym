package net.frju.flym.ui.discover

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
import androidx.fragment.app.FragmentTransaction
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import net.fred.feedex.R
import net.frju.flym.App
import net.frju.flym.GlideApp
import net.frju.flym.data.entities.Feed
import net.frju.flym.data.entities.SearchFeedResult
import net.frju.flym.service.FetcherService
import net.frju.flym.ui.entries.EntriesFragment
import net.frju.flym.ui.entries.EntryAdapter
import net.frju.flym.ui.main.MainActivity
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.layoutInflater
import org.jetbrains.anko.sdk21.listeners.onClick
import org.jetbrains.anko.sdk21.listeners.onEditorAction
import org.jetbrains.anko.sdk21.listeners.textChangedListener
import org.json.JSONException
import org.json.JSONObject
import java.net.URLEncoder
import java.util.*
import kotlin.collections.ArrayList


class DiscoverActivity : AppCompatActivity(), FeedManagementInterface{

    private val TAG = "FeedSearchActivity"
    private var mSearchInput: AutoCompleteTextView? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feed_search)
        this.initSearchInputs()
    }

    private fun initSearchInputs(){
        var timer = Timer()
        mSearchInput = this.findViewById(R.id.et_search_input)
        mSearchInput?.let { searchInput ->

            // Handle IME Options Search Action
            searchInput.onEditorAction { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    val term = searchInput.text.toString().trim()
//                    searchForFeed(term)
                    goSearch(term)
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
//                                searchForFeed(term)
                                goSearch(term)
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
                    searchInput.setText("")
                }else{
                    // Todo: Swap to string resource
                    searchInput.snackbar("Please select from the results or provide a valid URL")
                }
            }
        }
    }

    private fun goSearch(query: String){
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fcv_discover_fragments)
        if (currentFragment is FeedSearchFragment) {
            currentFragment.search(query)
        } else {
            val fragment = FeedSearchFragment.newInstance(query)
            supportFragmentManager
                    .beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .replace(R.id.fcv_discover_fragments, fragment, FeedSearchFragment.TAG)
                    .commitAllowingStateLoss()
        }
    }

    private fun goDiscover(){

    }

    override fun addFeed(view:View, title:String, link:String){
        doAsync {
            val feedToAdd = Feed(link = link, title = title)
            App.db.feedDao().insert(feedToAdd)
        }
    }

    override fun deleteFeed(view: View, feed: SearchFeedResult){
        doAsync {
            App.db.feedDao().deleteByLink(feed.link)
        }
    }

}