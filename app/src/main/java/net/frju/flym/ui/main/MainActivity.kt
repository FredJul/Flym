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
import android.annotation.SuppressLint
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
import com.codekidlabs.storagechooser.StorageChooser
import com.rometools.opml.feed.opml.Attribute
import com.rometools.opml.feed.opml.Opml
import com.rometools.opml.feed.opml.Outline
import com.rometools.opml.io.impl.OPML20Generator
import com.rometools.rome.io.WireFeedInput
import com.rometools.rome.io.WireFeedOutput
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_edit_feed.view.*
import kotlinx.android.synthetic.main.fragment_entries.*
import kotlinx.android.synthetic.main.view_main_containers.*
import kotlinx.android.synthetic.main.view_main_drawer_header.*
import net.fred.feedex.R
import net.frju.flym.App
import net.frju.flym.data.entities.Feed
import net.frju.flym.data.entities.FeedWithCount
import net.frju.flym.data.utils.PrefUtils
import net.frju.flym.service.AutoRefreshJobService
import net.frju.flym.ui.about.AboutActivity
import net.frju.flym.ui.entries.EntriesFragment
import net.frju.flym.ui.entrydetails.EntryDetailsActivity
import net.frju.flym.ui.entrydetails.EntryDetailsFragment
import net.frju.flym.ui.feeds.FeedAdapter
import net.frju.flym.ui.feeds.FeedGroup
import net.frju.flym.ui.feeds.FeedListEditActivity
import net.frju.flym.ui.settings.SettingsActivity
import net.frju.flym.utils.closeKeyboard
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.notificationManager
import org.jetbrains.anko.sdk21.listeners.onClick
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.textColor
import org.jetbrains.anko.textResource
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.Reader
import java.io.StringReader
import java.io.Writer
import java.net.URL
import java.util.Date


class MainActivity : AppCompatActivity(), MainNavigator, AnkoLogger {

    companion object {
        const val EXTRA_FROM_NOTIF = "EXTRA_FROM_NOTIF"

		var isInForeground = false

        private const val TAG_DETAILS = "TAG_DETAILS"
        private const val TAG_MASTER = "TAG_MASTER"

        private const val OLD_GNEWS_TO_IGNORE = "http://news.google.com/news?"

        private const val AUTO_IMPORT_OPML_REQUEST_CODE = 1
        private const val CHOOSE_OPML_REQUEST_CODE = 2
        private const val EXPORT_OPML_REQUEST_CODE = 3
        private val NEEDED_PERMS = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        private val BACKUP_OPML = File(Environment.getExternalStorageDirectory(), "/Flym_auto_backup.opml")
        private const val RETRIEVE_FULLTEXT_OPML_ATTR = "retrieveFullText"
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
			FeedSearchDialog(this).show()
        }

		App.db.feedDao().observeAllWithCount.observe(this@MainActivity, Observer {
            it?.let {
                val newFeedGroups = mutableListOf<FeedGroup>()

				val all = FeedWithCount(feed = Feed().apply {
					id = Feed.ALL_ENTRIES_ID
					title = getString(R.string.all_entries)
				}, entryCount = it.sumBy { it.entryCount })
                newFeedGroups.add(FeedGroup(all, listOf()))

				val subFeedMap = it.groupBy { it.feed.groupId }

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
					} else {
						drawer_hint.textColor = Color.WHITE
						drawer_hint.textResource = R.string.drawer_explanation
					}
                }

				feedAdapter.onFeedClick { view, feedWithCount ->
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
										feed_link.setText(feedWithCount.feed.link)
									}

                                    AlertDialog.Builder(this@MainActivity)
											.setTitle(R.string.menu_edit_feed)
                                            .setView(input)
                                            .setPositiveButton(android.R.string.ok) { dialog, which ->
												val newName = input.feed_name.text.toString()
												val newLink = input.feed_link.text.toString()
												if (newName.isNotBlank() && newLink.isNotBlank()) {
                                                    doAsync {
														// Need to do a copy to not directly modify the memory and being able to detect changes
														val newFeed = feedWithCount.feed.copy().apply {
															title = newName
															link = newLink
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
                                            .setPositiveButton(android.R.string.yes) { _, _ ->
												doAsync { App.db.feedDao().delete(feedWithCount.feed) }
                                            }.setNegativeButton(android.R.string.no, null)
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

        toolbar.setNavigationIcon(R.drawable.ic_menu_24dp)
        toolbar.setNavigationOnClickListener { toggleDrawer() }

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
                                FeedSearchDialog(this).show()
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
			goToEntriesList(feedGroups[0].feedWithCount.feed)
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
			startActivity<EntryDetailsActivity>(EntryDetailsFragment.ARG_ENTRY_ID to entryId, EntryDetailsFragment.ARG_ALL_ENTRIES_IDS to allEntryIds)
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

	private fun isOldFlymAppInstalled() =
			packageManager.getInstalledApplications(PackageManager.GET_META_DATA).any { it.packageName == "net.fred.feedex" }

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

	@AfterPermissionGranted(CHOOSE_OPML_REQUEST_CODE)
	private fun pickOpml() {
		if (!EasyPermissions.hasPermissions(this, *NEEDED_PERMS)) {
			EasyPermissions.requestPermissions(this, getString(R.string.storage_request_explanation), CHOOSE_OPML_REQUEST_CODE, *NEEDED_PERMS)
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
			EasyPermissions.requestPermissions(this, getString(R.string.storage_request_explanation), EXPORT_OPML_REQUEST_CODE, *NEEDED_PERMS)
		} else {
			if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED || Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED_READ_ONLY) {
				doAsync {
					try {
						val opmlFileName = "Flym_" + System.currentTimeMillis() + ".opml"
						val opmlFilePath = Environment.getExternalStorageDirectory().toString() + "/" + opmlFileName

						exportOpml(FileWriter(opmlFilePath))

						uiThread { toast(String.format(getString(R.string.message_exported_to), opmlFileName)) }
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
			EasyPermissions.requestPermissions(this, getString(R.string.welcome_title_with_opml_import), AUTO_IMPORT_OPML_REQUEST_CODE, *NEEDED_PERMS)
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
					val fixedReader = StringReader(file.readText().replace("<opml version=['\"][0-9]\\.[0-9]['\"]>".toRegex(), "<opml>"))
					parseOpml(fixedReader)
				} catch (e: Exception) {
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
