package com.xian.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.TextureView;


//import com.libyuv.util.YuvUtil;

import com.xian.camera.enums.CameraFacing;
import com.xian.camera.enums.CameraStateCode;
import com.xian.camera.listeners.CameraStatesListener;
import com.xian.camera.listeners.PreviewCallbackListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Administrator on 201 8/6/20.
 */

public class CameraManager implements Camera.PreviewCallback, Camera.ErrorCallback {

    private final String TAG = "CameraManager";
    private boolean isOpenCamera = false;
    private Camera mCamera;
    private int previewWidth = 640;
    private int previewHeight = 480;
    private byte[] previewData;
    private Context mContext;
    private static CameraManager cameraManager;
    private TextureView previewTextureView;
    private static List<CameraStatesListener> mCameraStatesListenerList = new ArrayList<>();
    private static Map<PreviewCallbackListener, DispatchHandler> mPreviewCallbackListenerList = new ConcurrentHashMap<>();
    private static Map<DispatchHandler, MyRunable> runnables = new ConcurrentHashMap<>();
    private Handler initCameraHandler;//初始化摄像头线程
    private byte[] mBuffer; // 预览缓冲数据，使用可以让底层减少重复创建byte[]，起到重用的作用
    private int openFailCount = 0;//打开摄像头失败的次数
    private boolean reopen = false;//打开失败后，是否重新打开

