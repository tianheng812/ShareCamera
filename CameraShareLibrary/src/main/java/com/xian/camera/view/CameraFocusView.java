package com.xian.camera.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.gdsayee.camera_share.R;
import com.xian.camera.CameraManager;

/**
 * Created by xian on 2019/6/5.
 */

public class CameraFocusView extends FrameLayout implements View.OnTouchListener {

    private int cameraId;//前后摄像头
    private int orientation;//预览角度
    private int previewWidth;//预览的宽度
    private int previewHeight;//预览的高度
    private boolean mirror;//镜像
    private FocusCirceView focusCirceView;
    private CameraSurfaceView cameraSurfaceView;

    public CameraFocusView(Context context) {
        this(context, null);
    }

    public CameraFocusView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraFocusView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.share_camera);
        //获取对应的属性值
        this.cameraId = a.getInt(R.styleable.share_camera_camera_id, 0);
        this.orientation = a.getInt(R.styleable.share_camera_display_orientation, 90);
        this.previewWidth = a.getInt(R.styleable.share_camera_preview_width, 0);
        this.previewHeight = a.getInt(R.styleable.share_camera_preview_height, 0);
        this.mirror = a.getBoolean(R.styleable.share_camera_preview_mirror, false);

        focusCirceView = new FocusCirceView(context);
        cameraSurfaceView = new CameraSurfaceView(context);
        cameraSurfaceView.setMirror(mirror);
        cameraSurfaceView.setCameraId(cameraId);
        cameraSurfaceView.setOrientation(orientation);
        cameraSurfaceView.setPreviewWidth(previewWidth);
        cameraSurfaceView.setPreviewHeight(previewHeight);

        setOnTouchListener(this);
        addView(cameraSurfaceView, new
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        addView(focusCirceView, new
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)); //添加视图FocusCirceView
    }


    public CameraSurfaceView getCameraSurfaceView() {
        return cameraSurfaceView;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN://0
                float x = event.getX();
                float y = event.getY();
                if (focusCirceView != null) {
                    focusCirceView.myViewScaleAnimation(focusCirceView);//动画效果
                    focusCirceView.setPoint(x, y);

                }
                CameraManager.getInstance().autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {

                    }
                });
            case MotionEvent.ACTION_UP:
                //抬起时清除画布,并移除视图
                focusCirceView.deleteCanvas();
        }
        return true;
    }


}
