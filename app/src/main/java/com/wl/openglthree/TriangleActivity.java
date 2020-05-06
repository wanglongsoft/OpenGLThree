package com.wl.openglthree;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.opengl.GLSurfaceView;
import android.os.Bundle;

import com.wl.function.TriangleRenderer;

public class TriangleActivity extends AppCompatActivity {

    private final String TAG = "TriangleActivity";
    private GLSurfaceView mGLSurfaceView;
    private TriangleRenderer triangleRenderer;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);//始终竖屏
        setContentView(R.layout.activity_triangle);
        drawTriangle();
    }

    private void drawTriangle() {
        triangleRenderer = new TriangleRenderer();
        mGLSurfaceView = findViewById(R.id.gl_surface_view);
        mGLSurfaceView.setEGLContextClientVersion(3);
        mGLSurfaceView.setRenderer(triangleRenderer);
        triangleRenderer.setResource(getResources());
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }
}
