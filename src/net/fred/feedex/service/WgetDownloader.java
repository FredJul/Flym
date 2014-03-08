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

package net.fred.feedex.service;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.net.URL;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.os.Build;

import net.fred.feedex.Constants;
import net.fred.feedex.MainApplication;
import net.fred.feedex.provider.FeedData;
import net.fred.feedex.provider.FeedData.EntryColumns;
import net.fred.feedex.provider.FeedData.FeedColumns;
import net.fred.feedex.provider.FeedData.TaskColumns;
import net.fred.feedex.utils.NetworkUtils;
import net.fred.feedex.utils.PrefUtils;

public class WgetDownloader {

    private static final String WGET = "wget";

    private static final String MOBILE_USERAGENT = "--user-agent=Mozilla/5.0 (Linux; U; Android 4.0.4; GT-N7000 Build/IMM76L; CyanogenMod-9.1.0) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30";

    private static final String DESKTOP_USERAGENT = "--user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.63 Safari/537.36";

    private static final String[] WGET_ARGS = { "--tries=3", "--retry-connrefused", "--timeout=60", "--limit-rate=100k", "--wait=0.2", "--restrict-file-names=windows", "--no-check-certificate", "--timestamping", "--force-directories", "--convert-links", "--page-requisites", "--span-hosts" };

    private static final String WGET_TARGET_FILE_LOG = "\nSaving to: ";

    private static final String DATE_MARKER = "date-marker";

    private static boolean running = false;

    private static File WEBCACHEFOLDER = null;

    public static void download(final Context context, final String feedId) {
        if (running) {
            return;
        }
        new Thread(new Runnable() {
            public void run() {
                try {
                    running = true;
                    downloadThread(context, feedId);
                } finally {
                    running = false;
                }
            }
        }).start();
    }

    private static void downloadThread(Context context, String feedId) {

        //System.out.println("WgetDownloader.downloadThread: " + (feedId == null ? "all feeds" : feedId));

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (!checkWifi(connectivityManager) || context.getExternalFilesDir(null) == null || !extractWgetBinary(context))
            return;

        if (WEBCACHEFOLDER == null)
            WEBCACHEFOLDER = new File(context.getExternalFilesDir(null), "webcache");

        boolean clearWebCache = false;
        if (deleteWebCacheIfNeeded())
            clearWebCache = true;

        if (WEBCACHEFOLDER.mkdirs())
            new File(WEBCACHEFOLDER, DATE_MARKER).mkdirs();

        //System.out.println("WgetDownloader.downloadThread - processing");

        Cursor feedCursor = context.getContentResolver().query(feedId == null ? FeedColumns.CONTENT_URI : FeedColumns.CONTENT_URI(feedId),
                new String[] {FeedColumns._ID, FeedColumns.RETRIEVE_DESKTOP_WEBPAGE},
                FeedColumns.RETRIEVE_WEBPAGE + Constants.DB_IS_TRUE + Constants.DB_OR + FeedColumns.RETRIEVE_DESKTOP_WEBPAGE + Constants.DB_IS_TRUE , null, null);

        while (feedCursor.moveToNext()) {

            if (clearWebCache) {
                ContentValues values = new ContentValues();
                values.putNull(EntryColumns.MOBILIZED_HTML);
                context.getContentResolver().update(EntryColumns.CONTENT_URI(feedCursor.getString(0)), values, null, null);
            }

            boolean requestDesktopPage = (feedCursor.getInt(1) == 1);


            String filter = EntryColumns.MOBILIZED_HTML + Constants.DB_IS_NULL + Constants.DB_AND +
                    "(" + EntryColumns.IS_READ + " IS NOT 1" + Constants.DB_OR +
                    EntryColumns.IS_FAVORITE + Constants.DB_IS_TRUE + ")";
            //System.out.println("WgetDownloader.downloadThread feed " + feedCursor.getString(0) + " desktop webpage " + requestDesktopPage + " WHERE " + filter );
            Cursor entryCursor = context.getContentResolver().query(EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(feedCursor.getString(0)),
                    new String[] {EntryColumns._ID, EntryColumns.LINK},
                    filter, null, null);

            while (entryCursor.moveToNext()) {

                //System.out.println("WgetDownloader.downloadThread feed " + feedCursor.getString(0) + " entry " + entryCursor.getString(0) + " " + entryCursor.getString(1));

                if ( !checkWifi(connectivityManager) ) {
                    entryCursor.close();
                    feedCursor.close();
                    return;
                }

                ArrayList<String> args = new ArrayList<String>();
                args.add(new File(context.getFilesDir(), WGET).getAbsolutePath());
                args.addAll(Arrays.asList(WGET_ARGS));
                args.add(requestDesktopPage ? DESKTOP_USERAGENT : MOBILE_USERAGENT);
                args.add(entryCursor.getString(1));

                //System.out.println("Launching wget for page: " + entryCursor.getString(1));
                StringBuilder log = new StringBuilder();
                int i1 = -1, i2 = -1;

                Process process = null;
                try {
                    ProcessBuilder processBuilder = new ProcessBuilder().directory(WEBCACHEFOLDER).command(args).redirectErrorStream(true);

                    // SOCKS proxy is not supported
                    if (PrefUtils.getBoolean(PrefUtils.PROXY_ENABLED, false) && "0".equals(PrefUtils.getString(PrefUtils.PROXY_TYPE, "0"))) {
                        processBuilder.environment().put("http_proxy", "http://" + PrefUtils.getString(PrefUtils.PROXY_HOST, "") + ":" + PrefUtils.getString(PrefUtils.PROXY_PORT, "8080") + "/");
                        processBuilder.environment().put("https_proxy", "https://" + PrefUtils.getString(PrefUtils.PROXY_HOST, "") + ":" + PrefUtils.getString(PrefUtils.PROXY_PORT, "8080") + "/");
                    }

                    process = processBuilder.start();
                    while (true) {
                        InputStream out = process.getInputStream();
                        byte buf[] = new byte[256];
                        int len = out.read(buf);
                        if (len < 0) {
                            break;
                        }
                        if (i1 <= 0 || i2 <= 0) {
                            log.append(new String(buf, 0, len));
                            i1 = log.indexOf("\nSaving to: ");
                            i2 = i1 > 0 ? log.indexOf("\n", i1 + 1) : -1;
                        }
                        //System.out.println("wget log: " + new String(buf, 0, len));
                        if (!checkWifi(connectivityManager)) {
                            entryCursor.close();
                            feedCursor.close();
                            return;
                        }
                    }
                } catch (Exception e) {
                    //System.out.println("Error launching wget: " + e.toString());
                } finally {
                    if (process != null) {
                        try {
                            process.getInputStream().close();
                            process.getOutputStream().close();
                            process.destroy();
                        } catch (Exception e) {
                        }
                    }
                }

                ContentValues values = new ContentValues();
                values.putNull(EntryColumns.MOBILIZED_HTML);
                if (i1 > 0 && i2 > 0) {
                    File savedPage = new File(WEBCACHEFOLDER, log.substring(i1 + WGET_TARGET_FILE_LOG.length() + 1, i2 - 1));
                    values.put(EntryColumns.MOBILIZED_HTML, Constants.FILE_SCHEME + savedPage.getAbsolutePath());
                    //System.out.println("wget target file: " + savedPage.getAbsolutePath());
                }
                context.getContentResolver().update(EntryColumns.CONTENT_URI(entryCursor.getString(0)), values, null, null);
            }
            entryCursor.close();
        }
        feedCursor.close();
    }

