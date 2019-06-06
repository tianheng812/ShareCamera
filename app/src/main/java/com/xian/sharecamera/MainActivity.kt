package com.xian.sharecamera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.hardware.Camera
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import com.xian.camera.CameraManager
import com.xian.camera.listeners.PreviewCallbackListener
import com.xian.camera.utils.ImageUtils
import com.xian.camera.utils.LogUtils
import com.xian.camera.view.CameraFocusView

class MainActivity : AppCompatActivity() {


    lateinit var iv_takePicture: ImageView
    lateinit var cameraFocusView: CameraFocusView

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        iv_takePicture = findViewById(R.id.iv_takePicture);
        cameraFocusView = findViewById(R.id.textureView)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.CAMERA),
                        100);
            }
        }
    }


    var previewCallBackListener = PreviewCallbackListener { data, scaleWidth, scaleHeight ->
        //尽量不要在这里做耗时操作
        val yuvToBitMap = ImageUtils.yuvToBitMap(data, scaleWidth, scaleHeight,90)
        LogUtils.d("图片的宽度：" + yuvToBitMap.width + " --高度--> " + yuvToBitMap.height)
    }

    /**
     * 拍照
     */
    fun onTakePicture(v: View) {
        CameraManager.getInstance().takePicture(null, null, Camera.PictureCallback { data, camera ->
            var bitmap = BitmapFactory.decodeByteArray(data, 0, data.size);
            var returnBm = ImageUtils.rotateBitmapByDegree(bitmap, 90)
            runOnUiThread {
                iv_takePicture.setImageBitmap(returnBm)
                CameraManager.getInstance().startPreview() //重新打开预览，要不会停止预览
            }
        })
    }


    override fun onResume() {
        super.onResume()
        //注： 如何想返回和预览尺寸一样大的，请使用CameraManager.getInstance().registerPreviewCallbackListener(previewCallBackListener)
        CameraManager.getInstance().registerPreviewCallbackListener(previewCallBackListener, 640, 480)
    }

    override fun onStop() {
        super.onStop()
        CameraManager.getInstance().unregisterPreviewCallbackListener(previewCallBackListener)
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == 100) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //判断是否勾选禁止后不再询问
                CameraManager.getInstance().openCamera()
                CameraManager.getInstance().setPreviewDisplay(cameraFocusView.cameraSurfaceView)
                CameraManager.getInstance().startPreview()
            } else {
                Toast.makeText(this, "摄像头权限被拒绝", Toast.LENGTH_SHORT).show()
            }
        }

    }

}
