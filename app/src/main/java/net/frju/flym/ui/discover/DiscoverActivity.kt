package net.frju.flym.ui.discover

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.URLUtil
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import net.fred.feedex.R
import net.frju.flym.App
import net.frju.flym.data.entities.Feed
import net.frju.flym.data.entities.SearchFeedResult
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.sdk21.listeners.onClick
import org.jetbrains.anko.sdk21.listeners.onEditorAction
import org.jetbrains.anko.sdk21.listeners.textChangedListener
import java.util.*


class DiscoverActivity : AppCompatActivity(), FeedManagementInterface{

    private val TAG = "FeedSearchActivity"
    private var mSearchInput: AutoCompleteTextView? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feed_search)
        this.initSearchInputs()
        this.goDiscover()
    }

    private fun initSearchInputs(){
        var timer = Timer()
        mSearchInput = this.findViewById(R.id.et_search_input)
        mSearchInput?.let { searchInput ->

            // Handle IME Options Search Action
            searchInput.onEditorAction { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    val term = searchInput.text.toString().trim()
                    if (term.isNotEmpty()) {
                        goSearch(term)
                    }
                    true
                }
                false
            }

            // Handle search after N ms pause after input
            searchInput.textChangedListener {
                afterTextChanged {
                    val term = searchInput.text.toString().trim()
                    if (term.isNotEmpty()){
                        timer.cancel()
                        timer = Timer()
                        timer.schedule(object: TimerTask() {
                            override fun run() {
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
                    .addToBackStack(DiscoverFragment.TAG)
                    .commitAllowingStateLoss()
        }
    }

    private fun goDiscover(){
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fcv_discover_fragments)
        if (currentFragment !is DiscoverFragment){
            val fragment = DiscoverFragment.newInstance()
            supportFragmentManager
                    .beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .replace(R.id.fcv_discover_fragments, fragment, DiscoverFragment.TAG)
                    .commitAllowingStateLoss()
        }
    }

    override fun searchForFeed(query: String) {
        mSearchInput?.setText(query)
    }

    override fun addFeed(view:View, title:String, link:String){
        doAsync {
            val feedToAdd = Feed(link = link, title = title)
            App.db.feedDao().insert(feedToAdd)
            view.context.runOnUiThread {
                view.snackbar(R.string.feed_added)
            }
        }
    }

    override fun deleteFeed(view: View, feed: SearchFeedResult){
        doAsync {
            App.db.feedDao().deleteByLink(feed.link)
        }
    }

    override fun previewFeed(view: View, feed: SearchFeedResult) {
        TODO("Not yet implemented")
    }
}