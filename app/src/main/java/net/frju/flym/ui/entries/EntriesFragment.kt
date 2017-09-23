package net.frju.flym.ui.entries

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.arch.paging.PagedList
import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import kotlinx.android.synthetic.main.fragment_entries.*
import kotlinx.android.synthetic.main.view_main_containers.*
import net.fred.feedex.R
import net.frju.flym.App
import net.frju.flym.data.entities.EntryWithFeed
import net.frju.flym.data.entities.Feed
import net.frju.flym.service.FetcherService
import net.frju.flym.ui.about.AboutActivity
import net.frju.flym.ui.main.MainNavigator
import net.frju.flym.utils.indefiniteSnackbar
import net.frju.parentalcontrol.utils.PrefUtils
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.sdk21.coroutines.onClick
import org.jetbrains.anko.support.v4.startActivity
import java.util.*


class EntriesFragment : Fragment() {

    private val navigator: MainNavigator by lazy { activity as MainNavigator }

    private val adapter = EntryAdapter({ entry ->
        navigator.goToEntryDetails(entry, entryIds!!)
    }, { entry ->
        entry.favorite = !entry.favorite

        (view as? ImageView)?.let {
            if (entry.favorite) {
                it.setImageResource(R.drawable.ic_star_white_24dp)
            } else {
                it.setImageResource(R.drawable.ic_star_border_white_24dp)
            }
        }

        doAsync {
            App.db.entryDao().insertAll(entry)
        }
    })
    private var feed: Feed? = null
    private var listDisplayDate = Date().time
    private var newEntriesSnackbar: Snackbar? = null
    private var entriesLiveData: LiveData<PagedList<EntryWithFeed>>? = null
    private var entryIdsLiveData: LiveData<List<String>>? = null
    private var entryIds: List<String>? = null
    private var newCountLiveData: LiveData<Long>? = null

    private val prefListener = OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (PrefUtils.IS_REFRESHING == key) {
            refreshSwipeProgress()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_entries, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (arguments?.containsKey(ARG_FEED) == true) {
            feed = arguments.getParcelable(ARG_FEED)
        }

        setupToolbar()
        setupRecyclerView()

        bottom_navigation.setOnNavigationItemSelectedListener {
            recycler_view.post {
                initDataObservers()
                recycler_view.scrollToPosition(0)
            }
            true
        }

        read_all_fab.onClick {
            doAsync {
                entryIds?.withIndex()?.groupBy { it.index / 300 }?.map { it.value.map { it.value } }?.forEach {
                    App.db.entryDao().markAsRead(it)
                }
            }
        }

        if (savedInstanceState != null) {
            adapter.selectedEntryId = savedInstanceState.getString(STATE_SELECTED_ENTRY_ID)
            listDisplayDate = savedInstanceState.getLong(STATE_LIST_DISPLAY_DATE)
        }

        initDataObservers()
    }

