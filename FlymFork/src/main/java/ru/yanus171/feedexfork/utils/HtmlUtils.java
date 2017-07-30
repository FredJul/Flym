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

package ru.yanus171.feedexfork.utils;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.service.FetcherService;

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

    public static final String URL_SPACE = "%20";

    private static final Pattern IMG_PATTERN = Pattern.compile("<img\\s+[^>]*src=\\s*['\"]([^'\"]+)['\"][^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ADS_PATTERN = Pattern.compile("<div class=('|\")mf-viral('|\")><table border=('|\")0('|\")>.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern LAZY_LOADING_PATTERN = Pattern.compile("\\s+src=[^>]+\\s+original[-]*src=(\"|')", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMPTY_IMAGE_PATTERN = Pattern.compile("<img\\s+(height=['\"]1['\"]\\s+width=['\"]1['\"]|width=['\"]1['\"]\\s+height=['\"]1['\"])\\s+[^>]*src=\\s*['\"]([^'\"]+)['\"][^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern NON_HTTP_IMAGE_PATTERN = Pattern.compile("\\s+(href|src)=(\"|')//", Pattern.CASE_INSENSITIVE);
    private static final Pattern BAD_IMAGE_PATTERN = Pattern.compile("<img\\s+[^>]*src=\\s*['\"]([^'\"]+)\\.img['\"][^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern START_BR_PATTERN = Pattern.compile("^(\\s*<br\\s*[/]*>\\s*)*", Pattern.CASE_INSENSITIVE);
    private static final Pattern END_BR_PATTERN = Pattern.compile("(\\s*<br\\s*[/]*>\\s*)*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern MULTIPLE_BR_PATTERN = Pattern.compile("(\\s*<br\\s*[/]*>\\s*){3,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMPTY_LINK_PATTERN = Pattern.compile("<a\\s+[^>]*></a>", Pattern.CASE_INSENSITIVE);
    //public static boolean mIsDownloadingImagesForEntryView = false;


    public static String improveHtmlContent(String content, String baseUri) {
        content = ADS_PATTERN.matcher(content).replaceAll("");

        if (content != null) {
            // remove some ads
            content = ADS_PATTERN.matcher(content).replaceAll("");
            // remove lazy loading images stuff
            content = LAZY_LOADING_PATTERN.matcher(content).replaceAll(" src=$1");

            // clean by JSoup
            content = Jsoup.clean(content, baseUri, JSOUP_WHITELIST);

            // remove empty or bad images
            content = EMPTY_IMAGE_PATTERN.matcher(content).replaceAll("");
            content = BAD_IMAGE_PATTERN.matcher(content).replaceAll("");
            // remove empty links
            content = EMPTY_LINK_PATTERN.matcher(content).replaceAll("");
            // fix non http image paths
            content = NON_HTTP_IMAGE_PATTERN.matcher(content).replaceAll(" $1=$2http://");
            // remove trailing BR & too much BR
            content = START_BR_PATTERN.matcher(content).replaceAll("");
            content = END_BR_PATTERN.matcher(content).replaceAll("");
            content = MULTIPLE_BR_PATTERN.matcher(content).replaceAll("<br><br>");
        }

        return content;
    }

    public static ArrayList<String> getImageURLs(String content) {
        ArrayList<String> images = new ArrayList<>();

        if (!TextUtils.isEmpty(content)) {
            Matcher matcher = IMG_PATTERN.matcher(content);
            int index = 0;
            while (matcher.find() && ( ( index < FetcherService.mMaxImageDownloadCount ) || ( FetcherService.mMaxImageDownloadCount == 0 ) ) ) {
                images.add(matcher.group(1).replace(" ", URL_SPACE));
                index++;
            }
            FetcherService.mMaxImageDownloadCount = PrefUtils.getImageDownloadCount();
        }


        return images;
    }

    public static String replaceImageURLs(String content, final long entryId) {

        if (!TextUtils.isEmpty(content)) {
            boolean needDownloadPictures = PrefUtils.getBoolean(PrefUtils.DISPLAY_IMAGES, true);//NetworkUtils.needDownloadPictures();
            final ArrayList<String> imagesToDl = new ArrayList<>();

            Matcher matcher = IMG_PATTERN.matcher(content);
            int index = 0;
            while ( matcher.find()  ) {
                index++;
                String match = matcher.group(1);
                //String match1 = matcher.group(1).replace(" ", URL_SPACE);
                String imgPath = NetworkUtils.getDownloadedImagePath(entryId, match);

                if (new File(imgPath).exists()) {
                    content = content.replace(match, Constants.FILE_SCHEME + imgPath);
                } else if (needDownloadPictures) {
                    if ( ( index < FetcherService.mMaxImageDownloadCount ) || ( FetcherService.mMaxImageDownloadCount == 0 ) ) {
                        imagesToDl.add(match);
                        content = content.replace(match, Constants.FILE_SCHEME + imgPath);
                    } else {
                        String match0 = matcher.group(0);
                        String html = getButtonHtml("downloadImage('" + match + "')" , getString( R.string.downloadOneImage ) ) +
                                      getButtonHtml("downloadNextImages()" , getString( R.string.downloadNext ) + PrefUtils.getImageDownloadCount() ) + "<br/>";

                        content = content.replace(match0, html + match0.replace(match, Constants.FILE_SCHEME + imgPath));
                    }
                }
            }

            //FetcherService.mMaxImageDownloadCount = PrefUtils.getImageDownloadCount();

            // Download the images if needed
            if (!imagesToDl.isEmpty() ) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                    //FetcherService.addImagesToDownload(String.valueOf(entryId), imagesToDl);
                    FetcherService.downloadEntryImages( entryId, imagesToDl );
                }
                }).start();
            }

        }

        return content;
    }

    private static String getString( int id ) {
        return MainApplication.getContext().getString(id);

    }
    @NonNull
    private static String getButtonHtml(String methodName, String caption) {
        final String BUTTON_START = "<i onclick=\"";
        //final String BUTTON_MIDDLE = " onclick=\"";
        final String BUTTON_END = "\">" + caption + "   </i>";
        //String html = BUTTON_SECTION_START + BUTTON_START + "Download image" + BUTTON_MIDDLE + "ImageDownloadJavaScriptObject.downloadImage('" + match + "');" + BUTTON_END + BUTTON_SECTION_END;
        return BUTTON_START + "ImageDownloadJavaScriptObject." + methodName + ";" + BUTTON_END;
    }

    public static String getMainImageURL(String content) {
        if (!TextUtils.isEmpty(content)) {
            Matcher matcher = IMG_PATTERN.matcher(content);

            while (matcher.find()) {
                String imgUrl = matcher.group(1).replace(" ", URL_SPACE);
                if (isCorrectImage(imgUrl)) {
                    return imgUrl;
                }
            }
        }

        return null;
    }

    public static String getMainImageURL(ArrayList<String> imgUrls) {
        for (String imgUrl : imgUrls) {
            if (isCorrectImage(imgUrl)) {
                return imgUrl;
            }
        }

        return null;
    }

    private static boolean isCorrectImage(String imgUrl) {
        if (!imgUrl.endsWith(".gif") && !imgUrl.endsWith(".GIF") && !imgUrl.endsWith(".img") && !imgUrl.endsWith(".IMG")) {
            return true;
        }

        return false;
    }
}
