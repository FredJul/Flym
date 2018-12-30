/**
 * Flym
 * <p/>
 * Copyright (c) 2012-2015 Frederic Julian
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * <p/>
 * <p/>
 * Some parts of this software are based on "Sparse rss" under the MIT license (see
 * below). Please refers to the original project to identify which parts are under the
 * MIT license.
 * <p/>
 * Copyright (c) 2010-2012 Stefan Handschuh
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package ru.yanus171.feedexfork.parser;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.Html;
import android.text.TextUtils;
import android.util.Pair;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.service.MarkItem;
import ru.yanus171.feedexfork.utils.ArticleTextExtractor;

import ru.yanus171.feedexfork.provider.FeedData.EntryColumns;
import ru.yanus171.feedexfork.provider.FeedData.FeedColumns;

import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.NetworkUtils;


public class HTMLParser {
	static public int Parse(final String feedID, String feedUrl ) {
		//if (!TextUtils.isEmpty(content)) {
		int result = 0;
            FetcherService.Status().ChangeProgress( "Loading main page");

		/* check and optionally find favicon */
		try {
			NetworkUtils.retrieveFavicon(MainApplication.getContext(), new URL(feedUrl), feedID);
		} catch (Throwable ignored) {
		}


		HttpURLConnection connection = null;
		Document doc = null;
		try {
			connection = NetworkUtils.setupConnection(feedUrl);
			doc = Jsoup.parse(connection.getInputStream(), null, "");
		} catch (Exception e) {
			FetcherService.Status().SetError( e.getLocalizedMessage(), e );
		} finally {
			if (connection != null)
				connection.disconnect();
		}

		Uri uriMainEntry = FetcherService.LoadLink( feedID, feedUrl, "", FetcherService.ForceReload.Yes, true).first;
            
		ContentResolver cr = MainApplication.getContext().getContentResolver();
		{
			Cursor cursor = cr.query(uriMainEntry, new String[]{EntryColumns.TITLE}, null, null, null);
			if (cursor.moveToFirst()) {
				ContentValues values = new ContentValues();
				values.put(FeedColumns.NAME, cursor.getString(0));
				cr.update(FeedColumns.CONTENT_URI(feedID), values, FeedColumns.NAME + Constants.DB_IS_NULL, null);
			}
			cursor.close();
		}
		FeedFilters filters = new FeedFilters(String.valueOf( feedID ) );

		class Item {
			public String mUrl;
			public String mCaption;
			public Item( String url, String caption ){
				mUrl = url;
				mCaption = caption;
			}
		}
		ArrayList<Item> listItem = new ArrayList<Item>();
		String content = ArticleTextExtractor.extractContent(doc, feedUrl, null, ArticleTextExtractor.MobilizeType.Yes, false);
		doc = Jsoup.parse(content);
		{
			Elements list = doc.select("a");
			final Pattern BASE_URL = Pattern.compile("(http|https).[\\/]+[^/]+");
			for (Element el : list) {
				if (FetcherService.isCancelRefresh())
					break;
				String link = el.attr("href");
				Dog.v("link before = " + link);
				Matcher matcher = BASE_URL.matcher(link);
				if (!matcher.find()) {
					matcher = BASE_URL.matcher(feedUrl);
					if (matcher.find()) {
						link = matcher.group() + link;
						link = link.replace( "//", "/" );
					}
				}
				Dog.v("link after = " + link);
				if (link.endsWith(".pdf") || link.endsWith(".epub") || link.endsWith(".doc")  || link.endsWith(".docx"))
					continue;

				if (filters.isEntryFiltered(el.text(), "", link, ""))
					continue;

				listItem.add(new Item(link, el.text()));

			}
		}
		for ( Item item: listItem ) {
			if ( FetcherService.isCancelRefresh() )
				break;
			int status = FetcherService.Status().Start(String.format( "Loading page %d/%d", listItem.indexOf( item ) + 1, listItem.size() ) ); try {
				Pair<Uri, Boolean> load = FetcherService.LoadLink(feedID, item.mUrl, item.mCaption, FetcherService.ForceReload.No, true);
				Uri uri = load.first;
				if ( load.second ) {
					result++;
					Cursor cursor = cr.query(uri, new String[]{EntryColumns.TITLE, EntryColumns.AUTHOR}, null, null, null);
					cursor.moveToFirst();

					if (filters.isMarkAsStarred(cursor.getString(0), cursor.getString(1), item.mUrl, "")) {
						synchronized ( FetcherService.mMarkAsStarredFoundList ) {
							FetcherService.mMarkAsStarredFoundList.add(new MarkItem(feedID, cursor.getString(0),  item.mUrl));
						}
						{
							ContentValues values = new ContentValues();
							values.put(EntryColumns.IS_FAVORITE, 1);
							cr.update(uri, values, null, null);
						}

					}
					cursor.close();

				}
			} finally { FetcherService.Status().End(status); }
		}
		synchronized ( FetcherService.mCancelRefresh ) {
			FetcherService.mCancelRefresh = false;
		}

		{
			ContentValues values = new ContentValues();
			values.put( FeedColumns.LAST_UPDATE, System.currentTimeMillis() );
			cr.update( FeedColumns.CONTENT_URI( feedID ), values, null, null );
		}
		{
			ContentValues values = new ContentValues();
			values.put( EntryColumns.DATE, System.currentTimeMillis() );
			values.put( EntryColumns.SCROLL_POS, 0 );
			values.putNull( EntryColumns.IS_READ );
			cr.update( uriMainEntry, values, null, null );
		}
		// img in a tag
		/*Matcher matcher = Pattern.compile("<a href=[^>]+>(.)+?</a>").matcher(content);
		while ( matcher.find() ) {
			Document doc = Jsoup.Parse(matcher.group(), null, "");
			//String link = matcher.group().replace( "<a href=\"", "" );
			FetcherService.OpenExternalLink( link, intent.getStringExtra( Constants.TITLE_TO_LOAD ), null  );
		}*/


		return result;
	}
}