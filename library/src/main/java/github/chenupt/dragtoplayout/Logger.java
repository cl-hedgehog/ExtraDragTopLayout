/***
 * This is free and unencumbered software released into the public domain.
 * <p/>
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 * <p/>
 * For more information, please refer to <http://unlicense.org/>
 */

package github.chenupt.dragtoplayout;

import android.util.Log;


public class Logger {

    final static String TAG = "TEST";

    private Logger() {
        /* Protect from instantiations */
    }

    public static void d(String message, Object obj) {
        if (obj != null) {
            Log.d(TAG, message + obj.toString());
        } else {
            Log.d(TAG, message);
        }
    }

    public static void e(String message, Object obj) {
        if (obj != null) {
            Log.e(TAG, message + obj.toString());
        } else {
            Log.e(TAG, message);
        }
    }

    public static void d(String message) {
        Log.d(TAG, message);
    }

    public static void e(String message) {
        Log.e(TAG, message);
    }


    public static void i(String message, Object obj) {
        if (obj != null) {
            Log.i(TAG, message + obj.toString());
        } else {
            Log.i(TAG, message);
        }
    }

    public static void i(String message) {
        Log.i(TAG, message);
    }
}
