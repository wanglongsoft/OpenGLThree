package com.wl.function;

import android.graphics.Bitmap;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class BitmapDrawer implements IDrawer {

    private final String TAG = "OpenGLApp";
    private FloatBuffer vertexBuffer;
    private FloatBuffer textureBuffer;

    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;
    static float bitmapCoords[] = {
            //OpenGL ES世界坐标,像素点应该显示在哪个位置由世界坐标决定
            -1f, -1f, 0f, // left bottom
            1f, -1f, 0f, // right bottom
            -1f, 1f, 0f,  // left top
            1f, 1f, 0f,   // right top
    };

    static float bitmapCoords_90[] = {
            //OpenGL ES世界坐标,像素点应该显示在哪个位置由世界坐标决定
            -1f, 1f, 0f, // left bottom
            -1f, -1f, 0f,// right bottom
            1f, 1f, 0f,  // left top
            1f, -1f, 0f,   // right top
    };

    static float bitmapCoords_180[] = {
            //OpenGL ES世界坐标,像素点应该显示在哪个位置由世界坐标决定
            1f, 1f, 0f, // left bottom
            -1f, 1f, 0f,// right bottom
            1f, -1f, 0f,  // left top
            -1f, -1f, 0f,   // right top
    };

    static float textureCoords[] = {
            //OpenGL ES纹理坐标, 表示世界坐标指定的位置点想要显示的颜色，应该在纹理上的哪个位置获取
            0f,  1f, 0f, // left bottom
            1f, 1f, 0f, // right bottom
            0f, 0f, 0f, // left top
            1f, 0f, 0f,  // right top
    };

    static float textureCoords_left_right[] = {//纹理左右换位置
            //OpenGL ES纹理坐标, 表示世界坐标指定的位置点想要显示的颜色，应该在纹理上的哪个位置获取
            1f,  1f, 0f, // left bottom
            0f, 1f, 0f, // right bottom
            1f, 0f, 0f, // left top
            0f, 0f, 0f,  // right top
    };

    static float textureCoords_top_bottom[] = {//纹理上下换位置
            //OpenGL ES纹理坐标, 表示世界坐标指定的位置点想要显示的颜色，应该在纹理上的哪个位置获取
            0f, 0f, 0f, // left bottom
            1f, 0f, 0f, // right bottom
            0f, 1f, 0f, // left top
            1f, 1f, 0f,  // right top
    };

    private final String vertexShaderCode =
            "attribute vec4 aPosition;" +
                    "uniform mat4 uMatrix;" +
                    "attribute vec2 aCoordinate;" +
                    "varying vec2 vCoordinate;" +
                    "void main() {" +
                    "    gl_Position = aPosition * uMatrix;" +
                    "    vCoordinate = aCoordinate;" +
                    "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform sampler2D uTexture;" +
                    "varying vec2 vCoordinate;" +
                    "uniform int vChangeType;" +
                    "void main() {" +
                    "    vec4 color = texture2D(uTexture, vCoordinate);" +
                    "    if(vChangeType == 1) {" +
                    "        gl_FragColor = color;" +
                    "    } else {" +
                    "        gl_FragColor = vec4(1, 0, 0, 1);" +
                    "    }" +
                    "}";

    //OpenGL程序ID
    private int mProgram;
    private int mTextureId;

    // 顶点坐标接收者
    private int mVertexHandle;
    // 纹理坐标接收者
    private int mTexturePosHandler;
    // 纹理接收者
    private int mTextureHandler;

    private int mChangeTypeHandler;

    private int mVertexMatrixHandler;

    private final int vertexCount = bitmapCoords.length / COORDS_PER_VERTEX;
    private final int vertexStride = COORDS_PER_VERTEX * 4;

    private Bitmap mBitmap;
    private int render_width = -1;
    private int render_height = -1;
    private int bitmap_width = -1;
    private int bitmap_height = -1;
    private float matrix_scale[] = new float[16];

    public BitmapDrawer(Bitmap bitmap) {
        // 初始化ByteBuffer，长度为arr数组的长度*4，因为一个float占4个字节
        ByteBuffer bb = ByteBuffer.allocateDirect(bitmapCoords.length * 4);
        // 数组排列用nativeOrder
        bb.order(ByteOrder.nativeOrder());
        // 从ByteBuffer创建一个浮点缓冲区
        vertexBuffer = bb.asFloatBuffer();
        // 将坐标添加到FloatBuffer
        vertexBuffer.put(bitmapCoords);
        // 设置缓冲区来读取第一个坐标
        vertexBuffer.position(0);

        ByteBuffer tt = ByteBuffer.allocateDirect(textureCoords.length * 4);
        // 数组排列用nativeOrder
        tt.order(ByteOrder.nativeOrder());
        // 从ByteBuffer创建一个浮点缓冲区
        textureBuffer = tt.asFloatBuffer();
        // 将坐标添加到FloatBuffer
        textureBuffer.put(textureCoords);
        // 设置缓冲区来读取第一个坐标
        textureBuffer.position(0);

        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER,
                vertexShaderCode);
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER,
                fragmentShaderCode);

        // 创建空的OpenGL ES程序
        mProgram = GLES30.glCreateProgram();

        // 添加顶点着色器到程序中
        GLES30.glAttachShader(mProgram, vertexShader);

        // 添加片段着色器到程序中
        GLES30.glAttachShader(mProgram, fragmentShader);

        // 创建OpenGL ES程序可执行文件
        GLES30.glLinkProgram(mProgram);

        // 获取顶点着色器的位置的句柄
        mVertexHandle = GLES30.glGetAttribLocation(mProgram, "aPosition");
        // 获取纹理句柄
        mTexturePosHandler = GLES30.glGetAttribLocation(mProgram, "aCoordinate");
        //新增获取纹理接收者】
        mTextureHandler = GLES30.glGetUniformLocation(mProgram, "uTexture");

        mVertexMatrixHandler = GLES30.glGetUniformLocation(mProgram, "uMatrix");

        mChangeTypeHandler = GLES30.glGetUniformLocation(mProgram, "vChangeType");
        Log.d(TAG, "BitmapDrawer mChangeTypeHandler: " + mChangeTypeHandler);

        mBitmap = bitmap;
        bitmap_width = mBitmap.getWidth();
        bitmap_height = mBitmap.getHeight();
        Log.d(TAG, "BitmapDrawer mProgram: " + mProgram);
    }

    public void draw() {
        // 将程序添加到OpenGL ES环境
        GLES30.glUseProgram(mProgram);

        activateTexture();
        bindBitmapToTexture();
        // 启用三角形顶点位置的句柄
        GLES30.glEnableVertexAttribArray(mVertexHandle);
        // 启用三角形纹理句柄
        GLES30.glEnableVertexAttribArray(mTexturePosHandler);

        GLES30.glEnableVertexAttribArray(mVertexMatrixHandler);

        GLES30.glUniformMatrix4fv(mVertexMatrixHandler, 1, false, matrix_scale, 0);

        GLES30.glUniform1i(mChangeTypeHandler, 1);
        //准备三角形坐标数据
        GLES30.glVertexAttribPointer(mVertexHandle, COORDS_PER_VERTEX,
                GLES30.GL_FLOAT, false,
                vertexStride, vertexBuffer);

        GLES30.glVertexAttribPointer(mTexturePosHandler, COORDS_PER_VERTEX,
                GLES30.GL_FLOAT, false,
                12, textureBuffer);

        // 绘制图形
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, vertexCount);

    }

    @Override
    public void setTextureID(int id) {
        mTextureId = id;
    }

    @Override
    public void release() {
        GLES30.glDisableVertexAttribArray(mVertexHandle);
        GLES30.glDisableVertexAttribArray(mTexturePosHandler);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        int[] texture = {mTextureId};
        GLES30.glDeleteTextures(1, texture, 0);
        GLES30.glDeleteProgram(mProgram);
    }

    @Override
    public void setRenderWidth(int width) {
        render_width = width;
    }

    @Override
    public void setRenderHeight(int height) {
        render_height = height;
        initDefMatrix();
    }

    private void initDefMatrix() {
        Log.d(TAG, "initDefMatrix render_width: " + render_width + " render_height: "
                + render_height + " bitmap_width: " + bitmap_width + " bitmap_height: " + bitmap_height);
        if((bitmap_width < render_width) && (bitmap_height < render_height)) {// 图片宽高小于视图宽高，原图显示
            float widthRatio = (float) render_width / bitmap_width;
            float heightRatio = (float) render_height / bitmap_height;
            Matrix.orthoM(
                    matrix_scale, 0,
                    -widthRatio, widthRatio,
                    -heightRatio, heightRatio,
                    -1f, 3f
            );
            return;
        }
        float originRatio = (float) bitmap_width / bitmap_height;
        float worldRatio = (float) render_width / render_height;
        Log.d(TAG, "initDefMatrix originRatio : " + originRatio + " worldRatio : " + worldRatio);
        if (worldRatio > 1) {
            if (originRatio > worldRatio) {
                float actualRatio = originRatio / worldRatio;
                Matrix.orthoM(
                        matrix_scale, 0,
                        -1f, 1f,
                        -actualRatio, actualRatio,
                        -1f, 3f
                );
            } else {// 原始比例小于窗口比例，缩放高度度会导致高度超出，因此，高度以窗口为准，缩放宽度
                float actualRatio = worldRatio / originRatio;
                Matrix.orthoM(
                        matrix_scale, 0,
                        -actualRatio, actualRatio,
                        -1f, 1f,
                        -1f, 3f
                );
            }
        } else {
            if (originRatio > worldRatio) {
                float actualRatio = originRatio / worldRatio;
                Matrix.orthoM(
                        matrix_scale, 0,
                        -1f, 1f,
                        -actualRatio, actualRatio,
                        -1f, 3f
                );
            } else {// 原始比例小于窗口比例，缩放高度会导致高度超出，因此，高度以窗口为准，缩放宽度
                float actualRatio = worldRatio / originRatio;
                Matrix.orthoM(
                        matrix_scale, 0,
                        -actualRatio, actualRatio,
                        -1f, 1f,
                        -1f, 3f
                );
            }
        }
//        for(int i = 0; i < 16; i++) {
//            Log.d(TAG, "initDefMatrix matrix_scale[" + i + "] = " + matrix_scale[i]);
//        }
    }

    public int loadShader(int type, String shaderCode) {

        // 创造顶点着色器类型(GLES30.GL_VERTEX_SHADER)
        // 或者是片段着色器类型 (GLES30.GL_FRAGMENT_SHADER)
        int shader = GLES30.glCreateShader(type);
        // 添加上面编写的着色器代码并编译它
        GLES30.glShaderSource(shader, shaderCode);
        GLES30.glCompileShader(shader);
        return shader;

    }

    public void activateTexture() {
        //激活指定纹理单元
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        //绑定纹理ID到纹理单元
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextureId);
        //将激活的纹理单元传递到着色器里面
        GLES30.glUniform1i(mTextureHandler, 0);
        //配置边缘过渡参数
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, (float) GLES30.GL_NEAREST);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, (float) GLES30.GL_NEAREST);
        //配置纹理环绕方式
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_MIRRORED_REPEAT);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_MIRRORED_REPEAT);
    }

    public void bindBitmapToTexture() {//纹理与图片绑定
        if(null != mBitmap && !mBitmap.isRecycled()) {
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, mBitmap, 0);
        }
    }
}
