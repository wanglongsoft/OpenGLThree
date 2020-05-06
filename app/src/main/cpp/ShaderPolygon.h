//
// Created by 24909 on 2020/3/23.
//

#ifndef FFMPEGPLAYER_SHADEROPENGL_H
#define FFMPEGPLAYER_SHADEROPENGL_H

#include "ShaderUtils.h"
#include <GLES3/gl3.h>
#include <EGL/egl.h>
#include <math.h>
#include "GlobalContexts.h"
#include "LogUtils.h"
#include <cstdlib>

#define GET_STR(x) #x

class ShaderPolygon {//绘制多边形，顶点颜色随机, 变数越大越接近圆形
public:
    ShaderPolygon(GlobalContexts *global_context);
    ~ShaderPolygon();
    GLuint LoadShader(GLenum type, const char *shaderSrc);
    GLuint LoadProgram(const char *vShaderStr, const char *fShaderStr);
    int CreateProgram();
    void Render();
    void setVideoSize(int width, int height);
    void setWindowSize(int width, int height);
    void initDefMatrix();
    void orthoM(float m[], int mOffset,
                float left, float right, float bottom, float top,
                float near, float far);
    void createRegularPolygon(int i);
    void polygonAdd();
    void polygonSubtraction();

    const char *vertex_shader_graphical_code = GET_STR(
            in vec4 aPosition;
            in vec4 aColor;
            out vec4 vColor;
            uniform mat4 uMatrix;
            void main() {
                gl_Position = aPosition * uMatrix;
                vColor = aColor;
            }
    );

    const char *fragment_shader_graphical_code = GET_STR(
            precision mediump float;
            in vec4 vColor;
            out vec4 fragColor;
            void main() {
                fragColor = vColor;
            }
    );

    float matrix_scale[16];
    int RegularPolygonNum = 6;
    GLint gl_program;
    GLint gl_position;
    GLint gl_color;
    GLint gl_uMatrix;
    GLint gl_video_width;
    GLint gl_video_height;
    GLint gl_window_width;
    GLint gl_window_height;
    GlobalContexts *context;
    float *vertex_coords = NULL;
    float *color_coords = NULL;
    const char *gles_version = "#version 300 es\n";
    ShaderUtils *shader_utils = NULL;
    std::string *vertex_shader_graphical = NULL;
    std::string *fragment_shader_graphical = NULL;
    char * vertex_shader_graphical_local = NULL;
    char * fragment_shader_graphical_local = NULL;
    int vertex_length = 0;
    int fragment_length = 0;
};


#endif //FFMPEGPLAYER_SHADEROPENGL_H
