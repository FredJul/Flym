package net.frju.flym.ui.entries

import net.frju.flym.data.entities.EntryWithFeed
import net.idik.lib.slimadapter.SlimAdapter

class EntryAdapter : SlimAdapter() {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return (data[position] as EntryWithFeed).id.hashCode().toLong()
    }

    var selectedEntryId: String? = null
        set(newValue) {
            field = newValue
            notifyDataSetChanged()
            previousEntry = findPreviousEntry()
            nextEntry = findNextEntry()
        }
    var previousEntry: EntryWithFeed? = null
        private set
    var nextEntry: EntryWithFeed? = null
        private set

    private fun findPreviousEntry(): EntryWithFeed? {
        data?.let {
            for (i in 0 until it.size) {
                if ((it[i] as EntryWithFeed).id == selectedEntryId) {
                    if (i == 0) {
                        return null
                    }
                    return it[i - 1] as EntryWithFeed?
                }
            }
        }

        return null
    }

    private fun findNextEntry(): EntryWithFeed? {
        data?.let {
            for (i in 0 until it.size) {
                if ((it[i] as EntryWithFeed).id == selectedEntryId) {
                    if (i == it.size - 1) {
                        return null
                    }
                    return it[i + 1] as EntryWithFeed?
                }
            }
        }

        return null
    }
}