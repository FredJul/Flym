package net.frju.flym.ui.entrydetails

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.content.Intent
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
import net.frju.flym.service.FetcherService
import net.frju.flym.ui.main.MainNavigator
import net.frju.flym.utils.isOnline
import net.frju.parentalcontrol.utils.PrefUtils
import org.jetbrains.anko.appcompat.v7.coroutines.onMenuItemClick
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.jetbrains.annotations.NotNull


class EntryDetailsFragment : Fragment() {

	companion object {

		private val ARG_ENTRY = "ARG_ENTRY"
		private val ARG_ALL_ENTRIES_IDS = "ARG_ALL_ENTRIES_IDS"

		fun newInstance(entry: EntryWithFeed, allEntryIds: List<String>): EntryDetailsFragment {
			val fragment = EntryDetailsFragment()
			fragment.arguments = bundleOf(ARG_ENTRY to entry, ARG_ALL_ENTRIES_IDS to allEntryIds)
			return fragment
		}
	}

	private val navigator: MainNavigator by lazy { activity as MainNavigator }

	private lateinit var entry: EntryWithFeed
	private var allEntryIds = emptyList<String>()
		set(value) {
			field = value

			val currentIdx = allEntryIds.indexOf(entry.id)

			previousId = if (currentIdx == 0) {
				null
			} else {
				allEntryIds[currentIdx - 1]
			}

			nextId = if (currentIdx >= allEntryIds.size - 1) {
				null
			} else {
				allEntryIds[currentIdx + 1]
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
										navigator.setSelectedEntryId(newEntry.id)
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
										navigator.setSelectedEntryId(newEntry.id)
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
			entry.read = true
			App.db.entryDao().insert(entry)
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
				refresh_layout.isRefreshing = false

				if (entry.mobilizedContent.isNullOrEmpty() && preferFullText) {
					doAsync {
						App.db.entryDao().findByIdWithFeed(entry.id)?.let { newEntry ->
							uiThread {
								entry = newEntry
								entry_view.setEntry(entry, preferFullText)
							}
						}
					}
				}
			}
		})
	}

	private fun setupToolbar() {
		toolbar.apply {
			menu.clear()
			inflateMenu(R.menu.fragment_entry_details)

			if (activity?.containers_layout?.hasTwoColumns() == false) {
				setNavigationIcon(R.drawable.ic_back_24dp)
				setNavigationOnClickListener { activity?.onBackPressed() }
			}

			if (entry.favorite) {
				val item = menu.findItem(R.id.menu_entry_details__favorite)
				item.setTitle(R.string.menu_unstar).setIcon(R.drawable.ic_star_white_24dp)
			}

			onMenuItemClick { item ->
				when (item?.itemId) {
					R.id.menu_entry_details__favorite -> {
						entry.favorite = !entry.favorite

						if (entry.favorite) {
							item.setTitle(R.string.menu_unstar).setIcon(R.drawable.ic_star_white_24dp)
						} else {
							item.setTitle(R.string.menu_star).setIcon(R.drawable.ic_star_border_white_24dp)
						}

						doAsync {
							App.db.entryDao().insert(entry)
						}
					}
					R.id.menu_entry_details__share -> {
						startActivity(Intent.createChooser(
								Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_SUBJECT, title).putExtra(Intent.EXTRA_TEXT, entry.link)
										.setType("text/plain"), getString(R.string.menu_share)
						))
					}
					R.id.menu_entry_details__fulltext -> {
						if (entry.mobilizedContent.isNullOrEmpty()) {
							this@EntryDetailsFragment.context?.let { c ->
								if (c.isOnline()) {
									doAsync {
										FetcherService.addEntriesToMobilize(listOf(entry.id))
										c.startService(Intent(c, FetcherService::class.java).setAction(FetcherService.ACTION_MOBILIZE_FEEDS))
									}
								} else {
									//TODO UiUtils.showMessage(c, R.string.network_error);
								}
							}
						} else {
							preferFullText = !preferFullText
							entry_view.setEntry(entry, preferFullText)
						}
					}
				}
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
