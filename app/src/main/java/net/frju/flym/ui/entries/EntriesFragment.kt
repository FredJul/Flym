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

package net.frju.flym.ui.entries

import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_entries.*
import kotlinx.android.synthetic.main.view_entry.view.*
import kotlinx.android.synthetic.main.view_main_containers.*
import net.fred.feedex.R
import net.frju.flym.App
import net.frju.flym.data.entities.EntryWithFeed
import net.frju.flym.data.entities.Feed
import net.frju.flym.data.utils.PrefConstants
import net.frju.flym.service.FetcherService
import net.frju.flym.ui.main.MainNavigator
import net.frju.flym.utils.closeKeyboard
import net.frju.flym.utils.getPrefBoolean
import net.frju.flym.utils.registerOnPrefChangeListener
import net.frju.flym.utils.unregisterOnPrefChangeListener
import org.jetbrains.anko.appcompat.v7.titleResource
import org.jetbrains.anko.attr
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.colorAttr
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.notificationManager
import org.jetbrains.anko.sdk21.listeners.onClick
import org.jetbrains.anko.support.v4.defaultSharedPreferences
import org.jetbrains.anko.support.v4.dip
import org.jetbrains.anko.support.v4.share
import org.jetbrains.anko.uiThread
import q.rorbin.badgeview.Badge
import q.rorbin.badgeview.QBadgeView
import java.util.Date


class EntriesFragment : Fragment() {

	companion object {

		private const val ARG_FEED = "ARG_FEED"
		private const val STATE_FEED = "STATE_FEED"
		private const val STATE_SEARCH_TEXT = "STATE_SEARCH_TEXT"
		private const val STATE_SELECTED_ENTRY_ID = "STATE_SELECTED_ENTRY_ID"
		private const val STATE_LIST_DISPLAY_DATE = "STATE_LIST_DISPLAY_DATE"

		fun newInstance(feed: Feed?): EntriesFragment {
			return EntriesFragment().apply {
				feed?.let {
					arguments = bundleOf(ARG_FEED to feed)
				}
			}
		}
	}

	var feed: Feed? = null
		set(value) {
			field = value

			setupTitle()
			bottom_navigation.post { initDataObservers() } // Needed to retrieve the correct selected tab position
		}

	private val navigator: MainNavigator by lazy { activity as MainNavigator }

	private val adapter = EntryAdapter({ entryWithFeed ->
		navigator.goToEntryDetails(entryWithFeed.entry.id, entryIds!!)
	}, { entryWithFeed ->
		share(entryWithFeed.entry.link.orEmpty(), entryWithFeed.entry.title.orEmpty())
	}, { entryWithFeed, view ->
		entryWithFeed.entry.favorite = !entryWithFeed.entry.favorite

		view.favorite_icon?.let {
			if (entryWithFeed.entry.favorite) {
				it.setImageResource(R.drawable.ic_star_24dp)
			} else {
				it.setImageResource(R.drawable.ic_star_border_24dp)
			}
		}

		doAsync {
			if (entryWithFeed.entry.favorite) {
				App.db.entryDao().markAsFavorite(entryWithFeed.entry.id)
			} else {
				App.db.entryDao().markAsNotFavorite(entryWithFeed.entry.id)
			}
		}
	})
	private var listDisplayDate = Date().time
	private var entriesLiveData: LiveData<PagedList<EntryWithFeed>>? = null
	private var entryIdsLiveData: LiveData<List<String>>? = null
	private var entryIds: List<String>? = null
	private var newCountLiveData: LiveData<Long>? = null
	private var unreadBadge: Badge? = null
	private var searchText: String? = null
	private val searchHandler = Handler()

	private val prefListener = OnSharedPreferenceChangeListener { sharedPreferences, key ->
		if (PrefConstants.IS_REFRESHING == key) {
			refreshSwipeProgress()
		}
	}

