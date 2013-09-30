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

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;

import net.fred.feedex.Constants;
import net.fred.feedex.MainApplication;
import net.fred.feedex.provider.FeedData.EntryColumns;
import net.fred.feedex.provider.FeedData.FeedColumns;
import net.fred.feedex.provider.FeedData.FilterColumns;
import net.fred.feedex.provider.FeedData.TaskColumns;
import net.fred.feedex.utils.NetworkUtils;

public class FeedDataContentProvider extends ContentProvider {

    public static final int URI_GROUPED_FEEDS = 1;
    public static final int URI_GROUPS = 2;
    public static final int URI_GROUP = 3;
    public static final int URI_FEEDS_FOR_GROUPS = 4;
    public static final int URI_FEEDS = 5;
    public static final int URI_FEED = 6;
    public static final int URI_FILTERS = 7;
    public static final int URI_FILTERS_FOR_FEED = 8;
    public static final int URI_ENTRIES_FOR_FEED = 9;
    public static final int URI_ENTRY_FOR_FEED = 10;
    public static final int URI_ENTRIES_FOR_GROUP = 11;
    public static final int URI_ENTRY_FOR_GROUP = 12;
    public static final int URI_ENTRIES = 13;
    public static final int URI_ENTRY = 14;
    public static final int URI_FAVORITES = 15;
    public static final int URI_FAVORITES_ENTRY = 16;
    public static final int URI_TASKS = 17;
    public static final int URI_TASK = 18;

