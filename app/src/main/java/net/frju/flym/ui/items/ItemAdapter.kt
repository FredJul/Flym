package net.frju.flym.ui.items

import net.frju.flym.data.entities.ItemWithFeed
import net.idik.lib.slimadapter.SlimAdapter

class ItemAdapter : SlimAdapter() {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return (data[position] as ItemWithFeed).id.hashCode().toLong()
    }

    var selectedItemId: String? = null
        set(newValue) {
            field = newValue
            notifyDataSetChanged()
            previousItem = findPreviousItem()
            nextItem = findNextItem()
        }
    var previousItem: ItemWithFeed? = null
        private set
    var nextItem: ItemWithFeed? = null
        private set

    private fun findPreviousItem(): ItemWithFeed? {
        data?.let {
            for (i in 0 until it.size) {
                if ((it[i] as ItemWithFeed).id == selectedItemId) {
                    if (i == 0) {
                        return null
                    }
                    return it[i - 1] as ItemWithFeed?
                }
            }
        }

        return null
    }

    private fun findNextItem(): ItemWithFeed? {
        data?.let {
            for (i in 0 until it.size) {
                if ((it[i] as ItemWithFeed).id == selectedItemId) {
                    if (i == it.size - 1) {
                        return null
                    }
                    return it[i + 1] as ItemWithFeed?
                }
            }
        }

        return null
    }
}