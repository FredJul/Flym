package net.frju.flym.data

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import android.content.Context
import android.os.Environment
import com.rometools.opml.feed.opml.Opml
import com.rometools.rome.io.WireFeedInput
import net.frju.flym.App
import net.frju.flym.data.converters.Converters
import net.frju.flym.data.dao.EntryDao
import net.frju.flym.data.dao.FeedDao
import net.frju.flym.data.dao.TaskDao
import net.frju.flym.data.entities.Entry
import net.frju.flym.data.entities.Feed
import net.frju.flym.data.entities.Task
import org.jetbrains.anko.doAsync
import java.io.File


@Database(entities = arrayOf(Feed::class, Entry::class, Task::class), version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

	companion object {
		private const val DATABASE_NAME = "db"
		private val BACKUP_OPML = File(Environment.getExternalStorageDirectory(), "/Flym_auto_backup.opml")

		fun createDatabase(context: Context): AppDatabase {
			return Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DATABASE_NAME)
					.addCallback(object : Callback() {
						override fun onCreate(db: SupportSQLiteDatabase) {
							super.onCreate(db)

//                            val values = ContentValues()
//                            values.put("feedTitle", "Google News")
//                            values.put("feedLink", "https://news.google.fr/?output=rss")
//                            values.put("fetchError", false)
//                            values.put("retrieveFullText", true)
//                            values.put("isGroup", false)
//                            values.put("displayPriority", 0)
//                            db.insert("feeds", SQLiteDatabase.CONFLICT_REPLACE, values)

							// TODO permisions request in activity
							// Import the OPML backup if possible
							doAsync {
								if (BACKUP_OPML.exists()) {
									var id = 1L
									val feedList = mutableListOf<Feed>()
									val opml = WireFeedInput().build(BACKUP_OPML) as Opml
									opml.outlines.forEach {
										if (it.xmlUrl != null || it.children.isNotEmpty()) {
											val topLevelFeed = Feed()
											topLevelFeed.id = id++
											topLevelFeed.title = it.title
											feedList.add(topLevelFeed)

											if (it.xmlUrl != null) {
												topLevelFeed.link = it.xmlUrl
												topLevelFeed.retrieveFullText = it.getAttributeValue("retrieveFullText") == "true"
											} else {
												topLevelFeed.isGroup = true

												it.children.filter { it.xmlUrl != null }.forEach {
													val subLevelFeed = Feed()
													subLevelFeed.id = id++
													subLevelFeed.title = it.title
													subLevelFeed.link = it.xmlUrl
													subLevelFeed.retrieveFullText = it.getAttributeValue("retrieveFullText") == "true"
													subLevelFeed.groupId = topLevelFeed.id
													feedList.add(subLevelFeed)
												}
											}
										}
									}

									if (feedList.isNotEmpty()) {
										App.db.feedDao().insert(*feedList.toTypedArray())
									}
								}
							}
						}
					})
					.build()
		}
	}

	abstract fun feedDao(): FeedDao
	abstract fun entryDao(): EntryDao
	abstract fun taskDao(): TaskDao
}