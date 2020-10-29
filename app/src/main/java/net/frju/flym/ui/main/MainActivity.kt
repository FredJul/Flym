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

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.*
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import com.rometools.opml.feed.opml.Attribute
import com.rometools.opml.feed.opml.Opml
import com.rometools.opml.feed.opml.Outline
import com.rometools.opml.io.impl.OPML20Generator
import com.rometools.rome.io.WireFeedInput
import com.rometools.rome.io.WireFeedOutput
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_edit_feed.view.*
import kotlinx.android.synthetic.main.fragment_entries.*
import kotlinx.android.synthetic.main.view_main_drawer_header.*
import net.fred.feedex.R
import net.frju.flym.App
import net.frju.flym.data.entities.Feed
import net.frju.flym.data.entities.FeedWithCount
import net.frju.flym.data.utils.PrefConstants
import net.frju.flym.service.AutoRefreshJobService
import net.frju.flym.service.FetcherService
import net.frju.flym.ui.about.AboutActivity
import net.frju.flym.ui.discover.DiscoverActivity
import net.frju.flym.ui.entries.EntriesFragment
import net.frju.flym.ui.entrydetails.EntryDetailsActivity
import net.frju.flym.ui.entrydetails.EntryDetailsFragment
import net.frju.flym.ui.feeds.FeedAdapter
import net.frju.flym.ui.feeds.FeedGroup
import net.frju.flym.ui.feeds.FeedListEditActivity
import net.frju.flym.ui.settings.SettingsActivity
import net.frju.flym.utils.*
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk21.listeners.onClick
import pub.devrel.easypermissions.EasyPermissions
import java.io.*
import java.net.URL
import java.util.*


class MainActivity : AppCompatActivity(), MainNavigator, AnkoLogger {

    companion object {
        const val EXTRA_FROM_NOTIF = "EXTRA_FROM_NOTIF"

        var isInForeground = false

        private const val TAG_DETAILS = "TAG_DETAILS"
        private const val TAG_MASTER = "TAG_MASTER"

        private const val OLD_GNEWS_TO_IGNORE = "http://news.google.com/news?"

        private const val WRITE_OPML_REQUEST_CODE = 2
        private const val READ_OPML_REQUEST_CODE = 3
        private const val RETRIEVE_FULLTEXT_OPML_ATTR = "retrieveFullText"

        private const val INTENT_UNREADS = "net.frju.flym.intent.UNREADS"
        private const val INTENT_ALL = "net.frju.flym.intent.ALL"
        private const val INTENT_FAVORITES = "net.frju.flym.intent.FAVORITES"
    }

    private val feedGroups = mutableListOf<FeedGroup>()
    private val feedAdapter = FeedAdapter(feedGroups)

