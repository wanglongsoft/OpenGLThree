package com.wl.function;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;

public class PermissionUtils {
    public static void askPermission(Activity context, String[] permissions, int req, Runnable runnable){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            for (int i = 0; i < permissions.length; ++i) {
                int result= ActivityCompat.checkSelfPermission(context,permissions[i]);
                if(result != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(context, new String[]{permissions[i]}, req);
                }
            }
            if(runnable != null) {
                runnable.run();
            }
        }else{
            if(runnable != null) {
                runnable.run();
            }
        }
    }
}
