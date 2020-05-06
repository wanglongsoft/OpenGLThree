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

import com.wl.function.CameraHelper;
import com.wl.function.OpenGLControl;
import com.wl.function.PermissionUtils;
import com.wl.function.PictureRunnable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CameraActivity extends AppCompatActivity {

    private final String TAG = "CameraActivity";
    private CameraHelper myCamera;
    private OpenGLControl openGLControl;
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
        openGLControl = new OpenGLControl();
        setContentView(R.layout.activity_camera);
        PermissionUtils.askPermission(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE}, 100, null);
        cameraPreview();
    }

    private void cameraPreview() {
        isSaveImg = false;
        executorService = new ThreadPoolExecutor(2,4,0, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(10));
        myCamera = new CameraHelper();
        myCamera.setPictureRawCaptureListener(new CameraHelper.OnPictureRawCapture() {
            @Override
            public void onCapture(byte[] data, int width, int height, int video_rotation ) {
                openGLControl.rendSurface(data, width, height, video_rotation);
                if(isSaveImg) {
                    isSaveImg = false;
                    executorService.execute(new PictureRunnable(data, width, height, ImageFormat.NV21, video_rotation));
                }
            }
        });

        mSwicthCamera = findViewById(R.id.swicth_camera);
        mSwicthCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myCamera.switchCamera();
            }
        });

        mPreView = findViewById(R.id.start_preview);
        mPreView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myCamera.startPreview();
            }
        });
        initSurfaceView();
        cameraCommon();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        if(null != myCamera) {
            myCamera.stopPreview();
        }
        if(null != executorService) {
            executorService.shutdownNow();
        }
        openGLControl.releaseSurface();
    }
}