    override fun onCreate(savedInstanceState: Bundle?) {
        setupNoActionBarTheme()

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        more.onClick {
            it?.let { view ->
                PopupMenu(this@MainActivity, view).apply {
                    menuInflater.inflate(R.menu.menu_drawer_header, menu)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.reorder -> startActivity<FeedListEditActivity>()
                            R.id.import_feeds -> pickOpml()
                            R.id.export_feeds -> exportOpml()
                            R.id.menu_entries__about -> goToAboutMe()
                            R.id.menu_entries__settings -> goToSettings()
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
            goToFeedSearch()
        }

        App.db.feedDao().observeAllWithCount.observe(this@MainActivity, { nullableFeeds ->
            nullableFeeds?.let { feeds ->
                val newFeedGroups = mutableListOf<FeedGroup>()

                val all = FeedWithCount(feed = Feed().apply {
                    id = Feed.ALL_ENTRIES_ID
                    title = getString(R.string.all_entries)
                }, entryCount = feeds.sumBy { it.entryCount })
                newFeedGroups.add(FeedGroup(all, listOf()))

                val subFeedMap = feeds.groupBy { it.feed.groupId }

                newFeedGroups.addAll(
                        subFeedMap[null]?.map { FeedGroup(it, subFeedMap[it.feed.id].orEmpty()) }.orEmpty()
                )

                // Do not always call notifyParentDataSetChanged to avoid selection loss during refresh
                if (hasFeedGroupsChanged(feedGroups, newFeedGroups)) {
                    feedGroups.clear()
                    feedGroups += newFeedGroups
                    feedAdapter.notifyParentDataSetChanged(true)

                    if (hasFetchingError()) {
                        drawer_hint.textColor = Color.RED
                        drawer_hint.textResource = R.string.drawer_fetch_error_explanation
                        toolbar.setNavigationIcon(R.drawable.ic_menu_red_highlight_24dp)
                    } else {
                        drawer_hint.textColor = Color.WHITE
                        drawer_hint.textResource = R.string.drawer_explanation
                        toolbar.setNavigationIcon(R.drawable.ic_menu_24dp)
                    }
                }

                feedAdapter.onFeedClick { _, feedWithCount ->
                    goToEntriesList(feedWithCount.feed)
                    closeDrawer()
                }

                feedAdapter.onFeedLongClick { view, feedWithCount ->
                    PopupMenu(this, view).apply {
                        setOnMenuItemClickListener { item ->
                            when (item.itemId) {
                                R.id.mark_all_as_read -> doAsync {
                                    when {
                                        feedWithCount.feed.id == Feed.ALL_ENTRIES_ID -> App.db.entryDao().markAllAsRead()
                                        feedWithCount.feed.isGroup -> App.db.entryDao().markGroupAsRead(feedWithCount.feed.id)
                                        else -> App.db.entryDao().markAsRead(feedWithCount.feed.id)
                                    }
                                }
                                R.id.edit_feed -> {
                                    @SuppressLint("InflateParams")
                                    val input = layoutInflater.inflate(R.layout.dialog_edit_feed, null, false).apply {
                                        feed_name.setText(feedWithCount.feed.title)
                                        if (feedWithCount.feed.isGroup) {
                                            feed_link.isGone = true
                                        } else {
                                            feed_link.setText(feedWithCount.feed.link)
                                        }
                                    }

                                    AlertDialog.Builder(this@MainActivity)
                                            .setTitle(R.string.menu_edit_feed)
                                            .setView(input)
                                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                                val newName = input.feed_name.text.toString()
                                                val newLink = input.feed_link.text.toString()
                                                if (newName.isNotBlank() && (newLink.isNotBlank() || feedWithCount.feed.isGroup)) {
                                                    doAsync {
                                                        // Need to do a copy to not directly modify the memory and being able to detect changes
                                                        val newFeed = feedWithCount.feed.copy().apply {
                                                            title = newName
                                                            if (!feedWithCount.feed.isGroup) {
                                                                link = newLink
                                                            }
                                                        }
                                                        App.db.feedDao().update(newFeed)
                                                    }
                                                }
                                            }
                                            .setNegativeButton(android.R.string.cancel, null)
                                            .show()
                                }
                                R.id.reorder -> startActivity<FeedListEditActivity>()
                                R.id.delete -> {
                                    AlertDialog.Builder(this@MainActivity)
                                            .setTitle(feedWithCount.feed.title)
                                            .setMessage(if (feedWithCount.feed.isGroup) R.string.question_delete_group else R.string.question_delete_feed)
                                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                                doAsync { App.db.feedDao().delete(feedWithCount.feed) }
                                            }.setNegativeButton(android.R.string.cancel, null)
                                            .show()
                                }
                                R.id.enable_full_text_retrieval -> doAsync { App.db.feedDao().enableFullTextRetrieval(feedWithCount.feed.id) }
                                R.id.disable_full_text_retrieval -> doAsync { App.db.feedDao().disableFullTextRetrieval(feedWithCount.feed.id) }
                            }
                            true
                        }
                        inflate(R.menu.menu_drawer_feed)

                        when {
                            feedWithCount.feed.id == Feed.ALL_ENTRIES_ID -> {
                                menu.findItem(R.id.edit_feed).isVisible = false
                                menu.findItem(R.id.delete).isVisible = false
                                menu.findItem(R.id.reorder).isVisible = false
                                menu.findItem(R.id.enable_full_text_retrieval).isVisible = false
                                menu.findItem(R.id.disable_full_text_retrieval).isVisible = false
                            }
                            feedWithCount.feed.isGroup -> {
                                menu.findItem(R.id.enable_full_text_retrieval).isVisible = false
                                menu.findItem(R.id.disable_full_text_retrieval).isVisible = false
                            }
                            feedWithCount.feed.retrieveFullText -> menu.findItem(R.id.enable_full_text_retrieval).isVisible = false
                            else -> menu.findItem(R.id.disable_full_text_retrieval).isVisible = false
                        }

                        show()
                    }
                }
            }
        })

        if (savedInstanceState == null) {
            // First open => we open the drawer for you
            if (getPrefBoolean(PrefConstants.FIRST_OPEN, true)) {
                putPrefBoolean(PrefConstants.FIRST_OPEN, false)
                openDrawer()

                showAlertDialog(R.string.welcome_title) { goToFeedSearch() }
            } else {
                closeDrawer()
            }

            goToEntriesList(null)
        }

        if (getPrefBoolean(PrefConstants.REFRESH_ON_STARTUP, defValue = true)) {
            try { // Some people seems to sometimes have a IllegalStateException on this
                startService(Intent(this, FetcherService::class.java)
                        .setAction(FetcherService.ACTION_REFRESH_FEEDS)
                        .putExtra(FetcherService.FROM_AUTO_REFRESH, true))
            } catch (t: Throwable) {
                // Nothing to do, the refresh can still be triggered manually
            }
        }

        AutoRefreshJobService.initAutoRefresh(this)

        handleImplicitIntent(intent)
    }

