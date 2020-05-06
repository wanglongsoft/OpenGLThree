//
// Created by 24909 on 2020/3/23.
//

#include "ShaderPolygon.h"

ShaderPolygon::ShaderPolygon(GlobalContexts *global_context) {
    LOGD("ShaderPolygon Constructor in");
    gl_program = -1;
    gl_uMatrix = -1;
    gl_video_width = -1;
    gl_video_height = -1;
    gl_window_width = -1;
    gl_window_height = -1;
    context = global_context;
    context->gl_video_width = context->gl_window_width - 50;//设置渲染区域大小
    context->gl_video_height = context->gl_window_width - 50;
    setVideoSize(context->gl_video_width, context->gl_video_height);
    setWindowSize(context->gl_window_width, context->gl_window_height);
    createRegularPolygon(RegularPolygonNum);
    shader_utils = new ShaderUtils();

    vertex_length = strlen(gles_version) + strlen(vertex_shader_graphical_code) + 1;
    vertex_shader_graphical_local = new char[vertex_length];
    memset(vertex_shader_graphical_local, 0, vertex_length);
    strcat(vertex_shader_graphical_local, gles_version);
    strcat(vertex_shader_graphical_local, vertex_shader_graphical_code);

    fragment_length = strlen(gles_version) + strlen(fragment_shader_graphical_code) + 1;
    fragment_shader_graphical_local = new char[fragment_length];
    memset(fragment_shader_graphical_local, 0, fragment_length);
    strcat(fragment_shader_graphical_local, gles_version);
    strcat(fragment_shader_graphical_local, fragment_shader_graphical_code);
    LOGD("ShaderPolygon Constructor out");
}

ShaderPolygon::~ShaderPolygon() {
    LOGD("ShaderPolygon Destructor in");
    if(NULL != vertex_coords) {
        delete vertex_coords;
        vertex_coords = NULL;
    }
    if(NULL != color_coords) {
        delete color_coords;
        color_coords = NULL;
    }
    if(NULL != vertex_shader_graphical) {
        delete vertex_shader_graphical;
        vertex_shader_graphical = NULL;
    }
    if(NULL != fragment_shader_graphical) {
        delete fragment_shader_graphical;
        fragment_shader_graphical = NULL;
    }

    if(NULL != vertex_shader_graphical_local) {
        delete vertex_shader_graphical_local;
        vertex_shader_graphical_local = NULL;
    }

    if(NULL != fragment_shader_graphical_local) {
        delete fragment_shader_graphical_local;
        fragment_shader_graphical_local = NULL;
    }

    if(NULL != shader_utils) {
        delete shader_utils;
        shader_utils = NULL;
    }
    LOGD("ShaderPolygon Destructor out");
}

GLuint ShaderPolygon::LoadShader(GLenum type, const char *shaderSrc) {
    LOGD("LoadShader type : %d", type);
    LOGD("LoadShader shaderSrc : %s", shaderSrc);
    GLuint shader;
    shader = glCreateShader(type);
    if (shader == 0) {
        return 0;
    }
    glShaderSource(shader, 1, &shaderSrc, NULL);
    glCompileShader(shader);
    GLint status = GL_FALSE;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &status);
    if (status == GL_FALSE) {
        GLint length = 0;
        glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &length);
        GLchar log[length + 1];
        glGetShaderInfoLog(shader, length, &length, log);
        LOGD("glCompileShader fail: %s", log);
        glDeleteShader(shader);
        return 0;
    }
    return shader;
}

