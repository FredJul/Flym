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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.text.Html;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.view.EntryView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Pattern;

public class NetworkUtils {

    //public static final File IMAGE_FOLDER_FILE = new File(MainApplication.getContext().getCacheDir(), "images/");
    //public static final String IMAGE_FOLDER = IMAGE_FOLDER_FILE.getAbsolutePath() + '/';
    public static final String TEMP_PREFIX = "TEMP__";
    //public static final File FOLDER = new File(Environment.getExternalStorageDirectory(), "feedex/");
    public static final String ID_SEPARATOR = "__";

    private static final String FILE_FAVICON = "/favicon.ico";
    private static final String PROTOCOL_SEPARATOR = "://";

    private static final CookieManager COOKIE_MANAGER = new CookieManager() {{
        CookieHandler.setDefault(this);
    }};

    public static String getDownloadedOrDistantImageUrl(long entryId, String imgUrl) {
        File dlImgFile = new File(NetworkUtils.getDownloadedImagePath(entryId, imgUrl));
        if (dlImgFile.exists()) {
            return Uri.fromFile(dlImgFile).toString();
        } else {
            return null;//imgUrl;
        }
    }

    public static String getDownloadedImagePath(long entryId, String imgUrl) {
        return FileUtils.GetImagesFolder().getAbsolutePath() + "/" + entryId + ID_SEPARATOR + StringUtils.getMd5(imgUrl.replace(" ", HtmlUtils.URL_SPACE));
    }

    private static String getTempDownloadedImagePath(long entryId, String imgUrl) {
        return FileUtils.GetImagesFolder().getAbsolutePath() + "/" + TEMP_PREFIX + entryId + ID_SEPARATOR + StringUtils.getMd5(imgUrl);
    }

    public static void downloadImage(final long entryId, String imgUrl/*, boolean updateGUI*/ ) throws IOException {
        if ( FetcherService.isCancelRefresh() )
            return;
        String tempImgPath = getTempDownloadedImagePath(entryId, imgUrl);
        String finalImgPath = getDownloadedImagePath(entryId, imgUrl);

        if (!new File(tempImgPath).exists() && !new File(finalImgPath).exists()) {
            HttpURLConnection imgURLConnection = null;
            try {
                //IMAGE_FOLDER_FILE.mkdir(); // create images dir

                // Compute the real URL (without "&eacute;", ...)
                String realUrl = Html.fromHtml(imgUrl).toString();
                imgURLConnection = setupConnection(realUrl);



                FileOutputStream fileOutput = new FileOutputStream(tempImgPath);
                InputStream inputStream = imgURLConnection.getInputStream();

                int bytesRecieved = 0;
                int progressBytes = 0;
                final int cStep = 1024 * 10;
                byte[] buffer = new byte[2048];
                int bufferLength;
                FetcherService.getObservable().ChangeProgress(getProgressText(bytesRecieved));
                while (!FetcherService.isCancelRefresh() && (bufferLength = inputStream.read(buffer)) > 0) {
                    fileOutput.write(buffer, 0, bufferLength);
                    bytesRecieved += bufferLength;
                    progressBytes += bufferLength;
                    if (progressBytes >= cStep) {
                        progressBytes = 0;
                        FetcherService.getObservable().ChangeProgress(getProgressText(bytesRecieved));
                    }
                }
                FetcherService.getObservable().AddBytes( bytesRecieved );
                fileOutput.flush();
                fileOutput.close();
                inputStream.close();

                new File(tempImgPath).renameTo(new File(finalImgPath));

            } catch (IOException e) {
                new File(tempImgPath).delete();
                throw e;
            } finally {
                if (imgURLConnection != null) {
                    imgURLConnection.disconnect();
                }
            }
        }
        //if ( updateGUI )
            EntryView.NotifyToUpdate( entryId );
    }

    private static String getProgressText(int bytesRecieved) {
        return String.format("%d KB ...", bytesRecieved / 1024);
    }

    public static synchronized void deleteEntriesImagesCache(Uri entriesUri, String selection, String[] selectionArgs) {
        if (FileUtils.GetImagesFolder().exists()) {
            PictureFilenameFilter filenameFilter = new PictureFilenameFilter();

            Cursor cursor = MainApplication.getContext().getContentResolver().query(entriesUri, FeedData.EntryColumns.PROJECTION_ID, selection, selectionArgs, null);

            while (cursor.moveToNext()) {
                filenameFilter.setEntryId(cursor.getString(0));

                File[] files = FileUtils.GetImagesFolder().listFiles(filenameFilter);
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
            }
            cursor.close();
        }
    }

