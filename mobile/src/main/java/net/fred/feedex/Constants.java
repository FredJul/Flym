/**
 * Flym
 * <p/>
 * Copyright (c) 2012-2015 Frederic Julian
 * Copyright (c) 2010-2012 Stefan Handschuh
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

package net.fred.feedex;

import android.app.NotificationManager;
import android.content.Context;
import android.database.MatrixCursor;
import android.provider.BaseColumns;

public final class Constants {

    public static final NotificationManager NOTIF_MGR = (NotificationManager) MainApplication.getContext().getSystemService(Context.NOTIFICATION_SERVICE);

    public static final String INTENT_FROM_WIDGET = "fromWidget";

    public static final String FEED_ID = "feedid";

    public static final String DB_COUNT = "COUNT(*)";
    public static final String DB_IS_TRUE = "=1";
    public static final String DB_IS_FALSE = "=0";
    public static final String DB_IS_NULL = " IS NULL";
    public static final String DB_IS_NOT_NULL = " IS NOT NULL";
    public static final String DB_DESC = " DESC";
    public static final String DB_ASC = " ASC";
    public static final String DB_ARG = "=?";
    public static final String DB_AND = " AND ";
    public static final String DB_OR = " OR ";

    public static final String HTTP_SCHEME = "http://";
    public static final String HTTPS_SCHEME = "https://";
    public static final String FILE_SCHEME = "file://";

    public static final String HTML_LT = "&lt;";
    public static final String HTML_GT = "&gt;";
    public static final String LT = "<";
    public static final String GT = ">";

    public static final String TRUE = "true";
    public static final String FALSE = "false";

    public static final String ENCLOSURE_SEPARATOR = "[@]"; // exactly three characters!

    public static final String HTML_QUOT = "&quot;";
    public static final String QUOT = "\"";
    public static final String HTML_APOSTROPHE = "&#39;";
    public static final String APOSTROPHE = "'";
    public static final String AMP = "&";
    public static final String AMP_SG = "&amp;";
    public static final String SLASH = "/";
    public static final String COMMA_SPACE = ", ";

    public static final String UTF8 = "UTF-8";

    public static final String FROM_AUTO_REFRESH = "from_auto_refresh";

    public static final String MIMETYPE_TEXT_PLAIN = "text/plain";

    public static final int UPDATE_THROTTLE_DELAY = 500;

    public static final String FETCH_PICTURE_MODE_WIFI_ONLY_PRELOAD = "WIFI_ONLY_PRELOAD";
    public static final String FETCH_PICTURE_MODE_ALWAYS_PRELOAD = "ALWAYS_PRELOAD";

    public static final MatrixCursor EMPTY_CURSOR = new MatrixCursor(new String[]{BaseColumns._ID});
}
