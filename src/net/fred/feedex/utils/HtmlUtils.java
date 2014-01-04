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

import android.util.Pair;

import net.fred.feedex.Constants;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlUtils {

    private static final Whitelist JSOUP_WHITELIST = Whitelist.basicWithImages().addTags("iframe", "video", "audio", "source", "track")
            .addAttributes("iframe", "src", "frameborder", "height", "width")
            .addAttributes("video", "src", "controls", "height", "width", "poster")
            .addAttributes("audio", "src", "controls")
            .addAttributes("source", "src", "type")
            .addAttributes("track", "src", "kind", "srclang", "label");

    // middle() is group 1; s* is important for non-whitespaces; ' also usable
    private static final Pattern IMG_PATTERN = Pattern.compile("<img\\s+[^>]*src=\\s*['\"]([^'\"]+)['\"][^>]*>", Pattern.CASE_INSENSITIVE);
    private static final String URL_SPACE = "%20";

    public static Pair<String, Vector<String>> improveHtmlContent(String content, String baseUri, boolean fetchImages) {
        if (content != null) {
            // remove lazy loading images stuff
            content = content.replaceAll("(?i)\\s+src=[^>]+\\s+original[-]*src=(\"|')", " src=$1");
            // clean by jsoup
            content = Jsoup.clean(content, baseUri, JSOUP_WHITELIST);
            // remove bad image paths
            content = content.replaceAll("(?i)\\s+(href|src)=(\"|')//", " $1=$2http://");

            if (content.length() > 0) {
                Vector<String> images = null;
                if (fetchImages) {
                    images = new Vector<String>(4);

                    Matcher matcher = IMG_PATTERN.matcher(content);

                    while (matcher.find()) {
                        String match = matcher.group(1).replace(" ", URL_SPACE);

                        images.add(match);
                        try {
                            // replace the '%' that may occur while urlencode such that the img-src url (in the abstract text) does reinterpret the
                            // parameters
                            content = content.replace(
                                    match,
                                    (Constants.FILE_URL + NetworkUtils.IMAGE_FOLDER + Constants.IMAGEID_REPLACEMENT + URLEncoder.encode(
                                            match.substring(match.lastIndexOf('/') + 1), Constants.UTF8)).replace(NetworkUtils.PERCENT, NetworkUtils.PERCENT_REPLACE));
                        } catch (UnsupportedEncodingException e) {
                            // UTF-8 should be supported
                        }
                    }
                }

                return new Pair<String, Vector<String>>(content, images);
            }
        }

        return new Pair<String, Vector<String>>(null, null);
    }
}
