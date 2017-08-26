package net.frju.flym.data

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import net.frju.flym.data.converters.Converters
import net.frju.flym.data.dao.FeedDao
import net.frju.flym.data.dao.ItemDao
import net.frju.flym.data.dao.TaskDao
import net.frju.flym.data.entities.Feed
import net.frju.flym.data.entities.Item
import net.frju.flym.data.entities.Task
import java.util.*


@Database(entities = arrayOf(Feed::class, Item::class, Task::class), version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun feedDao(): FeedDao
    abstract fun itemDao(): ItemDao
    abstract fun taskDao(): TaskDao

    companion object {
        private const val DATABASE_NAME = "db"

        fun createDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DATABASE_NAME)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)

                            val values = ContentValues()
                            values.put("feedId", UUID.randomUUID().toString())
                            values.put("feedTitle", "Google News")
                            values.put("feedLink", "https://news.google.fr/?output=rss")
                            values.put("feedCreationDate", Date().time)
                            values.put("fetchError", false)
                            values.put("retrieveFullText", true)
                            values.put("isGroup", false)
                            values.put("displayPriority", 0)
                            db.insert("feeds", SQLiteDatabase.CONFLICT_REPLACE, values)
                        }
                    })
                    .build()
        }
    }
}