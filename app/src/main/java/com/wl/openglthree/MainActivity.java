package com.wl.openglthree;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "OpenGLThree";

    private Button mTriangle;
    private Button mGraphic;
    private Button mPolygon;
    private Button mCameraOne;
    private Button mCameraTwo;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);//始终竖屏
        Log.d(TAG, "onCreate: ");
        setContentView(R.layout.activity_main);
        mTriangle = findViewById(R.id.button_triangle);
        mTriangle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, TriangleActivity.class);
                startActivity(intent);
            }
        });

        mPolygon = findViewById(R.id.button_polygon);
        mPolygon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, PolygonActivity.class);
                startActivity(intent);
            }
        });

        mGraphic = findViewById(R.id.button_graphic);
        mGraphic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, VeinsActivity.class);
                startActivity(intent);
            }
        });

        mCameraOne = findViewById(R.id.camera_preview);
        mCameraOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, CameraActivity.class);
                startActivity(intent);
            }
        });

        mCameraTwo = findViewById(R.id.camera2_preview);
        mCameraTwo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, Camera2Activity.class);
                startActivity(intent);
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
    }
}
