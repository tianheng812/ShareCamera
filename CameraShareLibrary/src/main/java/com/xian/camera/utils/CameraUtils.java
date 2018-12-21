package com.xian.camera.utils;

/**
 * Created by Administrator on 2016/3/4 0004.
 */

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraUtils {
    private static final String TAG = "CameraUtils";
    private CameraSizeComparator sizeComparator = new CameraSizeComparator();
    private CameraSizeComparator2 sizeComparator2 = new CameraSizeComparator2();
    private static CameraUtils myCamPara = null;

    private CameraUtils() {

    }

    public static CameraUtils getInstance() {
        if (myCamPara == null) {
            myCamPara = new CameraUtils();
            return myCamPara;
        } else {
            return myCamPara;
        }
    }

    /**
     * 176  144   25344
     * 320  240   76800
     * 352  288   101376
     * 640  480   307200
     * 960  544   522240
     * 960  720   691200
     * 1280  720   921600
     * 1280  960   1228800
     * 1920  1080   2073600
     * pictureSizes:width = 320 height = 240
     * pictureSizes:width = 640 height = 480
     * pictureSizes:width = 1280 height = 720
     * pictureSizes:width = 1920 height = 1088
     * pictureSizes:width = 2560 height = 1440
     * pictureSizes:width = 1920 height = 1920
     * pictureSizes:width = 2560 height = 1920
     * pictureSizes:width = 3264 height = 1840
     * pictureSizes:width = 2448 height = 2448
     * pictureSizes:width = 3264 height = 2448
     * 08-22 16:00:12.146 10041-10041/com.sayee.property E/xxxxxxxxxxxxx: previewSizes:width = 176 height = 144
     * 08-22 16:00:12.146 10041-10041/com.sayee.property E/xxxxxxxxxxxxx: previewSizes:width = 320 height = 240
     * 08-22 16:00:12.146 10041-10041/com.sayee.property E/xxxxxxxxxxxxx: previewSizes:width = 352 height = 288
     * 08-22 16:00:12.146 10041-10041/com.sayee.property E/xxxxxxxxxxxxx: previewSizes:width = 480 height = 320
     * 08-22 16:00:12.146 10041-10041/com.sayee.property E/xxxxxxxxxxxxx: previewSizes:width = 480 height = 368
     * 08-22 16:00:12.146 10041-10041/com.sayee.property E/xxxxxxxxxxxxx: previewSizes:width = 640 height = 480
     * 08-22 16:00:12.146 10041-10041/com.sayee.property E/xxxxxxxxxxxxx: previewSizes:width = 720 height = 480
     * 08-22 16:00:12.146 10041-10041/com.sayee.property E/xxxxxxxxxxxxx: previewSizes:width = 800 height = 480
     * 08-22 16:00:12.146 10041-10041/com.sayee.property E/xxxxxxxxxxxxx: previewSizes:width = 800 height = 600
     * 08-22 16:00:12.146 10041-10041/com.sayee.property E/xxxxxxxxxxxxx: previewSizes:width = 864 height = 480
     * 08-22 16:00:12.146 10041-10041/com.sayee.property E/xxxxxxxxxxxxx: previewSizes:width = 960 height = 540
     * 08-22 16:00:12.146 10041-10041/com.sayee.property E/xxxxxxxxxxxxx: previewSizes:width = 960 height = 720
     * 08-22 16:00:12.146 10041-10041/com.sayee.property E/xxxxxxxxxxxxx: previewSizes:width = 1280 height = 720
     * 08-22 16:00:12.146 10041-10041/com.sayee.property E/xxxxxxxxxxxxx: previewSizes:width = 1280 height = 960
     * 08-22 16:00:12.146 10041-10041/com.sayee.property E/xxxxxxxxxxxxx: previewSizes:width = 1088 height = 1088
     * 08-22 16:00:12.147 10041-10041/com.sayee.property E/xxxxxxxxxxxxx: previewSizes:width = 1440 height = 1080
     * 08-22 16:00:12.147 10041-10041/com.sayee.property E/xxxxxxxxxxxxx: previewSizes:width = 1920 height = 1080
     * 08-22 16:00:12.147 10041-10041/com.sayee.property E/xxxxxxxxxxxxx: previewSizes:width = 1920 height = 1088
     * 08-22 16:00:12.147 10041-10041/com.sayee.property E/xxxxxxxxxxxxx: previewSizes:width = 1872 height = 1120
     * 08-22 16:02:28.850 17922-17922/com.sayee.property E/xxxxxxxxxxxxx: pictureSizes:width = 640 height = 368
     * 08-22 16:02:28.850 17922-17922/com.sayee.property E/xxxxxxxxxxxxx: pictureSizes:width = 640 height = 480
     * 08-22 16:02:28.850 17922-17922/com.sayee.property E/xxxxxxxxxxxxx: pictureSizes:width = 1280 height = 720
     * 08-22 16:02:28.850 17922-17922/com.sayee.property E/xxxxxxxxxxxxx: pictureSizes:width = 1280 height = 960
     * 08-22 16:02:28.850 17922-17922/com.sayee.property E/xxxxxxxxxxxxx: pictureSizes:width = 2592 height = 1944
     * 08-22 16:02:28.850 17922-17922/com.sayee.property E/xxxxxxxxxxxxx: pictureSizes:width = 2592 height = 1456
     * 08-22 16:02:28.850 17922-17922/com.sayee.property E/xxxxxxxxxxxxx: previewSizes:width = 176 height = 144
     * 08-22 16:02:28.850 17922-17922/com.sayee.property E/xxxxxxxxxxxxx: previewSizes:width = 352 height = 288
     * 08-22 16:02:28.850 17922-17922/com.sayee.property E/xxxxxxxxxxxxx: previewSizes:width = 320 height = 240
     * 08-22 16:02:28.850 17922-17922/com.sayee.property E/xxxxxxxxxxxxx: previewSizes:width = 640 height = 480
     * 08-22 16:02:28.850 17922-17922/com.sayee.property E/xxxxxxxxxxxxx: previewSizes:width = 960 height = 544
     * 08-22 16:02:28.850 17922-17922/com.sayee.property E/xxxxxxxxxxxxx: previewSizes:width = 960 height = 720
     * 08-22 16:02:28.850 17922-17922/com.sayee.property E/xxxxxxxxxxxxx: previewSizes:width = 1280 height = 720
     * 08-22 16:02:28.850 17922-17922/com.sayee.property E/xxxxxxxxxxxxx: previewSizes:width = 1280 height = 960
     * 08-22 16:02:28.850 17922-17922/com.sayee.property E/xxxxxxxxxxxxx: previewSizes:width = 1920 height = 1080
     * 08-22 16:02:28.850 17922-17922/com.sayee.property E/xxxxxxxxxxxxx: previewSizes:width = 1920 height = 960
     */

    public Size getPropPreviewSize(List<Size> list) {
        Collections.sort(list, sizeComparator);
        int index = 0;
        int minWith = 0;
        int index1 = 0;
        int minWith1 = 0;
        for (int i = 0; i < list.size(); i++) {
            Size size = list.get(i);
            Log.i("-----摄像头参数--->", size.width + "  ---> " + size.height + " ---> " + list.size());
            if (minWith < size.width && size.width / 16 == size.height / 9) {
                index = i;
                minWith = size.width;
            }
            if (minWith1 < size.width && size.width / 4 == size.height / 3) {
                index1 = i;
                minWith1 = size.width;
            }
        }

        Size size = list.get(index);
        Size size1 = list.get(index1);

        if (size.width <= size1.width && size.height <= size1.height) {
            size = size1;
        }

        Log.i("-----适合是摄像头参数--->", size.width + "  ---> " + size.height);
        return size;
    }


    /**
     * 判断摄像头分辨率是否是16:9
     *
     * @param w
     * @param h
     * @return
     */
    public boolean isCameraSize169(int w, int h) {
        if (w / 16 == h / 9) {
            return true;
        }
        return false;
    }

    public Size getPropPictureSize(List<Size> list, int minWidth) {
        Collections.sort(list, sizeComparator2);//不同的手机的排序不一样。先做由小到大的排序
        int i = 0;
        for (Size s : list) {
            if ((s.height >= minWidth)) {//取宽与屏幕宽一致或者宽大于屏幕宽的第一个.因为这里是竖屏的，所以实际上保存的图片的宽高颠倒，所以这里取s.height
                break;
            }
            i++;
        }
        if (i == list.size()) {
            i = list.size() - 1;//没有的话就用最大那个
        }
        return list.get(i);
    }

    public boolean equalRate(Size s, float rate) {
        float r = (float) (s.width) / (float) (s.height);
        if (Math.abs(r - rate) <= 0.03) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 由小到大
     *
     * @author Administrator
     */
    public class CameraSizeComparator implements Comparator<Size> {
        public int compare(Size lhs, Size rhs) {
            if (lhs.width == rhs.width) {
                return 0;
            } else if (lhs.width > rhs.width) {
                return 1;
            } else {
                return -1;
            }
        }

    }

    public class CameraSizeComparator2 implements Comparator<Size> {
        public int compare(Size lhs, Size rhs) {
            if (lhs.height == rhs.height) {
                if (lhs.width > rhs.width) {
                    return 1;
                }
                return -1;
            } else if (lhs.height > rhs.height) {
                return 1;
            } else {
                return -1;
            }
        }

    }

    /**
     * @param params
     */
    public void printSupportPreviewSize(Camera.Parameters params) {
        List<Size> previewSizes = params.getSupportedPreviewSizes();
        for (int i = 0; i < previewSizes.size(); i++) {
            Size size = previewSizes.get(i);
            Log.i(TAG, "previewSizes:width = " + size.width + " height = " + size.height);
        }

    }

    /**
     * @param params
     */
    public void printSupportPictureSize(Camera.Parameters params) {
        List<Size> pictureSizes = params.getSupportedPictureSizes();
        for (int i = 0; i < pictureSizes.size(); i++) {
            Size size = pictureSizes.get(i);
            Log.i(TAG, "pictureSizes:width = " + size.width + " height = " + size.height);
        }
    }

    /**
     * @param params
     */
    public void printSupportFocusMode(Camera.Parameters params) {
        List<String> focusModes = params.getSupportedFocusModes();
        for (String mode : focusModes) {
            Log.i(TAG, "focusModes--" + mode);
        }
    }

    /**
     * @param params
     */
    public void printSupportFlashMode(Camera.Parameters params) {
        List<String> focusModes = params.getSupportedFlashModes();
        for (String mode : focusModes) {
            Log.i(TAG, "focusModes--" + mode);
        }
    }

    /**
     * 从预览图中提取图片
     *
     * @param data
     * @param camera
     * @return
     */
    public Bitmap decodeToBitMap(byte[] data, Camera camera) {
        Size size = camera.getParameters().getPreviewSize();
        try {
            YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
            if (image != null) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, stream);
                Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());

                //因为图片会放生旋转，因此要对图片进行旋转到和手机在一个方向上
                Bitmap returnBitmap = rotateCameraBitmap(bmp);
                if (bmp != null && !bmp.isRecycled()) {
                    bmp.recycle();
                }
                stream.close();
                return returnBitmap;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public Bitmap rotateCameraBitmap(Bitmap bmp) {
        if (bmp == null) return null;
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        Bitmap newBmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
        if (!bmp.isRecycled()) {
            bmp.recycle();
        }
        return newBmp;
    }

}
