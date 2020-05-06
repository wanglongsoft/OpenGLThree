package com.wl.function;

import android.graphics.Bitmap;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GraphicRenderer implements GLSurfaceView.Renderer {

    private IDrawer mDrawer;
    private final String TAG = "OpenGLApp";
    private Bitmap mBitmap;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated: ");
        GLES30.glClearColor(0f, 0f, 0f, 1f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        mDrawer = new BitmapDrawer(mBitmap);
        mDrawer.setTextureID(createTextureIds(1)[0]);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(TAG, "onSurfaceChanged width : " + width + " height : " + height);
        GLES30.glViewport(0, 0, width, height);
        setRenderWidth(width);
        setRenderHeight(height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        Log.d(TAG, "onDrawFrame: ");
        mDrawer.draw();
    }

    int[] createTextureIds(int id) {
        int[] texture = new int[id];
        GLES30.glGenTextures(id, texture, 0); //生成纹理
        return texture;
    }

    public GraphicRenderer(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    public void setRenderWidth(int width) {
        mDrawer.setRenderWidth(width);
    }

    public void setRenderHeight(int height) {
        mDrawer.setRenderHeight(height);
    }
}
