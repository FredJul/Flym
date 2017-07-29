package net.frju.flym.ui.items

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import net.frju.flym.data.entities.Item
import net.frju.flym.data.entities.ItemWithFeed
import org.jetbrains.anko.sdk21.coroutines.onClick

class ItemsAdapter : RecyclerView.Adapter<ItemsAdapter.ItemViewHolder>() {

    var selectedItemId: String? = null
        set(newValue) {
            notifyDataSetChanged()
            field = newValue
        }
    private var items: List<ItemWithFeed>? = null
    private var onItemClickListener: ItemAdapterView.OnItemClickListener? = null

    class ItemViewHolder(var itemAdapterView: ItemAdapterView) : RecyclerView.ViewHolder(itemAdapterView)

    fun setOnItemClickListener(onItemClickListener: ItemAdapterView.OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemsAdapter.ItemViewHolder {
        return ItemViewHolder(ItemAdapterView(parent.context))
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.itemAdapterView.isSelected = selectedItemId == items!![position].id

        holder.itemAdapterView.setItem(items!![position])
        holder.itemAdapterView.onClick {
            val item = items!![holder.adapterPosition]
            selectedItemId = item.id

            onItemClickListener?.onItemClick(item)
        }
    }

    fun setItems(items: List<ItemWithFeed>) {
        this.items = items
        notifyDataSetChanged()
    }

    fun getPreviousItem(): Item? {
        items?.let {
            for (i in 0..it.size - 1) {
                if (it[i].id == selectedItemId) {
                    if (i == 0) {
                        return null
                    }
                    return it[i - 1]
                }
            }
        }

        return null
    }

    fun getNextItem(): Item? {
        items?.let {
            for (i in 0..it.size - 1) {
                if (it[i].id == selectedItemId) {
                    if (i == items!!.size - 1) {
                        return null
                    }
                    return it[i + 1]
                }
            }
        }

        return null
    }

    override fun getItemCount(): Int {
        return items?.size ?: 0
    }
}