GLuint ShaderPolygon::LoadProgram(const char *vShaderStr, const char *fShaderStr) {
    GLuint vertexShader;
    GLuint fragmentShader;
    GLuint mProgram;

    //eglMakeCurrent()函数来将当前的上下文切换，这样opengl的函数才能启动作用
    eglMakeCurrent(context->eglDisplay,
                   context->eglSurface, context->eglSurface,
                   context->eglContext);

    // Load the vertex/fragment shaders
    vertexShader = LoadShader(GL_VERTEX_SHADER, vShaderStr);
    fragmentShader = LoadShader(GL_FRAGMENT_SHADER, fShaderStr);

    // Create the program object
    mProgram = glCreateProgram();
    LOGD("glCreateProgram mProgram : %d", mProgram);
    context->mProgram = mProgram;
    gl_program = mProgram;

    // Attaches a shader object to a program object
    glAttachShader(mProgram, vertexShader);
    glAttachShader(mProgram, fragmentShader);

    // Link the program object
    glLinkProgram(mProgram);
    GLint status = 0;
    glGetProgramiv(mProgram, GL_LINK_STATUS, &status);
    if (status == 0) {
        GLint length = 0;
        glGetProgramiv(mProgram, GL_INFO_LOG_LENGTH, &length);
        GLchar log[length + 1];
        glGetProgramInfoLog(mProgram, length, &length, log);
        LOGD("glLinkProgram failed : %s", log);
        return -1;
    }
    // 获取顶点着色器的位置的句柄
    gl_position = glGetAttribLocation(mProgram, "aPosition");
    context->gl_position = gl_position;

    //获取片段着色器的颜色的句柄
    gl_color = glGetAttribLocation(mProgram, "aColor");
    context->gl_color = gl_color;
    gl_uMatrix = glGetUniformLocation(mProgram, "uMatrix");
    context->gl_uMatrix = gl_uMatrix;
    LOGD("LoadProgram gl_position : %d, gl_color : %d, gl_uMatrix : %d", context->gl_position, context->gl_color, context->gl_uMatrix);
    LOGD("LoadProgram out");
    return mProgram;
}

int ShaderPolygon::CreateProgram() {
    LOGD("CreateProgram vertex_shader_graphical_local");
    GLuint mProgram;
//    可以读Assets文件
//    vertex_shader_graphical = shader_utils->openAssetsFile2(context->assetManager, "polygon_vertex_shader.glsl");
//    fragment_shader_graphical = shader_utils->openAssetsFile2(context->assetManager, "polygon_fragment_shader.glsl");
//    mProgram = LoadProgram(vertex_shader_graphical->c_str(), fragment_shader_graphical->c_str());

//    可以读该类局部变量
    mProgram = LoadProgram(this->vertex_shader_graphical_local, this->fragment_shader_graphical_local);

    gl_program = mProgram;
    LOGD("CreateProgram : video_width: %d, video_height: %d, window_width：%d, window_height：%d",
         gl_video_width, gl_video_height, gl_window_width, gl_window_height);
    initDefMatrix();
    // 设置绘图的窗口(可以理解成在画布上划出一块区域来画图)
    glViewport(0, 0, context->gl_window_width, context->gl_window_height);
    return 0;
}

void ShaderPolygon::Render() {
    LOGD("Render in");
    // 清屏
    glClearColor(0, 0, 0, 1);
    glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
    glUseProgram(gl_program);

    glEnableVertexAttribArray(gl_position);
    glVertexAttribPointer(gl_position, 3, GL_FLOAT, false, 0, vertex_coords);

    glEnableVertexAttribArray(gl_uMatrix);
    glEnable(GL_DEPTH_TEST);
    glUniformMatrix4fv(gl_uMatrix, 1, false, matrix_scale);
    glEnableVertexAttribArray(gl_color);
    glVertexAttribPointer(gl_color, 4, GL_FLOAT, false, 0, color_coords);
    glDrawArrays(GL_TRIANGLE_FAN, 0, RegularPolygonNum + 2);
    // 交换显存(将surface显存和显示器的显存交换)
    eglSwapBuffers(context->eglDisplay, context->eglSurface);
    LOGD("Render out");
}

void ShaderPolygon::setVideoSize(int width, int height) {
    gl_video_width = width;
    gl_video_height = height;
}

void ShaderPolygon::setWindowSize(int width, int height) {
    gl_window_width = width;
    gl_window_height = height;
}

void ShaderPolygon::initDefMatrix() {
    if((gl_video_width < gl_window_width) && (gl_video_height < gl_window_height)) {// 图片宽高小于视图宽高，原图显示
        float widthRatio = (float) gl_window_width / gl_video_width;
        float heightRatio = (float) gl_window_height / gl_video_height;
        orthoM(
                matrix_scale, 0,
                -widthRatio, widthRatio,
                -heightRatio, heightRatio,
                -1, 3
        );
        return;
    }

    float originRatio = (float) gl_video_width / gl_video_height;
    float worldRatio = (float) gl_window_width / gl_window_height;
    if (worldRatio > 1) {
        if (originRatio > worldRatio) {
            float actualRatio = originRatio / worldRatio;
            orthoM(
                    matrix_scale, 0,
                    -1, 1,
                    -actualRatio, actualRatio,
                    -1, 3
            );
        } else {// 原始比例小于窗口比例，缩放高度度会导致高度超出，因此，高度以窗口为准，缩放宽度
            float actualRatio = worldRatio / originRatio;
            orthoM(
                    matrix_scale, 0,
                    -actualRatio, actualRatio,
                    -1, 1,
                    -1, 3
            );
        }
    } else {
        if (originRatio > worldRatio) {
            float actualRatio = originRatio / worldRatio;
            orthoM(
                    matrix_scale, 0,
                    -1, 1,
                    -actualRatio, actualRatio,
                    -1, 3
            );
        } else {// 原始比例小于窗口比例，缩放高度会导致高度超出，因此，高度以窗口为准，缩放宽度
            float actualRatio = worldRatio / originRatio;
            orthoM(
                    matrix_scale, 0,
                    -actualRatio, actualRatio,
                    -1, 1,
                    -1, 3
            );
        }
    }
}

