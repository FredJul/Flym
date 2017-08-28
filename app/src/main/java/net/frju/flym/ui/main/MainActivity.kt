package net.frju.flym.ui.main

import android.arch.lifecycle.LifecycleActivity
import android.arch.lifecycle.Observer
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.FragmentTransaction
import android.support.v4.view.GravityCompat
import android.support.v7.widget.LinearLayoutManager
import android.text.Html
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import ir.mirrajabi.searchdialog.SimpleSearchDialogCompat
import ir.mirrajabi.searchdialog.core.BaseFilter
import ir.mirrajabi.searchdialog.core.SearchResultListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.view_main_containers.view.*
import net.fred.feedex.R
import net.frju.flym.App
import net.frju.flym.data.entities.Feed
import net.frju.flym.data.entities.ItemWithFeed
import net.frju.flym.data.entities.SearchFeedResult
import net.frju.flym.service.FetcherService
import net.frju.flym.ui.itemdetails.ItemDetailsFragment
import net.frju.flym.ui.items.ItemsFragment
import okhttp3.Request
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.hintTextColor
import org.jetbrains.anko.sdk21.coroutines.onClick
import org.jetbrains.anko.textColor
import org.json.JSONObject
import java.net.URLEncoder
import java.util.*


class MainActivity : LifecycleActivity(), NavigationView.OnNavigationItemSelectedListener, MainNavigator {

