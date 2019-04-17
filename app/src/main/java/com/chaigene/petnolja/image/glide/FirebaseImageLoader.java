package com.chaigene.petnolja.image.glide;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StreamDownloadTask;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;

/**
 * ModelLoader implementation to download images from FirebaseStorage with Glide.
 * <p>
 * <p>
 * First, register this class in your AppGlideModule:
 * <pre>
 *         {@literal @}Override
 *         public void registerComponents(Context context, Registry registry) {
 *             // Register FirebaseImageLoader to handle StorageReference
 *             registry.append(StorageReference.class, InputStream.class,
 *                     new FirebaseImageLoader.Factory());
 *         }
 * </pre>
 * <p>
 * <p>
 * Then load a StorageReference into an ImageView.
 * <pre>
 *     StorageReference ref = FirebaseStorage.getInstance().getReference().child("myimage");
 *     ImageView iv = (ImageView) findViewById(R.id.my_image_view);
 *
 *     GlideApp.with(this)
 *         .load(ref)
 *         .into(iv);
 * </pre>
 */
public class FirebaseImageLoader implements ModelLoader<StorageReference, InputStream> {

    private static final String TAG = "FirebaseImageLoader";


    /**
     * Factory to create {@link FirebaseImageLoader}.
     */
    public static class Factory implements ModelLoaderFactory<StorageReference, InputStream> {

        @Override
        public ModelLoader<StorageReference, InputStream> build(MultiModelLoaderFactory factory) {
            return new FirebaseImageLoader();
        }

        @Override
        public void teardown() {
            // No-op
        }
    }

    @Nullable
    @Override
    public LoadData<InputStream> buildLoadData(StorageReference reference,
                                               int height,
                                               int width,
                                               Options options) {
        return new LoadData<>(new FirebaseStorageKey(reference), new FirebaseStorageFetcher(reference));
    }

    @Override
    public boolean handles(StorageReference reference) {
        return true;
    }

    private static class FirebaseStorageKey implements Key {

        private StorageReference mRef;

        public FirebaseStorageKey(StorageReference ref) {
            mRef = ref;
        }

        @Override
        public void updateDiskCacheKey(MessageDigest digest) {
            digest.update(mRef.getPath().getBytes(Charset.defaultCharset()));
        }
    }

    private static class FirebaseStorageFetcher implements DataFetcher<InputStream> {
        public static final String TAG = "FirebaseStorageFetcher";

        private StorageReference mRef;
        private StreamDownloadTask mStreamTask;
        private InputStream mInputStream;
        private FirebaseAppGlideModule.ResponseProgressListener mProgressListener;

        public FirebaseStorageFetcher(StorageReference ref) {
            // Log.i(TAG, "FirebaseStorageFetcher:ref_path:" + ref.getPath());
            mRef = ref;
            mProgressListener = new FirebaseAppGlideModule.DispatchingProgressListener();
        }

        @Override
        public void loadData(Priority priority, final DataCallback<? super InputStream> callback) {
            // Log.i(TAG, "loadData");
            mStreamTask = mRef.getStream();
            mStreamTask
                    .addOnSuccessListener(new OnSuccessListener<StreamDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(StreamDownloadTask.TaskSnapshot snapshot) {
                            String key = mRef.getPath();
                            InputStream in = snapshot.getStream();
                            long length = snapshot.getTotalByteCount();

                            try {
                                ProgressInputStream is = new ProgressInputStream(key, in, length);
                                is.setListener(new ProgressInputStream.StreamProgressListener() {
                                    @Override
                                    public void update(String key, long bytesRead, long contentLength) {
                                        Log.i(TAG, "onSuccess:update:" +
                                                "key:" + key +
                                                "|bytesRead:" + bytesRead +
                                                "|contentLength:" + contentLength);
                                        mProgressListener.update(key, bytesRead, contentLength);
                                    }
                                });
                                mInputStream = is;
                            } catch (IOException e) {
                                e.printStackTrace();
                                callback.onLoadFailed(e);
                                return;
                            }

                            callback.onDataReady(mInputStream);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            callback.onLoadFailed(e);
                        }
                    });
        }

        @Override
        public void cleanup() {
            // Close stream if possible
            if (mInputStream != null) {
                try {
                    mInputStream.close();
                    mInputStream = null;
                } catch (IOException e) {
                    Log.w(TAG, "Could not close stream", e);
                }
            }
        }

        @Override
        public void cancel() {
            // Cancel task if possible
            if (mStreamTask != null && mStreamTask.isInProgress()) {
                mStreamTask.cancel();
            }
        }

        @NonNull
        @Override
        public Class<InputStream> getDataClass() {
            return InputStream.class;
        }

        @NonNull
        @Override
        public DataSource getDataSource() {
            return DataSource.REMOTE;
        }
    }

    private static class ProgressInputStream extends InputStream {

        private final String key;
        private InputStream in;
        private long length, sumRead;
        private StreamProgressListener listener;

        public interface StreamProgressListener {
            void update(String key, long bytesRead, long contentLength);
        }

        public ProgressInputStream(String key, InputStream inputStream, long length) throws IOException {
            Log.i(TAG, "ProgressInputStream");
            this.key = key;
            this.in = inputStream;
            this.sumRead = 0;
            this.length = length;
        }

        @Override
        public int read(byte[] b) throws IOException {
            int readCount = in.read(b);
            evaluate(readCount);
            return readCount;
        }


        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int readCount = in.read(b, off, len);
            evaluate(readCount);
            return readCount;
        }

        @Override
        public long skip(long n) throws IOException {
            long skip = in.skip(n);
            evaluate(skip);
            return skip;
        }

        @Override
        public int read() throws IOException {
            int read = in.read();
            if (read != -1) {
                evaluate(1);
            }
            return read;
        }

        public ProgressInputStream setListener(StreamProgressListener listener) {
            this.listener = listener;
            return this;
        }

        private void evaluate(long readCount) {
            if (readCount != -1) {
                sumRead += readCount;
            }
            notifyListener();
        }

        private void notifyListener() {
            if (listener != null) listener.update(key, sumRead, length);
        }
    }
}