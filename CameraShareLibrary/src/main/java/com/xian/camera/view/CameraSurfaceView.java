package com.xian.camera.view;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

import com.xian.camera.CameraManager;


public class CameraSurfaceView extends TextureView implements TextureView.SurfaceTextureListener {

    private Matrix matrix = new Matrix();

    public CameraSurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        onMirror();
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        onMirror();
    }

    public CameraSurfaceView(Context context) {
        super(context);
        onMirror();
    }

    boolean hasMirror = false;

    public void onMirror() {
        hasMirror = true;
        setSurfaceTextureListener(this);
    }

    public void setMatrix(int w) {
        matrix.setScale(-1, 1, w / 2.0f, 0);
        setTransform(matrix);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        CameraManager.getInstance().openCamera();
        CameraManager.getInstance().setPreviewDisplay(this);
        CameraManager.getInstance().startPreview();
        Log.i("CameraSurfaceView  ", " 宽度：" + width);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}
