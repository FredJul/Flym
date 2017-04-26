package net.frju.flym.ui.items

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.view_item.view.*
import net.fred.feedex.R
import net.frju.flym.data.Item
import net.frju.flym.service.FetcherService

class ItemAdapterView : FrameLayout {

    private var item: Item? = null
    private var onItemClickListener: ItemAdapterView.OnItemClickListener? = null

    interface OnItemClickListener {

        fun onItemClick(item: Item)

        fun onItemFavoriteIconClick(item: Item)
    }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    private fun init() {
        LayoutInflater.from(context).inflate(R.layout.view_item, this, true)
    }

    fun setItem(item: Item) {
        val feedName = item.feed?.title ?: ""

        this.item = item
        title.text = item.title
        feed_name.text = feedName

        val mainImgUrl = if (TextUtils.isEmpty(item.imageLink)) null else FetcherService.getDownloadedOrDistantImageUrl(item.id, item.imageLink!!)

        val color = ColorGenerator.DEFAULT.getColor(item.feedId) // The color is specific to the feedId (which shouldn't change)
        val lettersForName = if (feedName.length < 2) feedName.toUpperCase() else feedName.substring(0, 2).toUpperCase()
        val letterDrawable = TextDrawable.builder().buildRect(lettersForName, color)
        if (mainImgUrl != null) {
            Glide.with(context).load(mainImgUrl).centerCrop().placeholder(letterDrawable).error(letterDrawable).into(main_icon)
        } else {
            Glide.clear(main_icon)
            main_icon.setImageDrawable(letterDrawable)
        }
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener?) {
        this.onItemClickListener = onItemClickListener
        if (onItemClickListener != null) {
            row.setOnClickListener { onItemClickListener.onItemClick(item!!) }
            favorite_icon.setOnClickListener { onItemClickListener.onItemFavoriteIconClick(item!!) }
        } else {
            row.setOnClickListener(null)
            favorite_icon.setOnClickListener(null)
        }
    }
}