    private fun initDataObservers() {
        entryIdsLiveData?.removeObservers(this)
        entryIdsLiveData = when {
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
        entriesLiveData = when {
            feed?.isGroup == true && bottom_navigation.selectedItemId == R.id.unreads -> App.db.entryDao().observeUnreadsByGroup(feed!!.id, listDisplayDate)
            feed?.isGroup == true && bottom_navigation.selectedItemId == R.id.favorites -> App.db.entryDao().observeFavoritesByGroup(feed!!.id, listDisplayDate)
            feed?.isGroup == true -> App.db.entryDao().observeByGroup(feed!!.id, listDisplayDate)

            feed != null && feed?.id != Feed.ALL_ENTRIES_ID && bottom_navigation.selectedItemId == R.id.unreads -> App.db.entryDao().observeUnreadsByFeed(feed!!.id, listDisplayDate)
            feed != null && feed?.id != Feed.ALL_ENTRIES_ID && bottom_navigation.selectedItemId == R.id.favorites -> App.db.entryDao().observeFavoritesByFeed(feed!!.id, listDisplayDate)
            feed != null && feed?.id != Feed.ALL_ENTRIES_ID -> App.db.entryDao().observeByFeed(feed!!.id, listDisplayDate)

            bottom_navigation.selectedItemId == R.id.unreads -> App.db.entryDao().observeAllUnreads(listDisplayDate)
            bottom_navigation.selectedItemId == R.id.favorites -> App.db.entryDao().observeAllFavorites(listDisplayDate)
            else -> App.db.entryDao().observeAll(listDisplayDate)
        }.create(0, 30)

        entriesLiveData?.observe(this, Observer<PagedList<EntryWithFeed>> { pagedList ->
            adapter.setList(pagedList)
        })

        newCountLiveData?.removeObservers(this)
        newCountLiveData = when {
            feed?.isGroup == true -> App.db.entryDao().observeNewEntriesCountByGroup(feed!!.id, listDisplayDate)
            feed != null && feed?.id != Feed.ALL_ENTRIES_ID -> App.db.entryDao().observeNewEntriesCountByFeed(feed!!.id, listDisplayDate)
            else -> App.db.entryDao().observeNewEntriesCount(listDisplayDate)
        }

        newCountLiveData?.observe(this, Observer<Long> { count ->
            if (count != null && count > 0L) {
                if (newEntriesSnackbar?.isShown != true) {
                    newEntriesSnackbar = coordinator.indefiniteSnackbar("$count new entries", "refresh") {
                        listDisplayDate = Date().time
                        initDataObservers()
                    }
                } else {
                    newEntriesSnackbar?.setText("$count new entries")
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        PrefUtils.registerOnPrefChangeListener(prefListener)
        refreshSwipeProgress()
    }

    override fun onStop() {
        super.onStop()
        PrefUtils.unregisterOnPrefChangeListener(prefListener)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_SELECTED_ENTRY_ID, adapter.selectedEntryId)
        outState.putLong(STATE_LIST_DISPLAY_DATE, listDisplayDate)

        super.onSaveInstanceState(outState)
    }

    private fun setupRecyclerView() {
        recycler_view.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(activity)
        recycler_view.layoutManager = layoutManager
        recycler_view.adapter = adapter

        refresh_layout.setOnRefreshListener {
            startRefresh()
        }

        recycler_view.emptyView = empty_view
    }

    private fun startRefresh() {
        if (!PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false)) {
            if (feed?.isGroup == false && feed?.id != Feed.ALL_ENTRIES_ID) {
                context.startService(Intent(context, FetcherService::class.java).setAction(FetcherService.ACTION_REFRESH_FEEDS).putExtra(FetcherService.EXTRA_FEED_ID,
                        feed?.id))
            } else {
                context.startService(Intent(context, FetcherService::class.java).setAction(FetcherService.ACTION_REFRESH_FEEDS))
            }
        }

        // In case there is no internet, the service won't even start, let's quickly stop the refresh animation
        refresh_layout.postDelayed({ refreshSwipeProgress() }, 500)
    }

    private fun setupToolbar() {
        activity?.toolbar?.apply {
            if (feed == null || feed?.id == Feed.ALL_ENTRIES_ID) {
                setTitle(R.string.all_entries)
            } else {
                title = feed?.title
            }

            menu.clear()
            inflateMenu(R.menu.fragment_entries)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_entries__about -> {
                        startActivity<AboutActivity>()
                        true
                    }
                    else -> false
                }
            }
        }
    }

    fun setSelectedEntryId(selectedEntryId: String) {
        adapter.selectedEntryId = selectedEntryId
    }

    private fun refreshSwipeProgress() {
        refresh_layout.isRefreshing = PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false)
    }

    companion object {

        private val ARG_FEED = "feed"
        private val STATE_SELECTED_ENTRY_ID = "STATE_SELECTED_ENTRY_ID"
        private val STATE_LIST_DISPLAY_DATE = "STATE_LIST_DISPLAY_DATE"

        fun newInstance(feed: Feed?): EntriesFragment {
            val fragment = EntriesFragment()
            feed?.let {
                fragment.arguments = bundleOf(ARG_FEED to feed)
            }
            return fragment
        }
    }
}
