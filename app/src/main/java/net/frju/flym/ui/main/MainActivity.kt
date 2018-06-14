/*
 * Copyright (c) 2012-2018 Frederic Julian
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

package net.frju.flym.ui.main

import android.Manifest
import android.app.AlertDialog
import android.arch.lifecycle.Observer
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.FragmentTransaction
import android.support.v4.view.GravityCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.text.Html
import android.util.Log
import android.webkit.URLUtil
import android.widget.EditText
import com.codekidlabs.storagechooser.StorageChooser
import com.rometools.opml.feed.opml.Attribute
import com.rometools.opml.feed.opml.Opml
import com.rometools.opml.feed.opml.Outline
import com.rometools.opml.io.impl.OPML20Generator
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.WireFeedInput
import com.rometools.rome.io.WireFeedOutput
import com.rometools.rome.io.XmlReader
import ir.mirrajabi.searchdialog.SimpleSearchDialogCompat
import ir.mirrajabi.searchdialog.core.BaseFilter
import ir.mirrajabi.searchdialog.core.SearchResultListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_entries.*
import kotlinx.android.synthetic.main.view_main_containers.*
import kotlinx.android.synthetic.main.view_main_drawer_header.*
import net.fred.feedex.R
import net.frju.flym.App
import net.frju.flym.data.entities.EntryWithFeed
import net.frju.flym.data.entities.Feed
import net.frju.flym.data.entities.SearchFeedResult
import net.frju.flym.data.utils.PrefUtils
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
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.hintTextColor
import org.jetbrains.anko.notificationManager
import org.jetbrains.anko.sdk21.listeners.onClick
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.textColor
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import org.json.JSONObject
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.Reader
import java.io.StringReader
import java.io.Writer
import java.net.URL
import java.net.URLEncoder
import java.util.ArrayList
import java.util.Date


class MainActivity : AppCompatActivity(), MainNavigator {

    companion object {
        const val EXTRA_FROM_NOTIF = "EXTRA_FROM_NOTIF"

        private const val TAG_DETAILS = "TAG_DETAILS"
        private const val TAG_MASTER = "TAG_MASTER"

        private const val FEED_SEARCH_TITLE = "title"
        private const val FEED_SEARCH_URL = "feedId"
        private const val FEED_SEARCH_DESC = "description"

        private const val OLD_GNEWS_TO_IGNORE = "http://news.google.com/news?"

        private val GNEWS_TOPIC_NAME = intArrayOf(R.string.google_news_top_stories, R.string.google_news_world, R.string.google_news_business, R.string.google_news_science_technology, R.string.google_news_entertainment, R.string.google_news_sports, R.string.google_news_health)

        private val GNEWS_TOPIC_CODE = arrayOf("", "WORLD", "BUSINESS", "SCITECH", "ENTERTAINMENT", "SPORTS", "HEALTH")

        private const val AUTO_IMPORT_OPML_REQUEST_CODE = 1
        private const val CHOOSE_OPML_REQUEST_CODE = 2
        private const val EXPORT_OPML_REQUEST_CODE = 3
        private val NEEDED_PERMS = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        private val BACKUP_OPML = File(Environment.getExternalStorageDirectory(), "/Flym_auto_backup.opml")
        private const val RETRIEVE_FULLTEXT_OPML_ATTR = "retrieveFullText"

        var isInForeground = false
    }

    private val feedGroups = mutableListOf<FeedGroup>()
    private val feedAdapter = FeedAdapter(feedGroups)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        more.onClick {
            it?.let {
                PopupMenu(this@MainActivity, it).apply {
                    menuInflater.inflate(R.menu.menu_drawer_header, menu)
                    setOnMenuItemClickListener {
                        when (it.itemId) {
                            R.id.import_feeds -> {
                                pickOpml()
                            }
                            R.id.export_feeds -> {
                                exportOpml()
                            }
                            R.id.menu_entries__about -> {
                                goToAboutMe()
                            }
                            R.id.menu_entries__settings -> {
                                goToSettings()
                            }
                        }
                        true
                    }
                    show()
                }
            }
        }
        nav.layoutManager = LinearLayoutManager(this)
        nav.adapter = feedAdapter

        add_feed_fab.onClick {
            showAddFeedPopup()
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
                                    when {
                                        feed.id == Feed.ALL_ENTRIES_ID -> App.db.entryDao().markAllAsRead()
                                        feed.isGroup -> App.db.entryDao().markGroupAsRead(feed.id)
                                        else -> App.db.entryDao().markAsRead(feed.id)
                                    }
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
                        inflate(R.menu.menu_drawer_feed)

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
            // First open => we open the drawer for you
            if (PrefUtils.getBoolean(PrefUtils.FIRST_OPEN, true)) {
                PrefUtils.putBoolean(PrefUtils.FIRST_OPEN, false)
                openDrawer()

                if (isOldFlymAppInstalled()) {
                    AlertDialog.Builder(this)
                            .setTitle(R.string.welcome_title_with_opml_import)
                            .setPositiveButton(android.R.string.yes) { _, _ ->
                                autoImportOpml()
                            }
                            .setNegativeButton(android.R.string.no, null)
                            .show()
                } else {
                    AlertDialog.Builder(this)
                            .setTitle(R.string.welcome_title)
                            .setPositiveButton(android.R.string.yes) { _, _ ->
                                showAddFeedPopup()
                            }
                            .setNegativeButton(android.R.string.no, null)
                            .show()
                }
            } else {
                closeDrawer()
            }

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

    private fun isOldFlymAppInstalled() =
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA).any { it.packageName == "net.fred.feedex" }

    @AfterPermissionGranted(CHOOSE_OPML_REQUEST_CODE)
    private fun pickOpml() {
        if (!EasyPermissions.hasPermissions(this, *NEEDED_PERMS)) {
            EasyPermissions.requestPermissions(this, getString(R.string.storage_request_explanation),
                    CHOOSE_OPML_REQUEST_CODE, *NEEDED_PERMS)
        } else {
            StorageChooser.Builder()
                    .withActivity(this)
                    .withFragmentManager(fragmentManager)
                    .withMemoryBar(true)
                    .allowCustomPath(true)
                    .setType(StorageChooser.FILE_PICKER)
                    .customFilter(arrayListOf("xml", "opml"))
                    .build()
                    .run {
                        show()
                        setOnSelectListener {
                            importOpml(File(it))
                        }
                    }
        }
    }

    @AfterPermissionGranted(EXPORT_OPML_REQUEST_CODE)
    private fun exportOpml() {
        if (!EasyPermissions.hasPermissions(this, *NEEDED_PERMS)) {
            EasyPermissions.requestPermissions(this, getString(R.string.storage_request_explanation),
                    EXPORT_OPML_REQUEST_CODE, *NEEDED_PERMS)
        } else {
            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED || Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED_READ_ONLY) {
                doAsync {
                    try {
                        val filename = (Environment.getExternalStorageDirectory().toString() + "/Flym_"
                                + System.currentTimeMillis() + ".opml")

                        exportOpml(FileWriter(filename))

                        uiThread { toast(String.format(getString(R.string.message_exported_to), filename)) }
                    } catch (e: Exception) {
                        uiThread { toast(R.string.error_feed_export) }
                    }
                }
            } else {
                toast(R.string.error_external_storage_not_available)
            }
        }
    }

    @AfterPermissionGranted(AUTO_IMPORT_OPML_REQUEST_CODE)
    private fun autoImportOpml() {
        if (!EasyPermissions.hasPermissions(this, *NEEDED_PERMS)) {
            EasyPermissions.requestPermissions(this, getString(R.string.welcome_title_with_opml_import),
                    AUTO_IMPORT_OPML_REQUEST_CODE, *NEEDED_PERMS)
        } else {
            if (BACKUP_OPML.exists()) {
                importOpml(BACKUP_OPML)
            } else {
                toast(R.string.cannot_find_feeds)
            }
        }
    }

    private fun importOpml(file: File) {
        doAsync {
            try {
                parseOpml(FileReader(file))
            } catch (e: Exception) {
                try {
                    // We try to remove the opml version number, it may work better in some cases
                    val fixedReader = StringReader(file.readText().replace("<opml version='[0-9]\\.[0-9]'>".toRegex(), "<opml>"))
                    parseOpml(fixedReader)
                } catch (e: Exception) {
                    Log.e("FRED", "", e)
                    uiThread { toast(R.string.cannot_find_feeds) }
                }
            }
        }
    }

    private fun parseOpml(opmlReader: Reader) {
        var id = 1L
        val feedList = mutableListOf<Feed>()
        val opml = WireFeedInput().build(opmlReader) as Opml
        opml.outlines.forEach {
            if (it.xmlUrl != null || it.children.isNotEmpty()) {
                val topLevelFeed = Feed()
                topLevelFeed.id = id++
                topLevelFeed.title = it.title

                if (it.xmlUrl != null) {
                    if (!it.xmlUrl.startsWith(OLD_GNEWS_TO_IGNORE)) {
                        topLevelFeed.link = it.xmlUrl
                        topLevelFeed.retrieveFullText = it.getAttributeValue(RETRIEVE_FULLTEXT_OPML_ATTR) == "true"
                        feedList.add(topLevelFeed)
                    }
                } else {
                    topLevelFeed.isGroup = true
                    feedList.add(topLevelFeed)

                    it.children.filter { it.xmlUrl != null && !it.xmlUrl.startsWith(OLD_GNEWS_TO_IGNORE) }.forEach {
                        val subLevelFeed = Feed()
                        subLevelFeed.id = id++
                        subLevelFeed.title = it.title
                        subLevelFeed.link = it.xmlUrl
                        subLevelFeed.retrieveFullText = it.getAttributeValue(RETRIEVE_FULLTEXT_OPML_ATTR) == "true"
                        subLevelFeed.groupId = topLevelFeed.id
                        feedList.add(subLevelFeed)
                    }
                }
            }
        }

        if (feedList.isNotEmpty()) {
            App.db.feedDao().insert(*feedList.toTypedArray())
        }
    }

    private fun exportOpml(opmlWriter: Writer) {
        val feeds = App.db.feedDao().all.groupBy { it.groupId }

        val opml = Opml().apply {
            feedType = OPML20Generator().type
            encoding = "utf-8"
            created = Date()
            outlines = feeds[null]?.map {
                Outline(it.title, if (it.link.isNotBlank()) URL(it.link) else null, null).apply {
                    children = feeds[it.id]?.map {
                        Outline(it.title, if (it.link.isNotBlank()) URL(it.link) else null, null).apply {
                            if (it.retrieveFullText) {
                                attributes.add(Attribute(RETRIEVE_FULLTEXT_OPML_ATTR, "true"))
                            }
                        }
                    }
                    if (it.retrieveFullText) {
                        attributes.add(Attribute(RETRIEVE_FULLTEXT_OPML_ATTR, "true"))
                    }
                }
            }
        }

        WireFeedOutput().output(opml, opmlWriter)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    private fun closeDrawer() {
        if (drawer?.isDrawerOpen(GravityCompat.START) == true) {
            drawer?.postDelayed({ drawer.closeDrawer(GravityCompat.START) }, 100)
        }
    }

    private fun openDrawer() {
        if (drawer?.isDrawerOpen(GravityCompat.START) == false) {
            drawer?.openDrawer(GravityCompat.START)
        }
    }

    private fun toggleDrawer() {
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

    private fun showAddFeedPopup() {
        val defaultFeeds = GNEWS_TOPIC_NAME.mapIndexed { index, name ->
            @Suppress("DEPRECATION")
            val link = if (GNEWS_TOPIC_CODE[index].isNotEmpty())
                "https://news.google.com/news/rss/headlines/section/topic/${GNEWS_TOPIC_CODE[index]}?ned=${resources.configuration.locale.language}"
            else
                "https://news.google.com/news/rss/?ned=${resources.configuration.locale.language}"

            SearchFeedResult(link, getString(name))
        }

        val searchDialog = SimpleSearchDialogCompat(this@MainActivity, getString(R.string.feed_search),
                getString(R.string.feed_search_hint), null, ArrayList(defaultFeeds),
                SearchResultListener<SearchFeedResult> { dialog, item, position ->
                    toast(R.string.feed_added)
                    dialog.dismiss()

                    val feedToAdd = Feed(link = item.link, title = item.name)
                    feedToAdd.retrieveFullText = defaultFeeds.contains(item) // do that automatically for google news feeds

                    doAsync { App.db.feedDao().insert(feedToAdd) }
                })

        val apiFilter = object : BaseFilter<SearchFeedResult>() {
            override fun performFiltering(charSequence: CharSequence): FilterResults? {
                doBeforeFiltering()

                val results = FilterResults()
                val array = ArrayList<SearchFeedResult>()

                if (charSequence.isNotEmpty()) {
                    try {
                        val searchStr = charSequence.toString()

                        if (URLUtil.isNetworkUrl(searchStr)) {
                            FetcherService.createCall(searchStr).execute().use { response ->
                                val romeFeed = SyndFeedInput().build(XmlReader(response.body()!!.byteStream()))

                                array.add(SearchFeedResult(searchStr, romeFeed.title, romeFeed.description))
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            val searchUrl = "https://cloud.feedly.com/v3/search/feeds?count=20&locale=" + resources.configuration.locale.language + "&query=" + URLEncoder.encode(searchStr, "UTF-8")
                            FetcherService.createCall(searchUrl).execute().use {
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
                        }
                    } catch (ignored: Throwable) {
                    }
                }

                if (array.isEmpty()) {
                    array.addAll(defaultFeeds)
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

    private fun clearDetails(): Boolean {
        supportFragmentManager.findFragmentByTag(TAG_DETAILS)?.let {
            supportFragmentManager
                    .beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .remove(it)
                    .commitAllowingStateLoss()
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
            supportFragmentManager
                    .beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .replace(R.id.frame_master, master, TAG_MASTER)
                    .commitAllowingStateLoss()
        }
    }

    override fun goToEntryDetails(entry: EntryWithFeed, allEntryIds: List<String>) {
        closeKeyboard()

        if (containers_layout.hasTwoColumns()) {
            containers_layout.state = MainNavigator.State.TWO_COLUMNS_WITH_DETAILS
            val fragment = EntryDetailsFragment.newInstance(entry, allEntryIds)
            supportFragmentManager
                    .beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .replace(R.id.frame_details, fragment, TAG_DETAILS)
                    .commitAllowingStateLoss()

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