    override fun onStart() {
        super.onStart()

        if (getPrefBoolean(PrefConstants.HIDE_NAVIGATION_ON_SCROLL, false)) {
            ViewCompat.setOnApplyWindowInsetsListener(nav) { _, insets ->
                val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                nav.updatePadding(bottom = systemInsets.bottom)
                drawer.updatePadding(left = systemInsets.left, right = systemInsets.right)
                guideline.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    guideBegin = systemInsets.top
                }
                drawer_content.updatePadding(left = systemInsets.left)
                drawer_content.updateLayoutParams<DrawerLayout.LayoutParams> {
                    width = resources.getDimensionPixelSize(R.dimen.nav_drawer_width) + systemInsets.left
                }
                insets
            }
        } else {
            ViewCompat.setOnApplyWindowInsetsListener(nav, null)
            nav.updatePadding(bottom = 0)
            drawer.updatePadding(left = 0, right = 0)
            guideline.updateLayoutParams<ConstraintLayout.LayoutParams> {
                guideBegin = 0
            }
            drawer_content.updatePadding(left = 0)
            drawer_content.updateLayoutParams<DrawerLayout.LayoutParams> {
                width = resources.getDimensionPixelSize(R.dimen.nav_drawer_width)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        setIntent(intent)

        handleImplicitIntent(intent)
    }

    private fun handleImplicitIntent(intent: Intent?) {
        // Has to be called on onStart (when the app is closed) and on onNewIntent (when the app is in the background)

        // Add feed urls from Open with
        if (intent?.action.equals(Intent.ACTION_VIEW)) {
            val search: String = intent?.data.toString()
            DiscoverActivity.newInstance(this, search)
            setIntent(null)
        }
        // Add feed urls from Share menu
        if (intent?.action.equals(Intent.ACTION_SEND)) {
            if (intent?.hasExtra(Intent.EXTRA_TEXT) == true) {
                val search = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (search != null) {
                    DiscoverActivity.newInstance(this, search)
                }
            }
            setIntent(null)
        }

        // If we just clicked on the notification, let's go back to the default view
        if (intent?.getBooleanExtra(EXTRA_FROM_NOTIF, false) == true && feedGroups.isNotEmpty()) {
            feedAdapter.selectedItemId = Feed.ALL_ENTRIES_ID
            goToEntriesList(feedGroups[0].feedWithCount.feed)
            bottom_navigation.selectedItemId = R.id.unreads
        }
    }

    private fun handleResumeOnlyIntents(intent: Intent?) {

        // If it comes from the All feeds App Shortcuts, select the right view
        if (intent?.action.equals(INTENT_ALL) && bottom_navigation.selectedItemId != R.id.all) {
            feedAdapter.selectedItemId = Feed.ALL_ENTRIES_ID
            bottom_navigation.selectedItemId = R.id.all
        }

        // If it comes from the Favorites feeds App Shortcuts, select the right view
        if (intent?.action.equals(INTENT_FAVORITES) && bottom_navigation.selectedItemId != R.id.favorites) {
            feedAdapter.selectedItemId = Feed.ALL_ENTRIES_ID
            bottom_navigation.selectedItemId = R.id.favorites
        }

        // If it comes from the Unreads feeds App Shortcuts, select the right view
        if (intent?.action.equals(INTENT_UNREADS) && bottom_navigation.selectedItemId != R.id.unreads) {
            feedAdapter.selectedItemId = Feed.ALL_ENTRIES_ID
            bottom_navigation.selectedItemId = R.id.unreads
        }
    }

    override fun onResume() {
        super.onResume()

        isInForeground = true
        notificationManager.cancel(0)

        handleResumeOnlyIntents(intent)
    }

    override fun onPause() {
        super.onPause()
        isInForeground = false
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        feedAdapter.onRestoreInstanceState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        feedAdapter.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
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

    override fun goToFeedSearch() = DiscoverActivity.newInstance(this)

    override fun goToEntryDetails(entryId: String, allEntryIds: List<String>) {
        closeKeyboard()

        if (containers_layout.hasTwoColumns()) {
            containers_layout.state = MainNavigator.State.TWO_COLUMNS_WITH_DETAILS
            val fragment = EntryDetailsFragment.newInstance(entryId, allEntryIds)
            supportFragmentManager
                    .beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .replace(R.id.frame_details, fragment, TAG_DETAILS)
                    .commitAllowingStateLoss()

            val listFragment = supportFragmentManager.findFragmentById(R.id.frame_master) as EntriesFragment
            listFragment.setSelectedEntryId(entryId)
        } else {
            if (getPrefBoolean(PrefConstants.OPEN_BROWSER_DIRECTLY, false)) {
                openInBrowser(entryId)
            } else {
                startActivity<EntryDetailsActivity>(EntryDetailsFragment.ARG_ENTRY_ID to entryId, EntryDetailsFragment.ARG_ALL_ENTRIES_IDS to allEntryIds.take(500)) // take() to avoid TransactionTooLargeException
            }
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

    private fun openInBrowser(entryId: String) {
        doAsync {
            App.db.entryDao().findByIdWithFeed(entryId)?.entry?.link?.let { url ->
                App.db.entryDao().markAsRead(listOf(entryId))
                browse(url)
            }
        }
    }

    private fun hasFeedGroupsChanged(feedGroups: List<FeedGroup>, newFeedGroups: List<FeedGroup>): Boolean {
        if (feedGroups != newFeedGroups) {
            return true
        }

        // Also need to check all sub groups (can't be checked in FeedGroup's equals)
        feedGroups.forEachIndexed { index, feedGroup ->
            if (feedGroup.feedWithCount != newFeedGroups[index].feedWithCount || feedGroup.subFeeds != newFeedGroups[index].subFeeds) {
                return true
            }
        }

        return false
    }

    private fun hasFetchingError(): Boolean {
        // Also need to check all sub groups (can't be checked in FeedGroup's equals)
        feedGroups.forEach { feedGroup ->
            if (feedGroup.feedWithCount.feed.fetchError || feedGroup.subFeeds.any { it.feed.fetchError }) {
                return true
            }
        }

        return false
    }

    private fun pickOpml() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*" // https://github.com/FredJul/Flym/issues/407
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, READ_OPML_REQUEST_CODE)
    }

    private fun exportOpml() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = "text/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_TITLE, "Flym_" + System.currentTimeMillis() + ".opml")
        }
        startActivityForResult(intent, WRITE_OPML_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)

        if (requestCode == READ_OPML_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            resultData?.data?.also { uri -> importOpml(uri) }
        } else if (requestCode == WRITE_OPML_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            resultData?.data?.also { uri -> exportOpml(uri) }
        }
    }

