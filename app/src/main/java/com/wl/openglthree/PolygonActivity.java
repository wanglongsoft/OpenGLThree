package com.wl.openglthree;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.wl.function.OpenGLControl;

public class PolygonActivity extends AppCompatActivity {

    private final String TAG = "PolygonActivity";

    private OpenGLControl openGLControl;
    private SurfaceView surfaceView;
    private Button polygonAdd;
    private Button polygonSubtraction;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: ");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);//始终竖屏
        openGLControl = new OpenGLControl();
        openGLControl.saveAssetManager(getAssets());
        setContentView(R.layout.activity_polygon);
        surfaceView = findViewById(R.id.surface_view);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                openGLControl.setSurfaceSize(width, height);
                openGLControl.setSurface(holder.getSurface());
                openGLControl.rendPolygon();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

        polygonAdd = findViewById(R.id.polygon_add);
        polygonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGLControl.rendPolygonAdd();
            }
        });

        polygonSubtraction = findViewById(R.id.polygon_subtraction);
        polygonSubtraction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGLControl.rendPolygonSubtraction();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        openGLControl.releaseSurface();
    }
}
