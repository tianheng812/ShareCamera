package com.xian.sharecamera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Camera
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import com.xian.camera.CameraManager
import com.xian.camera.utils.ImageUtils
import com.xian.camera.view.CameraSurfaceView

class MainActivity : AppCompatActivity() {

    lateinit var textureView: CameraSurfaceView
    lateinit var iv_takePicture: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textureView = findViewById(R.id.textureView);
        iv_takePicture = findViewById(R.id.iv_takePicture);
    }


    /**
     * 拍照
     */
    fun onTakePicture(v: View) {
        CameraManager.getInstance().takePicture(null, null, Camera.PictureCallback { data, camera ->
            var bitmap = BitmapFactory.decodeByteArray(data, 0, data.size);
            var returnBm=ImageUtils.rotateBitmapByDegree(bitmap,90)
            runOnUiThread { iv_takePicture.setImageBitmap(returnBm) }
        })
    }


    override fun onDestroy() {
        super.onDestroy()
        CameraManager.getInstance().releaseCamera()
    }
}
