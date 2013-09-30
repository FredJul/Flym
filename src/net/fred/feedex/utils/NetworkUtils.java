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
 */

package net.fred.feedex.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;

import net.fred.feedex.Constants;
import net.fred.feedex.MainApplication;
import net.fred.feedex.provider.FeedData;
import net.fred.feedex.provider.FeedDataContentProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class NetworkUtils {

    public static final File IMAGE_FOLDER_FILE = new File(MainApplication.getContext().getCacheDir(), "images/");
    public static final String IMAGE_FOLDER = IMAGE_FOLDER_FILE.getAbsolutePath() + '/';

    public static final String PERCENT = "%";
    // This can be any valid filename character sequence which does not contain '%'
    public static final String PERCENT_REPLACE = "____";

    private static final String GZIP = "gzip";
    private static final String FILE_FAVICON = "/favicon.ico";
    private static final String PROTOCOL_SEPARATOR = "://";
    private static final String _HTTP = "http";
    private static final String _HTTPS = "https";

    private static class PictureFilenameFilter implements FilenameFilter {
        private static final String REGEX = "__[^\\.]*\\.[A-Za-z]*";

        private Pattern pattern;

        public PictureFilenameFilter(String entryId) {
            setEntryId(entryId);
        }

        public PictureFilenameFilter() {
        }

        public void setEntryId(String entryId) {
            pattern = Pattern.compile(entryId + REGEX);
        }

        @Override
        public boolean accept(File dir, String filename) {
            return pattern.matcher(filename).find();
        }
    }

    public static void downloadImage(long entryId, String imgPath) throws IOException {
        IMAGE_FOLDER_FILE.mkdir(); // create images dir

        byte[] data = getBytes(new URL(imgPath).openStream());

        // see the comment where the img regex is executed for details about this replacement
        FileOutputStream fos = new FileOutputStream((IMAGE_FOLDER + entryId + Constants.IMAGEFILE_IDSEPARATOR + URLEncoder.encode(
                imgPath.substring(imgPath.lastIndexOf('/') + 1), Constants.UTF8)).replace(PERCENT, PERCENT_REPLACE));

        fos.write(data);
        fos.close();
    }

    public static synchronized void deleteFeedImagesCache(Uri entriesUri, String selection) {
        if (IMAGE_FOLDER_FILE.exists()) {
            PictureFilenameFilter filenameFilter = new PictureFilenameFilter();

            Cursor cursor = MainApplication.getContext().getContentResolver().query(entriesUri, FeedData.EntryColumns.PROJECTION_ID, selection, null, null);

            while (cursor.moveToNext()) {
                filenameFilter.setEntryId(cursor.getString(0));

                File[] files = IMAGE_FOLDER_FILE.listFiles(filenameFilter);
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
            }
            cursor.close();
        }
    }

    public static byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        byte[] buffer = new byte[4096];

        int n;
        while ((n = inputStream.read(buffer)) > 0) {
            output.write(buffer, 0, n);
        }

        byte[] result = output.toByteArray();

        output.close();
        inputStream.close();
        return result;
    }

    public static void retrieveFavicon(Context context, URL url, String id) {
        try {
            HttpURLConnection iconURLConnection = setupConnection(new URL(url.getProtocol() + PROTOCOL_SEPARATOR + url.getHost() + FILE_FAVICON));

            ContentValues values = new ContentValues();
            try {
                byte[] iconBytes = getBytes(getConnectionInputStream(iconURLConnection));
                values.put(FeedData.FeedColumns.ICON, iconBytes);
            } catch (Exception e) {
                // no icon found or error
                values.put(FeedData.FeedColumns.ICON, new byte[0]);
            } finally {
                iconURLConnection.disconnect();

                context.getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(id), values, null, null);
                FeedDataContentProvider.notifyGroupFromFeedId(id);
            }
        } catch (Throwable ignored) {
        }
    }

    public static HttpURLConnection setupConnection(String url) throws IOException {
        return setupConnection(new URL(url));
    }

    public static HttpURLConnection setupConnection(URL url) throws IOException {
        return setupConnection(url, 0);
    }

    public static HttpURLConnection setupConnection(URL url, int cycle) throws IOException {
        Proxy proxy = null;

        ConnectivityManager connectivityManager = (ConnectivityManager) MainApplication.getContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (PrefUtils.getBoolean(PrefUtils.PROXY_ENABLED, false)
                && (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || !PrefUtils.getBoolean(PrefUtils.PROXY_WIFI_ONLY, false))) {
            try {
                proxy = new Proxy("0".equals(PrefUtils.getString(PrefUtils.PROXY_TYPE, "0")) ? Proxy.Type.HTTP : Proxy.Type.SOCKS,
                        new InetSocketAddress(PrefUtils.getString(PrefUtils.PROXY_HOST, ""), Integer.parseInt(PrefUtils.getString(
                                PrefUtils.PROXY_PORT, "8080"))));
            } catch (Exception e) {
                proxy = null;
            }
        }

        if (proxy == null) {
            // Try to get the system proxy
            try {
                ProxySelector defaultProxySelector = ProxySelector.getDefault();
                List<Proxy> proxyList = defaultProxySelector.select(url.toURI());
                if (!proxyList.isEmpty()) {
                    proxy = proxyList.get(0);
                }
            } catch (Throwable ignored) {
            }
        }

        HttpURLConnection connection = proxy == null ? (HttpURLConnection) url.openConnection() : (HttpURLConnection) url.openConnection(proxy);

        connection.setDoInput(true);
        connection.setDoOutput(false);
        connection.setRequestProperty("User-agent", "Mozilla AppleWebKit Chrome Safari"); // some feeds need this to work properly
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        connection.setUseCaches(false);

        connection.setRequestProperty("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        connection.connect();

        String location = connection.getHeaderField("Location");

        if (location != null
                && (url.getProtocol().equals(_HTTP) && location.startsWith(Constants.HTTPS) || url.getProtocol().equals(_HTTPS)
                && location.startsWith(Constants.HTTP))) {
            // if location != null, the system-automatic redirect has failed
            // which indicates a protocol change

            connection.disconnect();

            if (cycle < 5) {
                return setupConnection(new URL(location), cycle + 1);
            } else {
                throw new IOException("Too many redirects.");
            }
        }
        return connection;
    }

    /**
     * This is a small wrapper for getting the properly encoded inputstream if is is gzip compressed and not properly recognized.
     */
    public static InputStream getConnectionInputStream(HttpURLConnection connection) throws IOException {
        InputStream inputStream = connection.getInputStream();

        if (GZIP.equals(connection.getContentEncoding()) && !(inputStream instanceof GZIPInputStream)) {
            return new GZIPInputStream(inputStream);
        } else {
            return inputStream;
        }
    }
}
