package com.xian.camera.enums;

/**
 * Created by xian on 2018/12/21.
 */

//摄像头状态码
public enum CameraStateCode {

    BEFORE_START_PREVIEW(1), //开始预览之前
    AFTER_START_PREVIEW(8), //开始预览之后
    BEFORE_STOP_PREVIEW(2), //停止预览之前
    BEFORE_RELEASE(3),//摄像头释放之前
    AFTER_RELEASE(4),//摄像头释放之后
    PARAMETER_UPDATE(5),//摄像头参数被修改
    CAMERA_ERROR_UNKNOWN(6),//摄像头发生未知错误
    CAMERA_ERROR_SERVER_DIED(7),//摄像头服务已经终止
    RELEASE_FAIL_EXITS_PRE(8);//有预览回调监听器为注销释放摄像失败失败

    int code;

    CameraStateCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}