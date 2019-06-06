package com.xian.camera.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;


/**
 * Created by xian on 2019/6/5.
 */
public class FocusCirceView extends View {
    private Paint paint;
    private static final String TAG = "FocusCirceView";
    private float mX = getWidth() / 2; //默认
    private float mY = getHeight() / 2;
    private boolean isDrawCircle = false;
    private Handler handler;

    public FocusCirceView(Context context) {
        this(context, null);
    }

    public FocusCirceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        handler = new Handler();
    }

    public void setPoint(float x, float y) {
        this.mX = x;
        this.mY = y;
        isDrawCircle = true;
    }

    //重写draw方法
    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        //实例化画笔
        //给画笔设置颜色 #f4f4f4
        paint.setColor(Color.parseColor("#cccccc"));
        //设置画笔属性
        paint.setStyle(Paint.Style.STROKE);//空心圆
        paint.setStrokeWidth(4);
        if (isDrawCircle) {
            canvas.drawCircle(mX, mY, 20, paint);
            canvas.drawCircle(mX, mY, 90, paint);
        }
    }

    public void deleteCanvas() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                paint.reset();
                isDrawCircle = false;
                invalidate();
            }
        }, 1000);

    }

    /***
     * 缩放动画
     */
    public void myViewScaleAnimation(View myView) {
        ScaleAnimation animation = new ScaleAnimation(1.1f, 1f, 1.1f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        animation.setDuration(300);
        animation.setFillAfter(false);
        animation.setRepeatCount(0);
        myView.startAnimation(animation);
    }

}