	init {
		setHasOptionsMenu(true)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
			inflater.inflate(R.layout.fragment_entries, container, false)

	override fun onActivityCreated(savedInstanceState: Bundle?) {
		super.onActivityCreated(savedInstanceState)

		if (savedInstanceState != null) {
			feed = savedInstanceState.getParcelable(STATE_FEED)
			adapter.selectedEntryId = savedInstanceState.getString(STATE_SELECTED_ENTRY_ID)
			listDisplayDate = savedInstanceState.getLong(STATE_LIST_DISPLAY_DATE)
			searchText = savedInstanceState.getString(STATE_SEARCH_TEXT)
		} else {
			feed = arguments?.getParcelable(ARG_FEED)
		}

		setupRecyclerView()

		bottom_navigation.setOnNavigationItemSelectedListener {
			recycler_view.post {
				listDisplayDate = Date().time
				initDataObservers()
				recycler_view.scrollToPosition(0)
			}

			activity?.toolbar?.menu?.findItem(R.id.menu_entries__share)?.isVisible = it.itemId == R.id.favorites
			true
		}

		unreadBadge = QBadgeView(context).bindTarget((bottom_navigation.getChildAt(0) as ViewGroup).getChildAt(0)).apply {
			setGravityOffset(35F, 0F, true)
			isShowShadow = false
			badgeBackgroundColor = when (defaultSharedPreferences.getString(PrefConstants.THEME, "DARK")) {
				"BLACK" -> ResourcesCompat.getColor(resources, R.color.colorAccentBlack, null)
				else -> requireContext().colorAttr(R.attr.colorPrimaryDark)
			}
		}

		read_all_fab.onClick { _ ->
			entryIds?.let { entryIds ->
				if (entryIds.isNotEmpty()) {
					doAsync {
						// TODO check if limit still needed
						entryIds.withIndex().groupBy { it.index / 300 }.map { pair -> pair.value.map { it.value } }.forEach {
							App.db.entryDao().markAsRead(it)
						}
					}

					coordinator.longSnackbar(R.string.marked_as_read, R.string.undo) { _ ->
						doAsync {
							// TODO check if limit still needed
							entryIds.withIndex().groupBy { it.index / 300 }.map { pair -> pair.value.map { it.value } }.forEach {
								App.db.entryDao().markAsUnread(it)
							}

							uiThread {
								// we need to wait for the list to be empty before displaying the new items (to avoid scrolling issues)
								listDisplayDate = Date().time
								initDataObservers()
							}
						}
					}
				}

				if (feed == null || feed?.id == Feed.ALL_ENTRIES_ID) {
					activity?.notificationManager?.cancel(0)
				}
			}
		}
	}

	private fun initDataObservers() {
		entryIdsLiveData?.removeObservers(this)
		entryIdsLiveData = when {
			searchText != null -> App.db.entryDao().observeIdsBySearch(searchText!!)
			feed?.isGroup == true && bottom_navigation.selectedItemId == R.id.unreads -> App.db.entryDao().observeUnreadIdsByGroup(feed!!.id, listDisplayDate)
			feed?.isGroup == true && bottom_navigation.selectedItemId == R.id.favorites -> App.db.entryDao().observeFavoriteIdsByGroup(feed!!.id, listDisplayDate)
			feed?.isGroup == true -> App.db.entryDao().observeIdsByGroup(feed!!.id, listDisplayDate)

			feed != null && feed?.id != Feed.ALL_ENTRIES_ID && bottom_navigation.selectedItemId == R.id.unreads -> App.db.entryDao().observeUnreadIdsByFeed(feed!!.id, listDisplayDate)
			feed != null && feed?.id != Feed.ALL_ENTRIES_ID && bottom_navigation.selectedItemId == R.id.favorites -> App.db.entryDao().observeFavoriteIdsByFeed(feed!!.id, listDisplayDate)
			feed != null && feed?.id != Feed.ALL_ENTRIES_ID -> App.db.entryDao().observeIdsByFeed(feed!!.id, listDisplayDate)

			bottom_navigation.selectedItemId == R.id.unreads -> App.db.entryDao().observeAllUnreadIds(listDisplayDate)
			bottom_navigation.selectedItemId == R.id.favorites -> App.db.entryDao().observeAllFavoriteIds(listDisplayDate)
			else -> App.db.entryDao().observeAllIds(listDisplayDate)
		}

		entryIdsLiveData?.observe(this, Observer<List<String>> { list ->
			entryIds = list
		})

		entriesLiveData?.removeObservers(this)
		entriesLiveData = LivePagedListBuilder(when {
			searchText != null -> App.db.entryDao().observeSearch(searchText!!)
			feed?.isGroup == true && bottom_navigation.selectedItemId == R.id.unreads -> App.db.entryDao().observeUnreadsByGroup(feed!!.id, listDisplayDate)
			feed?.isGroup == true && bottom_navigation.selectedItemId == R.id.favorites -> App.db.entryDao().observeFavoritesByGroup(feed!!.id, listDisplayDate)
			feed?.isGroup == true -> App.db.entryDao().observeByGroup(feed!!.id, listDisplayDate)

			feed != null && feed?.id != Feed.ALL_ENTRIES_ID && bottom_navigation.selectedItemId == R.id.unreads -> App.db.entryDao().observeUnreadsByFeed(feed!!.id, listDisplayDate)
			feed != null && feed?.id != Feed.ALL_ENTRIES_ID && bottom_navigation.selectedItemId == R.id.favorites -> App.db.entryDao().observeFavoritesByFeed(feed!!.id, listDisplayDate)
			feed != null && feed?.id != Feed.ALL_ENTRIES_ID -> App.db.entryDao().observeByFeed(feed!!.id, listDisplayDate)

			bottom_navigation.selectedItemId == R.id.unreads -> App.db.entryDao().observeAllUnreads(listDisplayDate)
			bottom_navigation.selectedItemId == R.id.favorites -> App.db.entryDao().observeAllFavorites(listDisplayDate)
			else -> App.db.entryDao().observeAll(listDisplayDate)
		}, 30).build()

		entriesLiveData?.observe(this, Observer<PagedList<EntryWithFeed>> { pagedList ->
			adapter.submitList(pagedList)
		})

		newCountLiveData?.removeObservers(this)
		newCountLiveData = when {
			feed?.isGroup == true -> App.db.entryDao().observeNewEntriesCountByGroup(feed!!.id, listDisplayDate)
			feed != null && feed?.id != Feed.ALL_ENTRIES_ID -> App.db.entryDao().observeNewEntriesCountByFeed(feed!!.id, listDisplayDate)
			else -> App.db.entryDao().observeNewEntriesCount(listDisplayDate)
		}

		newCountLiveData?.observe(this, Observer<Long> { count ->
			if (count != null && count > 0L) {
				// If we have an empty list, let's immediately display the new items
				if (entryIds?.isEmpty() == true && bottom_navigation.selectedItemId != R.id.favorites) {
					listDisplayDate = Date().time
					initDataObservers()
				} else {
					unreadBadge?.badgeNumber = count.toInt()
				}
			} else {
				unreadBadge?.hide(false)
			}
		})
	}

	override fun onStart() {
		super.onStart()
		context?.registerOnPrefChangeListener(prefListener)
		refreshSwipeProgress()
	}

	override fun onStop() {
		super.onStop()
		context?.unregisterOnPrefChangeListener(prefListener)
	}

	override fun onSaveInstanceState(outState: Bundle) {
		outState.putParcelable(STATE_FEED, feed)
		outState.putString(STATE_SELECTED_ENTRY_ID, adapter.selectedEntryId)
		outState.putLong(STATE_LIST_DISPLAY_DATE, listDisplayDate)
		outState.putString(STATE_SEARCH_TEXT, searchText)

		super.onSaveInstanceState(outState)
	}

	private fun setupRecyclerView() {
		recycler_view.setHasFixedSize(true)

		val layoutManager = LinearLayoutManager(activity)
		recycler_view.layoutManager = layoutManager
		recycler_view.adapter = adapter

		refresh_layout.setColorScheme(R.color.colorAccent,
				requireContext().attr(R.attr.colorPrimaryDark).resourceId,
				R.color.colorAccent,
				requireContext().attr(R.attr.colorPrimaryDark).resourceId)

		refresh_layout.setOnRefreshListener {
			startRefresh()
		}

		val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
			private val VELOCITY = dip(800).toFloat()

			override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
				return false
			}

			override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
				return VELOCITY
			}

