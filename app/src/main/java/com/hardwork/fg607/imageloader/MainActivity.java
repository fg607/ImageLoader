package com.hardwork.fg607.imageloader;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
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

public class MainActivity extends Activity implements AbsListView.OnScrollListener {

    private static final String TAG = "MainActivity";

    private List<String> mUrlList = new ArrayList<>();
    private ImageLoader mImageLoader;
    private GridView mGridView;
    private BaseAdapter mImageAdapter;

    private boolean mIsGridViewIdle = true;
    private int mImageWidth = 0;
    private boolean mIsWifiConnected = false;
    private boolean mCanGetBitmapFromNetwork = false;

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

        String[] imageUrls = {

                "http://192.168.8.102:8000/1460694199871.png",
                "http://192.168.8.102:8000/20160407_075152.jpg",
                "http://192.168.8.102:8000/20160407_075301.jpg",
                "http://192.168.8.102:8000/20160410_194610.jpg",
                "http://192.168.8.102:8000/20160410_194618.jpg",
                "http://192.168.8.102:8000/20160707_090554.jpg",
                "http://192.168.8.102:8000/2016_05_21_12_13_29.png",
                "http://192.168.8.102:8000/2016_06_19_09_11_15.png",
                "http://192.168.8.102:8000/clover.png",
                "http://192.168.8.102:8000/DIY.png",
                "http://192.168.8.102:8000/id1.jpg",
                "http://192.168.8.102:8000/id2.jpg",
                "http://192.168.8.102:8000/iphone.png",
                "http://192.168.8.102:8000/screenshot.jpg",
                "http://192.168.8.102:8000/Screenshot0.png",
                "http://192.168.8.102:8000/screenshot1.jpg",
                "http://192.168.8.102:8000/screenshot2.jpg",
                "http://192.168.8.102:8000/screenshot3.jpg",
                "http://192.168.8.102:8000/screenshot4.jpg",
                "http://192.168.8.102:8000/screenshot5.jpg"
        };

        for (String url : imageUrls) {

            mUrlList.add(url);
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

        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {

            mIsGridViewIdle = true;
            mImageAdapter.notifyDataSetChanged();
        } else {

            mIsGridViewIdle = false;
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

    }

    private class ImageAdapter extends BaseAdapter {

        private Context mContext;

        private LayoutInflater mInflater;

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

            if (!uri.equals(tag)) {

                imageView.setImageDrawable(new ColorDrawable(Color.BLACK));

            }

            if (mIsGridViewIdle && mCanGetBitmapFromNetwork) {

                imageView.setTag(uri);

                mImageLoader.bindBitmap(uri, imageView, mImageWidth, mImageWidth);
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
