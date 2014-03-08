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

package net.fred.feedex.parser;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Xml;

import net.fred.feedex.Constants;
import net.fred.feedex.MainApplication;
import net.fred.feedex.provider.FeedData.FeedColumns;
import net.fred.feedex.provider.FeedData.FilterColumns;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class OPML {

    public static final String BACKUP_OPML = Environment.getExternalStorageDirectory() + "/FeedEx_auto_backup.opml";

    private static final String[] FEEDS_PROJECTION = new String[]{FeedColumns._ID, FeedColumns.IS_GROUP, FeedColumns.NAME, FeedColumns.URL,
            FeedColumns.RETRIEVE_FULLTEXT, FeedColumns.RETRIEVE_WEBPAGE, FeedColumns.RETRIEVE_DESKTOP_WEBPAGE};
    private static final String[] FILTERS_PROJECTION = new String[]{FilterColumns.FILTER_TEXT, FilterColumns.IS_REGEX,
            FilterColumns.IS_APPLIED_TO_TITLE};

    private static final String START = "<?xml version='1.0' encoding='utf-8'?>\n<opml version='1.1'>\n<head>\n<title>FeedEx export</title>\n<dateCreated>";
    private static final String AFTER_DATE = "</dateCreated>\n</head>\n<body>\n";
    private static final String OUTLINE_TITLE = "\t<outline title='";
    private static final String OUTLINE_XMLURL = "' type='rss' xmlUrl='";
    private static final String OUTLINE_RETRIEVE_FULLTEXT = "' retrieveFullText='";
    private static final String OUTLINE_DOWNLOAD_WEBPAGE = "' downloadWebpage='";
    private static final String OUTLINE_DOWNLOAD_DESKTOP_WEBPAGE = "' downloadDesktopWebpage='";
    private static final String OUTLINE_INLINE_CLOSING = "'/>\n";
    private static final String OUTLINE_NORMAL_CLOSING = "'>\n";
    private static final String OUTLINE_END = "\t</outline>\n";
    private static final String FILTER_TEXT = "\t\t<filter text='";
    private static final String FILTER_IS_REGEX = "' isRegex='";
    private static final String FILTER_IS_APPLIED_TO_TITLE = "' isAppliedToTitle='";
    private static final String FILTER_CLOSING = "'/>\n";
    private static final String CLOSING = "</body>\n</opml>\n";

    private static final OPMLParser parser = new OPMLParser();
    private static boolean autoBackupEnabled = true;

    public static void importFromFile(String filename) throws IOException, SAXException {
        if (BACKUP_OPML.equals(filename)) {
            autoBackupEnabled = false;  // Do not write the auto backup file while reading it...
        }

        try {
            Xml.parse(new InputStreamReader(new FileInputStream(filename)), parser);
        } finally {
            autoBackupEnabled = true;
        }
    }

    public static void importFromFile(InputStream input) throws IOException, SAXException {
        Xml.parse(new InputStreamReader(input), parser);
    }

    public static void exportToFile(String filename) throws IOException {
        if (BACKUP_OPML.equals(filename) && !autoBackupEnabled) {
            return;
        }

        Cursor cursor = MainApplication.getContext().getContentResolver()
                .query(FeedColumns.GROUPS_CONTENT_URI, FEEDS_PROJECTION, null, null, null);

        StringBuilder builder = new StringBuilder(START);
        builder.append(System.currentTimeMillis());
        builder.append(AFTER_DATE);

        while (cursor.moveToNext()) {
            builder.append(OUTLINE_TITLE);
            builder.append(cursor.isNull(2) ? "" : TextUtils.htmlEncode(cursor.getString(2)));
            if (cursor.getInt(1) == 1) { // If it is a group
                builder.append(OUTLINE_NORMAL_CLOSING);

                Cursor cursorChildren = MainApplication.getContext().getContentResolver()
                        .query(FeedColumns.FEEDS_FOR_GROUPS_CONTENT_URI(cursor.getString(0)), FEEDS_PROJECTION, null, null, null);
                while (cursorChildren.moveToNext()) {
                    builder.append("\t");
                    builder.append(OUTLINE_TITLE);
                    builder.append(cursorChildren.isNull(2) ? "" : TextUtils.htmlEncode(cursorChildren.getString(2)));
                    builder.append(OUTLINE_XMLURL);
                    builder.append(TextUtils.htmlEncode(cursorChildren.getString(3)));
                    builder.append(OUTLINE_RETRIEVE_FULLTEXT);
                    builder.append(cursorChildren.getInt(4) == 1 ? Constants.TRUE : Constants.FALSE);
                    builder.append(OUTLINE_DOWNLOAD_WEBPAGE);
                    builder.append(cursorChildren.getInt(5) == 1 ? Constants.TRUE : Constants.FALSE);
                    builder.append(OUTLINE_DOWNLOAD_DESKTOP_WEBPAGE);
                    builder.append(cursorChildren.getInt(6) == 1 ? Constants.TRUE : Constants.FALSE);

                    Cursor cursorFilters = MainApplication.getContext().getContentResolver()
                            .query(FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(cursorChildren.getString(0)), FILTERS_PROJECTION, null, null, null);
                    if (cursorFilters.getCount() != 0) {
                        builder.append(OUTLINE_NORMAL_CLOSING);
                        while (cursorFilters.moveToNext()) {
                            builder.append("\t");
                            builder.append(FILTER_TEXT);
                            builder.append(TextUtils.htmlEncode(cursorFilters.getString(0)));
                            builder.append(FILTER_IS_REGEX);
                            builder.append(cursorFilters.getInt(1) == 1 ? Constants.TRUE : Constants.FALSE);
                            builder.append(FILTER_IS_APPLIED_TO_TITLE);
                            builder.append(cursorFilters.getInt(2) == 1 ? Constants.TRUE : Constants.FALSE);
                            builder.append(FILTER_CLOSING);
                        }
                        builder.append("\t");
                        builder.append(OUTLINE_END);
                    } else {
                        builder.append(OUTLINE_INLINE_CLOSING);
                    }
                    cursorFilters.close();
                }
                cursorChildren.close();

                builder.append(OUTLINE_END);
            } else {
                builder.append(OUTLINE_XMLURL);
                builder.append(TextUtils.htmlEncode(cursor.getString(3)));
                builder.append(OUTLINE_RETRIEVE_FULLTEXT);
                builder.append(cursor.getInt(4) == 1 ? Constants.TRUE : Constants.FALSE);
                builder.append(OUTLINE_DOWNLOAD_WEBPAGE);
                builder.append(cursor.getInt(5) == 1 ? Constants.TRUE : Constants.FALSE);
                builder.append(OUTLINE_DOWNLOAD_DESKTOP_WEBPAGE);
                builder.append(cursor.getInt(6) == 1 ? Constants.TRUE : Constants.FALSE);
                Cursor cursorFilters = MainApplication.getContext().getContentResolver()
                        .query(FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(cursor.getString(0)), FILTERS_PROJECTION, null, null, null);
                if (cursorFilters.getCount() != 0) {
                    builder.append(OUTLINE_NORMAL_CLOSING);
                    while (cursorFilters.moveToNext()) {
                        builder.append(FILTER_TEXT);
                        builder.append(TextUtils.htmlEncode(cursorFilters.getString(0)));
                        builder.append(FILTER_IS_REGEX);
                        builder.append(cursorFilters.getInt(1) == 1 ? Constants.TRUE : Constants.FALSE);
                        builder.append(FILTER_IS_APPLIED_TO_TITLE);
                        builder.append(cursorFilters.getInt(2) == 1 ? Constants.TRUE : Constants.FALSE);
                        builder.append(FILTER_CLOSING);
                    }
                    builder.append(OUTLINE_END);
                } else {
                    builder.append(OUTLINE_INLINE_CLOSING);
                }
                cursorFilters.close();
            }
        }
        builder.append(CLOSING);

        cursor.close();

        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));

        writer.write(builder.toString());
        writer.close();
    }

    private static class OPMLParser extends DefaultHandler {
        private static final String TAG_BODY = "body";
        private static final String TAG_OUTLINE = "outline";
        private static final String ATTRIBUTE_TITLE = "title";
        private static final String ATTRIBUTE_XMLURL = "xmlUrl";
        private static final String ATTRIBUTE_RETRIEVE_FULLTEXT = "retrieveFullText";
        private static final String ATTRIBUTE_DOWNLOAD_WEBPAGE = "downloadWebpage";
        private static final String ATTRIBUTE_DOWNLOAD_DESKTOP_WEBPAGE = "downloadDesktopWebpage";
        private static final String TAG_FILTER = "filter";
        private static final String ATTRIBUTE_TEXT = "text";
        private static final String ATTRIBUTE_IS_REGEX = "isRegex";
        private static final String ATTRIBUTE_IS_APPLIED_TO_TITLE = "isAppliedToTitle";

        private boolean bodyTagEntered = false;
        private boolean feedEntered = false;
        private boolean probablyValidElement = false;
        private String groupId = null;
        private String feedId = null;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (!bodyTagEntered) {
                if (TAG_BODY.equals(localName)) {
                    bodyTagEntered = true;
                    probablyValidElement = true;
                }
            } else if (TAG_OUTLINE.equals(localName)) {
                String url = attributes.getValue("", ATTRIBUTE_XMLURL);
                String title = attributes.getValue("", ATTRIBUTE_TITLE);

                ContentResolver cr = MainApplication.getContext().getContentResolver();

                if (url == null) { // No url => this is a group
                    if (title != null) {
                        ContentValues values = new ContentValues();
                        values.put(FeedColumns.IS_GROUP, true);
                        values.put(FeedColumns.NAME, title);

                        Cursor cursor = cr.query(FeedColumns.GROUPS_CONTENT_URI, null, FeedColumns.NAME + Constants.DB_ARG, new String[]{title}, null);

                        if (!cursor.moveToFirst()) {
                            groupId = cr.insert(FeedColumns.GROUPS_CONTENT_URI, values).getLastPathSegment();
                        }
                        cursor.close();
                    }

                } else { // Url => this is a feed
                    feedEntered = true;
                    ContentValues values = new ContentValues();

                    values.put(FeedColumns.URL, url);
                    values.put(FeedColumns.NAME, title != null && title.length() > 0 ? title : null);
                    if (groupId != null) {
                        values.put(FeedColumns.GROUP_ID, groupId);
                    }
                    values.put(FeedColumns.RETRIEVE_FULLTEXT, Constants.TRUE.equals(attributes.getValue("", ATTRIBUTE_RETRIEVE_FULLTEXT)));
                    values.put(FeedColumns.RETRIEVE_WEBPAGE, Constants.TRUE.equals(attributes.getValue("", ATTRIBUTE_DOWNLOAD_WEBPAGE)));
                    values.put(FeedColumns.RETRIEVE_DESKTOP_WEBPAGE, Constants.TRUE.equals(attributes.getValue("", ATTRIBUTE_DOWNLOAD_DESKTOP_WEBPAGE)));

                    Cursor cursor = cr.query(FeedColumns.CONTENT_URI, null, FeedColumns.URL + Constants.DB_ARG,
                            new String[]{url}, null);
                    feedId = null;
                    if (!cursor.moveToFirst()) {
                        feedId = cr.insert(FeedColumns.CONTENT_URI, values).getLastPathSegment();
                    }
                    cursor.close();
                }
            } else if (TAG_FILTER.equals(localName)) {
                if (feedEntered && feedId != null) {
                    ContentValues values = new ContentValues();
                    values.put(FilterColumns.FILTER_TEXT, attributes.getValue("", ATTRIBUTE_TEXT));
                    values.put(FilterColumns.IS_REGEX, Constants.TRUE.equals(attributes.getValue("", ATTRIBUTE_IS_REGEX)));
                    values.put(FilterColumns.IS_APPLIED_TO_TITLE, Constants.TRUE.equals(attributes.getValue("", ATTRIBUTE_IS_APPLIED_TO_TITLE)));

                    ContentResolver cr = MainApplication.getContext().getContentResolver();
                    cr.insert(FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(feedId), values);
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (bodyTagEntered && TAG_BODY.equals(localName)) {
                bodyTagEntered = false;
            } else if (TAG_OUTLINE.equals(localName)) {
                if (feedEntered) {
                    feedEntered = false;
                } else {
                    groupId = null;
                }
            }
        }

        @Override
        public void warning(SAXParseException e) throws SAXException {
            // ignore warnings
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            // ignore small errors
        }

        @Override
        public void endDocument() throws SAXException {
            if (!probablyValidElement) {
                throw new SAXException();
            } else {
                super.endDocument();
            }
        }
    }
}
