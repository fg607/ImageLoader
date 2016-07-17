package com.hardwork.fg607.imageloader.imageloaderutils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.FileDescriptor;

/**
 * Created by fg607 on 16-7-16.
 */
public class ImagerResizer {
    private static final String TAG = "ImagerResizer";

    public ImagerResizer(){

    }

    public  Bitmap decodeScaledBitmap(String filename, int scaleWidth, int scaleHeight) {

        if(scaleWidth==0 || scaleHeight==0 ){

            Log.e(TAG,"decodeScaledBitmap scaleWidth or scaleHeight can not be 0");
            return null;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(filename, options);

        options.inSampleSize = caculateInSampleSize(options,scaleWidth,scaleHeight);
        options.inJustDecodeBounds = false;
        Bitmap outputbitmap = BitmapFactory.decodeFile(filename, options);

        return outputbitmap;
    }

    public  Bitmap decodeScaledBitmap(Resources resources, int resId, int scaleWidth, int scaleHeight) {

        if(scaleWidth==0 || scaleHeight==0 ){

            Log.e(TAG,"decodeScaledBitmap scaleWidth or scaleHeight can not be 0");
            return null;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeResource(resources,resId, options);

        options.inSampleSize = caculateInSampleSize(options,scaleWidth,scaleHeight);
        options.inJustDecodeBounds = false;
        Bitmap outputbitmap = BitmapFactory.decodeResource(resources,resId, options);

        return outputbitmap;
    }


    public  Bitmap decodeScaledBitmap(FileDescriptor descriptor, int scaleWidth, int scaleHeight) {

        if(scaleWidth==0 || scaleHeight==0 ){

            Log.e(TAG,"decodeScaledBitmap scaleWidth or scaleHeight can not be 0");
            return null;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFileDescriptor(descriptor,null,options);

        options.inSampleSize = caculateInSampleSize(options,scaleWidth,scaleHeight);

        options.inJustDecodeBounds = false;
        Bitmap outputbitmap =  BitmapFactory.decodeFileDescriptor(descriptor,null,options);

        return outputbitmap;
    }

    private int caculateInSampleSize(BitmapFactory.Options options,int scaleWidth,int scaleHeight){

        //缩放系数
        int inSampleSize = 1;

        //bitmap 实际尺寸
        int height = options.outHeight;
        int width = options.outWidth;

        //根据scalewidth 和scaleheight计算缩放系数
        if(width>scaleWidth || height>scaleHeight){

            int halfWidht = width/2;
            int halfHeight = height/2;

            while ((halfHeight/inSampleSize)>=scaleHeight && (halfWidht/inSampleSize)>=scaleWidth){

                inSampleSize *=2;
            }

        }

        return inSampleSize;

    }

}
