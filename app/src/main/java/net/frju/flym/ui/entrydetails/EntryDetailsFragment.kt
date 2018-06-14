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

package net.frju.flym.ui.entrydetails

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_entry_details.*
import me.thanel.swipeactionview.SwipeActionView
import me.thanel.swipeactionview.SwipeGestureListener
import net.fred.feedex.R
import net.frju.flym.App
import net.frju.flym.data.entities.EntryWithFeed
import net.frju.flym.data.utils.PrefUtils
import net.frju.flym.service.FetcherService
import net.frju.flym.ui.main.MainNavigator
import net.frju.flym.utils.isOnline
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.uiThread
import org.jetbrains.annotations.NotNull


class EntryDetailsFragment : Fragment() {

	companion object {

		const val ARG_ENTRY = "ARG_ENTRY"
		const val ARG_ALL_ENTRIES_IDS = "ARG_ALL_ENTRIES_IDS"

		fun newInstance(entry: EntryWithFeed, allEntryIds: List<String>): EntryDetailsFragment {
			return EntryDetailsFragment().apply {
				arguments = bundleOf(ARG_ENTRY to entry, ARG_ALL_ENTRIES_IDS to allEntryIds)
			}
		}
	}

	private val navigator: MainNavigator? by lazy { activity as? MainNavigator }

	private lateinit var entry: EntryWithFeed
	private var allEntryIds = emptyList<String>()
		set(value) {
			field = value

			val currentIdx = value.indexOf(entry.id)

			previousId = if (currentIdx <= 0) {
				null
			} else {
				value[currentIdx - 1]
			}

			nextId = if (currentIdx < 0 || currentIdx >= value.size - 1) {
				null
			} else {
				value[currentIdx + 1]
			}
		}
	private var previousId: String? = null
	private var nextId: String? = null
	private var isMobilizingLiveData: LiveData<Int>? = null
	private var preferFullText = true

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
			inflater.inflate(R.layout.fragment_entry_details, container, false)

	override fun onDestroyView() {
		super.onDestroyView()
		entry_view.destroy()
	}

	override fun onActivityCreated(savedInstanceState: Bundle?) {
		super.onActivityCreated(savedInstanceState)

		refresh_layout.setColorScheme(R.color.colorAccent,
				R.color.colorPrimaryDark,
				R.color.colorAccent,
				R.color.colorPrimaryDark)

		refresh_layout.isEnabled = false

		doAsync {
			// getting the parcelable on UI thread can make it sluggish
			entry = arguments?.getParcelable(ARG_ENTRY)!!
			allEntryIds = arguments?.getStringArrayList(ARG_ALL_ENTRIES_IDS)!!

			uiThread {
				swipe_view.swipeGestureListener = object : SwipeGestureListener {
					override fun onSwipedLeft(@NotNull swipeActionView: SwipeActionView): Boolean {
						nextId?.let { nextId ->
							doAsync {
								App.db.entryDao().findByIdWithFeed(nextId)?.let { newEntry ->
									uiThread {
										setEntry(newEntry, allEntryIds)
										navigator?.setSelectedEntryId(newEntry.id)
									}
								}
							}
						}
						return true
					}

					override fun onSwipedRight(@NotNull swipeActionView: SwipeActionView): Boolean {
						previousId?.let { previousId ->
							doAsync {
								App.db.entryDao().findByIdWithFeed(previousId)?.let { newEntry ->
									uiThread {
										setEntry(newEntry, allEntryIds)
										navigator?.setSelectedEntryId(newEntry.id)
									}
								}
							}
						}
						return true
					}
				}

				updateUI()
			}
		}
	}

	private fun updateUI() {
		doAsync {
			App.db.entryDao().markAsRead(listOf(entry.id))
		}

		preferFullText = true
		entry_view.setEntry(entry, preferFullText)

		initDataObservers()

		setupToolbar()
	}

