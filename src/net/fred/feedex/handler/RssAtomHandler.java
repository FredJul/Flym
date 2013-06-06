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

package net.fred.feedex.handler;

import java.io.FileOutputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.fred.feedex.Constants;
import net.fred.feedex.MainApplication;
import net.fred.feedex.PrefsManager;
import net.fred.feedex.provider.FeedData;
import net.fred.feedex.provider.FeedData.EntryColumns;
import net.fred.feedex.provider.FeedData.FeedColumns;
import net.fred.feedex.provider.FeedData.FilterColumns;
import net.fred.feedex.provider.FeedDataContentProvider;
import net.fred.feedex.service.FetcherService;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.Html;
import android.util.Pair;

public class RssAtomHandler extends DefaultHandler {

	private static final String URL_SPACE = "%20";
	private static final String ANDRHOMBUS = "&#";
	private static final String HTML_TAG_REGEX = "<(.|\n)*?>";
	private static final String HTML_SPAN_REGEX = "<[/]?[ ]?span(.|\n)*?>";

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
	private static final String TAG_ENCODEDCONTENT = "encoded";
	private static final String TAG_SUMMARY = "summary";
	private static final String TAG_PUBDATE = "pubDate";
	private static final String TAG_DATE = "date";
	private static final String TAG_LASTBUILDDATE = "lastBuildDate";
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

	private static final String[][] TIMEZONES_REPLACE = { {"MEST", "+0200"}, {"EST", "-0500"}, {"PST", "-0800"} };

	private static long KEEP_TIME = 345600000l; // 4 days

	private static final DateFormat[] PUBDATE_DATE_FORMATS = { new SimpleDateFormat("d' 'MMM' 'yyyy' 'HH:mm:ss' 'Z", Locale.US),
			new SimpleDateFormat("d' 'MMM' 'yyyy' 'HH:mm:ss' 'z", Locale.US) };
	
	private static final DateFormat[] UPDATE_DATE_FORMATS = { new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US),
			new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSz", Locale.US) };
	private static final String Z = "Z";
	private static final String GMT = "GMT";

	// middle() is group 1; s* is important for non-whitespaces; ' also usable
	private static final Pattern IMG_PATTERN = Pattern.compile("<img src=\\s*['\"]([^'\"]+)['\"][^>]*>");

	private final Date lastUpdateDate;

	private final String id;

	private boolean entryTagEntered = false;
	private boolean titleTagEntered = false;
	private boolean updatedTagEntered = false;
	private boolean linkTagEntered = false;
	private boolean descriptionTagEntered = false;
	private boolean pubDateTagEntered = false;
	private boolean dateTagEntered = false;
	private boolean lastUpdateDateTagEntered = false;
	private boolean guidTagEntered = false;
	private boolean authorTagEntered = false;

	private StringBuilder title;
	private StringBuilder dateStringBuilder;
	private String feedLink;
	private Date entryDate;
	private StringBuilder entryLink;
	private StringBuilder description;
	private StringBuilder enclosure;
	private final Uri feedEntiresUri;
	private int newCount;
	private final String feedName;
	private String feedTitle;
	private String feedBaseUrl;
	private boolean done = false;
	private final Date keepDateBorder;
	private boolean fetchImages = false;
	private boolean cancelled = false;
	private final long now;
	private StringBuilder guid;
	private StringBuilder author, tmpAuthor;

	private final FeedFilters filters;

	private final ArrayList<ContentProviderOperation> inserts = new ArrayList<ContentProviderOperation>();
	private final ArrayList<Vector<String>> entriesImages = new ArrayList<Vector<String>>();

