package com.xian.camera.entity;

import android.os.Handler;
import android.os.Looper;

/**
 * Created by xian on 2019/6/5.
 */

public class DispatchHandler extends Handler {
    public int width;
    public int height;

    public DispatchHandler(Looper looper) {
        super(looper);
    }
}
