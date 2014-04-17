/**
 * FeedEx
 *
 * Copyright (c) 2012-2013 Frederic Julian
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

package net.fred.feedex.utils;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import net.fred.feedex.Constants;
import net.fred.feedex.MainApplication;
import net.fred.feedex.service.FetcherService;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlUtils {

    private static final Whitelist JSOUP_WHITELIST = Whitelist.relaxed().addTags("iframe", "video", "audio", "source", "track")
            .addAttributes("iframe", "src", "frameborder", "height", "width")
            .addAttributes("video", "src", "controls", "height", "width", "poster")
            .addAttributes("audio", "src", "controls")
            .addAttributes("source", "src", "type")
            .addAttributes("track", "src", "kind", "srclang", "label");

    // middle() is group 1; s* is important for non-whitespaces; ' also usable
    private static final Pattern IMG_PATTERN = Pattern.compile("<img\\s+[^>]*src=\\s*['\"]([^'\"]+)['\"][^>]*>", Pattern.CASE_INSENSITIVE);
    private static final String URL_SPACE = "%20";

    public static String improveHtmlContent(String content, String baseUri) {
        if (content != null) {
            // remove some ads
            content = content.replaceAll("(?i)<div class=('|\")mf-viral('|\")><table border=('|\")0('|\")>.*", "");
            // remove lazy loading images stuff
            content = content.replaceAll("(?i)\\s+src=[^>]+\\s+original[-]*src=(\"|')", " src=$1");
            // remove bad image paths
            content = content.replaceAll("(?i)\\s+(href|src)=(\"|')//", " $1=$2http://");
            // clean by jsoup
            content = Jsoup.clean(content, baseUri, JSOUP_WHITELIST);
        }

        return content;
    }

    public static ArrayList<String> getImageURLs(String content) {
        ArrayList<String> images = new ArrayList<String>();

        if (!TextUtils.isEmpty(content)) {
            Matcher matcher = IMG_PATTERN.matcher(content);

            while (matcher.find()) {
                images.add(matcher.group(1).replace(" ", URL_SPACE));
            }
        }

        return images;
    }

    public static String replaceImageURLs(String content, final long entryId) {

        if (!TextUtils.isEmpty(content)) {
            boolean needDownloadPictures = NetworkUtils.needDownloadPictures();
            final ArrayList<String> imagesToDl = new ArrayList<String>();

            Matcher matcher = IMG_PATTERN.matcher(content);
            while (matcher.find()) {
                String match = matcher.group(1).replace(" ", URL_SPACE);

                String imgPath = NetworkUtils.getDownloadedImagePath(entryId, match);
                if (new File(imgPath).exists()) {
                    content = content.replace(match, Constants.FILE_SCHEME + imgPath);
                } else if (needDownloadPictures) {
                    imagesToDl.add(match);
                }
            }

            // Download the images if needed
            if (!imagesToDl.isEmpty()) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        FetcherService.addImagesToDownload(String.valueOf(entryId), imagesToDl);
                        Context context = MainApplication.getContext();
                        context.startService(new Intent(context, FetcherService.class).setAction(FetcherService.ACTION_DOWNLOAD_IMAGES));
                    }
                }).start();
            }
        }

        return content;
    }
}
