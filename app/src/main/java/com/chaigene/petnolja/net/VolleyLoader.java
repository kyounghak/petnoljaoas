package com.chaigene.petnolja.net;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

public class VolleyLoader {
    public static String TAG = VolleyLoader.class.getSimpleName();

    private static VolleyLoader mInstance;

    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;
    private static Context mContext;

    public static synchronized VolleyLoader getInstance(Context context) {
        if (mInstance == null) mInstance = new VolleyLoader(context);
        return mInstance;
    }

    public static synchronized boolean isRunning() {
        return mInstance != null;
    }

//    public static VolleyLoader getInstance() {
//        return VolleyLoader.mInstance;
//    }

    // Should initialize with Application context
//    public static synchronized void createInstance(Context context) {
//        if (_instance == null) _instance = new VolleyLoader(context);
//    }

    public static synchronized void releaseInstance() {
        if (mInstance != null) mInstance = null;
    }

    private VolleyLoader(Context context) {
        mContext = context;
        mRequestQueue = getRequestQueue();

//        _imageLoader = new ImageLoader(_requestQueue, new ImageLoader.ImageCache() {
//            private final LruCache<String, Bitmap> cache = new LruCache<>(20);
//
//            @Override
//            public Bitmap getBitmap(String url) {
//                return cache.getUserPosts(url);
//            }
//
//            @Override
//            public void putBitmap(String url, Bitmap bitmap) {
//                cache.put(url, bitmap);
//            }
//        });
        mImageLoader = new ImageLoader(
                mRequestQueue,
                new LruBitmapCache(LruBitmapCache.getCacheSize(context))
        );
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            //_requestQueue = Volley.newRequestQueue(_context.getApplicationContext());

            // +OkHttp
            mRequestQueue = Volley.newRequestQueue(mContext.getApplicationContext(), new OkHttpStack(mContext));
            VolleyLog.DEBUG = true;
        }
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    public ImageLoader getImageLoader() {
        return mImageLoader;
    }

    public ResponseHolder getResponseHolder() {
        Log.i(TAG, "getResponseHolder, exec");
        return new ResponseHolder();
    }

    public class ResponseHolder<T> {
        private T value;

        ResponseHolder() {
            Log.i(TAG, "ResponseHolder, Constructor, exec");
        }

        public void setValue(T value) {
            Log.i(TAG, "ResponseHolder, setValue, value: " + value);
            this.value = value;
        }

        public T getValue() {
            Log.i(TAG, "ResponseHolder, getValue, value: " + value);
            return value;
        }
    }
}
