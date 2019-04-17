package com.chaigene.petnolja.image.glide;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;
import com.google.firebase.storage.StorageReference;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Glide module to register {@link com.firebase.ui.storage.images.FirebaseImageLoader}.
 * See: http://bumptech.github.io/glide/doc/generatedapi.html
 */
@GlideModule
public class FirebaseAppGlideModule extends AppGlideModule {
    public static final String TAG = "FirebaseAppGlideModule";

    @Override
    public void registerComponents(Context context, Glide glide, Registry registry) {
        // Register FirebaseImageLoader to handle StorageReference
        registry.append(StorageReference.class, InputStream.class, new FirebaseImageLoader.Factory());
    }

    public interface UIProgressListener {
        void onProgress(long bytesRead, long expectedLength);

        /**
         * Control how often the listener needs an update. 0% and 100% will always be dispatched.
         *
         * @return in percentage (0.2 = call {@link #onProgress} around every 0.2 percent of progress)
         */
        float getGranualityPercentage();
    }

    public static void forget(String key) {
        DispatchingProgressListener.forget(key);
    }

    public static void expect(String key, UIProgressListener listener) {
        Log.i(TAG, "expect:key:" + key);
        DispatchingProgressListener.expect(key, listener);
    }

    interface ResponseProgressListener {
        void update(String key, long bytesRead, long contentLength);
    }

    static class DispatchingProgressListener implements ResponseProgressListener {
        public static final String TAG = "DPListener";

        private static final Map<String, List<UIProgressListener>> LISTENERS = new HashMap<>();
        private static final Map<String, List<Long>> PROGRESSES = new HashMap<>();

        private final Handler handler;

        DispatchingProgressListener() {
            this.handler = new Handler(Looper.getMainLooper());
        }

        static void forget(String key) {
            LISTENERS.remove(key);
            PROGRESSES.remove(key);
        }

        static void expect(String key, UIProgressListener listener) {
            List<UIProgressListener> listeners = LISTENERS.get(key);
            if (listeners == null) listeners = new ArrayList<>();
            if (!listeners.contains(listener)) {
                listeners.add(listener);
                LISTENERS.put(key, listeners);
            }
        }

        @Override
        public void update(String key, final long bytesRead, final long contentLength) {
            Log.i(TAG, String.format(
                    "update:%s: %d/%d = %.2f%%%n",
                    key,
                    bytesRead,
                    contentLength,
                    (100f * bytesRead) / contentLength)
            );
            final List<UIProgressListener> listeners = LISTENERS.get(key);
            if (listeners == null) {
                return;
            }
            if (contentLength <= bytesRead) {
                forget(key);
            }
            for (final UIProgressListener listener : listeners) {
                int index = listeners.indexOf(listener);
                if (needsDispatch(key, index, bytesRead, contentLength, listener.getGranualityPercentage())) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onProgress(bytesRead, contentLength);
                        }
                    });
                }
            }
        }

        private boolean needsDispatch(String key, int index, long current, long total, float granularity) {
            if (granularity == 0 || current == 0 || total == current) {
                return true;
            }
            float percent = 100f * current / total;
            long currentProgress = (long) (percent / granularity);

            List<Long> progresses = PROGRESSES.get(key);
            if (progresses == null) progresses = new ArrayList<>();

            Long lastProgress = progresses.size() > index ? progresses.get(index) : null;
            if (lastProgress == null) {
                progresses.add(currentProgress);
                PROGRESSES.put(key, progresses);
                return true;
            } else if (currentProgress != lastProgress) {
                progresses.set(index, currentProgress);
                PROGRESSES.put(key, progresses);
                return true;
            } else {
                return false;
            }
        }
    }
}