# ShareCamera
android 摄像头共享封装


1.实现自定义的摄像头拍照，可以点击聚焦
2.实现预览数据压缩到想要的尺寸回调
3.支持布局文件配置摄像头的参数


在根的build.gradle中添加如下：

    allprojects {
        repositories {
            maven { url "https://raw.githubusercontent.com/tianheng812/ShareCamera/master" }
        }
     }

在项目的build.gradle中添加依赖

    implementation 'com.xian:sharecamera:1.0.1'


在要使用的xml添加入

```
<!--
    camera_id 0后置摄像头，1,前置摄像头
    display_orientation  角度
    preview_width  预览的宽度
    preview_height  预览的高度
    preview_mirror  是否需要镜像
    -->
    <com.xian.camera.view.CameraFocusView
        android:id="@+id/textureView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:camera_id="1"
        app:display_orientation="90"
        app:preview_height="1080"
        app:preview_mirror="false"
        app:preview_width="1920" />
```


申请摄像头权限

```
 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
       if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},100);
       }
  }


 @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 100) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //判断是否勾选禁止后不再询问
                CameraManager.getInstance().openCamera();
                CameraManager.getInstance().setPreviewDisplay(textureView.getCameraSurfaceView());
                CameraManager.getInstance().startPreview();
            } else {
                Toast.makeText(this, "摄像头权限被拒绝", Toast.LENGTH_SHORT).show();
            }
        }

    }

```



拍照：
```
	CameraManager.getInstance().takePicture(null, null, new Camera.PictureCallback() {
	            @Override
	            public void onPictureTaken(byte[] data, Camera camera) {
	                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
	                final Bitmap returnBm = ImageUtils.rotateBitmapByDegree(bitmap, 90);
	                runOnUiThread(new Runnable() {
	                    @Override
	                    public void run() {
	                        iv.setImageBitmap(returnBm);
	                        CameraManager.getInstance().startPreview(); //重新打开预览，要不会停止预览
	                    }
	                });
	            }
	        });
```

预览数据压缩：
```
    protected void onResume() {
        super.onResume();
        //注： 如何想返回和预览尺寸一样大的，请使用CameraManager.getInstance().registerPreviewCallbackListener(previewCallBackListener)
        //缩放640 480数据返回
        CameraManager.getInstance().registerPreviewCallbackListener(previewCallBackListener, 640, 480);
    }
   
        
    protected void onStop() {
        super.onStop();
        CameraManager.getInstance().unregisterPreviewCallbackListener(previewCallBackListener);
    }
    
    PreviewCallbackListener previewCallBackListener=new PreviewCallbackListener(){
    
        //预览数据压缩回调 
        public void onPreviewFrame(byte[] data, int scaleWidth, int scaleHeight) {
            Bitmap yuvToBitMap = ImageUtils.yuvToBitMap(data, scaleWidth, scaleHeight,90);
            LogUtils.d("图片的宽度：" + yuvToBitMap.getWidth() + " --高度--> " + yuvToBitMap.getHeight());
        }
    };
```

有问题可以qq联系: 812892724

