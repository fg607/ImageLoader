package com.hardwork.fg607.imageloader.imageloaderutils;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.ImageView;

import com.hardwork.fg607.imageloader.R;
import com.jakewharton.disklrucache.DiskLruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by fg607 on 16-7-16.
 */
public class ImageLoader {

    private static final String TAG = "ImageLoader";

    private static final long DISK_CACHE_SIZE = 1024*1024*50;
    private static final int DISK_CACHE_INDEX = 0;
    private static final int IO_BUFFER_SIZE = 8*1024;

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT +1;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT*2 -1;
    private static final long KEEP_ALIVE=10L;

    private static final int TAG_KEY_URI = R.id.imageloader_uri;
    private static final int MESSAGE_POST_RESULT = 1;

    private boolean mIsDiskLruCacheCreated = false;
    public Context mContext;
    private LruCache<String,Bitmap> mMemoryCache;
    private DiskLruCache mDiskLruCache;
    private ImagerResizer mImagerResizer = new ImagerResizer();

    private Handler mMainHandler = new Handler(Looper.getMainLooper()){

        @Override
        public void handleMessage(Message msg) {

           LoaderResult result = (LoaderResult) msg.obj;

            ImageView imageView = result.imageView;

            String uri = (String) imageView.getTag(TAG_KEY_URI);

            if(uri.equals(result.uri)){

                imageView.setImageBitmap(result.bitmap);

            }else {

                Log.w(TAG,"imageView被复用，uri改变了！");
            }
        }
    };

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {

        private final AtomicInteger mCount = new AtomicInteger(1);
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r,"ImageLoader#"+mCount.getAndIncrement());
        }
    };

    //线程池
    public static final Executor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(CORE_POOL_SIZE,MAXIMUM_POOL_SIZE,
            KEEP_ALIVE, TimeUnit.SECONDS,new LinkedBlockingDeque<Runnable>(),sThreadFactory);

    public static ImageLoader build(Context context){

        return new ImageLoader(context);

    }

    private ImageLoader(Context context){

        mContext = context.getApplicationContext();

        int maxMemory = (int) (Runtime.getRuntime().maxMemory()/1024);

        int cacheSize = maxMemory/8;

        mMemoryCache = new LruCache<String, Bitmap>(cacheSize){

            @Override
            protected int sizeOf(String key, Bitmap bitmap) {

                return bitmap.getRowBytes()*bitmap.getHeight()/1024;

            }
        };

        File diskCacheDir = getDiskCacheDir(mContext,"bitmap");

        if(!diskCacheDir.exists()){

            diskCacheDir.mkdirs();
        }

        if(getUsableSpace(diskCacheDir) >DISK_CACHE_SIZE){

            try {

                mDiskLruCache = DiskLruCache.open(diskCacheDir,1,1,DISK_CACHE_SIZE);

                mIsDiskLruCacheCreated = true;

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    private File getDiskCacheDir(Context context, String dirName) {

        boolean externalStorageAvailable = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        String cachePath;
        if(externalStorageAvailable){

            cachePath = context.getExternalCacheDir().getPath();

        }else {

            cachePath = context.getCacheDir().getPath();
        }

        return new File(cachePath+File.separator+dirName);
    }

    public void addBitmapToMemoryCache(String key,Bitmap bitmap){

        if(getBitmapFromMemoryCache(key)==null){

            mMemoryCache.put(key,bitmap);
        }
    }

    public Bitmap getBitmapFromMemoryCache(String key){

      return   mMemoryCache.get(key);

    }

    //同步加载bitmap(不能在UI线程调用)
    public Bitmap loadBitmap(String url,int reqWidth,int reqHeight){

        Bitmap bitmap = null;
        bitmap = loadBitmapFromMemoryCache(url);

        if(bitmap!=null){

            return bitmap;
        }

        try {
            bitmap = loadBitmapFromDiskCache(url,reqWidth,reqHeight);

            if(bitmap != null){

                return bitmap;
            }

            bitmap = loadBitmapFromHttp(url,reqWidth,reqHeight);

        } catch (IOException e) {

            e.printStackTrace();
        }

        if(bitmap == null && !mIsDiskLruCacheCreated){

            bitmap = downloadBitmapFromUrl(url);

        }

        return bitmap;
    }

    public void bindBitmap(String uri, ImageView imageView){

        bindBitmap(uri,imageView,0,0);
    }

    //异步加载bitmap(需要在UI线程中调用)
    public void bindBitmap(final String uri, final ImageView imageView, final int reqWidth, final int reqHeight){

        imageView.setTag(TAG_KEY_URI,uri);

        Bitmap bitmap = loadBitmapFromMemoryCache(uri);

        if(bitmap != null){

            imageView.setImageBitmap(bitmap);

            return;
        }

        Runnable loadBitmapTask = new Runnable() {
            @Override
            public void run() {

                Bitmap bitmap = loadBitmap(uri,reqWidth,reqHeight);

                if(bitmap != null){

                    LoaderResult loaderResult = new LoaderResult(imageView,uri,bitmap);
                    mMainHandler.obtainMessage(MESSAGE_POST_RESULT,loaderResult).sendToTarget();
                }
            }
        };

        THREAD_POOL_EXECUTOR.execute(loadBitmapTask);

    }

    private Bitmap downloadBitmapFromUrl(String urlString){

        Bitmap bitmap = null;
        HttpURLConnection connection = null;
        BufferedInputStream in = null;

        try {
            final URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(connection.getInputStream(),IO_BUFFER_SIZE);

            bitmap = BitmapFactory.decodeStream(in);


        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG,"download bitmap failed");
        }finally {

            if(connection != null){

                connection.disconnect();
            }

            if(in!=null){

                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        return bitmap;

    }

    private Bitmap loadBitmapFromMemoryCache(String url){

        String key = hashKeyFromUrl(url);

        return getBitmapFromMemoryCache(key);
    }

    private Bitmap loadBitmapFromHttp(String url,int reqWidth,int reqHeight) throws IOException{

        if(Looper.myLooper() == Looper.getMainLooper()){

            throw new RuntimeException("can not visit network from UI Thread");
        }

        if(mDiskLruCache == null){

            return  null;
        }

        String key = hashKeyFromUrl(url);

        DiskLruCache.Editor editor = mDiskLruCache.edit(key);

        if(editor!=null){

            OutputStream outputStream = editor.newOutputStream(DISK_CACHE_INDEX);

            if(downloadUrlToStream(url,outputStream)){

                editor.commit();
            }else {
                editor.abort();
            }

            mDiskLruCache.flush();
        }

        return loadBitmapFromDiskCache(url,reqWidth,reqHeight);

    }

    private Bitmap loadBitmapFromDiskCache(String url, int reqWidth, int reqHeight) throws IOException {


        if(Looper.myLooper() == Looper.getMainLooper()){

            Log.w(TAG,"load bitmap from UI Thread,is not recommended");
        }

        if(mDiskLruCache == null){

            return null;
        }

        Bitmap bitmap = null;

        String key = hashKeyFromUrl(url);

        DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);

        if(snapshot != null){

            FileInputStream inputStream = (FileInputStream) snapshot.getInputStream(DISK_CACHE_INDEX);

            FileDescriptor fd = inputStream.getFD();

            bitmap = mImagerResizer.decodeScaledBitmap(fd, reqWidth, reqHeight);

            if(bitmap!= null){

                addBitmapToMemoryCache(key,bitmap);
            }
        }

        return bitmap;
    }

    private boolean downloadUrlToStream(String urlString, OutputStream outputStream) {

        HttpURLConnection connection = null;

        BufferedInputStream in = null;
        BufferedOutputStream out = null;

        try {
            final URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(connection.getInputStream(),IO_BUFFER_SIZE);
            out = new BufferedOutputStream(outputStream,IO_BUFFER_SIZE);

            int b;
            //每次读取一个byte转换成0-255的int
            while ((b=in.read())!=-1){

                out.write(b);
            }
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG,"download bitmap failed");
        }finally {

            if(connection != null){

                connection.disconnect();
            }

            if(in!=null){

                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if(out!=null){

                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        return false;
    }

    private String hashKeyFromUrl(String url) {

        String cacheKey;

        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(url.getBytes());
            cacheKey = bytesToHexString(digest.digest());
        } catch (NoSuchAlgorithmException e) {

            cacheKey = String.valueOf(url.hashCode());
        }

        return cacheKey;
    }

    private String bytesToHexString(byte[] bytes) {

        StringBuilder builder = new StringBuilder();

        for (int i = 0;i<bytes.length;i++){

            String hex = Integer.toHexString(0xFF & bytes[i]);

            if(hex.length()==1){

                builder.append('0');
            }

            builder.append(hex);
        }

        return builder.toString();
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private long getUsableSpace(File path){

        if(Build.VERSION.SDK_INT> Build.VERSION_CODES.GINGERBREAD){

            return path.getUsableSpace();
        }

        final StatFs stats = new StatFs(path.getPath());

        return (long)stats.getBlockSize()*(long)stats.getAvailableBlocks();

    }

    private static class LoaderResult{

        public String uri;
        public ImageView imageView;
        public Bitmap bitmap;

        public LoaderResult(ImageView imageView,String uri,Bitmap bitmap){

            this.imageView = imageView;
            this.uri = uri;
            this.bitmap = bitmap;
        }
    }
}
