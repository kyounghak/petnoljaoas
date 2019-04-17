package com.chaigene.petnolja.manager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.ObjectKey;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.StorageReference;
import com.chaigene.petnolja.image.glide.GlideApp;
import com.chaigene.petnolja.image.glide.GlideRequest;
import com.chaigene.petnolja.image.glide.targets.ProgressTarget;
import com.chaigene.petnolja.util.CommonUtil;

import java.util.concurrent.ExecutionException;

public class GlideManager {
    public static final String TAG = "GlideManager";

    public static final int SCALE_TYPE_CENTER_CROP = 0;
    public static final int SCALE_TYPE_FIT_CENTER = 1;

    public static Task<Void> loadImage(@NonNull StorageReference imageRef,
                                       @NonNull ImageView imageView) {
        // Log.i(TAG, "loadImage");
        return loadImageWithSignature(imageRef, null, imageView, SCALE_TYPE_FIT_CENTER);
    }

    public static Task<Void> loadImage(@NonNull StorageReference imageRef,
                                       @NonNull ImageView imageView,
                                       @NonNull int scaleType) {
        // Log.i(TAG, "loadImage");
        return loadImageWithSignature(imageRef, null, imageView, scaleType);
    }

    @Deprecated
    public static Task<Void> loadImageWithSignature(@NonNull final StorageReference imageRef,
                                                    @NonNull final ImageView imageView) {

        // Log.i(TAG, "loadImageWithSignature:imageRef:" + imageRef.getPath());
        DatabaseReference signatureRef = DatabaseManager.getSignatureRef(imageRef);

        /*Log.i(TAG, "loadImageWithSignature:" +
                "imageRef:" + imageRef.getPath() + "/" +
                "signatureRef:" + DatabaseManager.getPath(signatureRef));*/

        return DatabaseManager.getSignature(signatureRef).continueWithTask(new Continuation<String, Task<Void>>() {
            @Override
            public Task<Void> then(@NonNull Task<String> task) throws Exception {
                String lastSignature = null;
                if (!task.isSuccessful()) {
                    Exception e = task.getException();
                    Log.w(TAG, "loadImage:GET_SIGNATURE:ERROR:", e);
                    // TODO: 소스 수정 필요
                    // if (!(e instanceof DatabaseManager.NullDataException)) throw e;
                } else {
                    lastSignature = task.getResult();
                    Log.d(TAG, "loadImage:GET_SIGNATURE:SUCCESS:lastSignature:" + lastSignature);
                }
                /*if (task.getResult() == null) {
                    Log.w(TAG, "loadImage:GET_SIGNATURE:ERROR:signature_is_not_exist.");
                }*/
                return loadImageWithSignature(imageRef, lastSignature, imageView, SCALE_TYPE_FIT_CENTER);
            }
        });
    }

    public static Task<Void> loadImageWithSignature(@NonNull final StorageReference imageRef,
                                                    @NonNull final DatabaseReference signatureRef,
                                                    @NonNull final ImageView imageView) {

        // Log.i(TAG, "loadImageWithSignature:imageRef:" + imageRef.getPath());
        return DatabaseManager.getString(signatureRef).continueWithTask(new Continuation<String, Task<Void>>() {
            @Override
            public Task<Void> then(@NonNull Task<String> task) throws Exception {
                String lastSignature = null;
                if (!task.isSuccessful()) {
                    Exception e = task.getException();
                    Log.w(TAG, "loadImage:GET_SIGNATURE:ERROR:", e);
                    // TODO: 소스 수정 필요
                    // if (!(e instanceof DatabaseManager.NullDataException)) throw e;
                } else {
                    lastSignature = task.getResult();
                    Log.d(TAG, "loadImage:GET_SIGNATURE:SUCCESS:lastSignature:" + lastSignature);
                }
                /*if (task.getResult() == null) {
                    Log.w(TAG, "loadImage:GET_SIGNATURE:ERROR:signature_is_not_exist.");
                }*/
                return loadImageWithSignature(imageRef, lastSignature, imageView, SCALE_TYPE_FIT_CENTER);
            }
        });
    }

    public static Task<Void> loadImageWithSignature(@NonNull final StorageReference imageRef,
                                                    @Nullable String signature,
                                                    @NonNull final ImageView imageView) {
        return loadImageWithSignature(imageRef, signature, imageView, SCALE_TYPE_FIT_CENTER);
    }