    private val feedGroups = mutableListOf<FeedGroup>()
    private val feedAdapter = FeedAdapter(feedGroups)
    val FEED_SEARCH_TITLE = "title"
    val FEED_SEARCH_URL = "feedId"
    val FEED_SEARCH_DESC = "description"
    val DEFAULT_ITEMS = arrayListOf(SearchFeedResult("http://www.nytimes.com/services/xml/rss/nyt/World.xml", "NY Times", "Word news"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        nav.layoutManager = LinearLayoutManager(this)
        nav.adapter = feedAdapter
        add_feed_fab.onClick {
            val searchDialog = SimpleSearchDialogCompat(this@MainActivity, "Search...",
                    "What are you looking for...?", null, DEFAULT_ITEMS,
                    SearchResultListener<SearchFeedResult> { dialog, item, position ->
                        Toast.makeText(this@MainActivity, "Added",
                                Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        doAsync { App.db.feedDao().insertAll(Feed(link = item.link, title = item.name)) }
                    })

            val apiFilter = object : BaseFilter<SearchFeedResult>() {
                override fun performFiltering(charSequence: CharSequence): FilterResults? {
                    doBeforeFiltering()

                    val results = FilterResults()
                    val array = ArrayList<SearchFeedResult>()

                    if (charSequence.isNotEmpty()) {
                        try {
                            val request = Request.Builder()
                                    .url("http://cloud.feedly.com/v3/search/feeds?count=20&locale=" + resources.configuration.locale.language + "&query=" + URLEncoder.encode(charSequence.toString(), "UTF-8"))
                                    .build()
                            FetcherService.HTTP_CLIENT.newCall(request).execute().use {

                                val jsonStr = it.body()!!.string()

                                // Parse results
                                val entries = JSONObject(jsonStr).getJSONArray("results")
                                for (i in 0 until entries.length()) {
                                    try {
                                        val entry = entries.get(i) as JSONObject
                                        val url = entry.get(FEED_SEARCH_URL).toString().replace("feed/", "")
                                        if (!url.isEmpty()) {
                                            array.add(
                                                    SearchFeedResult(url,
                                                            Html.fromHtml(entry.get(FEED_SEARCH_TITLE).toString()).toString(),
                                                            Html.fromHtml(entry.get(FEED_SEARCH_DESC).toString()).toString()))
                                        }
                                    } catch (ignored: Exception) {
                                    }
                                }
                            }
                        } catch (ignored: Exception) {
                        }
                    } else {
                        array.addAll(DEFAULT_ITEMS)
                    }

                    results.values = array
                    results.count = array.size
                    return results
                }

                override fun publishResults(charSequence: CharSequence, filterResults: FilterResults?) {
                    filterResults?.let {
                        @Suppress("UNCHECKED_CAST")
                        searchDialog.filterResultListener.onFilter(it.values as ArrayList<SearchFeedResult>)
                    }
                    doAfterFiltering()
                }
            }
            searchDialog.filter = apiFilter
            searchDialog.show()
            searchDialog.searchBox.apply {
                textColor = Color.BLACK
                hintTextColor = Color.GRAY
            }

        }

        App.db.feedDao().observeAll.observe(this@MainActivity, Observer {
            it?.let {
                feedGroups.clear()

                val all = Feed()
                all.id = Feed.ALL_ITEMS_ID
                all.title = getString(R.string.all_entries)
                feedGroups.add(FeedGroup(all, listOf()))

                val subFeedMap = it.groupBy { it.groupId }

                feedGroups.addAll(
                        subFeedMap[null]?.map { FeedGroup(it, subFeedMap[it.id] ?: listOf()) } ?: listOf()
                )

                feedAdapter.notifyParentDataSetChanged(true)

                feedAdapter.onFeedClick { view, feed ->
                    goToItemsList(feed)
                    closeDrawer()
                }
            }
        })

        containers_layout.custom_appbar.setOnNavigationClickListener(View.OnClickListener { toggleDrawer() })

        if (savedInstanceState == null) {
            closeDrawer()

            goToItemsList(null)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        feedAdapter.onRestoreInstanceState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        feedAdapter.onSaveInstanceState(outState)
    }

    fun closeDrawer() {
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.postDelayed({ drawer.closeDrawer(GravityCompat.START) }, 100)
        }
    }

    fun openDrawer() {
        if (drawer != null && !drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.openDrawer(GravityCompat.START)
        }
    }

    fun toggleDrawer() {
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else if (drawer != null && !drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.openDrawer(GravityCompat.START)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_main_nav__settings -> {
                closeDrawer()

                goToSettings()
            }

            R.id.menu_main_nav__feedback -> {
                closeDrawer()

                goToFeedback()
            }

            else -> return false
        }
        return true
    }

    fun goBack(): Boolean {
        val state = containers_layout.state
        if (state == MainNavigator.State.TWO_COLUMNS_WITH_DETAILS && !containers_layout.hasTwoColumns()) {
            if (clearDetails()) {
                containers_layout.state = MainNavigator.State.TWO_COLUMNS_EMPTY
                return true
            }
        }
        return false
    }

    override fun onBackPressed() {
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else if (!goBack()) {
            super.onBackPressed()
        }
    }

    private fun clearDetails(): Boolean {
        val details = supportFragmentManager.findFragmentByTag(TAG_DETAILS)
        if (details != null) {
            supportFragmentManager
                    .beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .remove(details)
                    .commit()
            return true
        }
        return false
    }

    override fun goToItemsList(feed: Feed?) {
        clearDetails()
        containers_layout.custom_appbar.setState(MainNavigator.State.TWO_COLUMNS_EMPTY)
        containers_layout.state = MainNavigator.State.TWO_COLUMNS_EMPTY
        val master = ItemsFragment.newInstance(feed)
        supportFragmentManager.beginTransaction().replace(R.id.frame_master, master, TAG_MASTER).commit()
    }

    override fun goToItemDetails(item: ItemWithFeed) {
        containers_layout.custom_appbar.setState(MainNavigator.State.TWO_COLUMNS_WITH_DETAILS)
        containers_layout.state = MainNavigator.State.TWO_COLUMNS_WITH_DETAILS
        val fragment = ItemDetailsFragment.newInstance(item)
        supportFragmentManager
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.frame_details, fragment, TAG_DETAILS)
                .commit()

        val listFragment = supportFragmentManager.findFragmentById(R.id.frame_master) as ItemsFragment
        listFragment.setSelectedItem(item)
    }

    override fun goToPreviousItem() {
        val listFragment = supportFragmentManager.findFragmentById(R.id.frame_master) as ItemsFragment
        val detailFragment = supportFragmentManager.findFragmentById(R.id.frame_details) as ItemDetailsFragment

        val previousItem = listFragment.getPreviousItem()
        if (previousItem != null) {
            listFragment.setSelectedItem(previousItem)
            detailFragment.setItem(previousItem)
        }
    }

    override fun goToNextItem() {
        val listFragment = supportFragmentManager.findFragmentById(R.id.frame_master) as ItemsFragment
        val detailFragment = supportFragmentManager.findFragmentById(R.id.frame_details) as ItemDetailsFragment

        val nextItem = listFragment.getNextItem()
        if (nextItem != null) {
            listFragment.setSelectedItem(nextItem)
            detailFragment.setItem(nextItem)
        }
    }

    override fun goToSettings() {
        //start new activity
    }

    override fun goToFeedback() {
        //start new activity
    }

    companion object {

        private val TAG_DETAILS = "TAG_DETAILS"
        private val TAG_MASTER = "TAG_MASTER"
    }
}
