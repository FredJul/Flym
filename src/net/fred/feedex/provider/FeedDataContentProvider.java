/**
 * FeedEx
 * 
 * Copyright (c) 2012-2013 Frederic Julian
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * 	
 * Some parts of this software are based on "Sparse rss" under the MIT license (see
 * below). Please refers to the original project to identify which parts are under the
 * MIT license.
 * 
 * Copyright (c) 2010-2012 Stefan Handschuh
 * 
 *     Permission is hereby granted, free of charge, to any person obtaining a copy
 *     of this software and associated documentation files (the "Software"), to deal
 *     in the Software without restriction, including without limitation the rights
 *     to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *     copies of the Software, and to permit persons to whom the Software is
 *     furnished to do so, subject to the following conditions:
 * 
 *     The above copyright notice and this permission notice shall be included in
 *     all copies or substantial portions of the Software.
 * 
 *     THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *     IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *     FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *     AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *     LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *     OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *     THE SOFTWARE.
 */

package net.fred.feedex.provider;

import java.io.File;

import net.fred.feedex.Constants;
import net.fred.feedex.MainApplication;
import net.fred.feedex.provider.FeedData.EntryColumns;
import net.fred.feedex.provider.FeedData.FeedColumns;
import net.fred.feedex.provider.FeedData.FilterColumns;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;

public class FeedDataContentProvider extends ContentProvider {
	private static final String FOLDER = Environment.getExternalStorageDirectory() + "/FeedEx/";
	private static final String DATABASE_NAME = "FeedEx.db";

	private static final int DATABASE_VERSION = 1;

	private static final int URI_GROUPS = 1;
	private static final int URI_GROUP = 2;
	private static final int URI_FEEDS_FOR_GROUPS = 3;
	private static final int URI_FEEDS = 4;
	private static final int URI_FEED = 5;
	private static final int URI_FILTERS = 6;
	private static final int URI_FILTERS_FOR_FEED = 7;
	private static final int URI_ENTRIES_FOR_FEED = 8;
	private static final int URI_ENTRY_FOR_FEED = 9;
	private static final int URI_ENTRIES = 10;
	private static final int URI_ENTRY = 11;
	private static final int URI_FAVORITES = 12;
	private static final int URI_FAVORITES_ENTRY = 13;

	private static final String TABLE_FEEDS = "feeds";
	private static final String TABLE_FILTERS = "filters";
	private static final String TABLE_ENTRIES = "entries";

	public static final String IMAGE_FOLDER = FOLDER + "images/";
	public static final File IMAGE_FOLDER_FILE = new File(IMAGE_FOLDER);

	private static final String BACKUP_OPML = FOLDER + "backup.opml";