    private static boolean extractWgetBinary(Context context) {

        File wgetPath = new File(context.getFilesDir(), WGET);

        if( wgetPath.exists() /* && wgetPath.canExecute() */ )
            return true;

        context.getFilesDir().mkdirs();
        String archList[] = {Build.CPU_ABI, Build.CPU_ABI2};
        for (String arch: archList) {
            BufferedOutputStream out = null;
            try {
                InputStream in = context.getAssets().open(arch + "/" + WGET);
                out = new BufferedOutputStream(new FileOutputStream(wgetPath));
                byte[] buf = new byte[1024];
                int len = 0;
                while (true) {
                    len = in.read(buf);
                    if (len < 0) {
                        break;
                    }
                    out.write(buf, 0, len);
                }
                out.close();
                in.close();
            } catch (Exception e) {
                if (out != null) {
                    try {
                        out.close();
                    } catch (Exception ee) {
                    }
                }
                wgetPath.delete();
                continue;
            }
            break;
        }

        if (!wgetPath.exists())
            return false;

        try {
            Process process = new ProcessBuilder().command("chmod", "755", wgetPath.toString()).redirectErrorStream(true).start();
            byte buf[] = new byte[1024];
            process.getInputStream().read(buf);
            process.getInputStream().close();
            process.waitFor();
        } catch (Exception e) {
            wgetPath.delete();
            return false;
        }
        return true;
    }

    private static boolean checkWifi(ConnectivityManager connectivityManager) {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || networkInfo.getState() != NetworkInfo.State.CONNECTED || networkInfo.getType() != ConnectivityManager.TYPE_WIFI) {
            if (Build.MODEL.equals("sdk")) // Emulator
                return true;
            return false;
        }
        return true;
    }

    private static boolean deleteWebCacheIfNeeded() {
        long keepTime = Long.parseLong(PrefUtils.getString(PrefUtils.KEEP_TIME, "4")) * 86400000l;
        System.out.println("============== " + DATE_MARKER + " " + new File(WEBCACHEFOLDER, DATE_MARKER).lastModified() + " keepTime " + keepTime + " curtime " + System.currentTimeMillis());
        if (new File(WEBCACHEFOLDER, DATE_MARKER).lastModified() + keepTime < System.currentTimeMillis()) {
            System.out.println("============== Deleting!");
            deleteRecursively(WEBCACHEFOLDER);
            return true;
        }
        return false;
    }

    public static boolean deleteRecursively(File dir)
    {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteRecursively(new File(dir, children[i]));
                if (!success)
                    return false;
            }
        }
        return dir.delete();
    }
}
