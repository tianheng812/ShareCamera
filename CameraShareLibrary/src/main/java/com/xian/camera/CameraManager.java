package com.xian.camera;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.SurfaceHolder;
import android.view.TextureView;

import com.xian.camera.entity.DispatchHandler;
import com.xian.camera.enums.CameraStateCode;
import com.xian.camera.listeners.CameraStatesListener;
import com.xian.camera.listeners.PreviewCallbackListener;
import com.xian.camera.utils.CameraUtils;
import com.xian.camera.utils.LogUtils;
import com.xian.camera.utils.YuvUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by xian on 201 8/6/20.
 */
public class CameraManager implements Camera.PreviewCallback, Camera.ErrorCallback {
    //摄像头是否打开
    private boolean isOpenCamera = false;
    //当前打开的摄像头ID
    private int currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    //摄像头打开角度
    private int currentCameraOrientation = 90;
    //是否在预览
    private boolean previewing = false;
    private Camera mCamera;
    private int previewWidth = 640;
    private int previewHeight = 480;
    private static CameraManager cameraManager;
    private TextureView previewTextureView;
    private static List<CameraStatesListener> mCameraStatesListenerList = new ArrayList<>();
    private static Map<PreviewCallbackListener, DispatchHandler> mPreviewCallbackListenerList = new ConcurrentHashMap<>();
    private static Map<DispatchHandler, MyRunnable> runnables = new ConcurrentHashMap<>();
    private Handler initCameraHandler;//初始化摄像头线程
    private byte[] mBuffer; // 预览缓冲数据，使用可以让底层减少重复创建byte[]，起到重用的作用
    private int openFailCount = 0;//打开摄像头失败的次数
    private boolean reopen = false;//打开失败后，是否重新打开


    private CameraManager() {
    }

    /**
     * 获取共享摄像头管理器实例
     *
     * @return
     */
    public static final synchronized CameraManager getInstance() {
        if (cameraManager == null) {
            synchronized (CameraManager.class) {
                if (cameraManager == null) {
                    cameraManager = new CameraManager();
                }
            }
        }
        return cameraManager;
    }


