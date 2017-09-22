package net.frju.flym.ui.entrydetails

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.chimbori.crux.articles.ArticleExtractor
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_entry_details.*
import me.thanel.swipeactionview.SwipeActionView
import me.thanel.swipeactionview.SwipeGestureListener
import net.fred.feedex.R
import net.frju.flym.App
import net.frju.flym.data.entities.EntryWithFeed
import net.frju.flym.service.FetcherService
import net.frju.flym.ui.main.MainNavigator
import net.frju.flym.utils.UTF8
import okhttp3.Request
import org.jetbrains.anko.appcompat.v7.coroutines.onMenuItemClick
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.jetbrains.annotations.NotNull


class EntryDetailsFragment : Fragment() {

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_entry_details, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        entry = arguments.getParcelable(ARG_ENTRY)
        allEntryIds = arguments.getStringArrayList(ARG_ALL_ENTRIES_IDS)

        setupToolbar()

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

    private fun updateUI() {
        doAsync {
            entry.read = true
            App.db.entryDao().insertAll(entry)
        }

        entry_view.setEntry(entry)
    }

    private fun setupToolbar() {
        toolbar.inflateMenu(R.menu.fragment_entry_details)

        if (!activity.containers_layout.hasTwoColumns()) {
            toolbar.setNavigationIcon(R.drawable.ic_back_24dp)
            toolbar.setNavigationOnClickListener { activity.onBackPressed() }
        }

        toolbar.onMenuItemClick { item ->
            when (item?.itemId) {
                R.id.menu_person_details__fulltext -> {
                    doAsync {
                        var rawHTML = ""
                        val request = Request.Builder()
                                .url("http://www.lepoint.fr/economie/apres-le-fmi-l-ocde-apporte-a-son-tour-son-soutien-aux-reformes-de-macron-14-09-2017-2156904_28.php")
                                .header("User-agent", "Mozilla/5.0 (compatible) AppleWebKit Chrome Safari") // some feeds need this to work properly
                                .addHeader("accept", "*/*")
                                .build()

                        FetcherService.HTTP_CLIENT.newCall(request).execute().use {
                            it.body()?.let { body ->
                                rawHTML = body.string()
                            }
                        }
                        val article = ArticleExtractor.with(entry.link, rawHTML)
                                .extractContent()  // If you only need metadata, you can skip `.extractorContent()`
                                .article()
                        val html = article.document.html()
                        uiThread {
                            entry_view.loadDataWithBaseURL("", html, "text/html", UTF8, null)
                        }
                    }
                }
            }
        }
    }

    fun setEntry(entry: EntryWithFeed, allEntryIds: List<String>) {
        this.entry = entry
        this.allEntryIds = allEntryIds
        arguments.putParcelable(ARG_ENTRY, entry)
        arguments.putStringArrayList(ARG_ALL_ENTRIES_IDS, ArrayList(allEntryIds))

        updateUI()
    }

    companion object {

        private val ARG_ENTRY = "ARG_ENTRY"
        private val ARG_ALL_ENTRIES_IDS = "ARG_ALL_ENTRIES_IDS"

        fun newInstance(entry: EntryWithFeed, allEntryIds: List<String>): EntryDetailsFragment {
            val fragment = EntryDetailsFragment()
            fragment.arguments = bundleOf(ARG_ENTRY to entry, ARG_ALL_ENTRIES_IDS to allEntryIds)
            return fragment
        }
    }
}
