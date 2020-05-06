//
// Created by 24909 on 2020/4/1.
//

#ifndef OPENGLSTART_SHADERUTILS_H
#define OPENGLSTART_SHADERUTILS_H

#include "LogUtils.h"
#include <cstring>
#include <string>
#include <android/asset_manager.h>

class ShaderUtils {
public:
    //多次读取同一个文件，有机率出错
    char * openAssetsFile(AAssetManager *mgr, char *file_name);
    //多次读取同一个文件，数据正常
    std::string * openAssetsFile2(AAssetManager *mgr, char *file_name);
};


#endif //OPENGLSTART_SHADERUTILS_H