    @Override
    public void onError(int error, Camera camera) {
        Log.e("onPreviewFrameerror", "摄像头错误onError CODE= " + error + "===线程ID=>" + Thread.currentThread().getId());
        boolean errorHandler = false;
        CameraStateCode stateCode = CameraStateCode.CAMERA_ERROR_UNKNOWN;
        switch (error) {
            case Camera.CAMERA_ERROR_SERVER_DIED:
                stateCode = CameraStateCode.CAMERA_ERROR_SERVER_DIED;
                break;
            case Camera.CAMERA_ERROR_UNKNOWN:
                stateCode = CameraStateCode.CAMERA_ERROR_UNKNOWN;
                break;
        }
        for (CameraStatesListener listener : mCameraStatesListenerList) {
            errorHandler = errorHandler | listener.onCameraStatesChanged(stateCode);
        }

        isOpenCamera = false;
        //如果该事件已经被处理，直接返回
        if (errorHandler) return;
        openFailCount++;

        try {
            if (mCamera != null)
                mCamera.reconnect();
            Log.d(TAG, "摄像头打开报错，重新连接");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 延迟打开摄像头
     */
    public void delayOpenCamera() {
        if (initCameraHandler != null) {
            initCameraHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    openCamera(currentCameraId);
                    setPreviewDisplay(previewTextureView);
                    startPreview();
                }
            }, (long) (Math.pow(openFailCount, 2) * 60_000));
        }
    }


    public List<Camera.Size> getCurrentCameraSupportSize() {
        List<Camera.Size> sizes = new ArrayList<>();
        if (mCamera != null) {
            sizes = mCamera.getParameters().getSupportedPreviewSizes();
        }
        return sizes;
    }

    public void registerCameraStatesListener(CameraStatesListener listener) {
        if (!mCameraStatesListenerList.contains(listener))
            mCameraStatesListenerList.add(listener);
    }

    public void unregisterCameraStatesListener(CameraStatesListener listener) {
        if (listener != null && mCameraStatesListenerList.contains(listener))
            mCameraStatesListenerList.remove(listener);
    }

    /**
     * 注册摄像头预览帧回调监听器
     *
     * @param mPreviewCallbackListener
     * @return 本次注册的监听器
     */
    public PreviewCallbackListener registerPreviewCallbackListener(PreviewCallbackListener mPreviewCallbackListener) {
        return registerPreviewCallbackListener(mPreviewCallbackListener, null);
    }

    /**
     * 注册摄像头预览帧回调监听器
     *
     * @param mPreviewCallbackListener
     * @return 本次注册的监听器
     */
    public synchronized PreviewCallbackListener registerPreviewCallbackListener(PreviewCallbackListener mPreviewCallbackListener, Camera.Size size) {
        if (mPreviewCallbackListener != null && !mPreviewCallbackListenerList.containsKey(mPreviewCallbackListener)) {
            if (size != null && (size.width > previewWidth && size.height > previewHeight)) {
                Camera.Parameters parameters = getCameraParameters();
                parameters.setPreviewSize(size.width, size.height);
                updateCameraParameters(parameters);
            }
            HandlerThread dispatchThread = new HandlerThread(mPreviewCallbackListener.toString());
            dispatchThread.start();
            DispatchHandler dispatchHandler = new DispatchHandler(dispatchThread.getLooper());
            dispatchHandler.size = size;
            mPreviewCallbackListenerList.put(mPreviewCallbackListener, dispatchHandler);
        }
        return mPreviewCallbackListener;
    }

    private class DispatchHandler extends Handler {
        Camera.Size size;

        public DispatchHandler(Looper looper) {
            super(looper);
        }
    }

    public synchronized void unregisterAllPreviewCallbackListener() {
        Set<PreviewCallbackListener> listeners = mPreviewCallbackListenerList.keySet();
        for (PreviewCallbackListener previewCallbackListener : listeners) {
            mPreviewCallbackListenerList.get(previewCallbackListener).getLooper().quit();
        }
        mPreviewCallbackListenerList.clear();
        runnables.clear();
    }

    /**
     * 注销该监听器
     *
     * @param mPreviewCallbackListener
     */
    public synchronized void unregisterPreviewCallbackListener(PreviewCallbackListener mPreviewCallbackListener) {
        if (mPreviewCallbackListener == null) return;
        Handler dispatchHandler = mPreviewCallbackListenerList.get(mPreviewCallbackListener);
        if (dispatchHandler != null) {
            runnables.remove(dispatchHandler);
            dispatchHandler.getLooper().quit();
        }
        mPreviewCallbackListenerList.remove(mPreviewCallbackListener);
    }


    Map<Camera.Size, byte[]> dispatchSizeMap = new HashMap<>();
    static Set<PreviewCallbackListener> listeners;
    private int size;

    /**
     * 摄像头预览数据回调
     *
     * @param data
     * @param camera
     */
    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera) {
        if (data == null) return;
        this.previewData = data;
        openFailCount = 0;//有视频流证明摄像头启动成功
        try {
            synchronized (this) {
                if (listeners == null || size != listeners.size())
                    listeners = mPreviewCallbackListenerList.keySet();
                size = listeners.size();
                dispatchSizeMap.clear();
                for (PreviewCallbackListener listener : listeners) {
                    final PreviewCallbackListener temp = listener;
                    DispatchHandler dispatchHandler = mPreviewCallbackListenerList.get(listener);
                    if (dispatchHandler == null) {
                        mPreviewCallbackListenerList.remove(listener);
                        continue;
                    }
                    Camera.Size size = dispatchHandler.size;
                    final byte[] dispatchData;
                    final int dispatchWidth;
                    final int dispatchHeight;
                    if (size == null || (this.previewWidth == size.width && this.previewHeight == size.height)) {
                        dispatchData = data;
                        dispatchWidth = this.previewWidth;
                        dispatchHeight = this.previewHeight;
                    } else {
                        Set<Camera.Size> dispatchSizeSet = dispatchSizeMap.keySet();
                        byte[] dispatchTemp = null;
                        for (Camera.Size dispatchSize : dispatchSizeSet) {
                            if (dispatchSize.width == size.width && dispatchSize.height == size.height) {
                                dispatchTemp = dispatchSizeMap.get(dispatchSize);
                                break;
                            }
                        }
                        if (dispatchTemp == null) {

                            dispatchTemp = new byte[size.width * size.height * 3 / 2];
                            byte[] yuvI420ToNV21 = new byte[dispatchTemp.length];
                            //YuvUtil.compressYUV(data, previewWidth, previewHeight, dispatchTemp, size.width, size.height, 0, 0, false);
                            //YuvUtil.yuvI420ToNV21(dispatchTemp, yuvI420ToNV21, size.width, size.height);
                            dispatchTemp = yuvI420ToNV21;
                            dispatchSizeMap.put(size, dispatchTemp);
                        }
                        dispatchData = dispatchTemp;
                        dispatchWidth = size.width;
                        dispatchHeight = size.height;
                    }

                    MyRunable runnable = runnables.get(dispatchHandler);
                    if (runnable == null) {
                        runnable = new MyRunable();
                        runnables.put(dispatchHandler, runnable);
                    }
                    runnable.distributePreviewFrame(temp, dispatchData, dispatchWidth, dispatchHeight);
                    dispatchHandler.post(runnable);
                }
            }
            if (mCamera != null)
                mCamera.addCallbackBuffer(data);

        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

    }


    private static class MyRunable implements Runnable {

        private PreviewCallbackListener temp;
        private byte[] dispatchData;
        private int dispatchWidth;
        private int dispatchHeight;

        public void distributePreviewFrame(final PreviewCallbackListener temp, final byte[] dispatchData, final int dispatchWidth, final int dispatchHeight) {
            this.temp = temp;
            this.dispatchData = dispatchData;
            this.dispatchWidth = dispatchWidth;
            this.dispatchHeight = dispatchHeight;
        }

        @Override
        public void run() {
            try {
                temp.onPreviewFrame(dispatchData, dispatchWidth, dispatchHeight);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }


    /**
     * 获取摄像头管理器
     *
     * @return
     */
    public static CameraManager getCameraManager() {
        if (cameraManager == null) {
            cameraManager = new CameraManager();
        }
        return cameraManager;
    }

    /**
     * 以SurfaceTexture方式设置摄像头预览画面
     *
     * @param surfaceTexture
     */
    public void setPreviewTexture(SurfaceTexture surfaceTexture) {
        try {
            mCamera.setPreviewTexture(surfaceTexture);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * 以SurfaceHolder方式设置摄像头预览画面
     *
     * @param holder
     */
    public void setPreviewDisplayOnHolder(SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(holder);
            this.previewTextureView = null;
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * 以TextureView方式设置摄像头预览画面
     *
     * @param preview
     */
    public void setPreviewDisplay(TextureView preview) {
        try {
            stopPreview();
            if (mCamera != null && preview != null)
                mCamera.setPreviewTexture(preview.getSurfaceTexture());
            this.previewTextureView = preview;
        } catch (Throwable t) {
            t.printStackTrace();
            Log.e("onErroronError", "设置预览异常==" + t.toString());
        }
    }

    /**
     * 拍照
     *
     * @param shutter
     * @param raw
     * @param jpeg
     */
    public void takePicture(Camera.ShutterCallback shutter, Camera.PictureCallback raw,
                            Camera.PictureCallback jpeg) {
        try {
            mCamera.takePicture(shutter, raw, jpeg);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * 拍照
     *
     * @param shutter
     * @param raw
     * @param jpeg
     */
    public void takePicture(Camera.ShutterCallback shutter, Camera.PictureCallback raw,
                            Camera.PictureCallback postview, Camera.PictureCallback jpeg) {
        try {
            mCamera.takePicture(shutter, raw, postview, jpeg);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * 摄像头是否在预览
     *
     * @return
     */
    public boolean isPreviewing() {
        return previewing;
    }


    private boolean previewing = false;


    /**
     * 开始摄像头预览
     */
    public void startPreview() {
        try {
            if (mCamera != null) {
                for (CameraStatesListener listener : mCameraStatesListenerList) {
                    listener.onCameraStatesChanged(CameraStateCode.BEFORE_START_PREVIEW);
                }
                setPreviewCallback();
                mCamera.startPreview();
                previewing = true;
                for (CameraStatesListener listener : mCameraStatesListenerList) {
                    listener.onCameraStatesChanged(CameraStateCode.AFTER_START_PREVIEW);
                }
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

    }

    /**
     * 停止预览
     */
    public void stopPreview() {
        if (mCamera != null) {
            for (CameraStatesListener listener : mCameraStatesListenerList) {
                listener.onCameraStatesChanged(CameraStateCode.BEFORE_STOP_PREVIEW);
            }
            mCamera.stopPreview();
            previewing = false;
        }
    }

    private CameraManager() {
    }

    /**
     * 获取共享摄像头管理器实例
     *
     * @return
     */
    public static final CameraManager getInstance() {
        if (cameraManager == null)
            cameraManager = new CameraManager();
        return cameraManager;
    }

    /**
     * 销毁当前管理器
     */
    public void destroy() {
        unregisterAllPreviewCallbackListener();
        releaseCamera();
        cameraManager = null;
    }

    //当前打开的摄像头ID
    private int currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;

    /**
     * 打开指定摄像头
     *
     * @param cameraId 指定摄像ID
     * @return 摄像打开与否
     */
    public synchronized boolean openCamera(final int cameraId) {
        if (initCameraHandler == null) {
            HandlerThread initCameraThread = new HandlerThread("camera init");
            initCameraThread.start();
            initCameraHandler = new Handler(initCameraThread.getLooper());
        }
        if (Looper.myLooper() == initCameraHandler.getLooper()) {
            synOpenCamera(cameraId);
        } else {
            final Object syn = new Object();
            synchronized (syn) {
                Log.e(TAG, "等待初始化摄像头开始锁定" + Thread.currentThread().getId());
                initCameraHandler.postAtFrontOfQueue(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (syn) {
                            synOpenCamera(cameraId);
                            syn.notifyAll();
                        }
                    }
                });
                try {
                    syn.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return isOpenCamera;
    }

    private void synOpenCamera(int cameraId) {
        if (currentCameraId == cameraId && mCamera != null) {
            isOpenCamera = true;
        } else {
            currentCameraId = cameraId;
            try {
                mCamera = Camera.open(currentCameraId);
               /* Camera.Parameters parameters = mCamera.getParameters();
                Camera.Size propPreviewSize = CamParaUtil.getInstance().getPropPreviewSize(parameters.getSupportedPreviewSizes());
                parameters.setPreviewSize(propPreviewSize.width, propPreviewSize.height);
                mCamera.setParameters(parameters);*/
                isOpenCamera = true;
                previewHeight = mCamera.getParameters().getPreviewSize().height;
                previewWidth = mCamera.getParameters().getPreviewSize().width;
                mCamera.setErrorCallback(CameraManager.this);
                mCamera.setDisplayOrientation(90);
                Log.d(TAG, "摄像头启动成功");
            } catch (Throwable e) {
                e.printStackTrace();
                isOpenCamera = false;
                Log.e(TAG, "摄像头启动失败 " + mCamera);
                mCamera = null;
                openFailCount++;
                if (reopen) {
                    delayOpenCamera();
                }
            }
        }
    }

    /**
     * 更新摄像头参数
     *
     * @param parameters
     */
    public void updateCameraParameters(Camera.Parameters parameters) {
        if (mCamera != null) {
            try {
                mCamera.setParameters(parameters);
                for (CameraStatesListener listener : mCameraStatesListenerList) {
                    listener.onCameraStatesChanged(CameraStateCode.PARAMETER_UPDATE);
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
                Log.e("updateCameraParameters", "设置摄像头参数错误");
                return;
            }
            previewHeight = parameters.getPreviewSize().height;
            previewWidth = parameters.getPreviewSize().width;
            setPreviewCallback();
        }
    }

    private void setPreviewCallback() {
        try {
            Camera.Parameters parameters = mCamera.getParameters();
            Camera.Size imageSize = parameters.getPreviewSize();
            previewWidth = imageSize.width;
            previewHeight = imageSize.height;
            mBuffer = new byte[previewWidth * previewHeight * 3 / 2];
            mCamera.setPreviewCallbackWithBuffer(this);
            mCamera.addCallbackBuffer(mBuffer);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取摄像头参数
     *
     * @return 当前摄像头使用参数
     */
    public Camera.Parameters getCameraParameters() {
        try {
            if (mCamera != null) return mCamera.getParameters();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 选择合适的FPS
     *
     * @param parameters
     * @param expectedThoudandFps 期望的FPS
     * @return
     */
    private int chooseFixedPreviewFps(Camera.Parameters parameters, int expectedThoudandFps) {
        List<int[]> supportedFps = parameters.getSupportedPreviewFpsRange();
        for (int[] entry : supportedFps) {
            if (entry[0] == entry[1] && entry[0] == expectedThoudandFps) {
                parameters.setPreviewFpsRange(entry[0], entry[1]);
                return entry[0];
            }
        }
        int[] temp = new int[2];
        int guess;
        parameters.getPreviewFpsRange(temp);
        if (temp[0] == temp[1]) {
            guess = temp[0];
        } else {
            guess = temp[1] / 2;
        }
        return guess;
    }


    /**
     * 根据CameraFacing参数打开后置或前置默认摄像头
     *
     * @param facing 前后置摄像头枚举类
     * @return 摄像头打开成功与否
     */
    public boolean openCamera(CameraFacing facing) {
        int numberOfCameras = Camera.getNumberOfCameras();
        int ifacing = Camera.CameraInfo.CAMERA_FACING_BACK;
        switch (facing) {
            case CAMERA_FACING_BACK:
                ifacing = Camera.CameraInfo.CAMERA_FACING_BACK;
                break;
            case CAMERA_FACING_FRONT:
                ifacing = Camera.CameraInfo.CAMERA_FACING_FRONT;
                break;
        }
        try {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            for (int i = 0; i < numberOfCameras; i++) {
                Camera.getCameraInfo(i, cameraInfo);
                if (cameraInfo.facing == ifacing) {
                    return openCamera(i);
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 打开默认摄像头（后置）
     *
     * @return 摄像头打开成功与否
     */
    public boolean openCamera() {
        if (mCamera == null) {
            try {
                int numberOfCameras = Camera.getNumberOfCameras();
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                for (int i = 0; i < numberOfCameras; i++) {
                    Camera.getCameraInfo(i, cameraInfo);
                    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                        openCamera(i);
                        break;
                    }
                }
                setPreviewDisplay(previewTextureView);
                startPreview();
            } catch (Throwable e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    /**
     * ----------------------------------------新旧代码分割线-------------------------------
     */


    public void setContext(Context context) {
        mContext = context;
    }

    public int getPreviewWidth() {
        return previewWidth;
    }

    public void setPreviewWidth(int previewWidth) {
        this.previewWidth = previewWidth;
    }

    public int getPreviewHeight() {
        return previewHeight;
    }

    public void setPreviewHeight(int previewHeight) {
        this.previewHeight = previewHeight;
    }


    /**
     * 释放摄像头资源
     */
    public synchronized void releaseCamera() {
        try {
            if (mCamera != null) {
                if (mPreviewCallbackListenerList.size() > 0) {
                    for (CameraStatesListener listener : mCameraStatesListenerList) {
                        listener.onCameraStatesChanged(CameraStateCode.RELEASE_FAIL_EXITS_PRE);
                    }
                    Log.i(TAG, "摄像头开始释放,返回");
                    return;
                }
                for (CameraStatesListener listener : mCameraStatesListenerList) {
                    listener.onCameraStatesChanged(CameraStateCode.BEFORE_RELEASE);
                }
                Log.i(TAG, "摄像头开始释放");
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.setPreviewCallbackWithBuffer(null);
                mCamera.release();
                isOpenCamera = false;
                previewing = false;
                for (CameraStatesListener listener : mCameraStatesListenerList) {
                    listener.onCameraStatesChanged(CameraStateCode.AFTER_RELEASE);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            Log.i(TAG, "摄像头释放异常");
        }
        mCamera = null;
        isOpenCamera = false;
        mCameraStatesListenerList.clear();
        if (initCameraHandler != null) {
            initCameraHandler.getLooper().quit();
            initCameraHandler = null;
        }
        Log.i(TAG, "摄像头释放完成");
    }


    public boolean isOpenCamera() {
        return isOpenCamera;
    }

    public byte[] getPreviewData() {
        return previewData;
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
