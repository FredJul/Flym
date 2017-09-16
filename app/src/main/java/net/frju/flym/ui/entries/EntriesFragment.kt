package net.frju.flym.ui.entries

import android.arch.lifecycle.LifecycleFragment
import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_entries.*
import kotlinx.android.synthetic.main.view_main_containers.*
import net.fred.feedex.R
import net.frju.flym.App
import net.frju.flym.GlideApp
import net.frju.flym.data.entities.EntryWithFeed
import net.frju.flym.data.entities.Feed
import net.frju.flym.service.FetcherService
import net.frju.flym.ui.main.MainNavigator
import net.frju.flym.utils.indefiniteSnackbar
import net.frju.flym.utils.loadFavicon
import net.frju.parentalcontrol.utils.PrefUtils
import net.idik.lib.slimadapter.viewinjector.IViewInjector
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.sdk21.coroutines.onClick
import java.util.*
import java.util.concurrent.TimeUnit


class EntriesFragment : LifecycleFragment() {

    private val navigator: MainNavigator by lazy { activity as MainNavigator }

    private var adapter: EntryAdapter? = null
    private var feed: Feed? = null
    private var unfilteredEntries: List<EntryWithFeed>? = null
    private var listDisplayDate = Date().time
    private val disposables = CompositeDisposable()
    private var newEntriesSnackbar: Snackbar? = null

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
                updateUI()
                recycler_view.scrollToPosition(0)
            }
            true
        }

        read_all_fab.onClick {
            unfilteredEntries?.let {
                for (entry in it) {
                    entry.read = true
                }

                updateUI()

                doAsync {
                    App.db.entryDao().insertAll(*it.toTypedArray())
                }
            }
        }

        if (savedInstanceState != null) {
            adapter?.selectedEntryId = savedInstanceState.getString(STATE_SELECTED_ENTRY_ID)
            listDisplayDate = savedInstanceState.getLong(STATE_LIST_DISPLAY_DATE)
        }

        initDataObservers()
    }

    private fun initDataObservers() {
        disposables.clear()

        val entriesFlow = when {
            feed?.isGroup == true -> App.db.entryDao().observeByGroup(feed!!.id, listDisplayDate)
            feed != null && feed?.id != Feed.ALL_ENTRIES_ID -> App.db.entryDao().observeByFeed(feed!!.id, listDisplayDate)
            else -> App.db.entryDao().observeAll(listDisplayDate)
        }

        disposables.add(
                entriesFlow.subscribeOn(Schedulers.io())
                        .debounce(100, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            unfilteredEntries = it
                            updateUI()
                        })

        val newCountFlow = when {
            feed?.isGroup == true -> App.db.entryDao().observeNewEntriesCountByGroup(feed!!.id, listDisplayDate)
            feed != null && feed?.id != Feed.ALL_ENTRIES_ID -> App.db.entryDao().observeNewEntriesCountByFeed(feed!!.id, listDisplayDate)
            else -> App.db.entryDao().observeNewEntriesCount(listDisplayDate)
        }

        disposables.add(
                newCountFlow.subscribeOn(Schedulers.io())
                        .debounce(100, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (it > 0) {
                        if (newEntriesSnackbar?.isShown != true) {
                            newEntriesSnackbar = coordinator.indefiniteSnackbar("$it new entries", "refresh") {
                                listDisplayDate = Date().time
                                initDataObservers()
                            }
                        } else {
                            newEntriesSnackbar?.setText("$it new entries")
                        }
                    }
                })
    }

    override fun onDestroy() {
        disposables.clear()
        super.onDestroy()
    }

    private fun updateUI() {
        empty_view_refresh_button.visibility = when (bottom_navigation.selectedItemId) {
            R.id.favorites -> View.GONE
            else -> View.VISIBLE
        }

        val entries = when (bottom_navigation.selectedItemId) {
            R.id.unreads -> unfilteredEntries?.filter { !it.read }
            R.id.favorites -> unfilteredEntries?.filter { it.favorite }
            else -> unfilteredEntries
        }
        adapter?.updateData(entries)
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
        outState.putString(STATE_SELECTED_ENTRY_ID, adapter?.selectedEntryId)
        outState.putLong(STATE_LIST_DISPLAY_DATE, listDisplayDate)

        super.onSaveInstanceState(outState)
    }

    private fun setupRecyclerView() {
        recycler_view.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(activity)
        recycler_view.layoutManager = layoutManager
        adapter = EntryAdapter().apply {
            register<EntryWithFeed>(R.layout.view_entry) { entry, injector ->
                injector
                        .clicked(R.id.entry_container) {
                            navigator.goToEntryDetails(entry)
                        }
                        .clicked(R.id.favorite_icon) { view ->
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
                        }
                        .with(R.id.title, IViewInjector.Action<TextView> { view ->
                            view.isEnabled = !entry.read
                            view.text = entry.title
                        })
                        .with(R.id.feed_name, IViewInjector.Action<TextView> { view ->
                            view.isEnabled = !entry.read
                            view.text = entry.feedTitle ?: ""
                        })
                        .with(R.id.main_icon, IViewInjector.Action<ImageView> { view ->
                            val feedName = entry.feedTitle ?: ""

                            val mainImgUrl = if (TextUtils.isEmpty(entry.imageLink)) null else FetcherService.getDownloadedOrDistantImageUrl(entry.id, entry.imageLink!!)

                            val color = ColorGenerator.DEFAULT.getColor(entry.feedId) // The color is specific to the feedId (which shouldn't change)
                            val lettersForName = if (feedName.length < 2) feedName.toUpperCase() else feedName.substring(0, 2).toUpperCase()
                            val letterDrawable = TextDrawable.builder().buildRect(lettersForName, color)
                            if (mainImgUrl != null) {
                                GlideApp.with(view.context).load(mainImgUrl).centerCrop().placeholder(letterDrawable).error(letterDrawable).into(view)
                            } else {
                                GlideApp.with(view.context).clear(view)
                                view.setImageDrawable(letterDrawable)
                            }
                        })
                        .with(R.id.feed_icon, IViewInjector.Action<ImageView> { view ->
                            view.loadFavicon(entry.feedLink)
                        })
                        .with(R.id.favorite_icon, IViewInjector.Action<ImageView> { view ->
                            if (entry.favorite) {
                                view.setImageResource(R.drawable.ic_star_white_24dp)
                            } else {
                                view.setImageResource(R.drawable.ic_star_border_white_24dp)
                            }
                        })
            }

            attachTo(recycler_view)
        }

        refresh_layout.setOnRefreshListener {
            startRefresh()
        }

        recycler_view.emptyView = empty_view

        empty_view_refresh_button.onClick {
            startRefresh()
        }
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
    }

    private fun setupToolbar() {
        activity?.toolbar?.setTitle(R.string.fragment_people__title)
        activity?.toolbar?.inflateMenu(R.menu.fragment_entries)
    }

    fun setSelectedEntry(selectedEntry: EntryWithFeed) {
        adapter?.selectedEntryId = selectedEntry.id
    }

    fun getPreviousEntry(): EntryWithFeed? {
        return adapter?.previousEntry
    }

    fun getNextEntry(): EntryWithFeed? {
        return adapter?.nextEntry
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
