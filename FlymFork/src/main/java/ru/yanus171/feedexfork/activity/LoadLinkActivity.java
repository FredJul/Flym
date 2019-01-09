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

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.util.regex.Matcher;

import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.HtmlUtils;

public class LoadLinkActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_SEND) && intent.hasExtra(Intent.EXTRA_TEXT)) {
            final String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            final Matcher m = HtmlUtils.HTTP_PATTERN.matcher(text);
            if (m.find()) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        FetcherService.OpenLink(FetcherService.LoadLink(FetcherService.GetExtrenalLinkFeedID(),
                                text.substring(m.start(), m.end()),
                                text.substring(0, m.start()),
                                FetcherService.ForceReload.No,
                                true).first);
                    }
                }).start();

            }
        } else if (intent.getScheme() != null && intent.getScheme().startsWith("http"))
            new Thread(new Runnable() {
                @Override
                public void run() {
                    FetcherService.OpenLink(FetcherService.LoadLink(FetcherService.GetExtrenalLinkFeedID(),
                            intent.getDataString(),
                            intent.getDataString(),
                            FetcherService.ForceReload.No,
                            true).first);
                }
            }).start();



        finish();

    }

}