    public static boolean needDownloadPictures() {
        String fetchPictureMode = PrefUtils.getString(PrefUtils.PRELOAD_IMAGE_MODE, Constants.FETCH_PICTURE_MODE_ALWAYS_PRELOAD);

        boolean downloadPictures = false;
        if (PrefUtils.getBoolean(PrefUtils.DISPLAY_IMAGES, true)) {
            if (Constants.FETCH_PICTURE_MODE_ALWAYS_PRELOAD.equals(fetchPictureMode)) {
                downloadPictures = true;
            } else if (Constants.FETCH_PICTURE_MODE_WIFI_ONLY_PRELOAD.equals(fetchPictureMode)) {
                ConnectivityManager cm = (ConnectivityManager) MainApplication.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo ni = cm.getActiveNetworkInfo();
                if (ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI) {
                    downloadPictures = true;
                }
            }
        }
        return downloadPictures;
    }

    public static String getBaseUrl(String link) {
        String baseUrl = link;
        int index = link.indexOf('/', 8); // this also covers https://
        if (index > -1) {
            baseUrl = link.substring(0, index);
        }

        return baseUrl;
    }

    public static byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        byte[] buffer = new byte[4096];

        int n;
        while ((n = inputStream.read(buffer)) > 0) {
            FetcherService.getObservable().AddBytes( n );
            output.write(buffer, 0, n);
        }

        byte[] result = output.toByteArray();

        output.close();
        inputStream.close();
        return result;
    }

    public static void retrieveFavicon(Context context, URL url, String id) {
        boolean success = false;
        HttpURLConnection iconURLConnection = null;

        try {
            iconURLConnection = setupConnection(new URL(url.getProtocol() + PROTOCOL_SEPARATOR + url.getHost() + FILE_FAVICON));

            byte[] iconBytes = getBytes(iconURLConnection.getInputStream());
            if (iconBytes != null && iconBytes.length > 0) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
                if (bitmap != null) {
                    if (bitmap.getWidth() != 0 && bitmap.getHeight() != 0) {
                        ContentValues values = new ContentValues();
                        values.put(FeedData.FeedColumns.ICON, iconBytes);
                        context.getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(id), values, null, null);
                        success = true;
                    }
                    bitmap.recycle();
                }
            }
        } catch (Throwable ignored) {
        } finally {
            if (iconURLConnection != null) {
                iconURLConnection.disconnect();
            }
        }

        if (!success) {
            // no icon found or error
            ContentValues values = new ContentValues();
            values.putNull(FeedData.FeedColumns.ICON);
            context.getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(id), values, null, null);
        }
    }

    public static HttpURLConnection setupConnection(String url) throws IOException {
        return setupConnection(new URL(url));
    }

    public static HttpURLConnection setupConnection(URL url) throws IOException {
        HttpURLConnection connection;
        FetcherService.getObservable().ChangeProgress(R.string.setupConnection);

        connection = new OkUrlFactory(new OkHttpClient()).open(url);

        connection.setDoInput(true);
        connection.setDoOutput(false);
        connection.setRequestProperty("User-agent", "Mozilla/5.0 (compatible) AppleWebKit Chrome Safari"); // some feeds need this to work properly
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("accept", "*/*");

        COOKIE_MANAGER.getCookieStore().removeAll(); // Cookie is important for some sites, but we clean them each times
        connection.connect();
        FetcherService.getObservable().ChangeProgress("");
        return connection;
    }

    private static class PictureFilenameFilter implements FilenameFilter {
        private static final String REGEX = "__[^\\.]*\\.[A-Za-z]*";

        private Pattern mPattern;

        public PictureFilenameFilter(String entryId) {
            setEntryId(entryId);
        }

        public PictureFilenameFilter() {
        }

        public void setEntryId(String entryId) {
            mPattern = Pattern.compile(entryId + REGEX);
        }

        @Override
        public boolean accept(File dir, String filename) {
            return mPattern.matcher(filename).find();
        }
    }
}
