package com.xian.camera.utils;

import android.util.Log;

/**
 * Created by xian on 2019/6/5.
 */

public class LogUtils {


    public static String TAG = "CameraShare";
    public static boolean isOutLog = true;

    public static void d(String s) {
        if (isOutLog)
            Log.d(TAG, s);
    }

    public static void e(String s) {
        if (isOutLog)
            Log.e(TAG, s);
    }

    public static void i(String s) {
        if (isOutLog)
            Log.e(TAG, s);
    }
}
