package com.wl.openglthree;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;

import com.wl.function.GraphicRenderer;

public class VeinsActivity extends AppCompatActivity {

    private final String TAG = "VeinsActivity";
    private GLSurfaceView mGLSurfaceView;
    private Bitmap bitmap;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);//始终竖屏
        setContentView(R.layout.activity_veins);
        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.msy);//显示目标图片
        drawGraphic();
    }

    private void drawGraphic() {
        mGLSurfaceView = findViewById(R.id.gl_surface_view);
        mGLSurfaceView.setEGLContextClientVersion(3);
        mGLSurfaceView.setRenderer(new GraphicRenderer(bitmap));
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy:");
        if(null != bitmap) {
            bitmap.recycle();
        }
    }
}