			override fun getSwipeVelocityThreshold(defaultValue: Float): Float {
				return VELOCITY
			}

			override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
				adapter.currentList?.get(viewHolder.adapterPosition)?.let { entryWithFeed ->
					entryWithFeed.entry.read = !entryWithFeed.entry.read
					doAsync {
						if (entryWithFeed.entry.read) {
							App.db.entryDao().markAsRead(listOf(entryWithFeed.entry.id))
						} else {
							App.db.entryDao().markAsUnread(listOf(entryWithFeed.entry.id))
						}

						coordinator.longSnackbar(R.string.marked_as_read, R.string.undo) { _ ->
							doAsync {
								if (entryWithFeed.entry.read) {
									App.db.entryDao().markAsUnread(listOf(entryWithFeed.entry.id))
								} else {
									App.db.entryDao().markAsRead(listOf(entryWithFeed.entry.id))
								}
							}
						}

						if (bottom_navigation.selectedItemId != R.id.unreads) {
							uiThread {
								adapter.notifyItemChanged(viewHolder.adapterPosition)
							}
						}
					}
				}
			}
		}

		// attaching the touch helper to recycler view
		ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recycler_view)

		recycler_view.emptyView = empty_view

		recycler_view.addOnScrollListener(object : RecyclerView.OnScrollListener() {
			override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
				super.onScrollStateChanged(recyclerView, newState)
				activity?.closeKeyboard()
			}
		})
	}

	private fun startRefresh() {
		if (context?.getPrefBoolean(PrefConstants.IS_REFRESHING, false) == false) {
			if (feed?.isGroup == false && feed?.id != Feed.ALL_ENTRIES_ID) {
				context?.startService(Intent(context, FetcherService::class.java).setAction(FetcherService.ACTION_REFRESH_FEEDS).putExtra(FetcherService.EXTRA_FEED_ID,
						feed?.id))
			} else {
				context?.startService(Intent(context, FetcherService::class.java).setAction(FetcherService.ACTION_REFRESH_FEEDS))
			}
		}

		// In case there is no internet, the service won't even start, let's quickly stop the refresh animation
		refresh_layout.postDelayed({ refreshSwipeProgress() }, 500)
	}

	private fun setupTitle() {
		activity?.toolbar?.apply {
			if (feed == null || feed?.id == Feed.ALL_ENTRIES_ID) {
				titleResource = R.string.all_entries
			} else {
				title = feed?.title
			}
		}
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		super.onCreateOptionsMenu(menu, inflater)

		inflater.inflate(R.menu.menu_fragment_entries, menu)

		menu.findItem(R.id.menu_entries__share).isVisible = bottom_navigation.selectedItemId == R.id.favorites

		val searchItem = menu.findItem(R.id.menu_entries__search)
		val searchView = searchItem.actionView as SearchView
		if (searchText != null) {
			searchItem.expandActionView()
			searchView.post {
				searchView.setQuery(searchText, false)
				searchView.clearFocus()
			}
		}

		searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
			override fun onQueryTextSubmit(query: String): Boolean {
				return false
			}

			override fun onQueryTextChange(newText: String): Boolean {
				if (searchText != null) { // needed because it can actually be called after the onMenuItemActionCollapse event
					searchText = newText

					// In order to avoid plenty of request, we add a small throttle time
					searchHandler.removeCallbacksAndMessages(null)
					searchHandler.postDelayed({
						initDataObservers()
					}, 700)
				}
				return false
			}
		})
		searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
			override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
				searchText = ""
				initDataObservers()
				bottom_navigation.isGone = true
				return true
			}

			override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
				searchText = null
				initDataObservers()
				bottom_navigation.isVisible = true
				return true
			}
		})
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.menu_entries__share -> {
				// TODO: will only work for the visible 30 items, need to find something better
				adapter.currentList?.joinToString("\n\n") { it.entry.title + ": " + it.entry.link }?.let { content ->
					val title = getString(R.string.app_name) + " " + getString(R.string.favorites)
					share(content.take(300000), title) // take() to avoid crashing with a too big intent
				}
			}
			R.id.menu_entries__about -> {
				navigator.goToAboutMe()
			}
			R.id.menu_entries__settings -> {
				navigator.goToSettings()
			}
		}

		return true
	}

	fun setSelectedEntryId(selectedEntryId: String) {
		adapter.selectedEntryId = selectedEntryId
	}

	private fun refreshSwipeProgress() {
		refresh_layout.isRefreshing = context?.getPrefBoolean(PrefConstants.IS_REFRESHING, false) ?: false
	}
}
