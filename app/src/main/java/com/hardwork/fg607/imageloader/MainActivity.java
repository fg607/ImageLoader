package com.hardwork.fg607.imageloader;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.hardwork.fg607.imageloader.imageloaderutils.ImageLoader;
import com.hardwork.fg607.imageloader.utils.MyUtils;
import com.hardwork.fg607.imageloader.view.SquareImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends Activity implements AbsListView.OnScrollListener{

    private static final String TAG = "MainActivity";

    private List<String> mUrlList = new ArrayList<>();
    private ImageLoader mImageLoader;
    private GridView mGridView;
    private BaseAdapter mImageAdapter;

    private boolean mIsGridViewIdle = true;
    private int mImageWidth = 0;
    private boolean mIsWifiConnected = false;
    private boolean mCanGetBitmapFromNetwork = false;

    private int mPreviousFirstVisibleItem=0;
    private long mPreviousEventTime=0;
    private double mScrollSpeed=0;

    private static final int MAX_SCROLLING_SPEED = 30;

    private static final Executor CACHED_THREAD_POOL = Executors.newCachedThreadPool();

    private int mSpeedCheckSwitch = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initData();
        initView();


        mImageLoader = ImageLoader.build(this);


    }

    private void initView() {

        mGridView = (GridView) findViewById(R.id.gridview);
        mImageAdapter = new ImageAdapter(this);
        mGridView.setAdapter(mImageAdapter);
        mGridView.setOnScrollListener(this);
    }

    private void initData() {

        String baseUrl="http://192.168.8.102:8000/";

        for (int i =0 ;i<46;i++){

            mUrlList.add(baseUrl+(i+1)+".png");
        }

        for (int i =0 ;i<46;i++){

            mUrlList.add(baseUrl+(i+1)+".png");
        }

        for (int i =0 ;i<46;i++){

            mUrlList.add(baseUrl+(i+1)+".png");
        }

        int screenWidth = MyUtils.getScreenMetrics(this).widthPixels;
        int space = (int) MyUtils.dp2px(this, 20f);

        mImageWidth = (screenWidth - space) / 3;

        mIsWifiConnected = MyUtils.isWifi(this);

        if (mIsWifiConnected) {
            mCanGetBitmapFromNetwork = true;
        }
    }
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

        //当滑动停止时更新可见item
        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {

            mIsGridViewIdle = true;
            mImageAdapter.notifyDataSetChanged();

        }
    }

    @Override
    public void onScroll(AbsListView view, final int firstVisibleItem, final int visibleItemCount, int totalItemCount) {

        mSpeedCheckSwitch++;

        if(mSpeedCheckSwitch==2){

            //用线程池处理大量线程的创建
            CACHED_THREAD_POOL.execute(new Runnable() {
                @Override
                public void run() {

                    //滚动速度较快时不更新item(防止快速滑动过程中产生大量的异步更新任务，导致卡顿)
                    if (mPreviousFirstVisibleItem != firstVisibleItem){

                        long currTime = System.currentTimeMillis();
                        long timeToScrollOneElement = currTime - mPreviousEventTime;
                        mScrollSpeed = ((double)1/timeToScrollOneElement)*1000;

                        mPreviousFirstVisibleItem = firstVisibleItem;
                        mPreviousEventTime = currTime;

                        Log.d("DBG", "Speed: " +mScrollSpeed + " elements/second");

                        if(mScrollSpeed>MAX_SCROLLING_SPEED){

                            mIsGridViewIdle = false;

                        }else {

                            mIsGridViewIdle = true;

                        }
                    }
                }
            });

            mSpeedCheckSwitch = 0;

        }


    }


    private class ImageAdapter extends BaseAdapter {

        private Context mContext;

        private LayoutInflater mInflater;

        private static final int ONE_SCREEN_ITEMS = 18;

        public ImageAdapter(Context context) {

            mContext = context;

            mInflater = LayoutInflater.from(context);


        }

        @Override
        public int getCount() {
            return mUrlList.size();
        }

        @Override
        public String getItem(int position) {
            return mUrlList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ImageViewHolder holder = null;

            if (convertView == null) {

                convertView = mInflater.inflate(R.layout.gridview_item, parent, false);

                holder = new ImageViewHolder(convertView);

                convertView.setTag(holder);

            } else {

                holder = (ImageViewHolder) convertView.getTag();

            }

            ImageView imageView = holder.imageView;

            String tag = (String) imageView.getTag();

            String uri = getItem(position);

            /*if (!uri.equals(tag)) {

                imageView.setImageDrawable(new ColorDrawable(Color.BLACK));

            }*/


            if(mCanGetBitmapFromNetwork){

                if (mIsGridViewIdle) {

                    imageView.setTag(uri);

                    mImageLoader.bindBitmap(uri, imageView, mImageWidth, mImageWidth);

                    //当滑动到最顶部和最底部时必须刷新
                }else if(position<ONE_SCREEN_ITEMS||position>mUrlList.size()-ONE_SCREEN_ITEMS){


                    imageView.setTag(uri);

                    mImageLoader.bindBitmap(uri, imageView, mImageWidth, mImageWidth);

                }

            }


            return convertView;

        }

        public class ImageViewHolder extends RecyclerView.ViewHolder {

            SquareImageView imageView;

            public ImageViewHolder(View itemView) {
                super(itemView);

                imageView = (SquareImageView) itemView.findViewById(R.id.image);
            }
        }
    }
}