    @Override
    public void onError(int error, Camera camera) {
        LogUtils.e("摄像头错误onError CODE= " + error + "===线程ID=>" + Thread.currentThread().getId());
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
            LogUtils.d("摄像头打开报错，重新连接");
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
                    openCamera(currentCameraId, previewWidth, previewHeight, currentCameraOrientation);
                    setPreviewDisplay(previewTextureView);
                    startPreview();
                }
            }, (long) (Math.pow(openFailCount, 2) * 60_000));
        }
    }


    /**
     * 返回摄像头支持的预览尺寸
     *
     * @return
     */
    public List<Camera.Size> getCameraSupportPreviewSizes() {
        List<Camera.Size> sizes = new ArrayList<>();
        if (mCamera != null) {
            sizes = mCamera.getParameters().getSupportedPreviewSizes();
        }
        return sizes;
    }


    /**
     * 返回摄像头支持的图片尺寸
     *
     * @return
     */
    public List<Camera.Size> getCameraSupportedPictureSizes() {
        List<Camera.Size> sizes = new ArrayList<>();
        if (mCamera != null) {
            sizes = mCamera.getParameters().getSupportedPictureSizes();
        }
        return sizes;
    }


    /**
     * 注册摄像头状态监听器
     *
     * @param listener
     */
    public void registerCameraStatesListener(CameraStatesListener listener) {
        if (!mCameraStatesListenerList.contains(listener))
            mCameraStatesListenerList.add(listener);
    }

    /**
     * 注销摄像头状态监听器
     *
     * @param listener
     */
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
        return registerPreviewCallbackListener(mPreviewCallbackListener, previewWidth, previewHeight);
    }


    /**
     * 注册摄像头预览帧回调监听器
     *
     * @param mPreviewCallbackListener
     * @param scaleWidth               缩放的宽度
     * @param scaleHeight              缩放的高度
     * @return 本次注册的监听器
     */
    public synchronized PreviewCallbackListener registerPreviewCallbackListener(PreviewCallbackListener mPreviewCallbackListener, int scaleWidth, int scaleHeight) {
        if (scaleWidth <= 0 || scaleHeight <= 0) {
            throw new IllegalArgumentException("width and height must be > 0");
        }

        if (mPreviewCallbackListener != null && !mPreviewCallbackListenerList.containsKey(mPreviewCallbackListener)) {
            if (scaleWidth > previewWidth && scaleHeight > previewHeight) {
                Camera.Parameters parameters = getCameraParameters();
                if (parameters != null && CameraUtils.getInstance().checkPreviewSize(parameters.getSupportedPreviewSizes(), scaleWidth, scaleHeight)) {
                    parameters.setPreviewSize(scaleWidth, scaleHeight);
                    updateCameraParameters(parameters);
                }
            }
            HandlerThread dispatchThread = new HandlerThread(mPreviewCallbackListener.toString());
            dispatchThread.start();
            DispatchHandler dispatchHandler = new DispatchHandler(dispatchThread.getLooper());
            dispatchHandler.width = scaleWidth;
            dispatchHandler.height = scaleHeight;
            mPreviewCallbackListenerList.put(mPreviewCallbackListener, dispatchHandler);
        }
        return mPreviewCallbackListener;
    }


    /**
     * 注销所有的预览监听
     */
    public synchronized void unregisterAllPreviewCallbackListener() {
        Set<PreviewCallbackListener> listeners = mPreviewCallbackListenerList.keySet();
        for (PreviewCallbackListener previewCallbackListener : listeners) {
            mPreviewCallbackListenerList.get(previewCallbackListener).getLooper().quit();
        }
        mPreviewCallbackListenerList.clear();
        runnables.clear();
    }

    /**
     * 注销预览监听器
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


    Map<String, byte[]> dispatchSizeMap = new HashMap<>();
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
                    int width = dispatchHandler.width;
                    int height = dispatchHandler.height;
                    final byte[] dispatchData;
                    final int dispatchWidth;
                    final int dispatchHeight;
                    if (this.previewWidth == width && this.previewHeight == height) {
                        dispatchData = data;
                        dispatchWidth = this.previewWidth;
                        dispatchHeight = this.previewHeight;
                    } else {
                        Set<String> dispatchSizeSet = dispatchSizeMap.keySet();
                        byte[] dispatchTemp = null;
                        for (String dispatchSize : dispatchSizeSet) {
                            String[] xes = dispatchSize.split("X");
                            int x = Integer.parseInt(xes[0]);
                            int y = Integer.parseInt(xes[1]);
                            if (x == width && y == height) {
                                dispatchTemp = dispatchSizeMap.get(dispatchSize);
                                break;
                            }
                        }
                        if (dispatchTemp == null) {
                            dispatchTemp = new byte[width * height * 3 / 2];
                            byte[] yuvI420ToNV21 = new byte[dispatchTemp.length];
                            YuvUtil.compressYUV(data, previewWidth, previewHeight, dispatchTemp, width, height, 0, 0, false);
                            YuvUtil.yuvI420ToNV21(dispatchTemp, yuvI420ToNV21, width, height);
                            dispatchTemp = yuvI420ToNV21;
                            dispatchSizeMap.put(width + "X" + height, dispatchTemp);
                        }
                        dispatchData = dispatchTemp;
                        dispatchWidth = width;
                        dispatchHeight = height;
                    }

                    MyRunnable runnable = runnables.get(dispatchHandler);
                    if (runnable == null) {
                        runnable = new MyRunnable();
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


    private static class MyRunnable implements Runnable {

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
     * 以SurfaceTexture方式设置摄像头预览画面
     *
     * @param surfaceTexture
     */
    public void setPreviewTexture(SurfaceTexture surfaceTexture) {
        try {
            if (mCamera != null) {
                mCamera.setPreviewTexture(surfaceTexture);
            }
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
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
                this.previewTextureView = null;
            }
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
            LogUtils.e("设置预览异常==" + t.toString());
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
            if (mCamera != null) {
                mCamera.takePicture(shutter, raw, jpeg);
            }
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
            if (mCamera != null) {
                mCamera.takePicture(shutter, raw, postview, jpeg);
            }
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


    /**
     * 销毁摄像头管理器，会释放摄像头
     */
    public void destroyCamera() {
        unregisterAllPreviewCallbackListener();
        releaseCamera();
        cameraManager = null;
    }


    /**
     * 打开摄像头
     *
     * @param cameraId 前后置摄像头  0是后置 1是前置
     * @return 摄像头打开成功与否
     */
    public synchronized boolean openCamera(final int cameraId, final int previewWidth, final int previewHeight, final int orientation) {
        if (initCameraHandler == null) {
            HandlerThread initCameraThread = new HandlerThread("camera init");
            initCameraThread.start();
            initCameraHandler = new Handler(initCameraThread.getLooper());
        }
        if (Looper.myLooper() == initCameraHandler.getLooper()) {
            synOpenCamera(cameraId, previewWidth, previewHeight, orientation);
        } else {
            final Object syn = new Object();
            synchronized (syn) {
                LogUtils.e("等待初始化摄像头开始锁定" + Thread.currentThread().getId());
                initCameraHandler.postAtFrontOfQueue(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (syn) {
                            synOpenCamera(cameraId, previewWidth, previewHeight, orientation);
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

    private void synOpenCamera(int cameraId, int previewWidth, int previewHeight, int orientation) {
        if (currentCameraId == cameraId && mCamera != null) {
            isOpenCamera = true;
        } else {
            currentCameraId = cameraId;
            try {
                mCamera = Camera.open(currentCameraId);
                if (previewWidth != 0 && previewHeight != 0) {
                    Camera.Parameters parameters = mCamera.getParameters();
                    Camera.Size propPreviewSize = CameraUtils.getInstance().getPropPreviewSize(parameters.getSupportedPreviewSizes(), previewWidth, previewHeight);
                    parameters.setPreviewSize(propPreviewSize.width, propPreviewSize.height);
                    mCamera.setParameters(parameters);
                }
                isOpenCamera = true;
                this.previewHeight = mCamera.getParameters().getPreviewSize().height;
                this.previewWidth = mCamera.getParameters().getPreviewSize().width;

                LogUtils.d("预览的宽度：" + this.previewWidth + " 高度：" + this.previewHeight);
                mCamera.setErrorCallback(CameraManager.this);
                if (orientation % 45 == 0) {
                    mCamera.setDisplayOrientation(orientation);
                } else {
                    mCamera.setDisplayOrientation(90);
                    currentCameraId = 90;
                }
                LogUtils.d("摄像头启动成功");
            } catch (Throwable e) {
                e.printStackTrace();
                isOpenCamera = false;
                LogUtils.d("摄像头启动失败 " + mCamera);
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
                LogUtils.e("设置摄像头参数错误");
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
     * 打开默认摄像头（后置）
     */
    public boolean openCamera() {
        if (mCamera == null) {
            try {
                int numberOfCameras = Camera.getNumberOfCameras();
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                for (int i = 0; i < numberOfCameras; i++) {
                    Camera.getCameraInfo(i, cameraInfo);
                    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                        openCamera(i, 0, 0, 90);
                        break;
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }


    /**
     * 打开摄像头
     *
     * @param cameraId
     */
    public void openCamera(int cameraId) {
        openCamera(cameraId, 0, 0, 90);
    }


    /**
     * 打开摄像头
     *
     * @param cameraId
     * @param orientation 摄像头角度
     */
    public void openCamera(int cameraId, int orientation) {
        openCamera(cameraId, 0, 0, orientation);
    }


    /**
     * 获取摄像头的预览宽度
     *
     * @return
     */
    public int getPreviewWidth() {
        return previewWidth;
    }

    /**
     * 获取摄像头的预览高度
     *
     * @return
     */
    public int getPreviewHeight() {
        return previewHeight;
    }


    /**
     * 释放摄像头资源，如果多个地方注册监听器，是不会释放摄像头的，如果想释放摄像头，请使用
     */
    public synchronized void releaseCamera() {
        try {
            if (mCamera != null) {
                if (mPreviewCallbackListenerList.size() > 0) {
                    for (CameraStatesListener listener : mCameraStatesListenerList) {
                        listener.onCameraStatesChanged(CameraStateCode.RELEASE_FAIL_EXITS_PRE);
                    }
                    LogUtils.d("摄像头开始释放,返回");
                    return;
                }
                for (CameraStatesListener listener : mCameraStatesListenerList) {
                    listener.onCameraStatesChanged(CameraStateCode.BEFORE_RELEASE);
                }
                LogUtils.d("摄像头开始释放");
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
            LogUtils.e("摄像头释放异常");
        }
        mCamera = null;
        isOpenCamera = false;
        mCameraStatesListenerList.clear();
        if (initCameraHandler != null) {
            initCameraHandler.getLooper().quit();
            initCameraHandler = null;
        }
        LogUtils.d("摄像头释放完成");
    }


    /**
     * 获取摄像头是否打开
     *
     * @return
     */
    public boolean isOpenCamera() {
        return isOpenCamera;
    }


    /**
     * 设置自动聚焦
     *
     * @param callback
     */
    public void autoFocus(Camera.AutoFocusCallback callback) {
        try {
            if (mCamera != null)
                mCamera.autoFocus(callback);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }




}
