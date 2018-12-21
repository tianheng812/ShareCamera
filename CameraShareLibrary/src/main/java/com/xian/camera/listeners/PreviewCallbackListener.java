package com.xian.camera.listeners;

/**
 * Created by xian on 2018/12/21.
 */

public interface PreviewCallbackListener {
    void onPreviewFrame(byte[] data, int previewWidth, int previewHeight);
}