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

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.ArticleTextExtractor;

import ru.yanus171.feedexfork.provider.FeedData.EntryColumns;
import ru.yanus171.feedexfork.provider.FeedData.FeedColumns;

import ru.yanus171.feedexfork.utils.Dog;



public class HTMLParser {
	static public void parse( long feedID, String feedUrl, String content ) {
		if (!TextUtils.isEmpty(content)) {

            FetcherService.Status().ChangeProgress( "Loading main page");

            Uri uriMainEntry = FetcherService.LoadLink( feedID, feedUrl, "", FetcherService.ForceReload.Yes );
            
			ContentResolver cr = MainApplication.getContext().getContentResolver();
			Cursor cursor = cr.query(uriMainEntry, new String[]{EntryColumns.TITLE}, null, null, null);
            if (cursor.moveToFirst()) {
				ContentValues values = new ContentValues();
				values.put( FeedColumns.NAME, cursor.getString( 0 ) );
				cr.update( FeedColumns.CONTENT_URI( feedID ), values, FeedColumns.NAME + Constants.DB_IS_NULL, null );
			}
			cursor.close();

			FeedFilters filters = new FeedFilters(String.valueOf( feedID ) );

			Document doc = Jsoup.parse(content);
            content  = ArticleTextExtractor.extractContent(doc, feedUrl, null, ArticleTextExtractor.Mobilize.Yes);
            doc = Jsoup.parse(content);
            Elements list = doc.select("a");
            final Pattern BASE_URL = Pattern.compile("(http|https).\\/\\/[^\\/]+\\/");
			for ( Element el : list ) {
				if ( FetcherService.isCancelRefresh() )
					break;
				int status = FetcherService.Status().Start(String.format( "Loading page %d/%d", list.indexOf( el ) + 1, list.size() ) ); try {
					String link = el.attr("href");
					//Dog.v("link = " + link);
					Matcher matcher = BASE_URL.matcher(link);
                    if ( !matcher.find() ) {
                        matcher = BASE_URL.matcher(feedUrl);
                        if ( matcher.find() ) {
                            link = matcher.group() + link;
                        }
                    }
					//Dog.v("link2 = " + link);
					if ( link.endsWith( ".pdf" ) )
						continue;

					if( filters.isEntryFiltered(el.text(), "", link, "" ) )
						continue;

					FetcherService.LoadLink(feedID, link, el.text(), FetcherService.ForceReload.No);
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
				Document doc = Jsoup.parse(matcher.group(), null, "");
                //String link = matcher.group().replace( "<a href=\"", "" );
				FetcherService.OpenExternalLink( link, intent.getStringExtra( Constants.TITLE_TO_LOAD ), null  );
            }*/

        }	
	}
}