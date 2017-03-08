package net.frju.flym.utils

import android.text.TextUtils
import android.util.Log
import net.fred.feedex.BuildConfig

object Dog {

    private val DEFAULT_TAG = "Unknown"

    /**
     * Send a verbose log message.

     * @param msg The message you would like logged.
     */
    fun v(msg: String) {
        if (BuildConfig.DEBUG) {
            Log.v(getDefaultTag(), msg)
        }
    }

    /**
     * Send a verbose log message and log the exception.

     * @param msg The message you would like logged.
     * *
     * @param tr  An exception to log
     */
    fun v(msg: String, tr: Throwable) {
        if (BuildConfig.DEBUG) {
            Log.v(getDefaultTag(), msg, tr)
        }
    }

    /**
     * Send a debug log message.

     * @param tag Used to identify the source of a log message.  It usually identifies
     * *            the class or activity where the log call occurs.
     * *
     * @param msg The message you would like logged.
     */
    fun v(tag: String, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.v(tag, msg)
        }
    }

    /**
     * Send a verbose log message and log the exception.

     * @param tag Used to identify the source of a log message.  It usually identifies
     * *            the class or activity where the log call occurs.
     * *
     * @param msg The message you would like logged.
     * *
     * @param tr  An exception to log
     */
    fun v(tag: String, msg: String, tr: Throwable) {
        if (BuildConfig.DEBUG) {
            Log.v(tag, msg, tr)
        }
    }

    /**
     * Send a debug log message.

     * @param msg The message you would like logged.
     */
    fun d(msg: String) {
        if (BuildConfig.DEBUG) {
            Log.d(getDefaultTag(), msg)
        }
    }

    /**
     * Send a debug log message and log the exception.

     * @param msg The message you would like logged.
     * *
     * @param tr  An exception to log
     */
    fun d(msg: String, tr: Throwable) {
        if (BuildConfig.DEBUG) {
            Log.d(getDefaultTag(), msg, tr)
        }
    }

    /**
     * Send a debug log message.

     * @param tag Used to identify the source of a log message.  It usually identifies
     * *            the class or activity where the log call occurs.
     * *
     * @param msg The message you would like logged.
     */
    fun d(tag: String, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, msg)
        }
    }

    /**
     * Send a debug log message and log the exception.

     * @param tag Used to identify the source of a log message.  It usually identifies
     * *            the class or activity where the log call occurs.
     * *
     * @param msg The message you would like logged.
     * *
     * @param tr  An exception to log
     */
    fun d(tag: String, msg: String, tr: Throwable) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, msg, tr)
        }
    }

    /**
     * Send an info log message.

     * @param msg The message you would like logged.
     */
    fun i(msg: String) {
        Log.i(getDefaultTag(), msg)
    }

    /**
     * Send a info log message and log the exception.

     * @param msg The message you would like logged.
     * *
     * @param tr  An exception to log
     */
    fun i(msg: String, tr: Throwable) {
        Log.i(getDefaultTag(), msg, tr)
    }

    /**
     * Send a info log message.

     * @param tag Used to identify the source of a log message.  It usually identifies
     * *            the class or activity where the log call occurs.
     * *
     * @param msg The message you would like logged.
     */
    fun i(tag: String, msg: String) {
        Log.i(tag, msg)
    }

    /**
     * Send a info log message and log the exception.

     * @param tag Used to identify the source of a log message.  It usually identifies
     * *            the class or activity where the log call occurs.
     * *
     * @param msg The message you would like logged.
     * *
     * @param tr  An exception to log
     */
    fun i(tag: String, msg: String, tr: Throwable) {
        Log.i(tag, msg, tr)
    }

    /**
     * Send a warn log message.

     * @param msg The message you would like logged.
     */
    fun w(msg: String) {
        Log.w(getDefaultTag(), msg)
    }

    /**
     * Send a warn log message and log the exception.

     * @param msg The message you would like logged.
     * *
     * @param tr  An exception to log
     */
    fun w(msg: String, tr: Throwable) {
        Log.w(getDefaultTag(), msg, tr)
    }

    /**
     * Send a warn log message and log the exception.

     * @param tr An exception to log
     */
    fun w(tr: Throwable) {
        Log.w(getDefaultTag(), tr)
    }

    /**
     * Send a warn log message.

     * @param tag Used to identify the source of a log message.  It usually identifies
     * *            the class or activity where the log call occurs.
     * *
     * @param msg The message you would like logged.
     */
    fun w(tag: String, msg: String) {
        Log.w(tag, msg)
    }

    /**
     * Send a warn log message and log the exception.

     * @param tag Used to identify the source of a log message.  It usually identifies
     * *            the class or activity where the log call occurs.
     * *
     * @param msg The message you would like logged.
     * *
     * @param tr  An exception to log
     */
    fun w(tag: String, msg: String, tr: Throwable) {
        Log.w(tag, msg, tr)
    }


    /**
     * Send an error log message.

     * @param msg The message you would like logged.
     */
    fun e(msg: String) {
        Log.e(getDefaultTag(), msg)
    }

    /**
     * Send a error log message and log the exception.

     * @param msg The message you would like logged.
     * *
     * @param tr  An exception to log
     */
    fun e(msg: String, tr: Throwable) {
        Log.e(getDefaultTag(), msg, tr)
    }

    /**
     * Send a error log message and log the exception.

     * @param tag Used to identify the source of a log message.  It usually identifies
     * *            the class or activity where the log call occurs.
     * *
     * @param msg The message you would like logged.
     * *
     * @param tr  An exception to log
     */
    fun e(tag: String, msg: String, tr: Throwable) {
        Log.e(tag, msg, tr)
    }

    /**
     * Send a error log message and log the exception.

     * @param tag Used to identify the source of a log message.  It usually identifies
     * *            the class or activity where the log call occurs.
     * *
     * @param msg The message you would like logged.
     */
    fun e(tag: String, msg: String) {
        Log.e(tag, msg)
    }

    fun getDefaultTag(): String {
        // Take the 5th one as the first four are produced by this method
        val elements = Thread.currentThread().stackTrace
        if (elements != null && elements.size > 4) {
            val top = elements[4]
            var className = top.className
            if (TextUtils.isEmpty(className)) {
                className = DEFAULT_TAG
            } else {
                val lastPoint = className.lastIndexOf(".")
                if (lastPoint > -1) {
                    className = className.substring(lastPoint + 1)
                }
            }

            if (!BuildConfig.DEBUG) {
                return className
            } else {
                if (TextUtils.isEmpty(top.methodName)) {
                    return className
                } else {
                    return className + "." + top.methodName + "(" + top.lineNumber + ")"
                }
            }
        } else {
            return DEFAULT_TAG
        }
    }

}
