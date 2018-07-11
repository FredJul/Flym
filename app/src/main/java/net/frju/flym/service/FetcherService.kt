/*
 * Copyright (c) 2012-2018 Frederic Julian
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package net.frju.flym.service

import android.annotation.TargetApi
import android.app.IntentService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.support.v4.app.NotificationCompat
import android.text.Html
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import net.dankito.readability4j.extended.Readability4JExtended
import net.fred.feedex.R
import net.frju.flym.App
import net.frju.flym.data.entities.Entry
import net.frju.flym.data.entities.Feed
import net.frju.flym.data.entities.Task
import net.frju.flym.data.entities.toDbFormat
import net.frju.flym.data.utils.PrefUtils
import net.frju.flym.ui.main.MainActivity
import net.frju.flym.utils.HtmlUtils
import net.frju.flym.utils.sha1
import okhttp3.Call
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Okio
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.connectivityManager
import org.jetbrains.anko.error
import org.jetbrains.anko.notificationManager
import org.jetbrains.anko.toast
import org.jsoup.Jsoup
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class FetcherService : IntentService(FetcherService::class.java.simpleName) {

	companion object : AnkoLogger {
		const val EXTRA_FEED_ID = "EXTRA_FEED_ID"

		private val COOKIE_MANAGER = CookieManager().apply {
			setCookiePolicy(CookiePolicy.ACCEPT_ALL)
		}

		private val HTTP_CLIENT: OkHttpClient = OkHttpClient.Builder()
				.connectTimeout(4, TimeUnit.SECONDS)
				.readTimeout(4, TimeUnit.SECONDS)
				.cookieJar(JavaNetCookieJar(COOKIE_MANAGER))
				.build()

		const val FROM_AUTO_REFRESH = "FROM_AUTO_REFRESH"

		const val ACTION_REFRESH_FEEDS = "net.frju.flym.REFRESH"
		const val ACTION_MOBILIZE_FEEDS = "net.frju.flym.MOBILIZE_FEEDS"
		const val ACTION_DOWNLOAD_IMAGES = "net.frju.flym.DOWNLOAD_IMAGES"

		private const val THREAD_NUMBER = 3
		private const val MAX_TASK_ATTEMPT = 3

		private val IMAGE_FOLDER_FILE = File(App.context.cacheDir, "images/")
		private val IMAGE_FOLDER = IMAGE_FOLDER_FILE.absolutePath + '/'
		private const val TEMP_PREFIX = "TEMP__"
		private const val ID_SEPARATOR = "__"

		fun createCall(url: String): Call = HTTP_CLIENT.newCall(Request.Builder()
				.url(url)
				.header("User-agent", "Mozilla/5.0 (compatible) AppleWebKit Chrome Safari") // some feeds need this to work properly
				.addHeader("accept", "*/*")
				.build())

		fun fetch(context: Context, isFromAutoRefresh: Boolean, action: String, feedId: Long = 0L) {
			if (PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false)) {
				return
			}

			val networkInfo = context.connectivityManager.activeNetworkInfo
			// Connectivity issue, we quit
			if (networkInfo == null || networkInfo.state != NetworkInfo.State.CONNECTED) {
				return
			}

			val skipFetch = isFromAutoRefresh && PrefUtils.getBoolean(PrefUtils.REFRESH_WIFI_ONLY, false)
					&& networkInfo.type != ConnectivityManager.TYPE_WIFI
			// We need to skip the fetching process, so we quit
			if (skipFetch) {
				return
			}

			when {
				ACTION_MOBILIZE_FEEDS == action -> {
					mobilizeAllEntries()
					downloadAllImages()
				}
				ACTION_DOWNLOAD_IMAGES == action -> downloadAllImages()
				else -> { // == Constants.ACTION_REFRESH_FEEDS
					PrefUtils.putBoolean(PrefUtils.IS_REFRESHING, true)

					val keepTime = PrefUtils.getString(PrefUtils.KEEP_TIME, "4").toLong() * 86400000L
					val keepDateBorderTime = if (keepTime > 0) System.currentTimeMillis() - keepTime else 0

					deleteOldEntries(keepDateBorderTime)
					COOKIE_MANAGER.cookieStore.removeAll() // Cookies are important for some sites, but we clean them each times

					var newCount = 0
					if (feedId == 0L) {
						newCount = refreshFeeds(keepDateBorderTime)
					} else {
						App.db.feedDao().findById(feedId)?.let {
							newCount = refreshFeed(it, keepDateBorderTime)
						}
					}

					if (newCount > 0) {
						if (!MainActivity.isInForeground) {
							val unread = App.db.entryDao().countUnread

							if (unread > 0) {
								val text = context.resources.getQuantityString(R.plurals.number_of_new_entries, unread.toInt(), unread)

								val notificationIntent = Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_FROM_NOTIF, true)
								val contentIntent = PendingIntent.getActivity(context, 0, notificationIntent,
										PendingIntent.FLAG_UPDATE_CURRENT)

								val channelId = "notif_channel"

								@TargetApi(Build.VERSION_CODES.O)
								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
									val channel = NotificationChannel(channelId, context.getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT)
									context.notificationManager.createNotificationChannel(channel)
								}

								val notifBuilder = NotificationCompat.Builder(context, channelId)
										.setContentIntent(contentIntent)
										.setSmallIcon(R.drawable.ic_statusbar_rss)
										.setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
										.setTicker(text)
										.setWhen(System.currentTimeMillis())
										.setAutoCancel(true)
										.setContentTitle(context.getString(R.string.flym_feeds))
										.setContentText(text)

								context.notificationManager.notify(0, notifBuilder.build())
							}
						} else {
							context.notificationManager.cancel(0)
						}
					}

					mobilizeAllEntries()
					downloadAllImages()

					PrefUtils.putBoolean(PrefUtils.IS_REFRESHING, false)
				}
			}
		}

		fun shouldDownloadPictures(): Boolean {
			val fetchPictureMode = PrefUtils.getString(PrefUtils.PRELOAD_IMAGE_MODE, PrefUtils.PRELOAD_IMAGE_MODE__WIFI_ONLY)

			if (PrefUtils.getBoolean(PrefUtils.DISPLAY_IMAGES, true)) {
				if (PrefUtils.PRELOAD_IMAGE_MODE__ALWAYS == fetchPictureMode) {
					return true
				} else if (PrefUtils.PRELOAD_IMAGE_MODE__WIFI_ONLY == fetchPictureMode) {
					val ni = App.context.connectivityManager.activeNetworkInfo
					if (ni != null && ni.type == ConnectivityManager.TYPE_WIFI) {
						return true
					}
				}
			}

			return false
		}

		fun getDownloadedImagePath(entryId: String, imgUrl: String): String =
				IMAGE_FOLDER + entryId + ID_SEPARATOR + imgUrl.sha1()

		private fun getTempDownloadedImagePath(entryId: String, imgUrl: String): String =
				IMAGE_FOLDER + TEMP_PREFIX + entryId + ID_SEPARATOR + imgUrl.sha1()

		fun getDownloadedOrDistantImageUrl(entryId: String, imgUrl: String): String {
			val dlImgFile = File(getDownloadedImagePath(entryId, imgUrl))
			return if (dlImgFile.exists()) {
				Uri.fromFile(dlImgFile).toString()
			} else {
				imgUrl
			}
		}

		fun addImagesToDownload(imgUrlsToDownload: Map<String, List<String>>) {
			if (imgUrlsToDownload.isNotEmpty()) {
				val newTasks = mutableListOf<Task>()
				for ((key, value) in imgUrlsToDownload) {
					for (img in value) {
						val task = Task()
						task.entryId = key
						task.imageLinkToDl = img
						newTasks.add(task)
					}
				}

				App.db.taskDao().insert(*newTasks.toTypedArray())
			}
		}

		fun addEntriesToMobilize(entryIds: List<String>) {
			val newTasks = mutableListOf<Task>()
			for (id in entryIds) {
				val task = Task()
				task.entryId = id
				newTasks.add(task)
			}

			App.db.taskDao().insert(*newTasks.toTypedArray())
		}


		private fun mobilizeAllEntries() {

			val tasks = App.db.taskDao().mobilizeTasks
			val imgUrlsToDownload = mutableMapOf<String, List<String>>()

			val downloadPictures = shouldDownloadPictures()

			for (task in tasks) {
				var success = false

				App.db.entryDao().findById(task.entryId)?.let { entry ->
					entry.link?.let { link ->
						try {
							createCall(link).execute().use {
								it.body()?.byteStream()?.let { input ->
									Readability4JExtended(link, Jsoup.parse(input, null, link)).parse().articleContent?.html()?.let {
										val mobilizedHtml = HtmlUtils.improveHtmlContent(it, getBaseUrl(link))

										if (downloadPictures) {
											val imagesList = HtmlUtils.getImageURLs(mobilizedHtml)
											if (imagesList.isNotEmpty()) {
												if (entry.imageLink == null) {
													entry.imageLink = HtmlUtils.getMainImageURL(imagesList)
												}
												imgUrlsToDownload[entry.id] = imagesList
											}
										} else if (entry.imageLink == null) {
											entry.imageLink = HtmlUtils.getMainImageURL(mobilizedHtml)
										}

										success = true

										entry.mobilizedContent = mobilizedHtml
										App.db.entryDao().update(entry)

										App.db.taskDao().delete(task)
									}
								}
							}
						} catch (t: Throwable) {
							error("Can't mobilize feedWithCount ${entry.link}", t)
						}
					}
				}

				if (!success) {
					if (task.numberAttempt + 1 > MAX_TASK_ATTEMPT) {
						App.db.taskDao().delete(task)
					} else {
						task.numberAttempt += 1
						App.db.taskDao().update(task)
					}
				}
			}

			addImagesToDownload(imgUrlsToDownload)
		}

		private fun downloadAllImages() {
			if (shouldDownloadPictures()) {
				val tasks = App.db.taskDao().downloadTasks
				for (task in tasks) {
					try {
						downloadImage(task.entryId, task.imageLinkToDl)

						// If we are here, everything is OK
						App.db.taskDao().delete(task)
					} catch (ignored: Exception) {
						if (task.numberAttempt + 1 > MAX_TASK_ATTEMPT) {
							App.db.taskDao().delete(task)
						} else {
							task.numberAttempt += 1
							App.db.taskDao().insert(task)
						}
					}
				}
			}
		}

		private fun refreshFeeds(keepDateBorderTime: Long): Int {

			val executor = Executors.newFixedThreadPool(THREAD_NUMBER) { r ->
				Thread(r).apply {
					priority = Thread.MIN_PRIORITY
				}
			}
			val completionService = ExecutorCompletionService<Int>(executor)

			var globalResult = 0

			val feeds = App.db.feedDao().allNonGroupFeeds
			for (feed in feeds) {
				completionService.submit {
					var result = 0
					try {
						result = refreshFeed(feed, keepDateBorderTime)
					} catch (e: Exception) {
						error("Can't fetch feedWithCount ${feed.link}", e)
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

		private fun refreshFeed(feed: Feed, keepDateBorderTime: Long): Int {
			val entries = mutableListOf<Entry>()
			val entriesToInsert = mutableListOf<Entry>()
			val imgUrlsToDownload = mutableMapOf<String, List<String>>()
			val downloadPictures = shouldDownloadPictures()

			val previousFeedState = feed.copy()
			try {
				createCall(feed.link).execute().use { response ->
					val input = SyndFeedInput()
					val romeFeed = input.build(XmlReader(response.body()!!.byteStream()))
					romeFeed.entries.filter { it.publishedDate?.time ?: Long.MAX_VALUE > keepDateBorderTime }.map { it.toDbFormat(feed) }.forEach { entries.add(it) }
					feed.update(romeFeed)
				}
			} catch (t: Throwable) {
				feed.fetchError = true
			}

			if (feed != previousFeedState) {
				App.db.feedDao().update(feed)
			}

			// First we remove the entries that we already have in db (no update to save data)
			val existingIds = App.db.entryDao().idsForFeed(feed.id)
			entries.removeAll { existingIds.contains(it.id) }

			val feedBaseUrl = getBaseUrl(feed.link)
			var foundExisting = false

			// Now we improve the html and find images
			for (entry in entries) {
				if (existingIds.contains(entry.id)) {
					foundExisting = true
				}

				if (entry.publicationDate != entry.fetchDate || !foundExisting) { // we try to not put back old entries, even when there is no date
					if (!existingIds.contains(entry.id)) {
						entriesToInsert.add(entry)

						entry.title = entry.title?.replace("\n", " ")?.trim()
						entry.description?.let { desc ->
							// Improve the description
							val improvedContent = HtmlUtils.improveHtmlContent(desc, feedBaseUrl)

							if (downloadPictures) {
								val imagesList = HtmlUtils.getImageURLs(improvedContent)
								if (imagesList.isNotEmpty()) {
									if (entry.imageLink == null) {
										entry.imageLink = HtmlUtils.getMainImageURL(imagesList)
									}
									imgUrlsToDownload[entry.id] = imagesList
								}
							} else if (entry.imageLink == null) {
								entry.imageLink = HtmlUtils.getMainImageURL(improvedContent)
							}

							entry.description = improvedContent
						}
					} else {
						foundExisting = true
					}
				}
			}

			// Update everything
			App.db.entryDao().insert(*(entriesToInsert.toTypedArray()))

			if (feed.retrieveFullText) {
				FetcherService.addEntriesToMobilize(entries.map { it.id })
			}

			addImagesToDownload(imgUrlsToDownload)

			return entries.size
		}

		private fun deleteOldEntries(keepDateBorderTime: Long) {
			if (keepDateBorderTime > 0) {
				App.db.entryDao().deleteOlderThan(keepDateBorderTime)
				// Delete the cache files
				deleteEntriesImagesCache(keepDateBorderTime)
			}
		}

		@Throws(IOException::class)
		private fun downloadImage(entryId: String, imgUrl: String) {
			val tempImgPath = getTempDownloadedImagePath(entryId, imgUrl)
			val finalImgPath = getDownloadedImagePath(entryId, imgUrl)

			if (!File(tempImgPath).exists() && !File(finalImgPath).exists()) {
				IMAGE_FOLDER_FILE.mkdir() // create images dir

				// Compute the real URL (without "&eacute;", ...)
				@Suppress("DEPRECATION")
				val realUrl = Html.fromHtml(imgUrl).toString()

				try {
					createCall(realUrl).execute().use { response ->
						response?.body()?.let { body ->
							val fileOutput = FileOutputStream(tempImgPath)

							val sink = Okio.buffer(Okio.sink(fileOutput))
							sink.writeAll(body.source())
							sink.close()

							File(tempImgPath).renameTo(File(finalImgPath))
						}
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
				val favoriteIds = App.db.entryDao().favoriteIds

				IMAGE_FOLDER_FILE.listFiles().forEach { file ->
					// If old file and not part of a favorite entry
					if (file.lastModified() < keepDateBorderTime && !favoriteIds.any { file.name.startsWith(it + ID_SEPARATOR) }) {
						file.delete()
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
	}

	private val handler = Handler()

	public override fun onHandleIntent(intent: Intent?) {
		if (intent == null) { // No intent, we quit
			return
		}

		val isFromAutoRefresh = intent.getBooleanExtra(FROM_AUTO_REFRESH, false)

		val networkInfo = connectivityManager.activeNetworkInfo
		// Connectivity issue, we quit
		if (networkInfo == null || networkInfo.state != NetworkInfo.State.CONNECTED) {
			if (ACTION_REFRESH_FEEDS == intent.action && !isFromAutoRefresh) {
				// Display a toast in that case
				handler.post { toast(R.string.network_error).show() }
			}
			return
		}

		fetch(this, isFromAutoRefresh, intent.action, intent.getLongExtra(EXTRA_FEED_ID, 0L))
	}
}
