package net.frju.flym.ui.main

import android.app.AlertDialog
import android.arch.lifecycle.Observer
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v4.view.GravityCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.text.Html
import android.widget.EditText
import android.widget.Toast
import ir.mirrajabi.searchdialog.SimpleSearchDialogCompat
import ir.mirrajabi.searchdialog.core.BaseFilter
import ir.mirrajabi.searchdialog.core.SearchResultListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_entries.*
import kotlinx.android.synthetic.main.view_main_containers.*
import net.fred.feedex.R
import net.frju.flym.App
import net.frju.flym.data.entities.EntryWithFeed
import net.frju.flym.data.entities.Feed
import net.frju.flym.data.entities.SearchFeedResult
import net.frju.flym.service.AutoRefreshJobService
import net.frju.flym.service.FetcherService
import net.frju.flym.ui.about.AboutActivity
import net.frju.flym.ui.entries.EntriesFragment
import net.frju.flym.ui.entrydetails.EntryDetailsActivity
import net.frju.flym.ui.entrydetails.EntryDetailsFragment
import net.frju.flym.ui.feeds.FeedAdapter
import net.frju.flym.ui.feeds.FeedGroup
import net.frju.flym.ui.feeds.FeedListEditActivity
import net.frju.flym.ui.settings.SettingsActivity
import net.frju.flym.utils.closeKeyboard
import okhttp3.Request
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk21.listeners.onClick
import org.json.JSONObject
import java.net.URLEncoder
import java.util.*


class MainActivity : AppCompatActivity(), MainNavigator {

    companion object {
        const val EXTRA_FROM_NOTIF = "EXTRA_FROM_NOTIF"

        private const val TAG_DETAILS = "TAG_DETAILS"
        private const val TAG_MASTER = "TAG_MASTER"

        private const val FEED_SEARCH_TITLE = "title"
        private const val FEED_SEARCH_URL = "feedId"
        private const val FEED_SEARCH_DESC = "description"
        //TODO better default feeds
        private val DEFAULT_FEEDS = arrayListOf(SearchFeedResult("http://www.nytimes.com/services/xml/rss/nyt/World.xml", "NY Times", "Word news"))

        var isInForeground = false
    }

    private val feedGroups = mutableListOf<FeedGroup>()
    private val feedAdapter = FeedAdapter(feedGroups)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        nav.layoutManager = LinearLayoutManager(this)
        nav.adapter = feedAdapter