	public RssAtomHandler(Date lastUpdateDate, final String id, String feedName, String url) {
		KEEP_TIME = Long.parseLong(PrefsManager.getString(PrefsManager.KEEP_TIME, "4")) * 86400000l;
		long keepDateBorderTime = KEEP_TIME > 0 ? System.currentTimeMillis() - KEEP_TIME : 0;

		keepDateBorder = new Date(keepDateBorderTime);
		this.lastUpdateDate = lastUpdateDate;
		this.id = id;
		this.feedName = feedName;
		feedEntiresUri = EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(id);

		filters = new FeedFilters(id);

		final String query = new StringBuilder(EntryColumns.DATE).append('<').append(keepDateBorderTime).append(Constants.DB_AND)
				.append(EntryColumns.WHERE_NOT_FAVORITE).toString();

		FeedData.deletePicturesOfFeed(MainApplication.getAppContext(), feedEntiresUri, query);

		MainApplication.getAppContext().getContentResolver().delete(feedEntiresUri, query, null);
		newCount = 0;

		int index = url.indexOf('/', 8); // this also covers https://

		if (index > -1) {
			feedBaseUrl = url.substring(0, index);
		}

		now = System.currentTimeMillis() - 1000; // by precaution
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
				entryLink = new StringBuilder();

				boolean foundLink = false;

				for (int n = 0, i = attributes.getLength(); n < i; n++) {
					if (ATTRIBUTE_HREF.equals(attributes.getLocalName(n))) {
						if (attributes.getValue(n) != null) {
							entryLink.append(attributes.getValue(n));
							foundLink = true;
							linkTagEntered = false;
						} else {
							linkTagEntered = true;
						}
						break;
					}
				}
				if (!foundLink) {
					linkTagEntered = true;
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
		} else if (TAG_DATE.equals(localName)) {
			dateTagEntered = true;
			dateStringBuilder = new StringBuilder();
		} else if (TAG_LASTBUILDDATE.equals(localName)) {
			lastUpdateDateTagEntered = true;
			dateStringBuilder = new StringBuilder();
		} else if (TAG_ENCODEDCONTENT.equals(localName)) {
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
		if (enclosure == null) { // fetch the first enclosure only
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
		} else if (updatedTagEntered) {
			dateStringBuilder.append(ch, start, length);
		} else if (linkTagEntered) {
			entryLink.append(ch, start, length);
		} else if (descriptionTagEntered) {
			description.append(ch, start, length);
		} else if (pubDateTagEntered) {
			dateStringBuilder.append(ch, start, length);
		} else if (dateTagEntered) {
			dateStringBuilder.append(ch, start, length);
		} else if (lastUpdateDateTagEntered) {
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
				|| (TAG_CONTENT.equals(localName) && !TAG_MEDIA_CONTENT.equals(qName)) || TAG_ENCODEDCONTENT.equals(localName)) {
			descriptionTagEntered = false;
		} else if (TAG_LINK.equals(localName)) {
			linkTagEntered = false;

			if (feedLink == null && !entryTagEntered && TAG_LINK.equals(qName)) { // Skip <atom10:link> tags
				feedLink = entryLink.toString();
			}
		} else if (TAG_UPDATED.equals(localName)) {
			entryDate = parseUpdateDate(dateStringBuilder.toString());
			updatedTagEntered = false;
		} else if (TAG_PUBDATE.equals(localName)) {
			entryDate = parsePubdateDate(dateStringBuilder.toString().replace("  ", " "));
			pubDateTagEntered = false;
		} else if (TAG_LASTBUILDDATE.equals(localName)) {
			entryDate = parsePubdateDate(dateStringBuilder.toString().replace("  ", " "));
			lastUpdateDateTagEntered = false;
		} else if (TAG_DATE.equals(localName)) {
			entryDate = parseUpdateDate(dateStringBuilder.toString());
			dateTagEntered = false;
		} else if (TAG_ENTRY.equals(localName) || TAG_ITEM.equals(localName)) {
			entryTagEntered = false;
			if (title != null && (entryDate == null || ((entryDate.after(lastUpdateDate)) && entryDate.after(keepDateBorder)))) {
				ContentValues values = new ContentValues();

				String improvedTitle = unescapeTitle(title.toString().trim());
				values.put(EntryColumns.TITLE, improvedTitle);

				// Improve the description
				Pair<String, Vector<String>> improvedDesc = improveFeedDescription(description.toString(), fetchImages);
				entriesImages.add(improvedDesc.second);
				if (improvedDesc.first != null) {
					values.put(EntryColumns.ABSTRACT, improvedDesc.first);
				}

				// Try to find if the entry is not filtered and need to be processed
				if (!filters.isEntryFiltered(improvedTitle, improvedDesc.first)) {

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
						if (feedBaseUrl != null && !entryLinkString.startsWith(Constants.HTTP) && !entryLinkString.startsWith(Constants.HTTPS)) {
							entryLinkString = feedBaseUrl
									+ (entryLinkString.startsWith(Constants.SLASH) ? entryLinkString : Constants.SLASH + entryLinkString);
						}
					}

					String[] existanceValues = enclosureString != null ? (guidString != null ? new String[] { entryLinkString, enclosureString,
							guidString } : new String[] { entryLinkString, enclosureString }) : (guidString != null ? new String[] { entryLinkString,
							guidString } : new String[] { entryLinkString });

					// First, try to update the feed
					ContentResolver cr = MainApplication.getAppContext().getContentResolver();
					if ((entryLinkString.length() == 0 && guidString == null)
							|| cr.update(feedEntiresUri, values, existanceStringBuilder.toString(), existanceValues) == 0) {

						// We put the date only for new entry (no need to change the past, you may already read it)
						if (entryDate != null) {
							values.put(EntryColumns.DATE, entryDate.getTime());
						} else {
							values.put(EntryColumns.DATE, now);
						}

						values.put(EntryColumns.LINK, entryLinkString);

						// We cannot update, we need to insert it
						inserts.add(ContentProviderOperation.newInsert(feedEntiresUri).withValues(values).build());

						newCount++;
					} else if (entryDate == null) {
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

	private static Date parseUpdateDate(String string) {
		string = string.replace(Z, GMT);
		for (DateFormat format : UPDATE_DATE_FORMATS) {
			try {
				return format.parse(string);
			} catch (ParseException e) {
			} // just do nothing
		}
		return null;
	}

	private static Date parsePubdateDate(String string) {
		// We remove the first part if necessary (the day display)
		int coma = string.indexOf(", ");
		if (coma != -1) {
			string = string.substring(coma + 2);
		}

		for (String[] timezoneReplace : TIMEZONES_REPLACE) {
			string = string.replace(timezoneReplace[0], timezoneReplace[1]);
		}
		
		for (DateFormat format : PUBDATE_DATE_FORMATS) {
			try {
				return format.parse(string);
			} catch (ParseException e) {
			} // just do nothing
		}
		return null;
	}

	private static String unescapeTitle(String title) {
		String result = title.replace(Constants.AMP_SG, Constants.AMP).replaceAll(HTML_TAG_REGEX, "").replace(Constants.HTML_LT, Constants.LT)
				.replace(Constants.HTML_GT, Constants.GT).replace(Constants.HTML_QUOT, Constants.QUOT)
				.replace(Constants.HTML_APOSTROPHE, Constants.APOSTROPHE);

		if (result.indexOf(ANDRHOMBUS) > -1) {
			return Html.fromHtml(result, null, null).toString();
		} else {
			return result;
		}
	}

	private static Pair<String, Vector<String>> improveFeedDescription(String content, boolean fetchImages) {
		if (content != null) {
			String newContent = content.trim().replaceAll(HTML_SPAN_REGEX, "");

			if (newContent.length() > 0) {
				Vector<String> images = null;
				if (fetchImages) {
					images = new Vector<String>(4);

					Matcher matcher = IMG_PATTERN.matcher(content);

					while (matcher.find()) {
						String match = matcher.group(1).replace(" ", URL_SPACE);

						images.add(match);
						newContent = newContent.replace(match, new StringBuilder(Constants.FILE_URL).append(FeedDataContentProvider.IMAGE_FOLDER)
								.append(Constants.IMAGEID_REPLACEMENT).append(match.substring(match.lastIndexOf('/') + 1)).toString());
					}
				}

				return new Pair<String, Vector<String>>(newContent, images);
			}
		}

		return new Pair<String, Vector<String>>(null, null);
	}

	private static void downloadImages(String entryId, Vector<String> images) {
		if (images != null) {
			FeedDataContentProvider.IMAGE_FOLDER_FILE.mkdir(); // create images dir
			for (String img : images) {
				try {
					byte[] data = FetcherService.getBytes(new URL(img).openStream());

					FileOutputStream fos = new FileOutputStream(new StringBuilder(FeedDataContentProvider.IMAGE_FOLDER).append(entryId)
							.append(Constants.IMAGEFILE_IDSEPARATOR).append(img.substring(img.lastIndexOf('/') + 1)).toString());

					fos.write(data);
					fos.close();
				} catch (Exception e) {
				}
			}
		}
	}

	@Override
	public void endDocument() throws SAXException {
		ContentResolver cr = MainApplication.getAppContext().getContentResolver();

		try {
			if (!inserts.isEmpty()) {
				ContentProviderResult[] results;
				results = cr.applyBatch(FeedData.AUTHORITY, inserts);
				cr.notifyChange(EntryColumns.CONTENT_URI, null);
				FeedDataContentProvider.notifyGroupFromFeedId(id);

				if (fetchImages) {
					for (int i = 0; i < results.length; ++i) {
						Vector<String> images = entriesImages.get(i);
						if (images != null) {
							downloadImages(results[i].uri.getLastPathSegment(), images);
						}
					}
				}
			}

		} catch (Exception e) {
		}

		ContentValues values = new ContentValues();
		if (feedName == null && feedTitle != null) {
			values.put(FeedColumns.NAME, feedTitle.toString().trim());
		}
		values.putNull(FeedColumns.ERROR);
		values.put(FeedColumns.LAST_UPDATE, now);
		if (cr.update(FeedColumns.CONTENT_URI(id), values, null, null) > 0) {
			FeedDataContentProvider.notifyGroupFromFeedId(id);
		}

		super.endDocument();
	}

	private class FeedFilters {
		ArrayList<Pair<String, Pair<Boolean, Boolean>>> mFilters = new ArrayList<Pair<String, Pair<Boolean, Boolean>>>();

		public FeedFilters(String feedId) {
			ContentResolver cr = MainApplication.getAppContext().getContentResolver();
			Cursor c = cr.query(FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(feedId), new String[] { FilterColumns.FILTER_TEXT, FilterColumns.IS_REGEX,
					FilterColumns.IS_APPLIED_TO_TITLE }, null, null, null);
			while (c.moveToNext()) {
				String filterText = c.getString(0);
				boolean isRegex = c.getInt(1) == 1;
				boolean isAppliedToTitle = c.getInt(2) == 1;

				mFilters.add(new Pair<String, Pair<Boolean, Boolean>>(filterText, new Pair<Boolean, Boolean>(isRegex, isAppliedToTitle)));
			}
			c.close();
		}

		public boolean isEntryFiltered(String title, String content) {
			boolean isFiltered = false;

			for (Pair<String, Pair<Boolean, Boolean>> filter : mFilters) {
				String filterText = filter.first;
				boolean isRegex = filter.second.first;
				boolean isAppliedToTitle = filter.second.second;

				if (isRegex) {
					Pattern p = Pattern.compile(filterText);
					if (isAppliedToTitle) {
						Matcher m = p.matcher(title);
						isFiltered = m.find();
					} else {
						Matcher m = p.matcher(content);
						isFiltered = m.find();
					}
				} else if ((isAppliedToTitle && title.contains(filterText)) || (!isAppliedToTitle && content.contains(filterText))) {
					isFiltered = true;
				}
			}

			return isFiltered;
		}
	}
}