void ShaderPolygon::orthoM(float *m, int mOffset, float left, float right, float bottom, float top,
                          float near, float far) {
    float r_width  = 1.0 / (right - left);
    float r_height = 1.0 / (top - bottom);
    float r_depth  = 1.0 / (far - near);
    float x =  2.0 * (r_width);
    float y =  2.0 * (r_height);
    float z = -2.0 * (r_depth);
    float tx = -(right + left) * r_width;
    float ty = -(top + bottom) * r_height;
    float tz = -(far + near) * r_depth;
    m[mOffset + 0] = x;
    m[mOffset + 5] = y;
    m[mOffset +10] = z;
    m[mOffset +12] = tx;
    m[mOffset +13] = ty;
    m[mOffset +14] = tz;
    m[mOffset +15] = 1;
    m[mOffset + 1] = 0;
    m[mOffset + 2] = 0;
    m[mOffset + 3] = 0;
    m[mOffset + 4] = 0;
    m[mOffset + 6] = 0;
    m[mOffset + 7] = 0;
    m[mOffset + 8] = 0;
    m[mOffset + 9] = 0;
    m[mOffset + 11] = 0;
}

void ShaderPolygon::createRegularPolygon(int rim_num) {
    LOGD("createRegularPolygon in");

    if(rim_num < 3) {
        LOGD("createRegularPolygon rim_num 小于 3, return");
        return;
    }
    float *pDouble = new float[rim_num * 3 + 3 * 2];
    float *cDouble = new float[rim_num * 4 + 4 * 1];
    *(pDouble) = 0;
    *(pDouble + 1) = 0;
    *(pDouble + 2) = 0;
    *(cDouble) = float(rand() % 1000000) / 1000000;//R 取随机值
    *(cDouble + 1) = float(rand() % 1000000) / 1000000;//G 取随机值
    *(cDouble + 2) = float(rand() % 1000000) / 1000000;//B 取随机值
    *(cDouble + 3) = float(rand() % 1000000) / 1000000;;//A 取随机值
    float radian_unit = 2 * M_PI / rim_num;
    for (int i = 0; i < rim_num; ++i) {
        *(pDouble + (i + 1) * 3) = cos(i * radian_unit);
        *(pDouble + (i + 1) * 3 + 1) = sin(i * radian_unit);
        *(pDouble + (i + 1) * 3 + 2) = 0;
        *(cDouble + (i + 1) * 4) = float(rand() % 1000000) / 1000000;
        *(cDouble + (i + 1) * 4 + 1) = float(rand() % 1000000) / 1000000;
        *(cDouble + (i + 1) * 4 + 2) = float(rand() % 1000000) / 1000000;
        *(cDouble + (i + 1) * 4 + 3) = float(rand() % 1000000) / 1000000;
    }
    *(pDouble + (rim_num + 1) * 3) = *(pDouble + 3);
    *(pDouble + (rim_num + 1) * 3 + 1) = *(pDouble + 4);
    *(pDouble + (rim_num + 1) * 3 + 2) = *(pDouble + 5);
    if(NULL != vertex_coords) {
        delete vertex_coords;
        vertex_coords = NULL;
    }
    if(NULL != color_coords) {
        delete color_coords;
        color_coords = NULL;
    }
    vertex_coords = pDouble;
    color_coords = cDouble;
    LOGD("createRegularPolygon out");
}

void ShaderPolygon::polygonAdd() {
    LOGD("polygonAdd in");
    RegularPolygonNum++;
    createRegularPolygon(RegularPolygonNum);
    Render();
    LOGD("polygonAdd out");
}

void ShaderPolygon::polygonSubtraction() {
    LOGD("polygonSubtraction in");
    if(RegularPolygonNum > 3) {
        RegularPolygonNum--;
        createRegularPolygon(RegularPolygonNum);
        Render();
    }
    LOGD("polygonSubtraction out");
}
