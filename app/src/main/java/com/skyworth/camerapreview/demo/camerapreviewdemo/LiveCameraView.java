package com.skyworth.camerapreview.demo.camerapreviewdemo;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by sunhong on 2017/11/1 0001.
 */

public class LiveCameraView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback, Camera.ErrorCallback {
    private final static String TAG = LiveCameraView.class.getSimpleName();
    private Context mContext;
    private Camera mCamera;
    protected Camera.Parameters mCameraParam;
    protected int mCameraId;
    private SurfaceHolder mSurfaceHolder;
    private LivePreviewCallback mLivePreviewCallback;
    private CameraPreviewResultCallback mPreviewCallback;
    public final static int ERROR_NO_CAMERA_DEVICE = 0XFFEE;
    public final static int ERROR_DEVICE_OPEN_ERROR = ERROR_NO_CAMERA_DEVICE + 1;
    public int PREVIEW_WIDTH = 1280;
    public int PREVIEW_HEIGHT = 720;

    public LiveCameraView(Context context) {
        super(context);
        init(context);
    }

    public LiveCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LiveCameraView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        Log.d(TAG, "LiveCameraView initialize");
        this.mContext = context;
        mSurfaceHolder = this.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public Camera getCameraInstance() {
        return mCamera;
    }

    public interface LivePreviewCallback {
        void onPreviewFrame(byte[] bytes, Camera camera);
    }

    public interface CameraPreviewResultCallback {
        void onSuccess(int previewWidth, int previewHeight, int cameraId);
        void onError(int reason);
    }

    public void setPreviewResultCallback(CameraPreviewResultCallback callback) {
        this.mPreviewCallback = callback;
    }

    public void setLivePreviewCallback(LivePreviewCallback callback) {
        this.mLivePreviewCallback = callback;
    }

    /**
     * open camera if exist
     * @return
     */
    private Camera openCamera() {
        Camera camera;
        int numCameras = Camera.getNumberOfCameras();
        if (numCameras == 0) {
            return null;
        }

        int index = 0;
//        while (index < numCameras) {
//            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
//            Camera.getCameraInfo(index, cameraInfo);
//            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//                break;
//            }
//            index++;
//        }

        try {
            if (index < numCameras) {
                camera = Camera.open(index);
                mCameraId = index;
            } else {
                camera = Camera.open(0);
                mCameraId = 0;
            }
        } catch (Exception e) {
            camera = null;
        }
        return camera;
    }

