/**
 * Flym
 *
 *
 * Copyright (c) 2012-2015 Frederic Julian
 *
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 *
 *
 *
 *
 * Some parts of this software are based on "Sparse rss" under the MIT license (see
 * below). Please refers to the original project to identify which parts are under the
 * MIT license.
 *
 *
 * Copyright (c) 2010-2012 Stefan Handschuh
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.frju.flym.service

import android.app.IntentService
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Handler
import android.text.Html
import android.text.TextUtils
import android.widget.Toast
import com.einmalfel.earl.EarlParser
import net.fred.feedex.R
import net.frju.flym.App
import net.frju.flym.data.entities.Feed
import net.frju.flym.data.entities.Item
import net.frju.flym.data.entities.Task
import net.frju.flym.data.entities.toDbFormat
import net.frju.flym.toMd5
import net.frju.flym.ui.main.MainActivity
import net.frju.flym.utils.ArticleTextExtractor
import net.frju.flym.utils.HtmlUtils
import net.frju.parentalcontrol.utils.PrefUtils
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Okio
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.CookieHandler
import java.net.CookieManager
import java.util.*
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class FetcherService : IntentService(FetcherService::class.java.simpleName) {

    private val handler = Handler()
    private val notifMgr by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private var downloadPictures = false

    public override fun onHandleIntent(intent: Intent?) {
        if (intent == null) { // No intent, we quit
            return
        }

        val isFromAutoRefresh = intent.getBooleanExtra(FROM_AUTO_REFRESH, false)

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        // Connectivity issue, we quit
        if (networkInfo == null || networkInfo.state != NetworkInfo.State.CONNECTED) {
            if (ACTION_REFRESH_FEEDS == intent.action && !isFromAutoRefresh) {
                // Display a toast in that case
                handler.post { Toast.makeText(this@FetcherService, R.string.network_error, Toast.LENGTH_SHORT).show() }
            }
            return
        }

        val skipFetch = isFromAutoRefresh && PrefUtils.getBoolean(PrefUtils.REFRESH_WIFI_ONLY, false)
                && networkInfo.type != ConnectivityManager.TYPE_WIFI
        // We need to skip the fetching process, so we quit
        if (skipFetch) {
            return
        }

        downloadPictures = shouldDownloadPictures()

        when {
            ACTION_MOBILIZE_FEEDS == intent.action -> {
                mobilizeAllEntries()
                downloadAllImages()
            }
            ACTION_DOWNLOAD_IMAGES == intent.action -> downloadAllImages()
            else -> { // == Constants.ACTION_REFRESH_FEEDS
                PrefUtils.putBoolean(PrefUtils.IS_REFRESHING, true)

                val keepTime = java.lang.Long.parseLong(PrefUtils.getString(PrefUtils.KEEP_TIME, "4")) * 86400000L
                val keepDateBorderTime = if (keepTime > 0) System.currentTimeMillis() - keepTime else 0

                deleteOldItems(keepDateBorderTime)
                COOKIE_MANAGER.cookieStore.removeAll() // Cookies are important for some sites, but we clean them each times

                var newCount = 0
                val feedId = intent.getLongExtra(EXTRA_FEED_ID, 0L)
                if (feedId == 0L) {
                    newCount = refreshFeeds()
                } else {
                    App.db.feedDao().findById(feedId)?.let {
                        newCount = refreshFeed(it)
                    }
                }

                if (newCount > 0) {
                    if (PrefUtils.getBoolean(PrefUtils.NOTIFICATIONS_ENABLED, true)) {
                        val unread = App.db.itemDao().countUnread

                        if (unread > 0) {
                            val text = resources.getQuantityString(R.plurals.number_of_new_entries, unread.toInt(), unread)

                            val notificationIntent = Intent(this, MainActivity::class.java)
                            val contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT)

                            val notifBuilder = Notification.Builder(this) //
                                    .setContentIntent(contentIntent) //
                                    .setSmallIcon(R.drawable.ic_statusbar_rss) //
                                    .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)) //
                                    .setTicker(text) //
                                    .setWhen(System.currentTimeMillis()) //
                                    .setAutoCancel(true) //
                                    .setContentTitle(getString(R.string.flym_feeds)) //
                                    .setContentText(text) //
                                    .setLights(0xffffffff.toInt(), 0, 0)

                            if (PrefUtils.getBoolean(PrefUtils.NOTIFICATIONS_VIBRATE, false)) {
                                notifBuilder.setVibrate(longArrayOf(0, 1000))
                            }

                            val ringtone = PrefUtils.getString(PrefUtils.NOTIFICATIONS_RINGTONE, "")
                            if (ringtone.isNotEmpty()) {
                                notifBuilder.setSound(Uri.parse(ringtone))
                            }

                            if (PrefUtils.getBoolean(PrefUtils.NOTIFICATIONS_LIGHT, false)) {
                                notifBuilder.setLights(0xffffffff.toInt(), 300, 1000)
                            }

                            notifMgr.notify(0, notifBuilder.build())
                        }
                    } else {
                        notifMgr.cancel(0)
                    }
                }

                mobilizeAllEntries()
                downloadAllImages()

                PrefUtils.putBoolean(PrefUtils.IS_REFRESHING, false)
            }
        }
    }

    private fun mobilizeAllEntries() {
        App.db.taskDao().mobilizeTasks.forEach { task ->

            var success = false

            App.db.itemDao().findById(task.itemId)?.let { item ->
                if (item.link != null) {
                    // Try to find a text indicator for better content extraction
                    var contentIndicator: String? = null
                    if (!TextUtils.isEmpty(item.description)) {
                        contentIndicator = Html.fromHtml(item.description).toString()
                        if (contentIndicator.length > 60) {
                            contentIndicator = contentIndicator.substring(20, 40)
                        }
                    }

                    val request = Request.Builder()
                            .url(item.link)
                            .header("User-agent", "Mozilla/5.0 (compatible) AppleWebKit Chrome Safari") // some feeds need this to work properly
                            .addHeader("accept", "*/*")
                            .build()
                    try {
                        HTTP_CLIENT.newCall(request).execute().use {

                            var mobilizedHtml = ArticleTextExtractor.extractContent(it.body()!!.byteStream(), contentIndicator)
                            if (mobilizedHtml != null) {
                                mobilizedHtml = HtmlUtils.improveHtmlContent(mobilizedHtml, getBaseUrl(item.link!!))

                                if (downloadPictures) {
                                    val imgUrlsToDownload = HtmlUtils.getImageURLs(mobilizedHtml)
                                    if (item.imageLink == null && imgUrlsToDownload.isNotEmpty()) {
                                        item.imageLink = HtmlUtils.getMainImageURL(imgUrlsToDownload)
                                    }
                                    addImagesToDownload(item.id, imgUrlsToDownload)
                                } else if (item.imageLink == null) {
                                    item.imageLink = HtmlUtils.getMainImageURL(mobilizedHtml)
                                }

                                success = true

                                App.db.taskDao().deleteAll(task)

                                item.mobilizedContent = mobilizedHtml
                                App.db.itemDao().insertAll(item)
                            }
                        }
                    } catch (_: Throwable) {
                    }
                }
            }

            if (!success) {
                if (task.numberAttempt + 1 > MAX_TASK_ATTEMPT) {
                    App.db.taskDao().deleteAll(task)
                } else {
                    task.numberAttempt += 1
                    App.db.taskDao().insertAll(task)
                }
            }
        }
    }

    private fun downloadAllImages() {
        App.db.taskDao().downloadTasks.forEach { task ->
            try {
                downloadImage(task.itemId, task.imageLinkToDl!!)

                // If we are here, everything is OK
                App.db.taskDao().deleteAll(task)
            } catch (ignored: Exception) {
                if (task.numberAttempt + 1 > MAX_TASK_ATTEMPT) {
                    App.db.taskDao().deleteAll(task)
                } else {
                    task.numberAttempt += 1
                    App.db.taskDao().insertAll(task)
                }
            }
        }
    }

    private fun refreshFeeds(): Int {

        val executor = Executors.newFixedThreadPool(THREAD_NUMBER) { r ->
            Thread(r).apply {
                priority = Thread.MIN_PRIORITY
            }
        }
        val completionService = ExecutorCompletionService<Int>(executor)

        var globalResult = 0

        val feeds = App.db.feedDao().all

        feeds.forEach {
            completionService.submit {
                var result = 0
                try {
                    result = refreshFeed(it)
                } catch (ignored: Exception) {
                }

                result
            }
        }

        for (i in 0 until feeds.size) {
            try {
                val f = completionService.take()
                globalResult += f.get()
            } catch (ignored: Exception) {
            }

        }

        executor.shutdownNow() // To purge observeAll threads

        return globalResult
    }

    private fun refreshFeed(feed: Feed): Int {
        val request = Request.Builder()
                .url(feed.link)
                .header("User-agent", "Mozilla/5.0 (compatible) AppleWebKit Chrome Safari") // some feeds need this to work properly
                .addHeader("accept", "*/*")
                .build()

        val itemsToInsert = mutableListOf<Item>()

        try {
            HTTP_CLIENT.newCall(request).execute().use { response ->
                val earlFeed = EarlParser.parseOrThrow(response.body()!!.byteStream(), 0)
                earlFeed.items.map { it.toDbFormat(feed) }.forEach { itemsToInsert.add(it) }
                feed.update(earlFeed)
            }
        } catch (e: Exception) {
            feed.fetchError = true
        }

        App.db.feedDao().insertAll(feed)

        // First we remove the items that we already have in db (no update to save data)
        val existingIds = App.db.itemDao().checkExistingIds(itemsToInsert.map { it.id })
        itemsToInsert.removeAll { existingIds.contains(it.id) }

        val feedBaseUrl = getBaseUrl(feed.link)

        // Now we improve the html and find images
        itemsToInsert.forEach { item ->
            item.description?.let { desc ->
                // Improve the description
                val improvedContent = HtmlUtils.improveHtmlContent(desc, feedBaseUrl)

                if (downloadPictures) {
                    val imgUrlsToDownload = HtmlUtils.getImageURLs(improvedContent)
                    if (item.imageLink == null && !imgUrlsToDownload.isEmpty()) {
                        item.imageLink = HtmlUtils.getMainImageURL(imgUrlsToDownload)
                    }
                    addImagesToDownload(item.id, imgUrlsToDownload)
                } else if (item.imageLink == null) {
                    item.imageLink = HtmlUtils.getMainImageURL(improvedContent)
                }

                item.description = improvedContent
            }
        }

        // Update everything
        App.db.itemDao().insertAll(*(itemsToInsert.toTypedArray()))

        if (feed.retrieveFullText) {
            FetcherService.addEntriesToMobilize(itemsToInsert.map { it.id })
        }

        return itemsToInsert.size
    }

    private fun deleteOldItems(keepDateBorderTime: Long) {
        if (keepDateBorderTime > 0) {
            App.db.itemDao().deleteOlderThan(keepDateBorderTime)
            // Delete the cache files
            deleteEntriesImagesCache(keepDateBorderTime)
        }
    }

    @Throws(IOException::class)
    private fun downloadImage(itemId: String, imgUrl: String) {
        val tempImgPath = getTempDownloadedImagePath(itemId, imgUrl)
        val finalImgPath = getDownloadedImagePath(itemId, imgUrl)

        if (!File(tempImgPath).exists() && !File(finalImgPath).exists()) {
            IMAGE_FOLDER_FILE.mkdir() // create images dir

            // Compute the real URL (without "&eacute;", ...)
            val realUrl = Html.fromHtml(imgUrl).toString()
            val request = Request.Builder()
                    .url(realUrl)
                    .build()

            try {
                HTTP_CLIENT.newCall(request).execute().use { response ->
                    val fileOutput = FileOutputStream(tempImgPath)

                    val sink = Okio.buffer(Okio.sink(fileOutput))
                    sink.writeAll(response?.body()?.source())
                    sink.close()

                    File(tempImgPath).renameTo(File(finalImgPath))
                }
            } catch (e: Exception) {
                File(tempImgPath).delete()
                throw e
            }
        }
    }

    private fun deleteEntriesImagesCache(keepDateBorderTime: Long) {
        if (IMAGE_FOLDER_FILE.exists()) {

            // We need to exclude favorite entries images to this cleanup
            val favorites = App.db.itemDao().favorites

            IMAGE_FOLDER_FILE.listFiles().forEach { file ->
                if (file.lastModified() < keepDateBorderTime) {
                    var isAFavoriteEntryImage = false
                    favorites.forEach loop@ {
                        if (file.name.startsWith(it.toString() + ID_SEPARATOR)) {
                            isAFavoriteEntryImage = true
                            return@loop
                        }
                    }
                    if (!isAFavoriteEntryImage) {
                        file.delete()
                    }
                }
            }
        }
    }

    private fun getBaseUrl(link: String): String {
        var baseUrl = link
        val index = link.indexOf('/', 8) // this also covers https://
        if (index > -1) {
            baseUrl = link.substring(0, index)
        }

        return baseUrl
    }

    companion object {
        const val EXTRA_FEED_ID = "EXTRA_FEED_ID"

        val HTTP_CLIENT: OkHttpClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

        const val FROM_AUTO_REFRESH = "from_auto_refresh"

        const val ACTION_REFRESH_FEEDS = "net.fred.feedex.REFRESH"
        const val ACTION_MOBILIZE_FEEDS = "net.fred.feedex.MOBILIZE_FEEDS"
        const val ACTION_DOWNLOAD_IMAGES = "net.fred.feedex.DOWNLOAD_IMAGES"

        private const val THREAD_NUMBER = 3
        private const val MAX_TASK_ATTEMPT = 3

        val IMAGE_FOLDER_FILE = File(App.context.cacheDir, "images/")
        val IMAGE_FOLDER = IMAGE_FOLDER_FILE.absolutePath + '/'
        const val TEMP_PREFIX = "TEMP__"
        const val ID_SEPARATOR = "__"

        private val COOKIE_MANAGER = object : CookieManager() {
            init {
                CookieHandler.setDefault(this)
            }
        }

        fun shouldDownloadPictures(): Boolean {
            val fetchPictureMode = PrefUtils.getString(PrefUtils.PRELOAD_IMAGE_MODE, PrefUtils.PRELOAD_IMAGE_MODE__WIFI_ONLY)

            var downloadPictures = false
            if (PrefUtils.getBoolean(PrefUtils.DISPLAY_IMAGES, true)) {
                if (PrefUtils.PRELOAD_IMAGE_MODE__ALWAYS == fetchPictureMode) {
                    downloadPictures = true
                } else if (PrefUtils.PRELOAD_IMAGE_MODE__WIFI_ONLY == fetchPictureMode) {
                    val ni = (App.context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo
                    if (ni != null && ni.type == ConnectivityManager.TYPE_WIFI) {
                        downloadPictures = true
                    }
                }
            }
            return downloadPictures
        }

        fun getDownloadedImagePath(itemId: String, imgUrl: String): String {
            return IMAGE_FOLDER + itemId + ID_SEPARATOR + imgUrl.toMd5()
        }

        fun getTempDownloadedImagePath(itemId: String, imgUrl: String): String {
            return IMAGE_FOLDER + TEMP_PREFIX + itemId + ID_SEPARATOR + imgUrl.toMd5()
        }

        fun getDownloadedOrDistantImageUrl(itemId: String, imgUrl: String): String {
            val dlImgFile = File(getDownloadedImagePath(itemId, imgUrl))
            return if (dlImgFile.exists()) {
                Uri.fromFile(dlImgFile).toString()
            } else {
                imgUrl
            }
        }

        fun addImagesToDownload(itemId: String, images: ArrayList<String>?) {
            if (images != null && !images.isEmpty()) {
                val newTasks = mutableListOf<Task>()
                images.forEach {
                    val task = Task()
                    task.itemId = itemId
                    task.imageLinkToDl = it
                    newTasks.add(task)
                }

                App.db.taskDao().insertAll(*newTasks.toTypedArray())
            }
        }

        fun addEntriesToMobilize(itemIds: List<String>) {
            val newTasks = mutableListOf<Task>()
            itemIds.forEach {
                val task = Task()
                task.itemId = it
                newTasks.add(task)
            }

            App.db.taskDao().insertAll(*newTasks.toTypedArray())
        }
    }
}