        add_feed_fab.onClick {
            //TODO strings in xml
            val searchDialog = SimpleSearchDialogCompat(this@MainActivity, "Search...",
                    "What are you looking for...?", null, DEFAULT_FEEDS,
                    SearchResultListener<SearchFeedResult> { dialog, item, position ->
                        Toast.makeText(this@MainActivity, "Added",
                                Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        doAsync { App.db.feedDao().insert(Feed(link = item.link, title = item.name)) }
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
                                it.body()?.let { body ->
                                    val jsonStr = body.string()

                                    // Parse results
                                    val entries = JSONObject(jsonStr).getJSONArray("results")
                                    for (i in 0 until entries.length()) {
                                        try {
                                            val entry = entries.get(i) as JSONObject
                                            val url = entry.get(FEED_SEARCH_URL).toString().replace("feed/", "")
                                            if (!url.isEmpty()) {
                                                @Suppress("DEPRECATION")
                                                array.add(
                                                        SearchFeedResult(url,
                                                                Html.fromHtml(entry.get(FEED_SEARCH_TITLE).toString()).toString(),
                                                                Html.fromHtml(entry.get(FEED_SEARCH_DESC).toString()).toString()))
                                            }
                                        } catch (ignored: Throwable) {
                                        }
                                    }
                                }
                            }
                        } catch (ignored: Throwable) {
                        }
                    } else {
                        array.addAll(DEFAULT_FEEDS)
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

                val all = Feed().apply {
                    id = Feed.ALL_ENTRIES_ID
                    title = getString(R.string.all_entries)
                }
                feedGroups.add(FeedGroup(all, listOf()))

                val subFeedMap = it.groupBy { it.groupId }

                feedGroups.addAll(
                        subFeedMap[null]?.map { FeedGroup(it, subFeedMap[it.id].orEmpty()) }.orEmpty()
                )

                feedAdapter.notifyParentDataSetChanged(true)

                feedAdapter.onFeedClick { view, feed ->
                    goToEntriesList(feed)
                    closeDrawer()
                }

                feedAdapter.onFeedLongClick { view, feed ->
                    PopupMenu(this, view).apply {
                        setOnMenuItemClickListener { item ->
                            when (item.itemId) {
                                R.id.mark_all_as_read -> doAsync {
                                    // TODO handle group and "all entries"
                                    App.db.entryDao().markAsRead(feed.id)
                                }
                                R.id.rename -> {
                                    val input = EditText(this@MainActivity).apply {
                                        setSingleLine(true)
                                        setText(feed.title)
                                    }

                                    AlertDialog.Builder(this@MainActivity)
                                            .setTitle(R.string.menu_rename_feed)
                                            .setView(input)
                                            .setPositiveButton(android.R.string.ok) { dialog, which ->
                                                val newName = input.text.toString()
                                                if (newName.isNotBlank()) {
                                                    doAsync {
                                                        feed.title = newName
                                                        App.db.feedDao().insert(feed)
                                                    }
                                                }
                                            }
                                            .setNegativeButton(android.R.string.cancel, null)
                                            .show()
                                }
                                R.id.reorder -> startActivity<FeedListEditActivity>()
                                R.id.delete -> {
                                    AlertDialog.Builder(this@MainActivity)
                                            .setTitle(feed.title)
                                            .setMessage(if (feed.isGroup) R.string.question_delete_group else R.string.question_delete_feed)
                                            .setPositiveButton(android.R.string.yes) { _, _ ->
                                                doAsync { App.db.feedDao().delete(feed) }
                                            }.setNegativeButton(android.R.string.no, null)
                                            .show()
                                }
                                R.id.enable_full_text_retrieval -> doAsync { App.db.feedDao().enableFullTextRetrieval(feed.id) }
                                R.id.disable_full_text_retrieval -> doAsync { App.db.feedDao().disableFullTextRetrieval(feed.id) }
                            }
                            true
                        }
                        inflate(R.menu.drawer_feed)

                        when {
                            feed.id == Feed.ALL_ENTRIES_ID -> {
                                menu.findItem(R.id.rename).isVisible = false
                                menu.findItem(R.id.delete).isVisible = false
                                menu.findItem(R.id.reorder).isVisible = false
                                menu.findItem(R.id.enable_full_text_retrieval).isVisible = false
                                menu.findItem(R.id.disable_full_text_retrieval).isVisible = false
                            }
                            feed.isGroup -> {
                                menu.findItem(R.id.enable_full_text_retrieval).isVisible = false
                                menu.findItem(R.id.disable_full_text_retrieval).isVisible = false
                            }
                            feed.retrieveFullText -> menu.findItem(R.id.enable_full_text_retrieval).isVisible = false
                            else -> menu.findItem(R.id.disable_full_text_retrieval).isVisible = false
                        }

                        show()
                    }
                }
            }
        })

        toolbar.setNavigationIcon(R.drawable.ic_menu_24dp)
        toolbar.setNavigationOnClickListener({ toggleDrawer() })

        if (savedInstanceState == null) {
            closeDrawer()

            goToEntriesList(null)
        }

        AutoRefreshJobService.initAutoRefresh(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        // If we just clicked on the notification, let's go back to the default view
        if (intent?.getBooleanExtra(EXTRA_FROM_NOTIF, false) == true && feedGroups.isNotEmpty()) {
            feedAdapter.selectedItemId = Feed.ALL_ENTRIES_ID
            goToEntriesList(feedGroups[0].feed)
            bottom_navigation.selectedItemId = R.id.unreads
        }
    }

    override fun onResume() {
        super.onResume()

        isInForeground = true
        notificationManager.cancel(0)
    }

    override fun onPause() {
        super.onPause()
        isInForeground = false
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
        if (drawer?.isDrawerOpen(GravityCompat.START) == true) {
            drawer?.postDelayed({ drawer.closeDrawer(GravityCompat.START) }, 100)
        }
    }

    fun openDrawer() {
        if (drawer?.isDrawerOpen(GravityCompat.START) == false) {
            drawer?.openDrawer(GravityCompat.START)
        }
    }

    fun toggleDrawer() {
        if (drawer?.isDrawerOpen(GravityCompat.START) == true) {
            drawer?.closeDrawer(GravityCompat.START)
        } else {
            drawer?.openDrawer(GravityCompat.START)
        }
    }

    private fun goBack(): Boolean {
        if (containers_layout.state == MainNavigator.State.TWO_COLUMNS_WITH_DETAILS && !containers_layout.hasTwoColumns()) {
            if (clearDetails()) {
                containers_layout.state = MainNavigator.State.TWO_COLUMNS_EMPTY
                return true
            }
        }
        return false
    }

    override fun onBackPressed() {
        if (drawer?.isDrawerOpen(GravityCompat.START) == true) {
            drawer?.closeDrawer(GravityCompat.START)
        } else if (toolbar.hasExpandedActionView()) {
            toolbar.collapseActionView()
        } else if (!goBack()) {
            super.onBackPressed()
        }
    }

    private fun clearDetails(): Boolean {
        supportFragmentManager.findFragmentByTag(TAG_DETAILS)?.let {
            supportFragmentManager
                    .beginTransaction()
                    .remove(it)
                    .commit()
            return true
        }
        return false
    }

    override fun goToEntriesList(feed: Feed?) {
        clearDetails()
        containers_layout.state = MainNavigator.State.TWO_COLUMNS_EMPTY

        // We try to reuse the fragment to avoid loosing the bottom tab position
        val currentFragment = supportFragmentManager.findFragmentById(R.id.frame_master)
        if (currentFragment is EntriesFragment) {
            currentFragment.feed = feed
        } else {
            val master = EntriesFragment.newInstance(feed)
            supportFragmentManager.beginTransaction().replace(R.id.frame_master, master, TAG_MASTER).commit()
        }
    }

    override fun goToEntryDetails(entry: EntryWithFeed, allEntryIds: List<String>) {
        closeKeyboard()

        if (containers_layout.hasTwoColumns()) {
            containers_layout.state = MainNavigator.State.TWO_COLUMNS_WITH_DETAILS
            val fragment = EntryDetailsFragment.newInstance(entry, allEntryIds)
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.frame_details, fragment, TAG_DETAILS)
                    .commit()

            val listFragment = supportFragmentManager.findFragmentById(R.id.frame_master) as EntriesFragment
            listFragment.setSelectedEntryId(entry.id)
        } else {
            startActivity<EntryDetailsActivity>(EntryDetailsFragment.ARG_ENTRY to entry, EntryDetailsFragment.ARG_ALL_ENTRIES_IDS to allEntryIds)
        }
    }

    override fun setSelectedEntryId(selectedEntryId: String) {
        val listFragment = supportFragmentManager.findFragmentById(R.id.frame_master) as EntriesFragment
        listFragment.setSelectedEntryId(selectedEntryId)
    }

    override fun goToAboutMe() {
        startActivity<AboutActivity>()
    }

    override fun goToSettings() {
        startActivity<SettingsActivity>()
    }
}
