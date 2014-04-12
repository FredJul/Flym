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

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.Html;
import android.text.TextUtils;

import net.fred.feedex.Constants;
import net.fred.feedex.MainApplication;
import net.fred.feedex.provider.FeedData;
import net.fred.feedex.provider.FeedData.EntryColumns;
import net.fred.feedex.provider.FeedData.FeedColumns;
import net.fred.feedex.provider.FeedData.FilterColumns;
import net.fred.feedex.service.FetcherService;
import net.fred.feedex.utils.HtmlUtils;
import net.fred.feedex.utils.NetworkUtils;
import net.fred.feedex.utils.PrefUtils;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RssAtomParser extends DefaultHandler {

    private static final String AND_SHARP = "&#";
    private static final String HTML_TEXT = "text/html";
    private static final String HTML_TAG_REGEX = "<(.|\n)*?>";

    private static final String TAG_RSS = "rss";
    private static final String TAG_RDF = "rdf";
    private static final String TAG_FEED = "feed";
    private static final String TAG_ENTRY = "entry";
    private static final String TAG_ITEM = "item";
    private static final String TAG_UPDATED = "updated";
    private static final String TAG_TITLE = "title";
    private static final String TAG_LINK = "link";
    private static final String TAG_DESCRIPTION = "description";
    private static final String TAG_MEDIA_DESCRIPTION = "media:description";
    private static final String TAG_CONTENT = "content";
    private static final String TAG_MEDIA_CONTENT = "media:content";
    private static final String TAG_ENCODED_CONTENT = "encoded";
    private static final String TAG_SUMMARY = "summary";
    private static final String TAG_PUBDATE = "pubDate";
    private static final String TAG_PUBLISHED = "published";
    private static final String TAG_DATE = "date";
    private static final String TAG_LAST_BUILD_DATE = "lastBuildDate";
    private static final String TAG_ENCLOSURE = "enclosure";
    private static final String TAG_GUID = "guid";
    private static final String TAG_AUTHOR = "author";
    private static final String TAG_CREATOR = "creator";
    private static final String TAG_NAME = "name";

    private static final String ATTRIBUTE_URL = "url";
    private static final String ATTRIBUTE_HREF = "href";
    private static final String ATTRIBUTE_TYPE = "type";
    private static final String ATTRIBUTE_LENGTH = "length";
    private static final String ATTRIBUTE_REL = "rel";

    private static final String[][] TIMEZONES_REPLACE = {{"MEST", "+0200"}, {"EST", "-0500"}, {"PST", "-0800"}};

    private static final DateFormat[] PUBDATE_DATE_FORMATS = {new SimpleDateFormat("d' 'MMM' 'yyyy' 'HH:mm:ss", Locale.US),
            new SimpleDateFormat("d' 'MMM' 'yyyy' 'HH:mm:ss' 'Z", Locale.US), new SimpleDateFormat("d' 'MMM' 'yyyy' 'HH:mm:ss' 'z", Locale.US)};

    private static final DateFormat[] UPDATE_DATE_FORMATS = {new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ssZ", Locale.US), new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss.SSSz", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd", Locale.US)};

    private final Date realLastUpdateDate;
    private long newRealLastUpdate;

    private final String id;

    private boolean entryTagEntered = false;
    private boolean titleTagEntered = false;
    private boolean updatedTagEntered = false;
    private boolean linkTagEntered = false;
    private boolean descriptionTagEntered = false;
    private boolean pubDateTagEntered = false;
    private boolean publishedTagEntered = false;
    private boolean dateTagEntered = false;
    private boolean lastBuildDateTagEntered = false;
    private boolean guidTagEntered = false;
    private boolean authorTagEntered = false;

    private StringBuilder title;
    private StringBuilder dateStringBuilder;
    private String feedLink;
    private Date entryDate;
    private Date entryUpdateDate;
    private Date previousEntryDate;
    private Date previousEntryUpdateDate;
    private StringBuilder entryLink;
    private StringBuilder description;
    private StringBuilder enclosure;
    private final Uri feedEntriesUri;
    private int newCount = 0;
    private final String feedName;
    private String feedTitle;
    private final String feedBaseUrl;
    private boolean done = false;
    private final Date keepDateBorder;
    private boolean fetchImages = false;
    private boolean retrieveFullText = false;
    private boolean cancelled = false;
    private long now = System.currentTimeMillis();
    private StringBuilder guid;
    private StringBuilder author, tmpAuthor;

    private final FeedFilters filters;

    private final ArrayList<ContentProviderOperation> inserts = new ArrayList<ContentProviderOperation>();
    private final ArrayList<ArrayList<String>> entriesImages = new ArrayList<ArrayList<String>>();

    public RssAtomParser(Date realLastUpdateDate, final String id, String feedName, String url, boolean retrieveFullText) {
        long keepTime = Long.parseLong(PrefUtils.getString(PrefUtils.KEEP_TIME, "4")) * 86400000l;
        long keepDateBorderTime = keepTime > 0 ? System.currentTimeMillis() - keepTime : 0;

        keepDateBorder = new Date(keepDateBorderTime);
        this.realLastUpdateDate = realLastUpdateDate;
        newRealLastUpdate = realLastUpdateDate.getTime();
        this.id = id;
        this.feedName = feedName;
        feedEntriesUri = EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(id);
        this.retrieveFullText = retrieveFullText;

        filters = new FeedFilters(id);

        // Remove old stuffs
        final String where = EntryColumns.DATE + '<' + keepDateBorderTime + Constants.DB_AND + EntryColumns.WHERE_NOT_FAVORITE;
        NetworkUtils.deleteFeedImagesCache(feedEntriesUri, where);
        MainApplication.getContext().getContentResolver().delete(feedEntriesUri, where, null);

        feedBaseUrl = NetworkUtils.getBaseUrl(url);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (TAG_UPDATED.equals(localName)) {
            updatedTagEntered = true;
            dateStringBuilder = new StringBuilder();
        } else if (TAG_ENTRY.equals(localName) || TAG_ITEM.equals(localName)) {
            entryTagEntered = true;
            description = null;
            entryLink = null;

            // Save the previous (if no date are found for this entry)
            previousEntryDate = entryDate;
            previousEntryUpdateDate = entryUpdateDate;
            entryDate = null;
            entryUpdateDate = null;

            // This is the retrieved feed title
            if (feedTitle == null && title != null && title.length() > 0) {
                feedTitle = title.toString();
            }
            title = null;
        } else if (TAG_TITLE.equals(localName)) {
            if (title == null) {
                titleTagEntered = true;
                title = new StringBuilder();
            }
        } else if (TAG_LINK.equals(localName)) {
            if (authorTagEntered) {
                return;
            }
            if (TAG_ENCLOSURE.equals(attributes.getValue("", ATTRIBUTE_REL))) {
                startEnclosure(attributes, attributes.getValue("", ATTRIBUTE_HREF));
            } else {
                // Get the link only if we don't have one or if its the good one (html)
                if (entryLink == null || HTML_TEXT.equals(attributes.getValue("", ATTRIBUTE_TYPE))) {
                    entryLink = new StringBuilder();

                    boolean foundLink = false;
                    String href = attributes.getValue("", ATTRIBUTE_HREF);
                    if (!TextUtils.isEmpty(href)) {
                        entryLink.append(href);
                        foundLink = true;
                        linkTagEntered = false;
                    } else {
                        linkTagEntered = true;
                    }

                    if (!foundLink) {
                        linkTagEntered = true;
                    }
                }
            }
        } else if ((TAG_DESCRIPTION.equals(localName) && !TAG_MEDIA_DESCRIPTION.equals(qName))
                || (TAG_CONTENT.equals(localName) && !TAG_MEDIA_CONTENT.equals(qName))) {
            descriptionTagEntered = true;
            description = new StringBuilder();
        } else if (TAG_SUMMARY.equals(localName)) {
            if (description == null) {
                descriptionTagEntered = true;
                description = new StringBuilder();
            }
        } else if (TAG_PUBDATE.equals(localName)) {
            pubDateTagEntered = true;
            dateStringBuilder = new StringBuilder();
        } else if (TAG_PUBLISHED.equals(localName)) {
            publishedTagEntered = true;
            dateStringBuilder = new StringBuilder();
        } else if (TAG_DATE.equals(localName)) {
            dateTagEntered = true;
            dateStringBuilder = new StringBuilder();
        } else if (TAG_LAST_BUILD_DATE.equals(localName)) {
            lastBuildDateTagEntered = true;
            dateStringBuilder = new StringBuilder();
        } else if (TAG_ENCODED_CONTENT.equals(localName)) {
            descriptionTagEntered = true;
            description = new StringBuilder();
        } else if (TAG_ENCLOSURE.equals(localName)) {
            startEnclosure(attributes, attributes.getValue("", ATTRIBUTE_URL));
        } else if (TAG_GUID.equals(localName)) {
            guidTagEntered = true;
            guid = new StringBuilder();
        } else if (TAG_NAME.equals(localName) || TAG_AUTHOR.equals(localName) || TAG_CREATOR.equals(localName)) {
            authorTagEntered = true;
            if (tmpAuthor == null) {
                tmpAuthor = new StringBuilder();
            }
        }
    }

    private void startEnclosure(Attributes attributes, String url) {
        if (enclosure == null && url != null) { // fetch the first enclosure only
            enclosure = new StringBuilder(url);
            enclosure.append(Constants.ENCLOSURE_SEPARATOR);

            String value = attributes.getValue("", ATTRIBUTE_TYPE);

            if (value != null) {
                enclosure.append(value);
            }
            enclosure.append(Constants.ENCLOSURE_SEPARATOR);
            value = attributes.getValue("", ATTRIBUTE_LENGTH);
            if (value != null) {
                enclosure.append(value);
            }
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (titleTagEntered) {
            title.append(ch, start, length);
        } else if (linkTagEntered) {
            entryLink.append(ch, start, length);
        } else if (descriptionTagEntered) {
            description.append(ch, start, length);
        } else if (updatedTagEntered || pubDateTagEntered || publishedTagEntered || dateTagEntered || lastBuildDateTagEntered) {
            dateStringBuilder.append(ch, start, length);
        } else if (guidTagEntered) {
            guid.append(ch, start, length);
        } else if (authorTagEntered) {
            tmpAuthor.append(ch, start, length);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (TAG_TITLE.equals(localName)) {
            titleTagEntered = false;
        } else if ((TAG_DESCRIPTION.equals(localName) && !TAG_MEDIA_DESCRIPTION.equals(qName)) || TAG_SUMMARY.equals(localName)
                || (TAG_CONTENT.equals(localName) && !TAG_MEDIA_CONTENT.equals(qName)) || TAG_ENCODED_CONTENT.equals(localName)) {
            descriptionTagEntered = false;
        } else if (TAG_LINK.equals(localName)) {
            linkTagEntered = false;

            if (feedLink == null && !entryTagEntered && TAG_LINK.equals(qName)) { // Skip <atom10:link> tags
                feedLink = entryLink.toString();
            }
        } else if (TAG_UPDATED.equals(localName)) {
            entryUpdateDate = parseUpdateDate(dateStringBuilder.toString());
            updatedTagEntered = false;
        } else if (TAG_PUBDATE.equals(localName)) {
            entryDate = parsePubdateDate(dateStringBuilder.toString());
            pubDateTagEntered = false;
        } else if (TAG_PUBLISHED.equals(localName)) {
            entryDate = parsePubdateDate(dateStringBuilder.toString());
            publishedTagEntered = false;
        } else if (TAG_LAST_BUILD_DATE.equals(localName)) {
            entryDate = parsePubdateDate(dateStringBuilder.toString());
            lastBuildDateTagEntered = false;
        } else if (TAG_DATE.equals(localName)) {
            entryDate = parseUpdateDate(dateStringBuilder.toString());
            dateTagEntered = false;
        } else if (TAG_ENTRY.equals(localName) || TAG_ITEM.equals(localName)) {
            entryTagEntered = false;

            boolean updateOnly = false;
            // Old entryDate but recent update date => we need to not insert it!
            if (entryUpdateDate != null && entryDate != null && (entryDate.before(realLastUpdateDate) || entryDate.before(keepDateBorder))) {
                updateOnly = true;
                if (entryUpdateDate.after(entryDate)) {
                    entryDate = entryUpdateDate;
                }
            } else if (entryDate == null && entryUpdateDate != null) { // only one updateDate, copy it into entryDate
                entryDate = entryUpdateDate;
            } else if (entryDate == null && entryUpdateDate == null) { // nothing, we need to retrieve the previous date
                entryDate = previousEntryDate;
                entryUpdateDate = previousEntryUpdateDate;
            }

            if (title != null && (entryDate == null || (entryDate.after(realLastUpdateDate) && entryDate.after(keepDateBorder)))) {
                ContentValues values = new ContentValues();

                if (entryDate != null && entryDate.getTime() > newRealLastUpdate) {
                    newRealLastUpdate = entryDate.getTime();
                }

                String improvedTitle = unescapeTitle(title.toString().trim());
                values.put(EntryColumns.TITLE, improvedTitle);

                String improvedContent = null;
                if (description != null) {
                    // Improve the description
                    improvedContent = HtmlUtils.improveHtmlContent(description.toString(), feedBaseUrl);
                    if (fetchImages) {
                        entriesImages.add(HtmlUtils.getImageURLs(improvedContent));
                    }
                    if (improvedContent != null) {
                        values.put(EntryColumns.ABSTRACT, improvedContent);
                    }
                }

                // Try to find if the entry is not filtered and need to be processed
                if (!filters.isEntryFiltered(improvedTitle, improvedContent)) {

                    if (author != null) {
                        values.put(EntryColumns.AUTHOR, author.toString());
                    }

                    String enclosureString = null;
                    StringBuilder existanceStringBuilder = new StringBuilder(EntryColumns.LINK).append(Constants.DB_ARG);

                    if (enclosure != null && enclosure.length() > 0) {
                        enclosureString = enclosure.toString();
                        values.put(EntryColumns.ENCLOSURE, enclosureString);
                        existanceStringBuilder.append(Constants.DB_AND).append(EntryColumns.ENCLOSURE).append(Constants.DB_ARG);
                    }

                    String guidString = null;

                    if (guid != null && guid.length() > 0) {
                        guidString = guid.toString();
                        values.put(EntryColumns.GUID, guidString);
                        existanceStringBuilder.append(Constants.DB_AND).append(EntryColumns.GUID).append(Constants.DB_ARG);
                    }

                    String entryLinkString = ""; // don't set this to null as we need *some* value

                    if (entryLink != null && entryLink.length() > 0) {
                        entryLinkString = entryLink.toString().trim();
                        if (feedBaseUrl != null && !entryLinkString.startsWith(Constants.HTTP_SCHEME) && !entryLinkString.startsWith(Constants.HTTPS_SCHEME)) {
                            entryLinkString = feedBaseUrl
                                    + (entryLinkString.startsWith(Constants.SLASH) ? entryLinkString : Constants.SLASH + entryLinkString);
                        }
                    }

                    String[] existanceValues = enclosureString != null ? (guidString != null ? new String[]{entryLinkString, enclosureString,
                            guidString} : new String[]{entryLinkString, enclosureString}) : (guidString != null ? new String[]{entryLinkString,
                            guidString} : new String[]{entryLinkString});

                    // First, try to update the feed
                    ContentResolver cr = MainApplication.getContext().getContentResolver();
                    boolean isUpdated = (!entryLinkString.isEmpty() || guidString != null)
                            && cr.update(feedEntriesUri, values, existanceStringBuilder.toString(), existanceValues) != 0;

                    // Insert it only if necessary
                    if (!isUpdated && !updateOnly) {
                        // We put the date only for new entry (no need to change the past, you may already read it)
                        if (entryDate != null) {
                            values.put(EntryColumns.DATE, entryDate.getTime());
                        } else {
                            values.put(EntryColumns.DATE, now--); // -1 to keep the good entries order
                        }

                        values.put(EntryColumns.LINK, entryLinkString);

                        // We cannot update, we need to insert it
                        inserts.add(ContentProviderOperation.newInsert(feedEntriesUri).withValues(values).build());

                        newCount++;
                    }

                    // No date, but we managed to update an entry => we already parsed the following entries and don't need to continue
                    if (isUpdated && entryDate == null) {
                        cancel();
                    }
                }
            } else {
                cancel();
            }
            description = null;
            title = null;
            enclosure = null;
            guid = null;
            author = null;
        } else if (TAG_RSS.equals(localName) || TAG_RDF.equals(localName) || TAG_FEED.equals(localName)) {
            done = true;
        } else if (TAG_GUID.equals(localName)) {
            guidTagEntered = false;
        } else if (TAG_NAME.equals(localName) || TAG_AUTHOR.equals(localName) || TAG_CREATOR.equals(localName)) {
            authorTagEntered = false;

            if (tmpAuthor != null && tmpAuthor.indexOf("@") == -1) { // no email
                if (author == null) {
                    author = new StringBuilder(tmpAuthor);
                } else { // this indicates multiple authors
                    boolean found = false;
                    for (String previousAuthor : author.toString().split(",")) {
                        if (previousAuthor.equals(tmpAuthor.toString())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        author.append(Constants.COMMA_SPACE);
                        author.append(tmpAuthor);
                    }
                }
            }

            tmpAuthor = null;
        }
    }

    public String getFeedLink() {
        return feedLink;
    }

    public int getNewCount() {
        return newCount;
    }

    public boolean isDone() {
        return done;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    private void cancel() throws SAXException {
        if (!cancelled) {
            cancelled = true;
            done = true;
            endDocument();

            throw new SAXException("Finished");
        }
    }

    public void setFetchImages(boolean fetchImages) {
        this.fetchImages = fetchImages;
    }

    private Date parseUpdateDate(String dateStr) {
        dateStr = improveDateString(dateStr);
        return parseUpdateDate(dateStr, true);
    }

    private Date parseUpdateDate(String dateStr, boolean tryAllFormat) {
        for (DateFormat format : UPDATE_DATE_FORMATS) {
            try {
                Date result = format.parse(dateStr);
                return (result.getTime() > now ? new Date(now) : result);
            } catch (ParseException ignored) {
            } // just do nothing
        }

        if (tryAllFormat)
            return parsePubdateDate(dateStr, false);
        else
            return null;
    }

    private Date parsePubdateDate(String dateStr) {
        dateStr = improveDateString(dateStr);
        return parsePubdateDate(dateStr, true);
    }

    private Date parsePubdateDate(String dateStr, boolean tryAllFormat) {
        for (DateFormat format : PUBDATE_DATE_FORMATS) {
            try {
                Date result = format.parse(dateStr);
                return (result.getTime() > now ? new Date(now) : result);
            } catch (ParseException ignored) {
            } // just do nothing
        }

        if (tryAllFormat)
            return parseUpdateDate(dateStr, false);
        else
            return null;
    }

    private String improveDateString(String dateStr) {
        // We remove the first part if necessary (the day display)
        int coma = dateStr.indexOf(", ");
        if (coma != -1) {
            dateStr = dateStr.substring(coma + 2);
        }

        dateStr = dateStr.replaceAll("([0-9])T([0-9])", "$1 $2").replaceAll("Z$", "").replaceAll("  ", " ").trim(); // fix useless char

        // Replace bad timezones
        for (String[] timezoneReplace : TIMEZONES_REPLACE) {
            dateStr = dateStr.replace(timezoneReplace[0], timezoneReplace[1]);
        }

        return dateStr;
    }

    private static String unescapeTitle(String title) {
        String result = title.replace(Constants.AMP_SG, Constants.AMP).replaceAll(HTML_TAG_REGEX, "").replace(Constants.HTML_LT, Constants.LT)
                .replace(Constants.HTML_GT, Constants.GT).replace(Constants.HTML_QUOT, Constants.QUOT)
                .replace(Constants.HTML_APOSTROPHE, Constants.APOSTROPHE);

        if (result.contains(AND_SHARP)) {
            return Html.fromHtml(result, null, null).toString();
        } else {
            return result;
        }
    }

    @Override
    public void warning(SAXParseException e) throws SAXException {
        // ignore warnings
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
        // ignore errors
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
        // ignore errors
    }

    @Override
    public void endDocument() throws SAXException {
        ContentResolver cr = MainApplication.getContext().getContentResolver();

        try {
            if (!inserts.isEmpty()) {
                ContentProviderResult[] results = cr.applyBatch(FeedData.AUTHORITY, inserts);

                if (fetchImages) {
                    for (int i = 0; i < results.length; ++i) {
                        ArrayList<String> images = entriesImages.get(i);
                        if (images != null) {
                            FetcherService.addImagesToDownload(results[i].uri.getLastPathSegment(), images);
                        }
                    }
                }

                if (retrieveFullText) {
                    long[] entriesId = new long[results.length];
                    for (int i = 0; i < results.length; i++) {
                        entriesId[i] = Long.valueOf(results[i].uri.getLastPathSegment());
                    }

                    FetcherService.addEntriesToMobilize(entriesId);
                }
            }
        } catch (Exception ignored) {
        }

        ContentValues values = new ContentValues();
        if (feedName == null && feedTitle != null) {
            values.put(FeedColumns.NAME, feedTitle.trim());
        }
        values.putNull(FeedColumns.ERROR);
        values.put(FeedColumns.LAST_UPDATE, System.currentTimeMillis() - 3000); // by precaution to not miss some feeds
        values.put(FeedData.FeedColumns.REAL_LAST_UPDATE, newRealLastUpdate);
        cr.update(FeedColumns.CONTENT_URI(id), values, null, null);

        super.endDocument();
    }

    private class FeedFilters {

        private class Rule {
            public String filterText;
            public boolean isRegex;
            public boolean isAppliedToTitle;
            public boolean isAcceptRule;
        }

        private final ArrayList<Rule> mFilters = new ArrayList<Rule>();

        public FeedFilters(String feedId) {
            ContentResolver cr = MainApplication.getContext().getContentResolver();
            Cursor c = cr.query(FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(feedId), new String[]{FilterColumns.FILTER_TEXT, FilterColumns.IS_REGEX,
                    FilterColumns.IS_APPLIED_TO_TITLE, FilterColumns.IS_ACCEPT_RULE}, null, null, null);
            while (c.moveToNext()) {
                Rule r = new Rule();
                r.filterText = c.getString(0);
                r.isRegex = c.getInt(1) == 1;
                r.isAppliedToTitle = c.getInt(2) == 1;
                r.isAcceptRule = c.getInt(3) == 1;
                mFilters.add(r);
            }
            c.close();

        }

        public boolean isEntryFiltered(String title, String content) {

            boolean isFiltered = false;

            for (Rule r : mFilters) {

                boolean isMatch = false;
                if (r.isRegex) {
                    Pattern p = Pattern.compile(r.filterText);
                    if (r.isAppliedToTitle) {
                        Matcher m = p.matcher(title);
                        isMatch = m.find();
                    } else if (content != null) {
                        Matcher m = p.matcher(content);
                        isMatch = m.find();
                    }
                } else if ((r.isAppliedToTitle && title.contains(r.filterText)) || (!r.isAppliedToTitle && content != null && content.contains(r.filterText))) {
                    isMatch = true;
                }

                if (r.isAcceptRule) {
                    if (isMatch) {
                        // accept rules override reject rules, the rest of the rules must be ignored
                        isFiltered = false;
                        break;
                    }
                } else if (isMatch) {
                    isFiltered = true;
                    // no break, there might be an accept rule later
                }
            }

            return isFiltered;
        }
    }
}