    private int displayOrientation(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
            default:
                degrees = 0;
                break;
        }
        int result = (0 - degrees + 360) % 360;
        if (Build.VERSION.SDK_INT >= 9) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(mCameraId, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = (info.orientation + degrees) % 360;
                result = (360 - result) % 360;
            } else {
                result = (info.orientation - degrees + 360) % 360;
            }
        }
        return result;
    }

    protected Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w / h;
        if (sizes == null)
            return null;
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        int targetHeight = h;
        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        } // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    /** 输出的照片为最高像素 */
    public Camera.Size getPictureSize(Camera.Parameters parametes) {
        List<Camera.Size> localSizes = parametes.getSupportedPictureSizes();
        Camera.Size biggestSize = null;
        Camera.Size fitSize = null;// 优先选预览界面的尺寸
        Camera.Size previewSize = parametes.getPreviewSize();
        float previewSizeScale = 0;
        if(previewSize != null) {
            previewSizeScale = previewSize.width / (float) previewSize.height;
        }

        if(localSizes != null) {
            int cameraSizeLength = localSizes.size();
            for (int n = 0; n < cameraSizeLength; n++) {
                Camera.Size size = localSizes.get(n);
                if(biggestSize == null) {
                    biggestSize = size;
                } else if(size.width >= biggestSize.width && size.height >= biggestSize.height) {
                    biggestSize = size;
                }

                // 选出与预览界面等比的最高分辨率
//                if(previewSizeScale > 0
//                        && size.width >= previewSize.width && size.height >= previewSize.height) {
//                    float sizeScale = size.width / (float) size.height;
//                    if(sizeScale == previewSizeScale) {
//                        if(fitSize == null) {
//                            fitSize = size;
//                        } else if(size.width >= fitSize.width && size.height >= fitSize.height) {
//                            fitSize = size;
//                        }
//                    }
//                }
            }

            // 如果没有选出fitSize, 那么最大的Size就是FitSize
            if(fitSize == null) {
                fitSize = biggestSize;
            }
//            parametes.setPictureSize(fitSize.width, fitSize.height);
        }
        return biggestSize;
    }

    protected Camera.Size getOptimalEqualPreviewSize(List<Camera.Size> sizes, int w, int h) {
        Camera.Size optimalSize = null;
        for (Camera.Size size : sizes) {
            if (size.width == w && size.height == h) {
                optimalSize = size;
                return optimalSize;
            }
        }
        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            optimalSize = getOptimalPreviewSize(sizes,
                    PREVIEW_WIDTH, PREVIEW_HEIGHT);
        }
        return optimalSize;
    }

    public void startPreview() {
        if (mSurfaceHolder != null) {
            mSurfaceHolder.addCallback(this);
        }

        if(null == mCamera) {
            mCamera = openCamera();
        }

        if(null == mCamera) {
            if(null != mPreviewCallback) {
                mPreviewCallback.onError(ERROR_NO_CAMERA_DEVICE);
            }
            return;
        }

        mCameraParam = mCamera.getParameters();
        mCameraParam.setPictureFormat(PixelFormat.JPEG);
        int degree = displayOrientation(mContext);
//        mCamera.setDisplayOrientation(degree);
        mCamera.setDisplayOrientation(0);

        int supportFormat = PixelFormat.UNKNOWN;
        Method getSupportedPreviewFormats = null;
        Method getSupportedPreviewSizes = null;
        try {
            getSupportedPreviewFormats = mCameraParam.getClass().getMethod("getSupportedPreviewFormats", new Class[]{});
            if(getSupportedPreviewFormats != null) {
                @SuppressWarnings("unchecked")
                List<Integer> formats = (List<Integer>) getSupportedPreviewFormats.invoke(mCameraParam, (Object[]) null);
                if (formats != null) {
                    for (int i = 0; i < formats.size(); i++) {
                        Log.d(TAG, "format: " + formats.get(i));
                    }
                    if (formats.contains(ImageFormat.NV21)) {
                        supportFormat = ImageFormat.NV21;
                    } else if (formats.contains(ImageFormat.YUY2)) {
                        supportFormat = ImageFormat.YUY2;
                    } else if (formats.contains(ImageFormat.YV12)) {
                        supportFormat = ImageFormat.YV12;
                    } else if (formats.contains(ImageFormat.RGB_565)) {
                        supportFormat = ImageFormat.RGB_565;
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
            getSupportedPreviewFormats = null;
        }

        if(supportFormat != PixelFormat.UNKNOWN) {
            mCameraParam.setPreviewFormat(supportFormat);
        } else {
            supportFormat = 17;
            mCameraParam.setPreviewFormat(supportFormat);
        }

        try {
            getSupportedPreviewSizes = mCameraParam.getClass().getMethod("getSupportedPreviewSizes", new Class[] {});
            if(getSupportedPreviewSizes != null) {
                @SuppressWarnings("unchecked")
                List<Camera.Size> frameSizes = (List<Camera.Size>) getSupportedPreviewSizes.invoke(mCameraParam, (Object[]) null);
                for(Camera.Size size : frameSizes) {
                    Log.d(TAG, "support preview size:" + size.width + "*" + size.height);
                }
                if(frameSizes != null) {
                    Camera.Size opSize = getOptimalEqualPreviewSize(frameSizes, PREVIEW_WIDTH, PREVIEW_HEIGHT);
                    if (opSize != null) {
                        PREVIEW_WIDTH  = opSize.width;
                        PREVIEW_HEIGHT = opSize.height;
                    }
                }
            }
        } catch (Exception e) {
            getSupportedPreviewSizes = null;
        }
        mCameraParam.setPreviewSize(PREVIEW_WIDTH,  PREVIEW_HEIGHT);
        Log.d(TAG, "preview size:" + PREVIEW_WIDTH + "*" + PREVIEW_HEIGHT);

        // get picture size
        Camera.Size picSize = getPictureSize(mCameraParam);
        if(picSize != null) {
            mCameraParam.setPictureSize(picSize.width, picSize.height);
        }

        mCamera.setParameters(mCameraParam);

        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            mCamera.stopPreview();
            mCamera.setErrorCallback(this);
            mCamera.setPreviewCallback(this);
        } catch (Exception e) {
            try {
                mCamera.release();
            } catch (RuntimeException e2) {
                e2.printStackTrace();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            mCamera = null;
        }
        mCamera.startPreview();

        if(null != mPreviewCallback) {
            mPreviewCallback.onSuccess(PREVIEW_WIDTH, PREVIEW_HEIGHT, mCameraId);
        }
    }

    public void stopPreview() {
        if (mCamera != null) {
            try {
                mCamera.setErrorCallback(null);
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    mCamera.release();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
                mCamera = null;
            }
        }
        if (mSurfaceHolder != null) {
            mSurfaceHolder.removeCallback(this);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "Start preview display[SURFACE-CREATED]");

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        if(mSurfaceHolder.getSurface() == null) {
            return;
        }
        Log.d(TAG, "Start preview display[SURFACE-CHANGED]");
        startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "Start preview display[SURFACE-DESTROYED]");
        stopPreview();
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        if(mLivePreviewCallback != null) {
            mLivePreviewCallback.onPreviewFrame(bytes, camera);
        }
    }

    @Override
    public void onError(int i, Camera camera) {
        if(null != mPreviewCallback) {
            mPreviewCallback.onError(i);
        }
    }
}
