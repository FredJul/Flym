package net.frju.flym.data.entities

import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder
import net.fred.feedex.R
import net.frju.flym.GlideApp
import net.frju.flym.service.FetcherService
import paperparcel.PaperParcel
import java.net.URL


@PaperParcel
class ItemWithFeed(var feedTitle: String? = null,
                   var feedLink: String = "",
                   var feedImageLink: String? = null,
                   var groupId: String? = null) : Item(), Parcelable, IFlexible<ItemWithFeed.ViewHolder> {

    companion object {
        @JvmField val CREATOR = PaperParcelItemWithFeed.CREATOR
    }

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        PaperParcelItemWithFeed.writeToParcel(this, dest, flags)
    }

    @Transient
    private var _enabled = true

    override fun setEnabled(enabled: Boolean) {
        _enabled = enabled
    }

    override fun isEnabled() = _enabled

    @Transient
    private var _swipeable = true

    override fun setSwipeable(swipeable: Boolean) {
        _swipeable = swipeable
    }

    override fun isSwipeable() = _swipeable

    @Transient
    private var _draggable = false

    override fun setDraggable(draggable: Boolean) {
        _draggable = draggable
    }

    override fun isDraggable() = _draggable

    @Transient
    private var _selectable = false

    override fun setSelectable(selectable: Boolean) {
        _selectable = selectable
    }

    override fun isSelectable() = _selectable

    @Transient
    private var _hidden = false

    override fun setHidden(hidden: Boolean) {
        _hidden = hidden
    }

    override fun isHidden() = _hidden

    override fun getSpanSize(spanCount: Int, position: Int) = 0

    override fun shouldNotifyChange(newItem: IFlexible<*>?) = true

    /**
     * For the item type we need an int value: the layoutResID is sufficient.
     */
    override fun getLayoutRes(): Int {
        return R.layout.view_item
    }

    /**
     * Delegates the creation of the ViewHolder to the user (AutoMap).
     * The infladed view is already provided as well as the Adapter.
     */
    override fun createViewHolder(view: View, adapter: FlexibleAdapter<*>): ViewHolder {
        return ViewHolder(view, adapter)
    }

    /**
     * The Adapter and the Payload are provided to perform and get more specific information.
     */
    override fun bindViewHolder(adapter: FlexibleAdapter<*>, holder: ViewHolder, position: Int, payloads: List<*>) {
        val feedName = feedTitle ?: ""

        holder.title.text = title
        holder.feed_name.text = feedName

        val mainImgUrl = if (TextUtils.isEmpty(imageLink)) null else FetcherService.getDownloadedOrDistantImageUrl(id, imageLink!!)

        val color = ColorGenerator.DEFAULT.getColor(feedId) // The color is specific to the feedId (which shouldn't change)
        val lettersForName = if (feedName.length < 2) feedName.toUpperCase() else feedName.substring(0, 2).toUpperCase()
        val letterDrawable = TextDrawable.builder().buildRect(lettersForName, color)
        if (mainImgUrl != null) {
            GlideApp.with(holder.contentView.context).load(mainImgUrl).centerCrop().placeholder(letterDrawable).error(letterDrawable).into(holder.main_icon)
        } else {
            GlideApp.with(holder.contentView.context).clear(holder.main_icon)
            holder.main_icon.setImageDrawable(letterDrawable)
        }

        val domain = URL(feedLink).host
        GlideApp.with(holder.contentView.context).load("https://www.google.com/s2/favicons?domain=$domain").error(R.mipmap.ic_launcher).into(holder.feed_icon)
    }

    override fun unbindViewHolder(adapter: FlexibleAdapter<out IFlexible<*>>, holder: ViewHolder, position: Int) {
        GlideApp.with(holder.contentView.context).clear(holder.main_icon)
    }

    /**
     * The ViewHolder used by this item.
     * Extending from FlexibleViewHolder is recommended especially when you will use
     * more advanced features.
     */
    inner class ViewHolder(view: View, adapter: FlexibleAdapter<*>) : FlexibleViewHolder(view, adapter) {

        val title: TextView = view.findViewById(R.id.title)
        val feed_name: TextView = view.findViewById(R.id.feed_name)
        val main_icon: ImageView = view.findViewById(R.id.main_icon)
        val feed_icon: ImageView = view.findViewById(R.id.feed_icon)

    }
}
