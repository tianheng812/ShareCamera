package com.xian.camera.utils;

import android.util.Log;

import java.util.Arrays;

/**
 * 作者：请叫我百米冲刺 on 2017/8/28 上午11:05
 * 邮箱：mail@hezhilin.cc
 */

public class YuvUtil {

    static {
        System.loadLibrary("yuvutil");
    }

    /**
     * YUV数据的基本的处理
     *
     * @param src        原始数据
     * @param width      原始的宽
     * @param height     原始的高
     * @param dst        输出数据
     * @param dst_width  输出的宽
     * @param dst_height 输出的高
     * @param mode       压缩模式。这里为0，1，2，3 速度由快到慢，质量由低到高，一般用0就好了，因为0的速度最快
     * @param degree     旋转的角度，90，180和270三种
     * @param isMirror   是否镜像，一般只有270的时候才需要镜像
     **/
    public static native void compressYUV(byte[] src, int width, int height, byte[] dst, int dst_width, int dst_height, int mode, int degree, boolean isMirror);

    /**
     * I420数据的裁剪操作
     *
     * @param src        原始数据
     * @param width      原始的宽
     * @param height     原始的高
     * @param dst        输出数据
     * @param dst_width  输出的宽
     * @param dst_height 输出的高
     * @param left       裁剪的x的开始位置，必须为偶数，否则显示会有问题
     * @param top        裁剪的y的开始位置，必须为偶数，否则显示会有问题
     **/
    public static native void cropYUV(byte[] src, int width, int height, byte[] dst, int dst_width, int dst_height, int left, int top);

    /**
     * nv21数据的裁剪操作
     *
     * @param src        原始数据
     * @param width      原始的宽
     * @param height     原始的高
     * @param dst        输出数据
     * @param dst_width  输出的宽
     * @param dst_height 输出的高
     * @param left       裁剪的x的开始位置，必须为偶数，否则显示会有问题
     * @param top        裁剪的y的开始位置，必须为偶数，否则显示会有问题
     **/
    public static native void cropNV21(byte[] src, int width, int height, byte[] dst, int dst_width, int dst_height, int left, int top);

    /**
     * 将I420转化为NV21
     *
     * @param i420Src 原始I420数据
     * @param nv21Src 转化后的NV21数据
     * @param width   输出的宽
     * @param width   输出的高
     **/
    public static native void yuvI420ToNV21(byte[] i420Src, byte[] nv21Src, int width, int height);

    public static native void NV21ToYuvI420(byte[] nv21Src, byte[] i420Src, int width, int height);

    public static byte[] yuvI420ToNV21(byte[] previewData, int previewWidth, int previewHeight) {

        int scaleWidth = 320;
        int scaleHeight = 240;
        if (previewWidth / 16 == previewHeight / 9) {
            scaleWidth = 352;
            scaleHeight = 288;
        }

        byte[] dstData = new byte[scaleWidth * scaleHeight * 3 / 2];
        byte[] yuvI420ToNV21 = new byte[scaleWidth * scaleHeight * 3 / 2];
        try {
            YuvUtil.compressYUV(previewData, previewWidth, previewHeight, dstData, scaleWidth, scaleHeight, 0, 0, false);
            YuvUtil.yuvI420ToNV21(dstData, yuvI420ToNV21, scaleWidth, scaleHeight);
            return yuvI420ToNV21;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        return previewData;
    }
}
