package net.frju.flym.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import net.fred.feedex.R
import net.frju.flym.App
import net.frju.flym.data.entities.Feed
import net.frju.flym.data.utils.PrefConstants.DECSYNC_ENABLED
import net.frju.flym.data.utils.PrefConstants.DECSYNC_FILE
import net.frju.flym.data.utils.PrefConstants.DECSYNC_USE_SAF
import net.frju.flym.data.utils.PrefConstants.UPDATE_FORCES_SAF
import net.frju.flym.service.FetcherService
import org.decsync.library.Decsync
import org.decsync.library.DecsyncPrefUtils
import org.decsync.library.getAppId
import org.jetbrains.anko.notificationManager
import java.io.File

val ownAppId = getAppId("Flym")
val defaultDecsyncDir = "${Environment.getExternalStorageDirectory()}/DecSync"
private const val TAG = "DecsyncUtils"
private const val ERROR_NOTIFICATION_ID = 1

class Extra

@ExperimentalStdlibApi
object DecsyncUtils {
    private var mDecsync: Decsync<Extra>? = null

    private fun getNewDecsync(context: Context): Decsync<Extra> {
        val decsync = if (context.getPrefBoolean(DECSYNC_USE_SAF, false)) {
            val decsyncDir = DecsyncPrefUtils.getDecsyncDir(context) ?: throw Exception(context.getString(R.string.settings_decsync_dir_not_configured))
            Decsync<Extra>(context, decsyncDir, "rss", null, ownAppId)
        } else {
            val decsyncDir = File(context.getPrefString(DECSYNC_FILE, defaultDecsyncDir))
            Decsync<Extra>(decsyncDir, "rss", null, ownAppId)
        }
        decsync.addListener(listOf("articles", "read"), ::readListener)
        decsync.addListener(listOf("articles", "marked"), ::markedListener)
        decsync.addListener(listOf("feeds", "subscriptions"), ::subscriptionsListener)
        decsync.addListener(listOf("feeds", "names"), ::feedNamesListener)
        decsync.addListener(listOf("feeds", "categories"), ::categoriesListener)
        decsync.addListener(listOf("categories", "names"), ::categoryNamesListener)
        decsync.addListener(listOf("categories", "parents"), ::categoryParentsListener)
        return decsync
    }

