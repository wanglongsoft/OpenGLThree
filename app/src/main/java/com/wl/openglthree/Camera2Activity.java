package com.wl.openglthree;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.graphics.ImageFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.wl.function.Camera2Helper;
import com.wl.function.OpenGLControl;
import com.wl.function.PermissionUtils;
import com.wl.function.PictureRunnable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Camera2Activity extends AppCompatActivity {

    private final String TAG = "Camera2Activity";
    private OpenGLControl openGLControl;
    private Camera2Helper myCamera2;
    private ExecutorService executorService;
    private SurfaceView mSurfaceView;
    private Button mSwicthCamera;
    private Button mPreView;
    private Button mTakePicture;
    private Button mWarmColor;
    private Button mCoolColor;
    private Button mNormalColor;
    private Button mBlackWhite;
    private Button mSplitScreen;

    private boolean isSaveImg;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);//始终竖屏
        setContentView(R.layout.activity_camera2);
        openGLControl = new OpenGLControl();
        PermissionUtils.askPermission(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE}, 100, null);
        camera2Preview();
    }

    private void camera2Preview() {
        isSaveImg = false;
        executorService = new ThreadPoolExecutor(2,4,0, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(10));
        mSwicthCamera = findViewById(R.id.swicth_camera);
        mSwicthCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myCamera2.switchCamera();
            }
        });

        myCamera2 = new Camera2Helper(this);
        mPreView = findViewById(R.id.start_preview);
        mPreView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myCamera2.startPreview();
            }
        });

        myCamera2.setOnPictureRawCapture(new Camera2Helper.OnPictureRawCapture() {
            @Override
            public void onCapture(byte[] ydata, byte[] udata, byte[] vdata, int width, int height, int video_rotation) {
                Log.d(TAG, "onCapture thread name: " + Thread.currentThread().getName());
                openGLControl.rendCamera2Surface(ydata, udata, vdata, width, height, video_rotation);
                if(isSaveImg) {
                    isSaveImg = false;
                    byte[] yuvdata = new byte[ydata.length + udata.length + vdata.length];
                    openGLControl.yuv420Tonv21(yuvdata, ydata, udata, vdata);
                    executorService.execute(new PictureRunnable(yuvdata, width, height, ImageFormat.NV21, video_rotation));
                }
            }
        });

        initSurfaceView();
        cameraCommon();
    }

    private void initSurfaceView() {
        mSurfaceView = findViewById(R.id.surface_view);
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.d(TAG, "surfaceChanged: ");
                openGLControl.setSurface(holder.getSurface());
                openGLControl.setSurfaceSize(width, height);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
    }

    private void cameraCommon() {
        mNormalColor = findViewById(R.id.normal_color_camera);
        mNormalColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGLControl.rendNormalColor();
            }
        });

        mBlackWhite = findViewById(R.id.black_white_camera);
        mBlackWhite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGLControl.rendBlackWhite();
            }
        });

        mTakePicture = findViewById(R.id.take_picture);
        mTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSaveImg = true;
            }
        });

        mWarmColor = findViewById(R.id.warm_color_camera);
        mWarmColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGLControl.rendWarmColor();
            }
        });

        mCoolColor = findViewById(R.id.cool_color_camera);
        mCoolColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGLControl.rendCoolColor();
            }
        });

        mSplitScreen = findViewById(R.id.split_screen);
        mSplitScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGLControl.rendSplitScreen();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        if(null != myCamera2) {
            myCamera2.stopPreview();
        }
        if(null != executorService) {
            executorService.shutdownNow();
        }
        openGLControl.releaseSurface();
    }
}
