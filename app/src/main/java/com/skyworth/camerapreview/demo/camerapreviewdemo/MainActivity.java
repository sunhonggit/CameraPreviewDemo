package com.skyworth.camerapreview.demo.camerapreviewdemo;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;

public class MainActivity extends Activity implements LiveCameraView.LivePreviewCallback, LiveCameraView.CameraPreviewResultCallback {
    LiveCameraView liveCameraPreview;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        liveCameraPreview = (LiveCameraView) findViewById(R.id.id_detect_surface_view);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startPreview();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPreview();
    }

    private void startPreview() {
        liveCameraPreview.setLivePreviewCallback(this);
        liveCameraPreview.setPreviewResultCallback(this);
        liveCameraPreview.startPreview();
    }

    private void stopPreview() {
        liveCameraPreview.setLivePreviewCallback(null);
        liveCameraPreview.setPreviewResultCallback(null);
        liveCameraPreview.stopPreview();
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {

    }

    @Override
    public void onSuccess(int previewWidth, int previewHeight, int cameraId) {

    }

    @Override
    public void onError(int reason) {

    }
}
