package com.xian.camera.utils;

import android.hardware.Camera;
import android.hardware.Camera.Size;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by xian on 2016/3/4 0004.
 */
public class CameraUtils {
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
     */
    public Size getPropPreviewSize(List<Size> list, int w, int h) {
        Collections.sort(list, sizeComparator);
        int index = 0;
        for (int i = 0; i < list.size(); i++) {
            Size size = list.get(i);
            LogUtils.i("-----摄像头参数--->" + size.width + "  ---> " + size.height + " ---> " + list.size());
            if (w == size.width && h == size.height) {
                index = i;
            }
        }
        Size size = list.get(index);
        LogUtils.i("-----适合是摄像头参数--->" + size.width + "  ---> " + size.height);
        return size;
    }


    /**
     * 检测是否有合适的预览尺寸
     * @param list
     * @param w
     * @param h
     * @return
     */
    public boolean checkPreviewSize(List<Size> list, int w, int h) {
        if (list == null) return false;
        for (int i = 0; i < list.size(); i++) {
            Size size = list.get(i);
            LogUtils.i("-----摄像头参数--->" + size.width + "  ---> " + size.height + " ---> " + list.size());
            if (w == size.width && h == size.height) {
                return true;
            }
        }
        return false;
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
            LogUtils.i("previewSizes:width = " + size.width + " height = " + size.height);
        }
    }

    /**
     * @param params
     */
    public void printSupportPictureSize(Camera.Parameters params) {
        List<Size> pictureSizes = params.getSupportedPictureSizes();
        for (int i = 0; i < pictureSizes.size(); i++) {
            Size size = pictureSizes.get(i);
            LogUtils.i("pictureSizes:width = " + size.width + " height = " + size.height);
        }
    }

    /**
     * @param params
     */
    public void printSupportFocusMode(Camera.Parameters params) {
        List<String> focusModes = params.getSupportedFocusModes();
        for (String mode : focusModes) {
            LogUtils.i("focusModes--" + mode);
        }
    }

    /**
     * @param params
     */
    public void printSupportFlashMode(Camera.Parameters params) {
        List<String> focusModes = params.getSupportedFlashModes();
        for (String mode : focusModes) {
            LogUtils.i("focusModes--" + mode);
        }
    }


    public void mirror2Data(byte[] src, int w, int h) { //src是原始yuv数组
        int i;
        int index;
        byte temp;
        int a, b;
        //mirror y
        for (i = 0; i < h; i++) {
            a = i * w;
            b = (i + 1) * w - 1;
            while (a < b) {
                temp = src[a];
                src[a] = src[b];
                src[b] = temp;
                a++;
                b--;
            }
        }
        // mirror u and v
        index = w * h;
        for (i = 0; i < h / 2; i++) {
            a = i * w;
            b = (i + 1) * w - 2;
            while (a < b) {
                temp = src[a + index];
                src[a + index] = src[b + index];
                src[b + index] = temp;

                temp = src[a + index + 1];
                src[a + index + 1] = src[b + index + 1];
                src[b + index + 1] = temp;
                a += 2;
                b -= 2;
            }
        }
    }

}
