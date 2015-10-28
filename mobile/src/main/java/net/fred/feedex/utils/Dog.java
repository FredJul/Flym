package net.fred.feedex.utils;

import android.text.TextUtils;
import android.util.Log;

import net.fred.feedex.BuildConfig;

public class Dog {

    private static final String DEFAULT_TAG = "Unknown";

    /**
     * Send a verbose log message.
     *
     * @param msg The message you would like logged.
     */
    public static void v(String msg) {
        if (BuildConfig.DEBUG) {
            Log.v(getTag(), msg);
        }
    }

    /**
     * Send a verbose log message and log the exception.
     *
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static void v(String msg, Throwable tr) {
        if (BuildConfig.DEBUG) {
            Log.v(getTag(), msg, tr);
        }
    }

    /**
     * Send a debug log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void v(String tag, String msg) {
        if (BuildConfig.DEBUG) {
            Log.v(tag, msg);
        }
    }

    /**
     * Send a verbose log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static void v(String tag, String msg, Throwable tr) {
        if (BuildConfig.DEBUG) {
            Log.v(tag, msg, tr);
        }
    }

    /**
     * Send a debug log message.
     *
     * @param msg The message you would like logged.
     */
    public static void d(String msg) {
        if (BuildConfig.DEBUG) {
            Log.d(getTag(), msg);
        }
    }

    /**
     * Send a debug log message and log the exception.
     *
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static void d(String msg, Throwable tr) {
        if (BuildConfig.DEBUG) {
            Log.d(getTag(), msg, tr);
        }
    }

    /**
     * Send a debug log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void d(String tag, String msg) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, msg);
        }
    }

    /**
     * Send a debug log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static void d(String tag, String msg, Throwable tr) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, msg, tr);
        }
    }

    /**
     * Send an info log message.
     *
     * @param msg The message you would like logged.
     */
    public static void i(String msg) {
        Log.i(getTag(), msg);
    }

    /**
     * Send a info log message and log the exception.
     *
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static void i(String msg, Throwable tr) {
        Log.i(getTag(), msg, tr);
    }

    /**
     * Send a info log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void i(String tag, String msg) {
        Log.i(tag, msg);
    }

    /**
     * Send a info log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static void i(String tag, String msg, Throwable tr) {
        Log.i(tag, msg, tr);
    }

    /**
     * Send a warn log message.
     *
     * @param msg The message you would like logged.
     */
    public static void w(String msg) {
        Log.w(getTag(), msg);
    }

    /**
     * Send a warn log message and log the exception.
     *
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static void w(String msg, Throwable tr) {
        Log.w(getTag(), msg, tr);
    }

    /**
     * Send a warn log message and log the exception.
     *
     * @param tr An exception to log
     */
    public static void w(Throwable tr) {
        Log.w(getTag(), tr);
    }

    /**
     * Send a warn log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void w(String tag, String msg) {
        Log.w(tag, msg);
    }

    /**
     * Send a warn log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static void w(String tag, String msg, Throwable tr) {
        Log.w(tag, msg, tr);
    }


    /**
     * Send an error log message.
     *
     * @param msg The message you would like logged.
     */
    public static void e(String msg) {
        Log.e(getTag(), msg);
    }

    /**
     * Send a error log message and log the exception.
     *
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static void e(String msg, Throwable tr) {
        Log.e(getTag(), msg, tr);
    }

    /**
     * Send a error log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static void e(String tag, String msg, Throwable tr) {
        Log.e(tag, msg, tr);
    }

    /**
     * Send a error log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void e(String tag, String msg) {
        Log.e(tag, msg);
    }

    private static String getTag() {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        if (elements != null && elements.length > 4) {

            // Take the 5th one as the first four are produced by this method
            StackTraceElement top = elements[4];
            String className = top.getClassName();
            if (TextUtils.isEmpty(className)) {
                className = DEFAULT_TAG;
            } else {
                int lastPoint = className.lastIndexOf(".");
                if (lastPoint > -1) {
                    className = className.substring(lastPoint + 1);
                }
            }

            if (!BuildConfig.DEBUG) {
                return className;
            } else {
                if (TextUtils.isEmpty(top.getMethodName())) {
                    return className;
                } else {
                    StringBuilder tag = new StringBuilder(className);
                    tag.append(".");
                    tag.append(top.getMethodName());
                    tag.append("(");
                    tag.append(top.getLineNumber());
                    tag.append(")");
                    return tag.toString();
                }
            }
        } else {
            return DEFAULT_TAG;
        }
    }

}
