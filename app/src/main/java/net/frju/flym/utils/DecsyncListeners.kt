package net.frju.flym.utils

import android.util.Log
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import net.frju.flym.App
import net.frju.flym.data.entities.Feed
import org.decsync.library.Decsync

private const val TAG = "DecsyncListeners"

@ExperimentalStdlibApi
object DecsyncListeners {
    fun readListener(path: List<String>, entry: Decsync.Entry, extra: Extra) {
        Log.d(TAG, "Execute read entry $entry")
        val uri = entry.key.jsonPrimitive.content
        val value = entry.value.jsonPrimitive.boolean
        val id = App.db.entryDao().idForUri(uri) ?: run {
            Log.i(TAG, "Unknown article $uri")
            return
        }
        if (value) {
            App.db.entryDao().markAsRead(listOf(id))
        } else {
            App.db.entryDao().markAsUnread(listOf(id))
        }
    }

    fun markedListener(path: List<String>, entry: Decsync.Entry, extra: Extra) {
        Log.d(TAG, "Execute mark entry $entry")
        val uri = entry.key.jsonPrimitive.content
        val value = entry.value.jsonPrimitive.boolean
        val id = App.db.entryDao().idForUri(uri) ?: run {
            Log.i(TAG, "Unknown article $uri")
            return
        }
        if (value) {
            App.db.entryDao().markAsFavorite(id)
        } else {
            App.db.entryDao().markAsNotFavorite(id)
        }
    }

    fun subscriptionsListener(path: List<String>, entry: Decsync.Entry, extra: Extra) {
        Log.d(TAG, "Execute subscribe entry $entry")
        val link = entry.key.jsonPrimitive.content
        val subscribed = entry.value.jsonPrimitive.boolean
        if (subscribed) {
            if (App.db.feedDao().findByLink(link) == null) {
                App.db.feedDao().insert(Feed(link = link))
            }
        } else {
            val feed = App.db.feedDao().findByLink(link) ?: run {
                Log.i(TAG, "Unknown feed $link")
                return
            }
            val groupId = feed.groupId
            App.db.feedDao().delete(feed)
            removeGroupIfEmpty(groupId)
        }
    }

    fun feedNamesListener(path: List<String>, entry: Decsync.Entry, extra: Extra) {
        Log.d(TAG, "Execute rename entry $entry")
        val link = entry.key.jsonPrimitive.content
        val name = entry.value.jsonPrimitive.content
        val feed = App.db.feedDao().findByLink(link) ?: run {
            Log.i(TAG, "Unknown feed $link")
            return
        }
        if (feed.title != name) {
            feed.title = name
            App.db.feedDao().update(feed)
        }
    }

    fun categoriesListener(path: List<String>, entry: Decsync.Entry, extra: Extra) {
        Log.d(TAG, "Execute move entry $entry")
        val link = entry.key.jsonPrimitive.content
        val catId = entry.value.jsonPrimitive.contentOrNull
        val feed = App.db.feedDao().findByLink(link) ?: run {
            Log.i(TAG, "Unknown feed $link")
            return
        }
        val groupId = catId?.let {
            App.db.feedDao().findByLink(catId)?.id ?: run {
                val group = Feed(link = catId, title = catId, isGroup = true)
                App.db.feedDao().insert(group)[0]
            }
        }
        if (feed.groupId != groupId) {
            val oldGroupId = feed.groupId
            feed.groupId = groupId
            App.db.feedDao().update(feed)
            removeGroupIfEmpty(oldGroupId)
        }
    }

    fun categoryNamesListener(path: List<String>, entry: Decsync.Entry, extra: Extra) {
        Log.d(TAG, "Execute category rename entry $entry")
        val catId = entry.key.jsonPrimitive.content
        val name = entry.value.jsonPrimitive.content
        val group = App.db.feedDao().findByLink(catId) ?: run {
            Log.i(TAG, "Unknown category $catId")
            return
        }
        if (group.title != name) {
            group.title = name
            App.db.feedDao().update(group)
        }
    }

    fun categoryParentsListener(path: List<String>, entry: Decsync.Entry, extra: Extra) {
        Log.i(TAG, "Nested categories are not supported")
    }

    private fun removeGroupIfEmpty(groupId: Long?) {
        if (groupId == null) return
        if (App.db.feedDao().allFeedsInGroup(groupId).isEmpty()) {
            val group = App.db.feedDao().findById(groupId) ?: return
            App.db.feedDao().delete(group)
        }
    }
}