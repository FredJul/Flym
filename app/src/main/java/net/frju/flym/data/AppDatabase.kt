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

package net.frju.flym.data

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import android.content.Context
import net.frju.flym.data.converters.Converters
import net.frju.flym.data.dao.EntryDao
import net.frju.flym.data.dao.FeedDao
import net.frju.flym.data.dao.TaskDao
import net.frju.flym.data.entities.Entry
import net.frju.flym.data.entities.Feed
import net.frju.flym.data.entities.Task
import org.jetbrains.anko.doAsync


@Database(entities = [Feed::class, Entry::class, Task::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        private const val DATABASE_NAME = "db"

        fun createDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DATABASE_NAME)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)

                            doAsync {
                                // insert => add max priority for the group
                                db.execSQL("""
                                CREATE TRIGGER feed_insert_priority
                                    AFTER INSERT
                                    ON feeds
                                BEGIN
                                   UPDATE feeds SET displayPriority = IFNULL((SELECT MAX(displayPriority) FROM feeds WHERE groupId IS NEW.groupId), 0) + 1 WHERE feedId = NEW.feedId;
                                END;
                                """.trimIndent())

                                // groupId changed => decrease priority of feeds from old group
                                db.execSQL("""
                                CREATE TRIGGER feed_update_decrease_priority
                                    BEFORE UPDATE OF lastManualActionUid
                                    ON feeds
                                    WHEN OLD.groupId IS NOT NEW.groupId
                                BEGIN
                                   UPDATE feeds SET displayPriority = displayPriority - 1 WHERE displayPriority > NEW.displayPriority AND groupId IS OLD.groupId AND feedId != NEW.feedId;
                                END;
                                """.trimIndent())

                                // groupId changed => increase priority of feeds from new group
                                db.execSQL("""
                                CREATE TRIGGER feed_update_increase_priority
                                    BEFORE UPDATE OF lastManualActionUid
                                    ON feeds
                                    WHEN OLD.groupId IS NOT NEW.groupId
                                BEGIN
                                   UPDATE feeds SET displayPriority = displayPriority + 1 WHERE displayPriority > NEW.displayPriority - 1 AND groupId IS NEW.groupId AND feedId != NEW.feedId;
                                END;
                                """.trimIndent())

                                // same groupId => decrease priority of some group's feeds
                                db.execSQL("""
                                CREATE TRIGGER feed_update_decrease_priority_same_group
                                    BEFORE UPDATE OF lastManualActionUid
                                    ON feeds
                                    WHEN OLD.groupId IS NEW.groupId AND NEW.displayPriority > OLD.displayPriority
                                BEGIN
                                   UPDATE feeds SET displayPriority = displayPriority - 1 WHERE (displayPriority BETWEEN OLD.displayPriority + 1 AND NEW.displayPriority) AND groupId IS OLD.groupId AND feedId != NEW.feedId;
                                END;
                                """.trimIndent())

                                // same groupId => increase priority of some group's feeds
                                db.execSQL("""
                                CREATE TRIGGER feed_update_increase_priority_same_group
                                    BEFORE UPDATE OF lastManualActionUid
                                    ON feeds
                                    WHEN OLD.groupId IS NEW.groupId AND NEW.displayPriority < OLD.displayPriority
                                BEGIN
                                   UPDATE feeds SET displayPriority = displayPriority + 1 WHERE (displayPriority BETWEEN NEW.displayPriority AND OLD.displayPriority - 1) AND groupId IS OLD.groupId AND feedId != NEW.feedId;
                                END;
                                """.trimIndent())
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