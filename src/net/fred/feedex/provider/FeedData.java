/**
 * FeedEx
 * 
 * Copyright (c) 2012-2013 Frederic Julian
 * Copyright (c) 2010-2012 Stefan Handschuh
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.fred.feedex.provider;

import java.io.File;

import net.fred.feedex.Constants;
import net.fred.feedex.handler.PictureFilenameFilter;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

public class FeedData {
	public static final String CONTENT = "content://";
	public static final String AUTHORITY = "net.fred.feedex.provider.FeedData";
	public static final String CONTENT_AUTHORITY = CONTENT + AUTHORITY;

	private static final String TYPE_PRIMARY_KEY = "INTEGER PRIMARY KEY AUTOINCREMENT";

	private static final String TYPE_EXTERNAL_ID = "INTEGER(7)";
	private static final String TYPE_TEXT = "TEXT";
	private static final String TYPE_TEXT_UNIQUE = "TEXT UNIQUE";
	private static final String TYPE_DATE_TIME = "DATETIME";
	private static final String TYPE_INT = "INT";
	private static final String TYPE_BOOLEAN = "INTEGER(1)";

	public static class FeedColumns implements BaseColumns {
		public static final Uri CONTENT_URI = Uri.parse(new StringBuilder(CONTENT_AUTHORITY).append("/feeds").toString());

		public static final String URL = "url";
		public static final String NAME = "name";
		public static final String IS_GROUP = "isgroup";
		public static final String IS_GROUP_COLLAPSED = "isgroupcollapsed";
		public static final String GROUP_ID = "groupid";
		public static final String LAST_UPDATE = "lastupdate";
		public static final String ICON = "icon";
		public static final String ERROR = "error";
		public static final String PRIORITY = "priority";
		public static final String FETCH_MODE = "fetchmode";

		public static final String[] COLUMNS = new String[] { _ID, URL, NAME, IS_GROUP, IS_GROUP_COLLAPSED, GROUP_ID, LAST_UPDATE, ICON, ERROR, PRIORITY, FETCH_MODE };
		public static final String[] TYPES = new String[] { TYPE_PRIMARY_KEY, TYPE_TEXT_UNIQUE, TYPE_TEXT, TYPE_BOOLEAN, TYPE_BOOLEAN, TYPE_EXTERNAL_ID, TYPE_DATE_TIME, "BLOB", TYPE_TEXT, TYPE_INT,
				TYPE_INT };

		public static final Uri CONTENT_URI(String feedId) {
			return Uri.parse(new StringBuilder(CONTENT_AUTHORITY).append("/feeds/").append(feedId).toString());
		}

		public static final Uri CONTENT_URI(long feedId) {
			return Uri.parse(new StringBuilder(CONTENT_AUTHORITY).append("/feeds/").append(feedId).toString());
		}

		public static final Uri GROUPS_CONTENT_URI = Uri.parse(new StringBuilder(CONTENT_AUTHORITY).append("/groups").toString());

		public static final Uri GROUPS_CONTENT_URI(String groupId) {
			return Uri.parse(new StringBuilder(CONTENT_AUTHORITY).append("/groups/").append(groupId).toString());
		}

		public static final Uri GROUPS_CONTENT_URI(long groupId) {
			return Uri.parse(new StringBuilder(CONTENT_AUTHORITY).append("/groups/").append(groupId).toString());
		}

		public static final Uri FEEDS_FOR_GROUPS_CONTENT_URI(String groupId) {
			return Uri.parse(new StringBuilder(CONTENT_AUTHORITY).append("/feeds_for_group/").append(groupId).toString());
		}

		public static final Uri FEEDS_FOR_GROUPS_CONTENT_URI(long groupId) {
			return Uri.parse(new StringBuilder(CONTENT_AUTHORITY).append("/feeds_for_group/").append(groupId).toString());
		}

		public static final String[] PROJECTION_ID = new String[] { FeedColumns._ID };
		public static final String[] PROJECTION_GROUP_ID = new String[] { FeedColumns.GROUP_ID };
		public static final String[] PROJECTION_PRIORITY = new String[] { FeedColumns.PRIORITY };
	}

	public static class FilterColumns implements BaseColumns {
		public static final Uri CONTENT_URI = Uri.parse(new StringBuilder(CONTENT_AUTHORITY).append("/filters").toString());

		public static final String FEED_ID = "feedid";
		public static final String FILTER_TEXT = "filtertext";
		public static final String IS_REGEX = "isregex";
		public static final String IS_APPLIED_TO_TITLE = "isappliedtotitle";

		public static final String[] COLUMNS = new String[] { _ID, FEED_ID, FILTER_TEXT, IS_REGEX, IS_APPLIED_TO_TITLE };
		public static final String[] TYPES = new String[] { TYPE_PRIMARY_KEY, TYPE_EXTERNAL_ID, TYPE_TEXT, TYPE_BOOLEAN, TYPE_BOOLEAN };

		public static final Uri FILTERS_FOR_FEED_CONTENT_URI(String feedId) {
			return Uri.parse(new StringBuilder(CONTENT_AUTHORITY).append("/filters_for_feed/").append(feedId).toString());
		}

		public static final Uri FILTERS_FOR_FEED_CONTENT_URI(long feedId) {
			return Uri.parse(new StringBuilder(CONTENT_AUTHORITY).append("/filters_for_feed/").append(feedId).toString());
		}
	}

	public static class EntryColumns implements BaseColumns {
		public static final String FEED_ID = "feedid";
		public static final String TITLE = "title";
		public static final String ABSTRACT = "abstract";
		public static final String MOBILIZED_HTML = "mobilized";
		public static final String DATE = "date";
		public static final String IS_READ = "isread";
		public static final String LINK = "link";
		public static final String IS_FAVORITE = "favorite";
		public static final String ENCLOSURE = "enclosure";
		public static final String GUID = "guid";
		public static final String AUTHOR = "author";

		public static final String[] COLUMNS = new String[] { _ID, FEED_ID, TITLE, ABSTRACT, MOBILIZED_HTML, DATE, IS_READ, LINK, IS_FAVORITE, ENCLOSURE, GUID, AUTHOR };
		public static final String[] TYPES = new String[] { TYPE_PRIMARY_KEY, TYPE_EXTERNAL_ID, TYPE_TEXT, TYPE_TEXT, TYPE_TEXT, TYPE_DATE_TIME, TYPE_BOOLEAN, TYPE_TEXT, TYPE_BOOLEAN, TYPE_TEXT,
				TYPE_TEXT, TYPE_TEXT };

		public static final Uri CONTENT_URI = Uri.parse(new StringBuilder(CONTENT_AUTHORITY).append("/entries").toString());
		public static final Uri FAVORITES_CONTENT_URI = Uri.parse(new StringBuilder(CONTENT_AUTHORITY).append("/favorites").toString());

		public static Uri ENTRIES_FOR_FEED_CONTENT_URI(String feedId) {
			return Uri.parse(new StringBuilder(CONTENT_AUTHORITY).append("/feeds/").append(feedId).append("/entries").toString());
		}

		public static Uri ENTRIES_FOR_FEED_CONTENT_URI(long feedId) {
			return Uri.parse(new StringBuilder(CONTENT_AUTHORITY).append("/feeds/").append(feedId).append("/entries").toString());
		}

		public static Uri CONTENT_URI(String entryId) {
			return Uri.parse(new StringBuilder(CONTENT_AUTHORITY).append("/entries/").append(entryId).toString());
		}

		public static Uri CONTENT_URI(long entryId) {
			return Uri.parse(new StringBuilder(CONTENT_AUTHORITY).append("/entries/").append(entryId).toString());
		}

		public static Uri PARENT_URI(String path) {
			return Uri.parse(new StringBuilder(CONTENT_AUTHORITY).append(path.substring(0, path.lastIndexOf('/'))).toString());
		}

		public static final String[] PROJECTION_ID = new String[] { EntryColumns._ID };

		public static final String WHERE_UNREAD = new StringBuilder("(").append(EntryColumns.IS_READ).append(Constants.DB_IS_NULL).append(Constants.DB_OR).append(EntryColumns.IS_READ)
				.append(Constants.DB_IS_FALSE).append(')').toString();

		public static final String WHERE_NOT_FAVORITE = new StringBuilder("(").append(EntryColumns.IS_FAVORITE).append(Constants.DB_IS_NULL).append(Constants.DB_OR).append(EntryColumns.IS_FAVORITE)
				.append(Constants.DB_IS_FALSE).append(')').toString();
	}

	public static synchronized void deletePicturesOfFeed(Context context, Uri entriesUri, String selection) {
		if (FeedDataContentProvider.IMAGE_FOLDER_FILE.exists()) {
			PictureFilenameFilter filenameFilter = new PictureFilenameFilter();

			Cursor cursor = context.getContentResolver().query(entriesUri, EntryColumns.PROJECTION_ID, selection, null, null);

			while (cursor.moveToNext()) {
				filenameFilter.setEntryId(cursor.getString(0));

				File[] files = FeedDataContentProvider.IMAGE_FOLDER_FILE.listFiles(filenameFilter);
				for (int n = 0, i = files != null ? files.length : 0; n < i; n++) {
					files[n].delete();
				}
			}
			cursor.close();
		}
	}

	public static final ContentValues getReadContentValues() {
		ContentValues values = new ContentValues();
		values.put(EntryColumns.IS_READ, true);
		return values;
	}

	public static final ContentValues getUnreadContentValues() {
		ContentValues values = new ContentValues();
		values.putNull(EntryColumns.IS_READ);
		return values;
	}
}
