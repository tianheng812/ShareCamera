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


import com.xian.camera.enums.CameraFacing;
import com.xian.camera.enums.CameraStateCode;
import com.xian.camera.listeners.CameraStatesListener;
import com.xian.camera.listeners.PreviewCallbackListener;
import com.xian.camera.utils.CameraUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private List<CameraStatesListener> mCameraStatesListenerList = new ArrayList<>();
    private Map<PreviewCallbackListener, DispatchHandler> mPreviewCallbackListenerList = new HashMap<>();
    private Handler initCameraHandler;//初始化摄像头线程

    @Override
    public void onError(int error, Camera camera) {
        Log.e("onPreviewFrameerror", "摄像头错误onError CODE=" + error + "===线程ID=>" + Thread.currentThread().getId());
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
        //如果该事件已经被处理，直接返回
        if (errorHandler) return;
        try {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.setPreviewCallbackWithBuffer(null);
            mCamera.release();
            isOpenCamera = false;
            mCamera = null;
            Log.i(TAG, "11摄像头开始释放11");
        } catch (Exception e) {
            e.printStackTrace();
        }
        openCamera(currentCameraId);
        setPreviewDisplay(previewTextureView);
        startPreview();
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
            dispatchHandler.getLooper().quit();
        }
        mPreviewCallbackListenerList.remove(mPreviewCallbackListener);
    }


    Map<Camera.Size, byte[]> dispatchSizeMap = new HashMap<>();

    long logFlag = System.currentTimeMillis();//摄像头预览打印log控制

    /**
     * 摄像头预览数据回调
     *
     * @param data
     * @param camera
     */
    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera) {
        final boolean printLog;

        if ((System.currentTimeMillis() - logFlag) > 5000) {//每隔2秒打印一次日志
            printLog = true;
            logFlag = System.currentTimeMillis();
        } else {
            printLog = false;
        }
        if (printLog)
            Log.e("onPreviewFrame", "摄像头预览分辨率：" + previewWidth + "x" + previewHeight);
        //Log.e("onPreviewFrame","---"+camera.getParameters().getPreviewSize().width+" "+Thread.currentThread().getId());
        //        camera.addCallbackBuffer(data);
        if (data == null) return;
        this.previewData = data;
        Set<PreviewCallbackListener> listeners = mPreviewCallbackListenerList.keySet();
        dispatchSizeMap.clear();
        synchronized (this) {
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
                dispatchHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (printLog)
                            Log.e("onPreviewFrame", "此次分发分辨率：" + dispatchWidth + "x" + dispatchHeight + "-->分发对象：" + temp);
                        temp.onPreviewFrame(dispatchData, dispatchWidth, dispatchHeight);
                    }
                });

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
     * 以TextureView方式设置摄像头预览画面
     *
     * @param surfaceTexture
     */
    public void setPreviewDisplay(SurfaceTexture surfaceTexture) {
        try {
            stopPreview();
            if (mCamera != null && surfaceTexture != null)
                mCamera.setPreviewTexture(surfaceTexture);
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
     * 打开摄像头
     */
    private CameraManager() {
        openCamera();
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
    private int currentCameraId = -1;

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
                mCamera = Camera.open(cameraId);
                Camera.Parameters parameters = mCamera.getParameters();
                Camera.Size propPreviewSize = CameraUtils.getInstance().getPropPreviewSize(parameters.getSupportedPreviewSizes());
                parameters.setPreviewSize(propPreviewSize.width, propPreviewSize.height);
                mCamera.setParameters(parameters);
                isOpenCamera = true;
                previewHeight = mCamera.getParameters().getPreviewSize().height;
                previewWidth = mCamera.getParameters().getPreviewSize().width;
                mCamera.setErrorCallback(CameraManager.this);
                mCamera.setDisplayOrientation(getCameraDisplayOrientation());
                Log.e(TAG, "摄像头启动成功");
            } catch (Exception e) {
                e.printStackTrace();
                isOpenCamera = false;
                mCamera = null;
                Log.e(TAG, "摄像头启动失败");
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
            Camera.Size imageSize = mCamera.getParameters().getPreviewSize();
            int lineBytes = imageSize.width * ImageFormat.getBitsPerPixel(mCamera.getParameters().getPreviewFormat()) / 8;
            previewWidth = imageSize.width;
            previewHeight = imageSize.height;
            //mCamera.addCallbackBuffer(new byte[lineBytes * imageSize.height]);
            //mCamera.addCallbackBuffer(new byte[lineBytes * imageSize.height]);
            mCamera.setPreviewCallback(this);
        } catch (Exception e) {
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
            } catch (Exception e) {
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


    public int getCameraDisplayOrientation() {
        int degrees = 90;
        if (mContext != null) {
            DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
            int heigth = dm.heightPixels;
            int width = dm.widthPixels;
            if (heigth < width) {
                degrees = 0;
            }
        }
        return degrees;
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
    public void releaseCamera() {
        try {
            if (mCamera != null) {
                if (mPreviewCallbackListenerList.size() > 0) {
                    for (CameraStatesListener listener : mCameraStatesListenerList) {
                        listener.onCameraStatesChanged(CameraStateCode.RELEASE_FAIL_EXITS_PRE);
                    }
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
        } catch (Exception e) {
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
