package net.frju.flym.ui.items

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import net.frju.androidquery.operation.function.CursorResult
import net.frju.flym.data.Item
import org.jetbrains.anko.onClick

/**
 * Created by Lucas on 04/01/2017.
 */

class ItemsAdapter : RecyclerView.Adapter<ItemsAdapter.ItemViewHolder>() {

    var selectedItemId: String? = null
        set(newValue) {
            notifyDataSetChanged()
            field = newValue
        }
    private var items: CursorResult<Item>? = null
    private var onItemClickListener: ItemAdapterView.OnItemClickListener? = null

    class ItemViewHolder(var itemAdapterView: ItemAdapterView) : RecyclerView.ViewHolder(itemAdapterView)

    fun setOnItemClickListener(onItemClickListener: ItemAdapterView.OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemsAdapter.ItemViewHolder {
        return ItemViewHolder(ItemAdapterView(parent.context))
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.itemAdapterView.isSelected = selectedItemId == items!![position]!!.id

        holder.itemAdapterView.setItem(items!!.get(position)!!)
        holder.itemAdapterView.onClick {
            val item = items!![holder.adapterPosition]!!
            selectedItemId = item.id

            onItemClickListener?.onItemClick(item)
        }
    }

    fun setItems(items: CursorResult<Item>) {
        this.items = items
        notifyDataSetChanged()
    }

    fun getPreviousItem(): Item? {
        if (items != null) {
            for (i in 0..items!!.count - 1) {
                if (items!![i]!!.id == selectedItemId) {
                    if (i == 0) {
                        return null
                    }
                    return items!!.get(i - 1)
                }
            }
        }

        return null
    }

    fun getNextItem(): Item? {
        if (items != null) {
            for (i in 0..items!!.count - 1) {
                if (items!![i]!!.id == selectedItemId) {
                    if (i == items!!.count - 1) {
                        return null
                    }
                    return items!!.get(i + 1)
                }
            }
        }

        return null
    }

    override fun getItemCount(): Int {
        return items?.count ?: 0
    }
}