    public static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        URI_MATCHER.addURI(FeedData.AUTHORITY, "grouped_feeds", URI_GROUPED_FEEDS);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "groups", URI_GROUPS);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "groups/#", URI_GROUP);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "groups/#/feeds", URI_FEEDS_FOR_GROUPS);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "feeds", URI_FEEDS);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "feeds/#", URI_FEED);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "feeds/#/entries", URI_ENTRIES_FOR_FEED);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "feeds/#/entries/#", URI_ENTRY_FOR_FEED);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "groups/#/entries", URI_ENTRIES_FOR_GROUP);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "groups/#/entries/#", URI_ENTRY_FOR_GROUP);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "filters", URI_FILTERS);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "feeds/#/filters", URI_FILTERS_FOR_FEED);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "entries", URI_ENTRIES);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "entries/#", URI_ENTRY);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "favorites", URI_FAVORITES);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "favorites/#", URI_FAVORITES_ENTRY);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "tasks", URI_TASKS);
        URI_MATCHER.addURI(FeedData.AUTHORITY, "tasks/#", URI_TASK);
    }

    private static final String FEEDS_TABLE_WITH_GROUP_PRIORITY = FeedColumns.TABLE_NAME + " LEFT JOIN (SELECT " + FeedColumns._ID + " AS joined_feed_id, " + FeedColumns.PRIORITY +
            " AS group_priority FROM " + FeedColumns.TABLE_NAME + ") AS f ON (" + FeedColumns.TABLE_NAME + '.' + FeedColumns.GROUP_ID + " = f.joined_feed_id)";
    private static final String ENTRIES_TABLE_WITH_FEED_INFO = EntryColumns.TABLE_NAME + " JOIN (SELECT " + FeedColumns._ID + " AS joined_feed_id, " + FeedColumns.NAME + ", " +
            FeedColumns.ICON + ", " + FeedColumns.GROUP_ID + " FROM " + FeedColumns.TABLE_NAME + ") AS f ON (" + EntryColumns.TABLE_NAME + '.' + EntryColumns.FEED_ID + " = f.joined_feed_id)";

    private DatabaseHelper mDatabaseHelper;

    @Override
    public String getType(Uri uri) {
        int option = URI_MATCHER.match(uri);

        switch (option) {
            case URI_GROUPED_FEEDS:
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
            case URI_ENTRIES_FOR_GROUP:
                return "vnd.android.cursor.dir/vnd.feedex.entry";
            case URI_FAVORITES_ENTRY:
            case URI_ENTRY:
            case URI_ENTRY_FOR_FEED:
            case URI_ENTRY_FOR_GROUP:
                return "vnd.android.cursor.item/vnd.feedex.entry";
            case URI_TASKS:
                return "vnd.android.cursor.dir/vnd.feedex.task";
            case URI_TASK:
                return "vnd.android.cursor.item/vnd.feedex.task";
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public boolean onCreate() {
        mDatabaseHelper = new DatabaseHelper(new Handler(), getContext());

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
            case URI_GROUPED_FEEDS: {
                queryBuilder.setTables(FEEDS_TABLE_WITH_GROUP_PRIORITY);
                sortOrder = "IFNULL(group_priority, " + FeedColumns.PRIORITY + "), IFNULL(" + FeedColumns.GROUP_ID + ", " + FeedColumns._ID + "), " + FeedColumns.IS_GROUP + " DESC, " + FeedColumns.PRIORITY;
                break;
            }
            case URI_GROUPS: {
                queryBuilder.setTables(FeedColumns.TABLE_NAME);
                queryBuilder.appendWhere(new StringBuilder(FeedColumns.IS_GROUP).append(Constants.DB_IS_TRUE).append(Constants.DB_OR)
                        .append(FeedColumns.GROUP_ID).append(Constants.DB_IS_NULL));
                break;
            }
            case URI_FEEDS_FOR_GROUPS: {
                queryBuilder.setTables(FeedColumns.TABLE_NAME);
                queryBuilder.appendWhere(new StringBuilder(FeedColumns.GROUP_ID).append('=').append(uri.getPathSegments().get(1)));
                break;
            }
            case URI_GROUP:
            case URI_FEED: {
                queryBuilder.setTables(FeedColumns.TABLE_NAME);
                queryBuilder.appendWhere(new StringBuilder(FeedColumns._ID).append('=').append(uri.getPathSegments().get(1)));
                break;
            }
            case URI_FEEDS: {
                queryBuilder.setTables(FeedColumns.TABLE_NAME);
                queryBuilder.appendWhere(new StringBuilder(FeedColumns.IS_GROUP).append(Constants.DB_IS_NULL));
                break;
            }
            case URI_FILTERS: {
                queryBuilder.setTables(FilterColumns.TABLE_NAME);
                break;
            }
            case URI_FILTERS_FOR_FEED: {
                queryBuilder.setTables(FilterColumns.TABLE_NAME);
                queryBuilder.appendWhere(new StringBuilder(FilterColumns.FEED_ID).append('=').append(uri.getPathSegments().get(1)));
                break;
            }
            case URI_ENTRY_FOR_FEED:
            case URI_ENTRY_FOR_GROUP: {
                queryBuilder.setTables(EntryColumns.TABLE_NAME);
                queryBuilder.appendWhere(new StringBuilder(EntryColumns._ID).append('=').append(uri.getPathSegments().get(3)));
                break;
            }
            case URI_ENTRIES_FOR_FEED: {
                queryBuilder.setTables(EntryColumns.TABLE_NAME);
                queryBuilder.appendWhere(new StringBuilder(EntryColumns.FEED_ID).append('=').append(uri.getPathSegments().get(1)));
                break;
            }
            case URI_ENTRIES_FOR_GROUP: {
                queryBuilder.setTables(ENTRIES_TABLE_WITH_FEED_INFO);
                queryBuilder.appendWhere(new StringBuilder(FeedColumns.GROUP_ID).append('=').append(uri.getPathSegments().get(1)));
                break;
            }
            case URI_ENTRIES: {
                queryBuilder.setTables(ENTRIES_TABLE_WITH_FEED_INFO);
                break;
            }
            case URI_FAVORITES_ENTRY:
            case URI_ENTRY: {
                queryBuilder.setTables(EntryColumns.TABLE_NAME);
                queryBuilder.appendWhere(new StringBuilder(EntryColumns._ID).append('=').append(uri.getPathSegments().get(1)));
                break;
            }
            case URI_FAVORITES: {
                queryBuilder.setTables(ENTRIES_TABLE_WITH_FEED_INFO);
                queryBuilder.appendWhere(new StringBuilder(EntryColumns.IS_FAVORITE).append(Constants.DB_IS_TRUE));
                break;
            }
            case URI_TASKS: {
                queryBuilder.setTables(TaskColumns.TABLE_NAME);
                break;
            }
            case URI_TASK: {
                queryBuilder.setTables(TaskColumns.TABLE_NAME);
                queryBuilder.appendWhere(new StringBuilder(EntryColumns._ID).append('=').append(uri.getPathSegments().get(1)));
                break;
            }
        }

        SQLiteDatabase database = mDatabaseHelper.getReadableDatabase();

        Cursor cursor = queryBuilder.query(database, projection, selection, selectionArgs, null, null, sortOrder);

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    private final String[] MAXPRIORITY = new String[]{"MAX(" + FeedColumns.PRIORITY + ")"};

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long newId;

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

                newId = database.insert(FeedColumns.TABLE_NAME, null, values);
                mDatabaseHelper.exportToOPML();

                break;
            }
            case URI_FILTERS: {
                newId = database.insert(FilterColumns.TABLE_NAME, null, values);
                break;
            }
            case URI_FILTERS_FOR_FEED: {
                values.put(FilterColumns.FEED_ID, uri.getPathSegments().get(1));
                newId = database.insert(FilterColumns.TABLE_NAME, null, values);
                break;
            }
            case URI_ENTRIES_FOR_FEED: {
                values.put(EntryColumns.FEED_ID, uri.getPathSegments().get(1));
                newId = database.insert(EntryColumns.TABLE_NAME, null, values);
                break;
            }
            case URI_ENTRIES: {
                newId = database.insert(EntryColumns.TABLE_NAME, null, values);
                break;
            }
            case URI_TASKS: {
                newId = database.insert(TaskColumns.TABLE_NAME, null, values);
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
                table = FeedColumns.TABLE_NAME;

                long feedId = Long.parseLong(uri.getPathSegments().get(1));
                where.append(FeedColumns._ID).append('=').append(feedId);

                if (values != null && values.containsKey(FeedColumns.PRIORITY)) {
                    Cursor priorityCursor = database.query(FeedColumns.TABLE_NAME, new String[]{FeedColumns.PRIORITY, FeedColumns.GROUP_ID},
                            FeedColumns._ID + "=" + feedId, null, null, null, null);
                    if (priorityCursor.moveToNext()) {
                        int oldPriority = priorityCursor.getInt(0);
                        String oldGroupId = priorityCursor.getString(1);
                        int newPriority = values.getAsInteger(FeedColumns.PRIORITY);
                        String newGroupId = values.getAsString(FeedColumns.GROUP_ID);

                        priorityCursor.close();

                        String oldGroupWhere = '(' + (oldGroupId != null ? FeedColumns.GROUP_ID + '=' + oldGroupId : FeedColumns.IS_GROUP
                                + Constants.DB_IS_TRUE + Constants.DB_OR + FeedColumns.GROUP_ID + Constants.DB_IS_NULL) + ')';

                        // If the group has changed, it is not only a +1 or -1 for priority...
                        if ((oldGroupId == null && newGroupId != null) || (oldGroupId != null && newGroupId == null)
                                || (oldGroupId != null && newGroupId != null && !oldGroupId.equals(newGroupId))) {

                            String priorityValue = FeedColumns.PRIORITY + "-1";
                            String priorityWhere = FeedColumns.PRIORITY + '>' + oldPriority;
                            database.execSQL("UPDATE " + FeedColumns.TABLE_NAME + " SET " + FeedColumns.PRIORITY + '=' + priorityValue + " WHERE "
                                    + oldGroupWhere + Constants.DB_AND + priorityWhere);

                            priorityValue = FeedColumns.PRIORITY + "+1";
                            priorityWhere = FeedColumns.PRIORITY + '>' + (newPriority - 1);
                            String newGroupWhere = '(' + (newGroupId != null ? FeedColumns.GROUP_ID + '=' + newGroupId : FeedColumns.IS_GROUP
                                    + Constants.DB_IS_TRUE + Constants.DB_OR + FeedColumns.GROUP_ID + Constants.DB_IS_NULL) + ')';
                            database.execSQL("UPDATE " + FeedColumns.TABLE_NAME + " SET " + FeedColumns.PRIORITY + '=' + priorityValue + " WHERE "
                                    + newGroupWhere + Constants.DB_AND + priorityWhere);

                        } else { // We move the item into the same group
                            if (newPriority > oldPriority) {
                                String priorityValue = FeedColumns.PRIORITY + "-1";
                                String priorityWhere = '(' + FeedColumns.PRIORITY + " BETWEEN " + (oldPriority + 1) + " AND " + newPriority + ')';
                                database.execSQL("UPDATE " + FeedColumns.TABLE_NAME + " SET " + FeedColumns.PRIORITY + '=' + priorityValue + " WHERE "
                                        + oldGroupWhere + Constants.DB_AND + priorityWhere);

                            } else if (newPriority < oldPriority) {
                                String priorityValue = FeedColumns.PRIORITY + "+1";
                                String priorityWhere = '(' + FeedColumns.PRIORITY + " BETWEEN " + newPriority + " AND " + (oldPriority - 1) + ')';
                                database.execSQL("UPDATE " + FeedColumns.TABLE_NAME + " SET " + FeedColumns.PRIORITY + '=' + priorityValue + " WHERE "
                                        + oldGroupWhere + Constants.DB_AND + priorityWhere);
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
                table = FeedColumns.TABLE_NAME;
                break;
            }
            case URI_FILTERS: {
                table = FilterColumns.TABLE_NAME;
                break;
            }
            case URI_FILTERS_FOR_FEED: {
                table = FilterColumns.TABLE_NAME;
                where.append(FilterColumns.FEED_ID).append('=').append(uri.getPathSegments().get(1));
                break;
            }
            case URI_ENTRY_FOR_FEED:
            case URI_ENTRY_FOR_GROUP: {
                table = EntryColumns.TABLE_NAME;
                where.append(EntryColumns._ID).append('=').append(uri.getPathSegments().get(3));
                break;
            }
            case URI_ENTRIES_FOR_FEED: {
                table = EntryColumns.TABLE_NAME;
                where.append(EntryColumns.FEED_ID).append('=').append(uri.getPathSegments().get(1));
                break;
            }
            case URI_ENTRIES_FOR_GROUP: {
                table = EntryColumns.TABLE_NAME;
                where.append(EntryColumns.FEED_ID).append(" IN (SELECT ").append(FeedColumns._ID).append(" FROM ").append(FeedColumns.TABLE_NAME).append(" WHERE ").append(FeedColumns.GROUP_ID).append('=').append(uri.getPathSegments().get(1)).append(')');
                break;
            }
            case URI_ENTRIES: {
                table = EntryColumns.TABLE_NAME;
                break;
            }
            case URI_FAVORITES_ENTRY:
            case URI_ENTRY: {
                table = EntryColumns.TABLE_NAME;
                where.append(EntryColumns._ID).append('=').append(uri.getPathSegments().get(1));
                break;
            }
            case URI_FAVORITES: {
                table = EntryColumns.TABLE_NAME;
                where.append(EntryColumns.IS_FAVORITE).append(Constants.DB_IS_TRUE);
                break;
            }
            case URI_TASKS: {
                table = TaskColumns.TABLE_NAME;
                break;
            }
            case URI_TASK: {
                table = TaskColumns.TABLE_NAME;
                where.append(TaskColumns._ID).append('=').append(uri.getPathSegments().get(1));
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

        if (FeedColumns.TABLE_NAME.equals(table)
                && (values.containsKey(FeedColumns.NAME) || values.containsKey(FeedColumns.URL) || values.containsKey(FeedColumns.PRIORITY))) {
            mDatabaseHelper.exportToOPML();
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
                table = FeedColumns.TABLE_NAME;

                String groupId = uri.getPathSegments().get(1);

                where.append(FeedColumns._ID).append('=').append(groupId);

                // Delete the sub feeds & their entries
                Cursor subFeedsCursor = database.query(FeedColumns.TABLE_NAME, FeedColumns.PROJECTION_ID, FeedColumns.GROUP_ID + "=" + groupId, null,
                        null, null, null);
                while (subFeedsCursor.moveToNext()) {
                    String feedId = subFeedsCursor.getString(0);
                    delete(FeedColumns.CONTENT_URI(feedId), null, null);
                }
                subFeedsCursor.close();

                // Update the priorities
                Cursor priorityCursor = database.query(FeedColumns.TABLE_NAME, FeedColumns.PROJECTION_PRIORITY, FeedColumns._ID + "=" + groupId, null,
                        null, null, null);

                if (priorityCursor.moveToNext()) {
                    int priority = priorityCursor.getInt(0);
                    String priorityWhere = FeedColumns.PRIORITY + " > " + priority;
                    String groupWhere = '(' + FeedColumns.IS_GROUP + Constants.DB_IS_TRUE + Constants.DB_OR + FeedColumns.GROUP_ID + Constants.DB_IS_NULL
                            + ')';
                    database.execSQL("UPDATE " + FeedColumns.TABLE_NAME + " SET " + FeedColumns.PRIORITY + " = " + FeedColumns.PRIORITY + "-1 WHERE "
                            + groupWhere + Constants.DB_AND + priorityWhere);
                }
                priorityCursor.close();
                break;
            }
            case URI_FEED: {
                table = FeedColumns.TABLE_NAME;

                final String feedId = uri.getPathSegments().get(1);

                // Remove also the feed entries
                new Thread() {
                    @Override
                    public void run() {
                        Uri entriesUri = EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(feedId);
                        delete(entriesUri, null, null);
                        delete(FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(feedId), null, null);
                        NetworkUtils.deleteFeedImagesCache(entriesUri, null);
                    }
                }.start();

                where.append(FeedColumns._ID).append('=').append(feedId);

                // Update the priorities
                Cursor priorityCursor = database.query(FeedColumns.TABLE_NAME, new String[]{FeedColumns.PRIORITY, FeedColumns.GROUP_ID},
                        FeedColumns._ID + '=' + feedId, null, null, null, null);

                if (priorityCursor.moveToNext()) {
                    int priority = priorityCursor.getInt(0);
                    String groupId = priorityCursor.getString(1);

                    String groupWhere = '(' + (groupId != null ? FeedColumns.GROUP_ID + '=' + groupId : FeedColumns.IS_GROUP + Constants.DB_IS_TRUE
                            + Constants.DB_OR + FeedColumns.GROUP_ID + Constants.DB_IS_NULL) + ')';
                    String priorityWhere = FeedColumns.PRIORITY + " > " + priority;

                    database.execSQL("UPDATE " + FeedColumns.TABLE_NAME + " SET " + FeedColumns.PRIORITY + " = " + FeedColumns.PRIORITY + "-1 WHERE "
                            + groupWhere + Constants.DB_AND + priorityWhere);
                }
                priorityCursor.close();
                break;
            }
            case URI_GROUPS:
            case URI_FEEDS: {
                table = FeedColumns.TABLE_NAME;
                break;
            }
            case URI_FEEDS_FOR_GROUPS: {
                table = FeedColumns.TABLE_NAME;
                where.append(FeedColumns.GROUP_ID).append('=').append(uri.getPathSegments().get(1));
                break;
            }
            case URI_FILTERS: {
                table = FilterColumns.TABLE_NAME;
                break;
            }
            case URI_FILTERS_FOR_FEED: {
                table = FilterColumns.TABLE_NAME;
                where.append(FilterColumns.FEED_ID).append('=').append(uri.getPathSegments().get(1));
                break;
            }
            case URI_ENTRY_FOR_FEED:
            case URI_ENTRY_FOR_GROUP: {
                table = EntryColumns.TABLE_NAME;
                where.append(EntryColumns._ID).append('=').append(uri.getPathSegments().get(3));
                break;
            }
            case URI_ENTRIES_FOR_FEED: {
                table = EntryColumns.TABLE_NAME;
                where.append(EntryColumns.FEED_ID).append('=').append(uri.getPathSegments().get(1));
                break;
            }
            case URI_ENTRIES_FOR_GROUP: {
                table = EntryColumns.TABLE_NAME;
                where.append(EntryColumns.FEED_ID).append(" IN (SELECT ").append(FeedColumns._ID).append(" FROM ").append(FeedColumns.TABLE_NAME).append(" WHERE ").append(FeedColumns.GROUP_ID).append('=').append(uri.getPathSegments().get(1)).append(')');
                break;
            }
            case URI_ENTRIES: {
                table = EntryColumns.TABLE_NAME;
                break;
            }
            case URI_FAVORITES_ENTRY:
            case URI_ENTRY: {
                table = EntryColumns.TABLE_NAME;
                where.append(EntryColumns._ID).append('=').append(uri.getPathSegments().get(1));
                break;
            }
            case URI_FAVORITES: {
                table = EntryColumns.TABLE_NAME;
                where.append(EntryColumns.IS_FAVORITE).append(Constants.DB_IS_TRUE);
                break;
            }
            case URI_TASKS: {
                table = TaskColumns.TABLE_NAME;
                break;
            }
            case URI_TASK: {
                table = TaskColumns.TABLE_NAME;
                where.append(TaskColumns._ID).append('=').append(uri.getPathSegments().get(1));
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

        if (FeedColumns.TABLE_NAME.equals(table)) {
            mDatabaseHelper.exportToOPML();
        }
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    private static String getFeedIdFromEntryId(long entryId) {
        ContentResolver cr = MainApplication.getContext().getContentResolver();
        Cursor c = cr.query(EntryColumns.CONTENT_URI(entryId), new String[]{EntryColumns.FEED_ID}, null, null, null);
        if (c.moveToFirst()) {
            return c.getString(0);
        }
        c.close();

        return null;
    }

    public static void notifyGroupFromFeedId(String feedId) {
        ContentResolver cr = MainApplication.getContext().getContentResolver();
        cr.notifyChange(FeedColumns.GROUPED_FEEDS_CONTENT_URI, null);
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

    public static void notifyAllFromEntryUri(Uri entryUri, boolean onlyStarredOrUnstarred) {
        ContentResolver cr = MainApplication.getContext().getContentResolver();

        try {
            long entryId = Long.parseLong(entryUri.getLastPathSegment());

            String uriStr = entryUri.toString();
            if (!onlyStarredOrUnstarred) {
                String feedId = FeedDataContentProvider.getFeedIdFromEntryId(entryId);
                if (feedId != null) {
                    FeedDataContentProvider.notifyGroupFromFeedId(feedId);
                }
                if (!uriStr.startsWith(FeedColumns.CONTENT_URI.toString())) {
                    cr.notifyChange(ContentUris.withAppendedId(FeedColumns.CONTENT_URI, entryId), null);
                }
            }

            if (!uriStr.startsWith(EntryColumns.FAVORITES_CONTENT_URI.toString())) {
                cr.notifyChange(ContentUris.withAppendedId(EntryColumns.FAVORITES_CONTENT_URI, entryId), null);
            }
            if (!uriStr.startsWith(EntryColumns.CONTENT_URI.toString())) {
                cr.notifyChange(ContentUris.withAppendedId(EntryColumns.CONTENT_URI, entryId), null);
            }
        } catch (Exception ignored) {
        }
    }
}
