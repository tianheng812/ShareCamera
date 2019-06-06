package com.xian.camera.listeners;

/**
 * Created by xian on 2018/12/21.
 */

public interface PreviewCallbackListener {
    /**
     * 预览数据回调
     * @param data 预览数据
     * @param scaleWidth 要返回缩放后的预览宽度
     * @param scaleHeight 要返回缩放后预览高度
     */
    void onPreviewFrame(byte[] data, int scaleWidth, int scaleHeight);
}