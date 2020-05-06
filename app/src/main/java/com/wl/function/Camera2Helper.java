package com.wl.function;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Camera2Helper {

    private static final String TAG = "Camera2Helper";

    public static final int WIDTH = 1440;
    public static final int HEIGHT = 1080;
    private CameraManager m_cameraManager = null;
    private HandlerThread m_handlerThread = null;
    private Handler m_handler = null;
    private Activity activity;
    private List<Surface> m_surface_lsit = null;
    private List<String> m_camera_available = null;
    private CameraDevice m_cameraDevice = null;
    private CaptureRequest.Builder m_captureRequestBuilder = null;
    private CaptureRequest m_captureRequest = null;
    private CameraCaptureSession m_cameraCaptureSession = null;
    private String m_cameraID = null;
    private String m_camera_orientation = null;
    private Surface m_surface = null;
    private Surface m_image_surface = null;
    private ImageReader image_reader_front = null;
    private ImageReader image_reader_back = null;
    private Size mPreviewSize = null;
    private OnPictureRawCapture m_picture_capture = null;
    private int video_rotation;
    private Range<Integer> fps;
    private boolean isStartPreview;

    private CameraManager.AvailabilityCallback m_availabilityCallback = new CameraManager.AvailabilityCallback() {
        @Override
        public void onCameraAvailable(String cameraId) {
            Log.d(TAG, "onCameraAvailable cameraId : " + cameraId);
            m_camera_available.add(cameraId);
        }

        @Override
        public void onCameraUnavailable(String cameraId) {
            Log.d(TAG, "onCameraUnavailable cameraId : " + cameraId);
            m_camera_available.remove(cameraId);
        }
    };

    private CameraDevice.StateCallback m_stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            Log.d(TAG, "onOpened: ");
            m_cameraDevice = cameraDevice;
            try {
                m_captureRequestBuilder = m_cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                if(!m_camera_orientation.isEmpty()) {//ImageReader Surface
                    m_surface_lsit.clear();
                    m_image_surface = getImageReaderSurface();
                    m_surface_lsit.add(m_image_surface);
                    if(null != m_surface) {
                        m_surface_lsit.add(m_surface);
                    }
                }
                m_captureRequestBuilder.addTarget(m_image_surface);
                if(null != m_surface) {
                    m_captureRequestBuilder.addTarget(m_surface);
                }
                m_cameraDevice.createCaptureSession(m_surface_lsit, m_cameraCaptureStateCallback, m_handler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            Log.d(TAG, "onDisconnected: ");
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice cameraDevice, int i) {
            Log.d(TAG, "onError code: " + i);
            cameraDevice.close();
        }

        @Override
        public void onClosed(CameraDevice camera) {
            super.onClosed(camera);
            Log.d(TAG, "onClosed: ");
            m_cameraDevice = null;
        }
    };

    private CameraCaptureSession.StateCallback m_cameraCaptureStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
            Log.d(TAG, "onConfigured: ");
            m_cameraCaptureSession = cameraCaptureSession;
            try {
                // 设置连续自动对焦
                m_captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                // 设置关闭闪光灯
                m_captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.FLASH_MODE_OFF);
                m_captureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fps);
                // 生成一个预览的请求
                m_captureRequest = m_captureRequestBuilder.build();
                // 开始预览，即设置反复请求
                m_cameraCaptureSession.setRepeatingRequest(m_captureRequest, null, m_handler);//m_captureCallback 暂不使用

            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
            Log.d(TAG, "onConfigureFailed: ");
        }
    };

    private CameraCaptureSession.CaptureCallback m_captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
            Log.d(TAG, "onCaptureStarted: ");
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Log.d(TAG, "onCaptureCompleted: ");
        }
    };
    
    private ImageReader.OnImageAvailableListener frontAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.d(TAG, "onImageAvailable: frontAvailableListener");
            Image image = reader.acquireLatestImage();
            yuv420ImageToYuv420(image.getPlanes()[0], image.getPlanes()[1], image.getPlanes()[2]);
            image.close();//一定要关闭
        }
    };

    private ImageReader.OnImageAvailableListener backAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.d(TAG, "onImageAvailable: backAvailableListener");
            Image image = reader.acquireLatestImage();
            yuv420ImageToYuv420(image.getPlanes()[0], image.getPlanes()[1], image.getPlanes()[2]);
            image.close();//一定要关闭
        }
    };

    public Camera2Helper(Activity activity) {
        checkApiLevel();
        this.activity = activity;
        initCamera2Info();
    }

    private void checkApiLevel() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            Log.e(TAG, "checkApiLevel: Api level is low");
            Toast.makeText(this.activity, "Camera2Helper init fail", Toast.LENGTH_LONG).show();
        } else {
            Log.d(TAG, "checkApiLevel: success");
        }
    }

    private void initCamera2Info() {
        Log.d(TAG, "initCamera2Info: ");
        m_handlerThread = new HandlerThread("CameraBackground");
        m_handlerThread.start();
        m_handler = new Handler(m_handlerThread.getLooper());
        m_camera_available = new ArrayList<>();
        m_surface_lsit = new ArrayList<>();
        mPreviewSize = new Size(WIDTH, HEIGHT);
        m_cameraManager = (CameraManager) this.activity.getSystemService(Context.CAMERA_SERVICE);
        m_cameraManager.registerAvailabilityCallback(m_availabilityCallback, m_handler);
        isStartPreview = false;
    }

    public void startPreview() {
        Log.d(TAG, "startPreview: ");
        if(isStartPreview) {
            return;
        }
        if(m_camera_available.size() > 0) {
            m_cameraID = m_camera_available.get(0);
        }
        getSupportParameter();
        if (this.activity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "startPreview: permission deny !");
            return;
        }
        try {
            m_cameraManager.openCamera(m_cameraID, m_stateCallback, m_handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void switchCamera(){
        Log.d(TAG, "switchCamera: ");
        if(null != m_cameraDevice) {
            m_cameraDevice.close();
        }
        isStartPreview = false;
        startPreview();
    }
    
    public void stopPreview() {
        Log.d(TAG, "stopPreview: ");
        if(null != this.m_picture_capture) {
            this.m_picture_capture = null;
        }
        if(null != m_cameraDevice) {
            m_cameraDevice.close();
            m_cameraDevice = null;
        }
        if(null != image_reader_front) {
            image_reader_front.close();
            image_reader_front = null;
        }
        if(null != image_reader_back) {
            image_reader_back.close();
            image_reader_back = null;
        }
        isStartPreview = false;
        m_cameraManager.unregisterAvailabilityCallback(m_availabilityCallback);
        if(null != m_handlerThread) {
            m_handlerThread.quitSafely();
        }
        try {
            m_handlerThread.join();
            m_handlerThread = null;
            m_handler = null;
        } catch (InterruptedException e) {
            Log.e(TAG, "stopBackgroundThread", e);
        }
    }

    public void setSurface(Surface surface) {
        Log.d(TAG, "setSurface: ");
        m_surface = surface;
        m_surface_lsit.add(surface);
    }

    private void getSupportParameter() {
        Log.d(TAG, "getSupportParameter:");
        try {
            CameraCharacteristics characteristics = m_cameraManager.getCameraCharacteristics(m_cameraID);
            Integer cameraOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
            Integer sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            Log.d(TAG, "getSupportParameter cameraOrientation: " + cameraOrientation);
            Log.d(TAG, "getSupportParameter sensorOrientation: " + sensorOrientation);
            video_rotation = sensorOrientation;
            StreamConfigurationMap config_map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            StreamConfigurationMap scmap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size previewSizes[] = scmap.getOutputSizes(ImageReader.class);
            for (int i = 0; i <previewSizes.length; i++) {//获取支持的previewSize
                Log.d(TAG, "getSupportParameter previewSizes width: " + previewSizes[i].getWidth()
                        + " height: " + previewSizes[i].getHeight());
            }

            int[] output_format = scmap.getOutputFormats();
            for (int i = 0; i <output_format.length; i++) {//获取支持的format
                Log.d(TAG, "getSupportParameter output_format: " + output_format[i]);
            }

            Range<Integer>[] fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
            Log.d(TAG, "get Support fpsRanges: " + Arrays.toString(fpsRanges));
            fps = new Range<>(25, 25);

            int format = ImageFormat.UNKNOWN;
            if(config_map.isOutputSupportedFor(ImageFormat.YUV_420_888)) {
                format = ImageFormat.YUV_420_888;
            } else if (config_map.isOutputSupportedFor(ImageFormat.YV12)) {
                format = ImageFormat.YV12;
            }

            Log.d(TAG, "getSupportParameter format: " + format);

            if (cameraOrientation == CameraCharacteristics.LENS_FACING_FRONT) {
                if(null != image_reader_front) {
                    image_reader_front.close();
                    image_reader_front = null;
                }
                m_camera_orientation = "FRONT";
                image_reader_front = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), format, 1);
                image_reader_front.setOnImageAvailableListener(frontAvailableListener, m_handler);
            } else if (cameraOrientation == CameraCharacteristics.LENS_FACING_BACK) {
                if(null != image_reader_back) {
                    image_reader_back.close();
                    image_reader_back = null;
                }
                m_camera_orientation = "BACK";
                image_reader_back = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), format, 1);
                image_reader_back.setOnImageAvailableListener(backAvailableListener, m_handler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Surface getImageReaderSurface() {
        Log.d(TAG, "getImageReaderSurface m_camera_orientation: " + m_camera_orientation);
        if(m_camera_orientation.equals("FRONT")) {
            return image_reader_front.getSurface();
        } else if(m_camera_orientation.equals("BACK")) {
            return image_reader_back.getSurface();
        } else {
            return null;
        }
    }

    //YUV_420_888, Y pixelStride: 1,  U pixelStride: 2, V pixelStride: 2,需要对U,V间隔索引取值
    //YUV_420_888，UV步长 == 2，需要间隔像素取值, 只取数据索引：0,2,4,6,8 ......
    private void yuv420ImageToYuv420(Image.Plane y_plane, Image.Plane u_plane, Image.Plane v_plane) {
//        Log.d(TAG, "yuv420ImageToYuv420"
//                + " y_stride : " + y_plane.getPixelStride()
//                + " u_stride : " + u_plane.getPixelStride()
//                + " v_stride : " + v_plane.getPixelStride());
        byte[] y_data = new byte[y_plane.getBuffer().remaining()];
        y_plane.getBuffer().get(y_data);

        int uv_data_length = (u_plane.getBuffer().remaining() + 1) / 2;

        byte[] u_data_palne = new byte[u_plane.getBuffer().remaining()];
        byte[] v_data_plane = new byte[v_plane.getBuffer().remaining()];

        byte[] u_data = new byte[uv_data_length];
        byte[] v_data = new byte[uv_data_length];

        u_plane.getBuffer().get(u_data_palne);
        v_plane.getBuffer().get(v_data_plane);

        for (int i = 0; i < uv_data_length; i++) {
            u_data[i] = u_data_palne[i * 2];
            v_data[i] = v_data_plane[i * 2];
        }
        if(null == this.m_picture_capture) {
            return;
        }
        this.m_picture_capture.onCapture(y_data, u_data, v_data, WIDTH, HEIGHT, video_rotation);
    }

    public interface OnPictureRawCapture{
        void onCapture(byte[] ydata, byte[] udata,byte[] vdata, int width, int height, int video_rotation);
    }

    public void setOnPictureRawCapture(OnPictureRawCapture listener) {
        this.m_picture_capture = listener;
    }
}
