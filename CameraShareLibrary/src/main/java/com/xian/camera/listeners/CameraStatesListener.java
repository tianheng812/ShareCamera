package com.xian.camera.listeners;

import com.xian.camera.enums.CameraStateCode;

/**
 * Created by xian on 2018/12/21.
 * 摄像头状态监听
 */
public interface CameraStatesListener {

    boolean onCameraStatesChanged(CameraStateCode stateCode);
}
