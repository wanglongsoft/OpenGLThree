package com.wl.function;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PictureRunnable implements Runnable {

    private final String TAG = "PictureRunnable";
    private static final String CAPTRUE_PATH = Environment.getExternalStorageDirectory() + File.separator + "filefilm" + File.separator;

    private int width;
    private int height;
    private int format;
    private byte[] imgRaw;

    private int video_rotation;

    public PictureRunnable(byte[] data, int width, int height, int format, int video_rotation) {
        Log.d(TAG, "PictureRunnable video_rotation: " + video_rotation);
        this.imgRaw = data;
        this.width = width;
        this.height = height;
        this.format = format;
        this.video_rotation = video_rotation;
    }

    @Override
    public void run() {
        createBmpAndSaveFile(imgRaw);
    }

    private void createBmpAndSaveFile(byte[] data) {
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        newOpts.inJustDecodeBounds = true;
        YuvImage yuvimage = new YuvImage(
                data,
                format,
                width,
                height,
                null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(new Rect(0, 0, width, height), 100, baos);
        byte[] rawImage = baos.toByteArray();
        Bitmap src_bitmap = BitmapFactory.decodeByteArray(rawImage, 0, rawImage.length);

        Matrix matrix = new Matrix();
        matrix.preRotate(this.video_rotation);
        Bitmap dst_bitmap = Bitmap.createBitmap(src_bitmap , 0, 0, src_bitmap .getWidth(),
                src_bitmap.getHeight(),matrix,true);
        try {
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (dst_bitmap == null)
            return;
        else {
            try {
                saveFile(dst_bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(!src_bitmap.isRecycled()) {
            src_bitmap.recycle();
        }
    }

    private void saveFile(Bitmap bm) throws IOException {
        String fileName = getCurrentTime()+".jpeg";
        File dirFile = new File(CAPTRUE_PATH);
        if(!dirFile.exists()){
            dirFile.mkdir();
        }
        File myCaptureFile = new File(CAPTRUE_PATH + fileName);
        Log.d(TAG, "saveFile: " + CAPTRUE_PATH + fileName);
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(myCaptureFile));
        bm.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        bos.flush();
        bos.close();
        bm.recycle();
        Log.d("CameraHelper","new img:" + myCaptureFile.getAbsolutePath());
    }

    private String getCurrentTime() {
        Date date = new Date();
        String strDateFormat = "yyyyMMddHHmmssSSS";
        SimpleDateFormat sdf = new SimpleDateFormat(strDateFormat);
        return sdf.format(date);
    }
}
