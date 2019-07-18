package com.xian.camera.view;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.TextureView;

import com.xian.camera.CameraManager;


public class CameraSurfaceView extends TextureView implements TextureView.SurfaceTextureListener {

    private int cameraId;
    private int orientation;
    private int previewWidth;
    private int previewHeight;
    private boolean mirror;

    private Matrix matrix = new Matrix();
    private int w = 0;

    public int getCameraId() {
        return cameraId;
    }

    public void setCameraId(int cameraId) {
        this.cameraId = cameraId;
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
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

    public boolean isMirror() {
        return mirror;
    }

    public void setMirror(boolean mirror) {
        this.mirror = mirror;
    }

    public CameraSurfaceView(Context context) {
        this(context, null);
    }


    public CameraSurfaceView(Context context, int cameraId, int previewWidth, int previewHeight, int orientation, boolean mirror) {
        this(context, null);
        this.cameraId = cameraId;
        this.previewHeight = previewHeight;
        this.previewWidth = previewWidth;
        this.orientation = orientation;
        this.mirror = mirror;
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setSurfaceTextureListener(this);
    }

    /**
     * 设置镜像
     */
    public void startMirror() {
        if (matrix == null) return;
        matrix.setScale(-1, 1, w / 2.0f, 0);
        setTransform(matrix);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        CameraManager.getInstance().openCamera(cameraId, previewWidth, previewHeight, orientation);
        CameraManager.getInstance().setPreviewDisplay(this);
        CameraManager.getInstance().startPreview();
        w = width;
        if (w != 0 && mirror) {
            startMirror();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        if (w != width) {
            w = width;
            if (mirror) {
                startMirror();
            }
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        CameraManager.getInstance().releaseCamera();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }


}