    private fun importOpml(uri: Uri) {
        doAsync {
            try {
                InputStreamReader(contentResolver.openInputStream(uri)!!).use { reader -> parseOpml(reader) }
            } catch (e: Exception) {
                try {
                    // We try to remove the opml version number, it may work better in some cases
                    val content = BufferedInputStream(contentResolver.openInputStream(uri)!!).bufferedReader().use { it.readText() }
                    val fixedReader = StringReader(content.replace("<opml version=['\"][0-9]\\.[0-9]['\"]>".toRegex(), "<opml>"))
                    parseOpml(fixedReader)
                } catch (e: Exception) {
                    uiThread { toast(R.string.cannot_find_feeds) }
                }
            }
        }
    }

    private fun exportOpml(uri: Uri) {
        doAsync {
            try {
                OutputStreamWriter(contentResolver.openOutputStream(uri)!!, Charsets.UTF_8).use { writer -> exportOpml(writer) }
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                        uiThread { toast(String.format(getString(R.string.message_exported_to), fileName)) }
                    }
                }
            } catch (e: Exception) {
                uiThread { toast(R.string.error_feed_export) }
            }
        }
    }

    private fun parseOpml(opmlReader: Reader) {
        var genId = 1L
        val feedList = mutableListOf<Feed>()
        val opml = WireFeedInput().build(opmlReader) as Opml
        opml.outlines.forEach { outline ->
            if (outline.xmlUrl != null || outline.children.isNotEmpty()) {
                val topLevelFeed = Feed().apply {
                    id = genId++
                    title = outline.title
                }

                if (outline.xmlUrl != null) {
                    if (!outline.xmlUrl.startsWith(OLD_GNEWS_TO_IGNORE)) {
                        topLevelFeed.link = outline.xmlUrl
                        topLevelFeed.retrieveFullText = outline.getAttributeValue(RETRIEVE_FULLTEXT_OPML_ATTR) == "true"
                        feedList.add(topLevelFeed)
                    }
                } else {
                    topLevelFeed.isGroup = true
                    feedList.add(topLevelFeed)

                    outline.children.filter { it.xmlUrl != null && !it.xmlUrl.startsWith(OLD_GNEWS_TO_IGNORE) }.forEach {
                        val subLevelFeed = Feed().apply {
                            id = genId++
                            title = it.title
                            link = it.xmlUrl
                            retrieveFullText = it.getAttributeValue(RETRIEVE_FULLTEXT_OPML_ATTR) == "true"
                            groupId = topLevelFeed.id
                        }

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
            outlines = feeds[null]?.map { feed ->
                Outline(feed.title, if (feed.link.isNotBlank()) URL(feed.link) else null, null).apply {
                    children = feeds[feed.id]?.map {
                        Outline(it.title, if (it.link.isNotBlank()) URL(it.link) else null, null).apply {
                            if (it.retrieveFullText) {
                                attributes.add(Attribute(RETRIEVE_FULLTEXT_OPML_ATTR, "true"))
                            }
                        }
                    }
                    if (feed.retrieveFullText) {
                        attributes.add(Attribute(RETRIEVE_FULLTEXT_OPML_ATTR, "true"))
                    }
                }
            }
        }

        WireFeedOutput().output(opml, opmlWriter)
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

    fun toggleDrawer() {
        if (drawer?.isDrawerOpen(GravityCompat.START) == true) {
            drawer?.closeDrawer(GravityCompat.START)
        } else {
            drawer?.openDrawer(GravityCompat.START)
        }
    }

    private fun goBack(): Boolean {
        if (containers_layout.state != MainNavigator.State.TWO_COLUMNS_WITH_DETAILS || containers_layout.hasTwoColumns()) return false
        if (clearDetails()) {
            containers_layout.state = MainNavigator.State.TWO_COLUMNS_EMPTY
            return true
        }
        return false
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
}