    fun getDecsync(context: Context): Decsync<Extra>? {
        if (mDecsync == null && context.getPrefBoolean(DECSYNC_ENABLED, false)) {
            if (Build.VERSION.SDK_INT >= 29 &&
                    !Environment.isExternalStorageLegacy() &&
                    !context.getPrefBoolean(DECSYNC_USE_SAF, false)) {
                context.putPrefBoolean(DECSYNC_ENABLED, false)
                context.putPrefBoolean(DECSYNC_USE_SAF, true)
                context.putPrefBoolean(UPDATE_FORCES_SAF, true)
                return null
            }
            try {
                mDecsync = getNewDecsync(context)
            } catch (e: Exception) {
                Log.e(TAG, "", e)
                context.putPrefBoolean(DECSYNC_ENABLED, false)

                val channelId = "channel_error"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                            channelId,
                            context.getString(R.string.channel_error_name),
                            NotificationManager.IMPORTANCE_DEFAULT
                    )
                    context.notificationManager.createNotificationChannel(channel)
                }
                val notification = NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.drawable.ic_statusbar_rss)
                        .setLargeIcon(
                                BitmapFactory.decodeResource(
                                        context.resources,
                                        R.mipmap.ic_launcher
                                )
                        )
                        .setContentTitle(context.getString(R.string.decsync_disabled))
                        .setContentText(e.localizedMessage)
                        .build()
                context.notificationManager.notify(ERROR_NOTIFICATION_ID, notification)
            }
        }
        return mDecsync
    }

    fun initSync(context: Context) {
        mDecsync = null
        context.startService(Intent(context, FetcherService::class.java)
                .setAction(FetcherService.ACTION_REFRESH_FEEDS)
                .putExtra(FetcherService.FROM_INIT_SYNC, true))
    }

    private fun readListener(path: List<String>, entry: Decsync.Entry, extra: Extra) {
        Log.d(TAG, "Execute read entry $entry")
        val uri = entry.key.jsonPrimitive.content
        val value = entry.value.jsonPrimitive.boolean
        val id = App.db.entryDao().idForUri(uri) ?: run {
            Log.i(TAG, "Unknown article $uri")
            return
        }
        if (value) {
            App.db.entryDao().markAsRead(listOf(id), false)
        } else {
            App.db.entryDao().markAsUnread(listOf(id), false)
        }
    }

    private fun markedListener(path: List<String>, entry: Decsync.Entry, extra: Extra) {
        Log.d(TAG, "Execute mark entry $entry")
        val uri = entry.key.jsonPrimitive.content
        val value = entry.value.jsonPrimitive.boolean
        val id = App.db.entryDao().idForUri(uri) ?: run {
            Log.i(TAG, "Unknown article $uri")
            return
        }
        if (value) {
            App.db.entryDao().markAsFavorite(id, false)
        } else {
            App.db.entryDao().markAsNotFavorite(id, false)
        }
    }

    private fun subscriptionsListener(path: List<String>, entry: Decsync.Entry, extra: Extra) {
        Log.d(TAG, "Execute subscribe entry $entry")
        val link = entry.key.jsonPrimitive.content
        val subscribed = entry.value.jsonPrimitive.boolean
        if (subscribed) {
            if (App.db.feedDao().findByLink(link) == null) {
                App.db.feedDao().insert(Feed(link = link), false)
            }
        } else {
            val feed = App.db.feedDao().findByLink(link) ?: run {
                Log.i(TAG, "Unknown feed $link")
                return
            }
            val groupId = feed.groupId
            App.db.feedDao().delete(feed, false)
            removeGroupIfEmpty(groupId)
        }
    }

    private fun feedNamesListener(path: List<String>, entry: Decsync.Entry, extra: Extra) {
        Log.d(TAG, "Execute rename entry $entry")
        val link = entry.key.jsonPrimitive.content
        val name = entry.value.jsonPrimitive.content
        val feed = App.db.feedDao().findByLink(link) ?: run {
            Log.i(TAG, "Unknown feed $link")
            return
        }
        feed.title = name
        App.db.feedDao().update(feed)
    }

    private fun categoriesListener(path: List<String>, entry: Decsync.Entry, extra: Extra) {
        Log.d(TAG, "Execute move entry $entry")
        val link = entry.key.jsonPrimitive.content
        val catId = entry.value.jsonPrimitive.contentOrNull
        val feed = App.db.feedDao().findByLink(link) ?: run {
            Log.i(TAG, "Unknown feed $link")
            return
        }
        val oldGroupId = feed.groupId
        val groupId = catId?.let {
            App.db.feedDao().findByLink(catId)?.id ?: run {
                val group = Feed(link = catId, title = catId, isGroup = true)
                App.db.feedDao().insert(group, false)
            }
        }
        feed.groupId = groupId
        App.db.feedDao().update(feed, false)
        removeGroupIfEmpty(oldGroupId)
    }

    private fun categoryNamesListener(path: List<String>, entry: Decsync.Entry, extra: Extra) {
        Log.d(TAG, "Execute category rename entry $entry")
        val catId = entry.key.jsonPrimitive.content
        val name = entry.value.jsonPrimitive.content
        val group = App.db.feedDao().findByLink(catId) ?: run {
            Log.i(TAG, "Unknown category $catId")
            return
        }
        group.title = name
        App.db.feedDao().update(group, false)
    }

    private fun categoryParentsListener(path: List<String>, entry: Decsync.Entry, extra: Extra) {
        Log.i(TAG, "Nested categories are not supported")
    }

    private fun removeGroupIfEmpty(groupId: Long?) {
        if (groupId == null) return
        if (App.db.feedDao().allFeedsInGroup(groupId).isEmpty()) {
            App.db.feedDao().deleteById(groupId, false)
        }
    }
}