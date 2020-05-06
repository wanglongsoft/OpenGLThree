package com.wl.function;

import android.content.res.Resources;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.wl.openglthree.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class TriangleRenderer implements GLSurfaceView.Renderer {

    private final String TAG = "OpenGLThree";

    private IDrawer drawer;
    private Resources resources;
    private String vertex_coder;
    private String fragmet_coder;
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated: ");
        GLES30.glClearColor(0, 0, 0, 1);
        vertex_coder = readResource(R.raw.vertex_shader);
        fragmet_coder = readResource(R.raw.fragment_shader);
        drawer = new TriangleDrawer(vertex_coder,fragmet_coder );
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(TAG, "onSurfaceChanged: ");
        GLES30.glViewport(0, 0, width, height);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        drawer.draw();
    }

    public void setResource(Resources resources) {
        this.resources = resources;
    }

    public String readResource(int resourceId) {//读取资源文件，也可以读取Assets文件
        StringBuilder builder = new StringBuilder();
        try {
            InputStream inputStream = this.resources.openRawResource(resourceId);
            InputStreamReader streamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(streamReader);
            String textLine;
            while ((textLine = bufferedReader.readLine()) != null) {
                builder.append(textLine);
                builder.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }
}
