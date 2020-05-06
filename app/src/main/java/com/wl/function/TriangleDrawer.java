package com.wl.function;

import android.opengl.GLES30;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class TriangleDrawer implements IDrawer {

    final String TAG = "OpenGLThree";
    private String vertex_coder;
    private String fragmet_coder;
    private int vertex_shader;
    private int fragmet_shader;
    private int mProgram;
    private int vertex_index;
    private int fragmet_index;
    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;
    private float scale_factor = 0.6f;
    private float[] vertexPoints = new float[]{
            -scale_factor, -scale_factor, 0.0f, // top
            scale_factor, -scale_factor, 0.0f, // bottom left
            0f, scale_factor, 0.0f  // bottom right
    };

    private float[] vertexPoints_rotate_90 = new float[]{
            -scale_factor, scale_factor, 0.0f, // top
            -scale_factor, -scale_factor, 0.0f, // bottom left
            scale_factor, 0, 0.0f  // bottom right
    };

    private float[] colors = {
        1f,  0f, 0f, 1f,
        0f, 1f, 0f, 1f,
        0f, 0f, 1f, 1f,
    };

    public TriangleDrawer(String vertex_coder, String fragmet_coder) {
        this.vertex_coder = vertex_coder;
        this.fragmet_coder = fragmet_coder;

        //分配内存空间,每个浮点型占4字节空间
        vertexBuffer = ByteBuffer.allocateDirect(vertexPoints_rotate_90.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        //传入指定的坐标数据
        vertexBuffer.put(vertexPoints_rotate_90);
        vertexBuffer.position(0);

        //分配内存空间,每个浮点型占4字节空间
        colorBuffer = ByteBuffer.allocateDirect(colors.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        //传入指定的数据
        colorBuffer.put(colors);
        colorBuffer.position(0);

        vertex_shader = compileShader(GLES30.GL_VERTEX_SHADER, this.vertex_coder);
        fragmet_shader = compileShader(GLES30.GL_FRAGMENT_SHADER, this.fragmet_coder);
        if(vertex_shader == 0 || fragmet_shader == 0) {
            Log.d(TAG, "compileShader fail");
            return;
        }
        mProgram = linkProgram(vertex_shader, fragmet_shader);
        vertex_index = GLES30.glGetAttribLocation(mProgram, "vPosition");
        fragmet_index = GLES30.glGetAttribLocation(mProgram, "aColor");
        Log.d(TAG, "TriangleDrawer vertex_shader: " + vertex_shader + " fragmet_shader : " + fragmet_shader + " mProgram : " + mProgram);
        Log.d(TAG, "TriangleDrawer vertex_index: " + vertex_index + " fragmet_index: " + fragmet_index);
    }

    @Override
    public void draw() {
        GLES30.glUseProgram(mProgram);
        GLES30.glVertexAttribPointer(vertex_index, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);
        //启用位置顶点属性
        GLES30.glEnableVertexAttribArray(vertex_index);

        GLES30.glVertexAttribPointer(fragmet_index, 4, GLES30.GL_FLOAT, false, 0, colorBuffer);
        //启用颜色顶点属性
        GLES30.glEnableVertexAttribArray(fragmet_index);
        // 绘制三角形
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 3);
    }

    @Override
    public void setTextureID(int id) {

    }

    @Override
    public void release() {

    }

    @Override
    public void setRenderWidth(int width) {

    }

    @Override
    public void setRenderHeight(int height) {

    }

    private int compileShader(int type, String shaderCode) {
        //创建一个着色器
        int shaderId = GLES30.glCreateShader(type);
        if (shaderId != 0) {
            //加载到着色器
            GLES30.glShaderSource(shaderId, shaderCode);
            //编译着色器
            GLES30.glCompileShader(shaderId);
            //检测状态
            final int[] compileStatus = new int[1];
            GLES30.glGetShaderiv(shaderId, GLES30.GL_COMPILE_STATUS, compileStatus, 0);
            if (compileStatus[0] == 0) {
                String logInfo = GLES30.glGetShaderInfoLog(shaderId);
                Log.d(TAG, "compileShader logInfo: " + logInfo);
                //创建失败
                GLES30.glDeleteShader(shaderId);
                return 0;
            }
            return shaderId;
        } else {
            //创建失败
            return 0;
        }
    }

    public int linkProgram(int vertexShaderId, int fragmentShaderId) {
        final int programId = GLES30.glCreateProgram();
        if (programId != 0) {
            //将顶点着色器加入到程序
            GLES30.glAttachShader(programId, vertexShaderId);
            //将片元着色器加入到程序中
            GLES30.glAttachShader(programId, fragmentShaderId);
            //链接着色器程序
            GLES30.glLinkProgram(programId);
            final int[] linkStatus = new int[1];
            GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] == 0) {
                String logInfo = GLES30.glGetProgramInfoLog(programId);
                Log.d(TAG, "linkProgram logInfo: " + logInfo);
                GLES30.glDeleteProgram(programId);
                return 0;
            }
            return programId;
        } else {
            //创建失败
            return 0;
        }
    }
}
