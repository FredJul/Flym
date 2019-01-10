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
 */

package ru.yanus171.feedexfork.activity;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.util.Date;
import java.util.regex.Matcher;

import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.service.FetcherService.ForceReload;
import ru.yanus171.feedexfork.utils.HtmlUtils;
import ru.yanus171.feedexfork.utils.Timer;
import ru.yanus171.feedexfork.utils.UiUtils;

import static ru.yanus171.feedexfork.service.FetcherService.GetEnryUri;

public class LoadLinkActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UiUtils.setPreferenceTheme(this);
        super.onCreate(savedInstanceState);
        final String TEXT = MainApplication.getContext().getString(R.string.loadingLink) + "...";

        final Intent intent = getIntent();
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_SEND) && intent.hasExtra(Intent.EXTRA_TEXT)) {
            final String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            final Matcher m = HtmlUtils.HTTP_PATTERN.matcher(text);
            if (m.find()) {
                final String url = text.substring(m.start(), m.end());
                final String title = text.substring(0, m.start());
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        LoadAndOpenLink(url, title, TEXT);
                    }
                }).start();
            }
        } else if (intent.getScheme() != null && intent.getScheme().startsWith("http"))
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final String url = intent.getDataString();
                    final String title = intent.getDataString();
                    LoadAndOpenLink(url, title, TEXT);
                }
            }).start();
        setContentView( android.R.layout.browser_link_context_header );
        TextView view = (TextView) findViewById( android.R.id.title );
        view.setText( R.string.loadingLink );
        finish();
    }

        private void LoadAndOpenLink(final String url, final String title, final String text) {
            final String feedID = FetcherService.GetExtrenalLinkFeedID();
            final ContentResolver cr = MainApplication.getContext().getContentResolver();
            final ForceReload forceReload;
            Uri entryUri = GetEnryUri( url );
            if ( entryUri == null ) {
                Timer timer = new Timer( "LoadAndOpenLink insert" );
                ContentValues values = new ContentValues();
                values.put(FeedData.EntryColumns.TITLE, title);
                values.put(FeedData.EntryColumns.SCROLL_POS, 0);
                values.put(FeedData.EntryColumns.DATE, (new Date()).getTime());
                values.put(FeedData.EntryColumns.LINK, url);
                values.put(FeedData.EntryColumns.ABSTRACT, text );
                values.put(FeedData.EntryColumns.MOBILIZED_HTML, text );
                entryUri = cr.insert(FeedData.EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(feedID), values);
                forceReload = ForceReload.Yes;
                timer.End();
            } else
                forceReload = ForceReload.No;
            FetcherService.OpenLink(entryUri);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    FetcherService.LoadLink(feedID, url, title, forceReload,true);
                }
            }).start();
    }

}

