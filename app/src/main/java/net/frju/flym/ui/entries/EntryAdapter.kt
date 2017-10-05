package net.frju.flym.ui.entries

import android.arch.paging.PagedListAdapter
import android.support.v7.recyclerview.extensions.DiffCallback
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.view_entry.view.*
import net.fred.feedex.R
import net.frju.flym.GlideApp
import net.frju.flym.data.entities.EntryWithFeed
import net.frju.flym.data.entities.Feed
import net.frju.flym.service.FetcherService
import org.jetbrains.anko.sdk21.coroutines.onClick

class EntryAdapter(private val globalClickListener: (EntryWithFeed) -> Unit, private val favoriteClickListener: (EntryWithFeed) -> Unit) : PagedListAdapter<EntryWithFeed, EntryAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {

        @JvmField
        val DIFF_CALLBACK = object : DiffCallback<EntryWithFeed>() {

            override fun areItemsTheSame(oldItem: EntryWithFeed, newItem: EntryWithFeed): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: EntryWithFeed, newItem: EntryWithFeed): Boolean {
                return oldItem.id == newItem.id && oldItem.read == newItem.read && oldItem.favorite == newItem.favorite // no need to do more complex in our case
            }
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(entry: EntryWithFeed, globalClickListener: (EntryWithFeed) -> Unit, favoriteClickListener: (EntryWithFeed) -> Unit) = with(itemView) {
            title.isEnabled = !entry.read
            title.text = entry.title

            feed_name.isEnabled = !entry.read
            feed_name.text = entry.feedTitle ?: ""

            val mainImgUrl = if (TextUtils.isEmpty(entry.imageLink)) null else FetcherService.getDownloadedOrDistantImageUrl(entry.id, entry.imageLink!!)

            val letterDrawable = Feed.getLetterDrawable(entry.feedId, entry.feedTitle)
            if (mainImgUrl != null) {
                GlideApp.with(context).load(mainImgUrl).centerCrop().placeholder(letterDrawable).error(letterDrawable).into(main_icon)
            } else {
                GlideApp.with(context).clear(main_icon)
                main_icon.setImageDrawable(letterDrawable)
            }

            if (entry.favorite) {
                favorite_icon.setImageResource(R.drawable.ic_star_white_24dp)
            } else {
                favorite_icon.setImageResource(R.drawable.ic_star_border_white_24dp)
            }
            favorite_icon.onClick { favoriteClickListener(entry) }

            onClick { globalClickListener(entry) }
        }

        fun clear() = with(itemView) {
            GlideApp.with(context).clear(main_icon)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.view_entry, parent, false)
        return EntryAdapter.ViewHolder(view)
    }

    override fun onBindViewHolder(holder: EntryAdapter.ViewHolder, position: Int) {
        val entry = getItem(position)
        if (entry != null) {
            holder.bind(entry, globalClickListener, favoriteClickListener)
        } else {
            // Null defines a placeholder item - PagedListAdapter will automatically invalidate
            // this row when the actual object is loaded from the database
            holder.clear()
        }

        holder.itemView.isSelected = (selectedEntryId == entry?.id)
    }

    var selectedEntryId: String? = null
        set(newValue) {
            field = newValue
            notifyDataSetChanged()
        }
}