    // 이미 signature가 존재할 경우.
    @SuppressLint("CheckResult")
    @SuppressWarnings("ConstantConditions")
    public static Task<Void> loadImageWithSignature(@NonNull final StorageReference imageRef,
                                                    @Nullable String signature,
                                                    @NonNull final ImageView imageView,
                                                    int scaleType) {
        Log.i(TAG, "loadImageWithSignature:" +
                "imageRef:" + imageRef.getPath() + "|" +
                "imageView:" + CommonUtil.getResourceName(imageView) + "|" +
                "signature:" + signature);
        final TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();

        Context c = imageView.getContext().getApplicationContext();

        GlideRequest<Drawable> request = GlideApp.with(c).load(imageRef);

        // request.diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true);

        Drawable oldImg = imageView.getDrawable();
        if (oldImg != null) request.placeholder(oldImg);

        // 기존에 존재하는 signature 일 경우 해당 파일을 가져오고 새로운 signature 일 경우 다운로드 한다.
        if (signature != null) request.signature(new ObjectKey(signature));

        GlideRequest<Drawable> builder = null;
        if (scaleType == SCALE_TYPE_CENTER_CROP)
            builder = request.centerCrop();
        else if (scaleType == SCALE_TYPE_FIT_CENTER)
            builder = request.fitCenter();

        builder.transition(DrawableTransitionOptions.withCrossFade());

        builder.listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                Log.w(TAG, "loadImage:listener:ERROR:" + e.getMessage());
                tcs.trySetException(e);
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                resource.getBounds().height();
                // Log.d(TAG, "loadImage:listener:SUCCESS:isFromMemoryCache:" + isFromMemoryCache + "|isFirstResource:" + isFirstResource);
                tcs.trySetResult(null);
                return false;
            }
        });

        final GlideRequest<Drawable> builderForUiThred = builder;
        CommonUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                builderForUiThred.into(imageView);
            }
        });
        return tcs.getTask();
    }

    @SuppressWarnings("ConstantConditions")
    public static Task<Void> loadImageWithTarget(@NonNull final StorageReference imageRef,
                                                 @Nullable String signature,
                                                 @NonNull final ImageView imageView,
                                                 int scaleType,
                                                 @NonNull final ProgressTarget<Bitmap> target) {
        Log.i(TAG, "loadImageWithSignature:" +
                "imageRef:" + imageRef.getPath() + "|" +
                "imageView:" + CommonUtil.getResourceName(imageView) + "|" +
                "signature:" + signature);
        final TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();

        Context c = imageView.getContext().getApplicationContext();

        target.setModel(imageRef);

        GlideRequest<Bitmap> request = GlideApp.with(c).asBitmap().load(imageRef);

        // request = request.diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true);

        Drawable oldImg = imageView.getDrawable();
        if (oldImg != null) request.placeholder(oldImg);

        // 기존에 존재하는 signature 일 경우 해당 파일을 가져오고 새로운 signature 일 경우 다운로드 한다.
        if (signature != null) request.signature(new ObjectKey(signature));

        GlideRequest<Bitmap> builder = null;
        if (scaleType == SCALE_TYPE_CENTER_CROP)
            builder = request.centerCrop();
        else if (scaleType == SCALE_TYPE_FIT_CENTER)
            builder = request.fitCenter();

        builder = builder.transition(BitmapTransitionOptions.withCrossFade());

        builder = builder.listener(new RequestListener<Bitmap>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                Log.w(TAG, "loadImage:listener:ERROR:" + e.getMessage());
                tcs.trySetException(e);
                return false;
            }

            @Override
            public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                // resource.getBounds().height();
                // Log.d(TAG, "loadImage:listener:SUCCESS:isFromMemoryCache:" + isFromMemoryCache + "|isFirstResource:" + isFirstResource);
                tcs.trySetResult(null);
                return false;
            }
        });

        builder.into(target);
        return tcs.getTask();
    }

    public static Task<Bitmap> loadImage(@NonNull Context context, @NonNull final StorageReference imageRef) {
        Log.i(TAG, "loadImage:bucket:" + imageRef.getBucket() + "|imageRef:" + imageRef.getPath());

        // GlideApp.get(context).clearDiskCache();
        // GlideApp.get(context).clearMemory();

        final TaskCompletionSource<Bitmap> tcs = new TaskCompletionSource<>();
        Context c = context.getApplicationContext();
        Bitmap bitmap = null;
        try {
            bitmap = GlideApp.with(c)
                    .asBitmap()
                    .load(imageRef)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .fitCenter()
                    .listener(new RequestListener<Bitmap>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                            // todo log exception to central service or something like that
                            Log.w(TAG, "loadImage:onLoadFailed:", e);
                            tcs.trySetException(e);
                            // important to return false so the error placeholder can be placed
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                            // everything worked out, so probably nothing to do
                            return false;
                        }
                    })
                    .submit()
                    .get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            tcs.trySetResult(null);
        } catch (ExecutionException e) {
            e.printStackTrace();
            tcs.trySetResult(null);
        }
        if (bitmap != null) tcs.trySetResult(bitmap);
        return tcs.getTask();
    }
}