	private static UriMatcher URI_MATCHER;

	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(FeedData.AUTHORITY, "groups", URI_GROUPS);
		URI_MATCHER.addURI(FeedData.AUTHORITY, "groups/#", URI_GROUP);
		URI_MATCHER.addURI(FeedData.AUTHORITY, "feeds_for_group/#", URI_FEEDS_FOR_GROUPS);
		URI_MATCHER.addURI(FeedData.AUTHORITY, "feeds", URI_FEEDS);
		URI_MATCHER.addURI(FeedData.AUTHORITY, "feeds/#", URI_FEED);
		URI_MATCHER.addURI(FeedData.AUTHORITY, "feeds/#/entries", URI_ENTRIES_FOR_FEED);
		URI_MATCHER.addURI(FeedData.AUTHORITY, "feeds/#/entries/#", URI_ENTRY_FOR_FEED);
		URI_MATCHER.addURI(FeedData.AUTHORITY, "filters", URI_FILTERS);
		URI_MATCHER.addURI(FeedData.AUTHORITY, "filters_for_feed/#", URI_FILTERS_FOR_FEED);
		URI_MATCHER.addURI(FeedData.AUTHORITY, "entries", URI_ENTRIES);
		URI_MATCHER.addURI(FeedData.AUTHORITY, "entries/#", URI_ENTRY);
		URI_MATCHER.addURI(FeedData.AUTHORITY, "favorites", URI_FAVORITES);
		URI_MATCHER.addURI(FeedData.AUTHORITY, "favorites/#", URI_FAVORITES_ENTRY);
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {

		private static final String DEFAULT_FEED_URL = "http://news.google.com/?output=rss";
		private static final String DEFAULT_FEED_NAME = "Google News";
		private static final String DEFAULT_GROUP_NAME = "News";

		private final Handler mHandler;

		public DatabaseHelper(Handler handler, Context context, String name, int version) {
			super(context, name, null, version);
			mHandler = handler;
		}

		@Override
		public void onCreate(SQLiteDatabase database) {
			database.execSQL(createTable(TABLE_FEEDS, FeedColumns.COLUMNS, FeedColumns.TYPES));
			database.execSQL(createTable(TABLE_FILTERS, FilterColumns.COLUMNS, FilterColumns.TYPES));
			database.execSQL(createTable(TABLE_ENTRIES, EntryColumns.COLUMNS, EntryColumns.TYPES));

			// Check if we need to import the backup
			File backupFile = new File(BACKUP_OPML);
			if (backupFile.exists()) {
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						// Perform an automated import of the backup
						try {
							OPML.importFromFile(BACKUP_OPML);
						} catch (Exception e) {
						}
					}
				});
			} else { // No database and no backup
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						// Automatically add an example feed
						try {
							ContentResolver cr = MainApplication.getAppContext().getContentResolver();

							ContentValues values = new ContentValues();
							values.put(FeedColumns.IS_GROUP, true);
							values.put(FeedColumns.NAME, DEFAULT_GROUP_NAME);
							cr.insert(FeedColumns.GROUPS_CONTENT_URI, values);

							Cursor groupCursor = cr.query(FeedColumns.GROUPS_CONTENT_URI, FeedColumns.PROJECTION_ID, null, null, null);
							if (groupCursor.moveToFirst()) {
								values = new ContentValues();
								values.put(FeedColumns.URL, DEFAULT_FEED_URL);
								values.put(FeedColumns.NAME, DEFAULT_FEED_NAME);
								values.put(FeedColumns.GROUP_ID, groupCursor.getString(0));

								cr.insert(FeedColumns.CONTENT_URI, values);
								cr.notifyChange(FeedColumns.GROUPS_CONTENT_URI, null);
							}
							groupCursor.close();
						} catch (Exception e) {
						}
					}
				});
			}
		}

		private String createTable(String tableName, String[] columns, String[] types) {
			if (tableName == null || columns == null || types == null || types.length != columns.length || types.length == 0) {
				throw new IllegalArgumentException("Invalid parameters for creating table " + tableName);
			} else {
				StringBuilder stringBuilder = new StringBuilder("CREATE TABLE ");

				stringBuilder.append(tableName);
				stringBuilder.append(" (");
				for (int n = 0, i = columns.length; n < i; n++) {
					if (n > 0) {
						stringBuilder.append(", ");
					}
					stringBuilder.append(columns[n]).append(' ').append(types[n]);
				}
				return stringBuilder.append(");").toString();
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
			// Nothing to do
		}
	}

	private DatabaseHelper mDatabaseHelper;

	@Override
	public String getType(Uri uri) {
		int option = URI_MATCHER.match(uri);

		switch (option) {
		case URI_GROUPS:
		case URI_FEEDS_FOR_GROUPS:
		case URI_FEEDS:
			return "vnd.android.cursor.dir/vnd.feedex.feed";
		case URI_GROUP:
		case URI_FEED:
			return "vnd.android.cursor.item/vnd.feedex.feed";
		case URI_FILTERS:
		case URI_FILTERS_FOR_FEED:
			return "vnd.android.cursor.dir/vnd.feedex.filter";
		case URI_FAVORITES:
		case URI_ENTRIES:
		case URI_ENTRIES_FOR_FEED:
			return "vnd.android.cursor.dir/vnd.feedex.entry";
		case URI_FAVORITES_ENTRY:
		case URI_ENTRY:
		case URI_ENTRY_FOR_FEED:
			return "vnd.android.cursor.item/vnd.feedex.entry";
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
	}

	@Override
	public boolean onCreate() {
		try {
			File folder = new File(FOLDER);
			folder.mkdir(); // maybe we use the boolean return value later
		} catch (Exception e) {
		}

		mDatabaseHelper = new DatabaseHelper(new Handler(), getContext(), DATABASE_NAME, DATABASE_VERSION);

		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

		int option = URI_MATCHER.match(uri);

		if ((option == URI_FEEDS || option == URI_GROUPS || option == URI_FEEDS_FOR_GROUPS) && sortOrder == null) {
			sortOrder = FeedColumns.PRIORITY;
		}

		switch (option) {
		case URI_GROUPS: {
			queryBuilder.setTables(TABLE_FEEDS);
			queryBuilder.appendWhere(new StringBuilder(FeedColumns.IS_GROUP).append(Constants.DB_IS_TRUE).append(Constants.DB_OR).append(FeedColumns.GROUP_ID).append(Constants.DB_IS_NULL));
			break;
		}
		case URI_FEEDS_FOR_GROUPS: {
			queryBuilder.setTables(TABLE_FEEDS);
			queryBuilder.appendWhere(new StringBuilder(FeedColumns.GROUP_ID).append('=').append(uri.getPathSegments().get(1)));
			break;
		}
		case URI_GROUP:
		case URI_FEED: {
			queryBuilder.setTables(TABLE_FEEDS);
			queryBuilder.appendWhere(new StringBuilder(FeedColumns._ID).append('=').append(uri.getPathSegments().get(1)));
			break;
		}
		case URI_FEEDS: {
			queryBuilder.setTables(TABLE_FEEDS);
			queryBuilder.appendWhere(new StringBuilder(FeedColumns.IS_GROUP).append(Constants.DB_IS_NULL));
			break;
		}
		case URI_FILTERS: {
			queryBuilder.setTables(TABLE_FILTERS);
			break;
		}
		case URI_FILTERS_FOR_FEED: {
			queryBuilder.setTables(TABLE_FILTERS);
			queryBuilder.appendWhere(new StringBuilder(FilterColumns.FEED_ID).append('=').append(uri.getPathSegments().get(1)));
			break;
		}
		case URI_ENTRY_FOR_FEED: {
			queryBuilder.setTables(TABLE_ENTRIES);
			queryBuilder.appendWhere(new StringBuilder(EntryColumns._ID).append('=').append(uri.getPathSegments().get(3)));
			break;
		}
		case URI_ENTRIES_FOR_FEED: {
			queryBuilder.setTables(TABLE_ENTRIES);
			queryBuilder.appendWhere(new StringBuilder(EntryColumns.FEED_ID).append('=').append(uri.getPathSegments().get(1)));
			break;
		}
		case URI_ENTRIES: {
			queryBuilder.setTables("entries join (select name, icon, _id as feed_id from feeds) as F on (entries.feedid = F.feed_id)");
			break;
		}
		case URI_FAVORITES_ENTRY:
		case URI_ENTRY: {
			queryBuilder.setTables(TABLE_ENTRIES);
			queryBuilder.appendWhere(new StringBuilder(EntryColumns._ID).append('=').append(uri.getPathSegments().get(1)));
			break;
		}
		case URI_FAVORITES: {
			queryBuilder.setTables("entries join (select name, icon, _id as feed_id from feeds) as F on (entries.feedid = F.feed_id)");
			queryBuilder.appendWhere(new StringBuilder(EntryColumns.IS_FAVORITE).append(Constants.DB_IS_TRUE));
			break;
		}
		}

		SQLiteDatabase database = mDatabaseHelper.getReadableDatabase();

		Cursor cursor = queryBuilder.query(database, projection, selection, selectionArgs, null, null, sortOrder);

		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}

	private final String[] MAXPRIORITY = new String[] { "MAX(" + FeedColumns.PRIORITY + ")" };

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		long newId = -1;

		int option = URI_MATCHER.match(uri);

		SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();

		switch (option) {
		case URI_GROUPS:
		case URI_FEEDS: {
			Cursor cursor;
			if (values.containsKey(FeedColumns.GROUP_ID)) {
				String groupId = values.getAsString(FeedColumns.GROUP_ID);
				cursor = query(FeedColumns.FEEDS_FOR_GROUPS_CONTENT_URI(groupId), MAXPRIORITY, null, null, null);
			} else {
				cursor = query(FeedColumns.GROUPS_CONTENT_URI, MAXPRIORITY, null, null, null);
			}

			if (cursor.moveToFirst()) { // normally this is always the case with MAX()
				values.put(FeedColumns.PRIORITY, cursor.getInt(0) + 1);
			} else {
				values.put(FeedColumns.PRIORITY, 1);
			}
			cursor.close();

			newId = database.insert(TABLE_FEEDS, null, values);
			try {
				OPML.exportToFile(BACKUP_OPML);
			} catch (Exception e) {
			}

			break;
		}
		case URI_FILTERS: {
			newId = database.insert(TABLE_FILTERS, null, values);
			break;
		}
		case URI_FILTERS_FOR_FEED: {
			values.put(FilterColumns.FEED_ID, uri.getPathSegments().get(1));
			newId = database.insert(TABLE_FILTERS, null, values);
			break;
		}
		case URI_ENTRIES_FOR_FEED: {
			values.put(EntryColumns.FEED_ID, uri.getPathSegments().get(1));
			newId = database.insert(TABLE_ENTRIES, null, values);
			break;
		}
		case URI_ENTRIES: {
			newId = database.insert(TABLE_ENTRIES, null, values);
			break;
		}
		default:
			throw new IllegalArgumentException("Illegal insert");
		}
		if (newId > -1) {
			getContext().getContentResolver().notifyChange(uri, null);
			return ContentUris.withAppendedId(uri, newId);
		} else {
			throw new SQLException("Could not insert row into " + uri);
		}
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		int option = URI_MATCHER.match(uri);

		String table = null;

		StringBuilder where = new StringBuilder();

		SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();

		switch (option) {
		case URI_FEED: {
			table = TABLE_FEEDS;

			long feedId = Long.parseLong(uri.getPathSegments().get(1));
			where.append(FeedColumns._ID).append('=').append(feedId);

			if (values != null && values.containsKey(FeedColumns.PRIORITY)) {
				Cursor priorityCursor = database.query(TABLE_FEEDS, new String[] { FeedColumns.PRIORITY, FeedColumns.GROUP_ID }, FeedColumns._ID + "=" + feedId, null, null, null, null);
				if (priorityCursor.moveToNext()) {
					int oldPriority = priorityCursor.getInt(0);
					String oldGroupId = priorityCursor.getString(1);
					int newPriority = values.getAsInteger(FeedColumns.PRIORITY);
					String newGroupId = values.getAsString(FeedColumns.GROUP_ID);

					priorityCursor.close();

					String oldGroupWhere = '(' + (oldGroupId != null ? FeedColumns.GROUP_ID + '=' + oldGroupId : FeedColumns.IS_GROUP + Constants.DB_IS_TRUE + Constants.DB_OR + FeedColumns.GROUP_ID
							+ Constants.DB_IS_NULL) + ')';

					// If the group has changed, it is not only a +1 or -1 for priority...
					if ((oldGroupId == null && newGroupId != null) || (oldGroupId != null && newGroupId == null) || (oldGroupId != null && newGroupId != null && !oldGroupId.equals(newGroupId))) {

						String priorityValue = FeedColumns.PRIORITY + "-1";
						String priorityWhere = FeedColumns.PRIORITY + '>' + oldPriority;
						database.execSQL("UPDATE " + TABLE_FEEDS + " SET " + FeedColumns.PRIORITY + '=' + priorityValue + " WHERE " + oldGroupWhere + Constants.DB_AND + priorityWhere);

						priorityValue = FeedColumns.PRIORITY + "+1";
						priorityWhere = FeedColumns.PRIORITY + '>' + (newPriority - 1);
						String newGroupWhere = '(' + (newGroupId != null ? FeedColumns.GROUP_ID + '=' + newGroupId : FeedColumns.IS_GROUP + Constants.DB_IS_TRUE + Constants.DB_OR
								+ FeedColumns.GROUP_ID + Constants.DB_IS_NULL) + ')';
						database.execSQL("UPDATE " + TABLE_FEEDS + " SET " + FeedColumns.PRIORITY + '=' + priorityValue + " WHERE " + newGroupWhere + Constants.DB_AND + priorityWhere);

					} else { // We move the item into the same group
						if (newPriority > oldPriority) {
							String priorityValue = FeedColumns.PRIORITY + "-1";
							String priorityWhere = '(' + FeedColumns.PRIORITY + " BETWEEN " + (oldPriority + 1) + " AND " + newPriority + ')';
							database.execSQL("UPDATE " + TABLE_FEEDS + " SET " + FeedColumns.PRIORITY + '=' + priorityValue + " WHERE " + oldGroupWhere + Constants.DB_AND + priorityWhere);

						} else if (newPriority < oldPriority) {
							String priorityValue = FeedColumns.PRIORITY + "+1";
							String priorityWhere = '(' + FeedColumns.PRIORITY + " BETWEEN " + newPriority + " AND " + (oldPriority - 1) + ')';
							database.execSQL("UPDATE " + TABLE_FEEDS + " SET " + FeedColumns.PRIORITY + '=' + priorityValue + " WHERE " + oldGroupWhere + Constants.DB_AND + priorityWhere);
						}
					}
				} else {
					priorityCursor.close();
				}
			}
			break;
		}
		case URI_GROUPS:
		case URI_FEEDS_FOR_GROUPS:
		case URI_FEEDS: {
			table = TABLE_FEEDS;
			break;
		}
		case URI_FILTERS: {
			table = TABLE_FILTERS;
			break;
		}
		case URI_FILTERS_FOR_FEED: {
			table = TABLE_FILTERS;
			where.append(FilterColumns.FEED_ID).append('=').append(uri.getPathSegments().get(1));
			break;
		}
		case URI_ENTRY_FOR_FEED: {
			table = TABLE_ENTRIES;
			where.append(EntryColumns._ID).append('=').append(uri.getPathSegments().get(3));
			break;
		}
		case URI_ENTRIES_FOR_FEED: {
			table = TABLE_ENTRIES;
			where.append(EntryColumns.FEED_ID).append('=').append(uri.getPathSegments().get(1));
			break;
		}
		case URI_ENTRIES: {
			table = TABLE_ENTRIES;
			break;
		}
		case URI_FAVORITES_ENTRY:
		case URI_ENTRY: {
			table = TABLE_ENTRIES;
			where.append(EntryColumns._ID).append('=').append(uri.getPathSegments().get(1));
			break;
		}
		case URI_FAVORITES: {
			table = TABLE_ENTRIES;
			where.append(EntryColumns.IS_FAVORITE).append(Constants.DB_IS_TRUE);
			break;
		}
		}

		if (!TextUtils.isEmpty(selection)) {
			if (where.length() > 0) {
				where.append(Constants.DB_AND).append(selection);
			} else {
				where.append(selection);
			}
		}

		int count = database.update(table, values, where.toString(), selectionArgs);

		// == is ok here
		if (table == TABLE_FEEDS && (values.containsKey(FeedColumns.NAME) || values.containsKey(FeedColumns.URL) || values.containsKey(FeedColumns.PRIORITY))) {
			try {
				OPML.exportToFile(BACKUP_OPML);
			} catch (Exception e) {
			}
		}
		if (count > 0) {
			getContext().getContentResolver().notifyChange(uri, null);
		}
		return count;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int option = URI_MATCHER.match(uri);

		String table = null;

		StringBuilder where = new StringBuilder();

		SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();

		switch (option) {
		case URI_GROUP: {
			table = TABLE_FEEDS;

			String groupId = uri.getPathSegments().get(1);

			where.append(FeedColumns._ID).append('=').append(groupId);

			// Delete the sub feeds & their entries
			Cursor subFeedsCursor = database.query(TABLE_FEEDS, FeedColumns.PROJECTION_ID, FeedColumns.GROUP_ID + "=" + groupId, null, null, null, null);
			while (subFeedsCursor.moveToNext()) {
				String feedId = subFeedsCursor.getString(0);
				delete(FeedColumns.CONTENT_URI(feedId), null, null);
			}
			subFeedsCursor.close();

			// Update the priorities
			Cursor priorityCursor = database.query(TABLE_FEEDS, FeedColumns.PROJECTION_PRIORITY, FeedColumns._ID + "=" + groupId, null, null, null, null);

			if (priorityCursor.moveToNext()) {
				int priority = priorityCursor.getInt(0);
				String priorityWhere = FeedColumns.PRIORITY + " > " + priority;
				String groupWhere = '(' + FeedColumns.IS_GROUP + Constants.DB_IS_TRUE + Constants.DB_OR + FeedColumns.GROUP_ID + Constants.DB_IS_NULL + ')';
				database.execSQL("UPDATE " + TABLE_FEEDS + " SET " + FeedColumns.PRIORITY + " = " + FeedColumns.PRIORITY + "-1 WHERE " + groupWhere + Constants.DB_AND + priorityWhere);
			}
			priorityCursor.close();
			break;
		}
		case URI_FEED: {
			table = TABLE_FEEDS;

			final String feedId = uri.getPathSegments().get(1);

			new Thread() {
				@Override
				public void run() {
					delete(EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(feedId), null, null);
					delete(FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(feedId), null, null);
				}
			}.start();

			where.append(FeedColumns._ID).append('=').append(feedId);

			// Update the priorities
			Cursor priorityCursor = database.query(TABLE_FEEDS, new String[] { FeedColumns.PRIORITY, FeedColumns.GROUP_ID }, FeedColumns._ID + "=" + feedId, null, null, null, null);

			if (priorityCursor.moveToNext()) {
				int priority = priorityCursor.getInt(0);
				String groupId = priorityCursor.getString(1);

				String groupWhere = '(' + (groupId != null ? FeedColumns.GROUP_ID + '=' + groupId : FeedColumns.IS_GROUP + Constants.DB_IS_TRUE + Constants.DB_OR + FeedColumns.GROUP_ID
						+ Constants.DB_IS_NULL) + ')';
				String priorityWhere = FeedColumns.PRIORITY + " > " + priority;

				database.execSQL("UPDATE " + TABLE_FEEDS + " SET " + FeedColumns.PRIORITY + " = " + FeedColumns.PRIORITY + "-1 WHERE " + groupWhere + Constants.DB_AND + priorityWhere);
			}
			priorityCursor.close();
			break;
		}
		case URI_GROUPS:
		case URI_FEEDS: {
			table = TABLE_FEEDS;
			break;
		}
		case URI_FEEDS_FOR_GROUPS: {
			table = TABLE_FEEDS;
			where.append(FeedColumns.GROUP_ID).append('=').append(uri.getPathSegments().get(1));
			break;
		}
		case URI_FILTERS: {
			table = TABLE_FILTERS;
			break;
		}
		case URI_FILTERS_FOR_FEED: {
			table = TABLE_FILTERS;
			where.append(FilterColumns.FEED_ID).append('=').append(uri.getPathSegments().get(1));
			break;
		}
		case URI_ENTRY_FOR_FEED: {
			table = TABLE_ENTRIES;
			where.append(EntryColumns._ID).append('=').append(uri.getPathSegments().get(3));
			break;
		}
		case URI_ENTRIES_FOR_FEED: {
			table = TABLE_ENTRIES;
			where.append(EntryColumns.FEED_ID).append('=').append(uri.getPathSegments().get(1));
			break;
		}
		case URI_ENTRIES: {
			table = TABLE_ENTRIES;
			break;
		}
		case URI_FAVORITES_ENTRY:
		case URI_ENTRY: {
			table = TABLE_ENTRIES;
			where.append(EntryColumns._ID).append('=').append(uri.getPathSegments().get(1));
			break;
		}
		case URI_FAVORITES: {
			table = TABLE_ENTRIES;
			where.append(EntryColumns.IS_FAVORITE).append(Constants.DB_IS_TRUE);
			break;
		}
		}

		if (!TextUtils.isEmpty(selection)) {
			if (where.length() > 0) {
				where.append(Constants.DB_AND);
			}
			where.append(selection);
		}

		int count = database.delete(table, where.toString(), selectionArgs);

		if (table == TABLE_FEEDS) { // == is ok here
			try {
				OPML.exportToFile(BACKUP_OPML);
			} catch (Exception e) {
			}
		}
		if (count > 0) {
			getContext().getContentResolver().notifyChange(uri, null);
		}
		return count;
	}

	public static String getFeedIdFromEntryId(long entryId) {
		ContentResolver cr = MainApplication.getAppContext().getContentResolver();
		Cursor c = cr.query(EntryColumns.CONTENT_URI(entryId), new String[] { EntryColumns.FEED_ID }, null, null, null);
		if (c.moveToFirst()) {
			return c.getString(0);
		}
		c.close();

		return null;
	}

	public static void notifyGroupFromFeedId(long feedId) {
		notifyGroupFromFeedId(Long.toString(feedId));
	}

	public static void notifyGroupFromFeedId(String feedId) {
		ContentResolver cr = MainApplication.getAppContext().getContentResolver();
		Cursor c = cr.query(FeedColumns.CONTENT_URI(feedId), FeedColumns.PROJECTION_GROUP_ID, null, null, null);
		if (c.moveToFirst()) {
			String groupId = c.getString(0);
			if (groupId == null) {
				cr.notifyChange(FeedColumns.GROUPS_CONTENT_URI, null);
			} else {
				cr.notifyChange(FeedColumns.FEEDS_FOR_GROUPS_CONTENT_URI(groupId), null);
			}
		}
		c.close();
	}
}
