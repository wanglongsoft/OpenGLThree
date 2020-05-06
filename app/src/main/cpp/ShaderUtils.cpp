//
// Created by 24909 on 2020/4/1.
//

#include "ShaderUtils.h"

//多次读取同一个文件，有机率出错
char *ShaderUtils::openAssetsFile(AAssetManager *mgr, char *file_name) {
    char *fileData = NULL;
    char *srcData = NULL;
    LOGD("ndkReadAssetsFile in : %s", file_name);
    if(NULL == mgr) {
        LOGD("ndkReadAssetsFile mgr == NULL, return");
        return fileData;
    }
    // 打开 Asset 文件夹下的文件
    AAsset *pathAsset = AAssetManager_open(mgr, file_name, AASSET_MODE_STREAMING);
    if(NULL == pathAsset) {
        LOGD("ndkReadAssetsFile pathAsset == NULL, return");
        return fileData;
    }
    srcData = (char *) AAsset_getBuffer(pathAsset);//多次读文件时，有机率出错
    fileData = new char[strlen(srcData) + 1];// 不用时，delete，否则内存泄漏
    memset(fileData, 0, strlen(srcData) + 1);
    strcpy(fileData, srcData);
    fileData[strlen(srcData)] = '\0';
    AAsset_close(pathAsset);
    LOGD("ndkReadAssetsFile out");
    return fileData;
}

//多次读取同一个文件，基本不出错
std::string *ShaderUtils::openAssetsFile2(AAssetManager *mgr, char *file_name) {
    //打开asset文件夹
    AAssetDir *dir = AAssetManager_openDir(mgr,"");
    const char *file = NULL;
    std::string *result = new std::string;

    while ((file = AAssetDir_getNextFileName(dir)) != NULL) {
        if (strcmp(file, file_name) == 0) {
            AAsset *asset = AAssetManager_open(mgr, file, AASSET_MODE_STREAMING);
            char buf[1024];
            int nb_read = 0;
            while ((nb_read = AAsset_read(asset, buf, 1024)) > 0) {
                result->append(buf, (unsigned long)nb_read);
            }
            AAsset_close(asset);
            break;
        }
    }
    AAssetDir_close(dir);
    return result;
}