	private fun initDataObservers() {
		isMobilizingLiveData?.removeObservers(this)
		refresh_layout.isRefreshing = false

		isMobilizingLiveData = App.db.taskDao().observeItemMobilizationTasksCount(entry.id)
		isMobilizingLiveData?.observe(this, Observer<Int> { count ->
			if (count ?: 0 > 0) {
				refresh_layout.isRefreshing = true

				// If the service is not started, start it here to avoid an infinite loading
				if (!PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false)) {
					context?.startService(Intent(context, FetcherService::class.java).setAction(FetcherService.ACTION_MOBILIZE_FEEDS))
				}
			} else {
				if (refresh_layout.isRefreshing) {
					doAsync {
						App.db.entryDao().findByIdWithFeed(entry.id)?.let { newEntry ->
							uiThread {
								entry = newEntry
								entry_view.setEntry(entry, preferFullText)

								setupToolbar()
							}
						}
					}
				}

				refresh_layout.isRefreshing = false
			}
		})
	}

	private fun setupToolbar() {
		toolbar.apply {
			menu.clear()
			inflateMenu(R.menu.menu_fragment_entry_details)

			if (activity?.containers_layout?.hasTwoColumns() != true) {
				setNavigationIcon(R.drawable.ic_back_white_24dp)
				setNavigationOnClickListener { activity?.onBackPressed() }
			}

			if (entry.favorite) {
				menu.findItem(R.id.menu_entry_details__favorite)
						.setTitle(R.string.menu_unstar)
						.setIcon(R.drawable.ic_star_white_24dp)
			}


			if (entry.mobilizedContent == null || !preferFullText) {
				menu.findItem(R.id.menu_entry_details__fulltext).isVisible = true
				menu.findItem(R.id.menu_entry_details__original_text).isVisible = false
			} else {
				menu.findItem(R.id.menu_entry_details__fulltext).isVisible = false
				menu.findItem(R.id.menu_entry_details__original_text).isVisible = true
			}

			setOnMenuItemClickListener { item ->
				when (item?.itemId) {
					R.id.menu_entry_details__favorite -> {
						entry.favorite = !entry.favorite
						entry.read = true // otherwise it marked it as unread again

						if (entry.favorite) {
							item.setTitle(R.string.menu_unstar).setIcon(R.drawable.ic_star_white_24dp)
						} else {
							item.setTitle(R.string.menu_star).setIcon(R.drawable.ic_star_border_white_24dp)
						}

						doAsync {
							App.db.entryDao().update(entry)
						}
					}
					R.id.menu_entry_details__open_browser -> {
						startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(entry.link)))
					}
					R.id.menu_entry_details__share -> {
						startActivity(Intent.createChooser(
								Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_SUBJECT, title).putExtra(Intent.EXTRA_TEXT, entry.link)
										.setType("text/plain"), getString(R.string.menu_share)
						))
					}
					R.id.menu_entry_details__fulltext -> {
						if (entry.mobilizedContent == null) {
							this@EntryDetailsFragment.context?.let { c ->
								if (c.isOnline()) {
									doAsync {
										FetcherService.addEntriesToMobilize(listOf(entry.id))
										c.startService(Intent(c, FetcherService::class.java).setAction(FetcherService.ACTION_MOBILIZE_FEEDS))
									}
								} else {
									toast(R.string.network_error).show()
								}
							}
						} else {
							preferFullText = true
							entry_view.setEntry(entry, preferFullText)

							setupToolbar()
						}
					}
					R.id.menu_entry_details__original_text -> {
						preferFullText = false
						entry_view.setEntry(entry, preferFullText)

						setupToolbar()
					}
					R.id.menu_entry_details__mark_as_unread -> {
						doAsync {
							App.db.entryDao().markAsUnread(listOf(entry.id))
						}
						if (activity?.containers_layout?.hasTwoColumns() != true) {
							activity?.onBackPressed()
						}
					}
					else -> {
					}
				}

				true
			}
		}
	}

	fun setEntry(entry: EntryWithFeed, allEntryIds: List<String>) {
		this.entry = entry
		this.allEntryIds = allEntryIds
		arguments?.putParcelable(ARG_ENTRY, entry)
		arguments?.putStringArrayList(ARG_ALL_ENTRIES_IDS, ArrayList(allEntryIds))

		updateUI()
	}
}
