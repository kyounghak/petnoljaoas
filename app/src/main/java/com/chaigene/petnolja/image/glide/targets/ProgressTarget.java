package com.chaigene.petnolja.image.glide.targets;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;

import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.storage.StorageReference;
import com.chaigene.petnolja.image.glide.FirebaseAppGlideModule;
import com.chaigene.petnolja.image.glide.GlideApp;

public abstract class ProgressTarget<Z> extends WrappingTarget<Z> implements FirebaseAppGlideModule.UIProgressListener {
    public static final String TAG = "ProgressTarget";

    private Context mContext;
    private StorageReference model;
    private boolean ignoreProgress = true;

    public ProgressTarget(Context context, Target<Z> target) {
        this(context, null, target);
    }

    public ProgressTarget(Context context, StorageReference model, Target<Z> target) {
        super(target);
        this.mContext = context;
        this.model = model;
    }

    public final StorageReference getModel() {
        return model;
    }

    public final void setModel(StorageReference model) {
        GlideApp.with(mContext).clear(this); // indirectly calls cleanup
        this.model = model;
    }

    /**
     * Convert a model into an path string that is used to match up the StorageReference download requests.
     *
     * @param model return the representation of the given model, DO NOT use {@link #getModel()} inside this method.
     * @return a stable path representation of the model, otherwise the progress reporting won't work
     */
    protected String toPathString(StorageReference model) {
        if (model == null) return null;
        return model.getPath();
    }

    @Override
    public float getGranualityPercentage() {
        return 1.0f;
    }

    @Override
    public void onProgress(long bytesRead, long expectedLength) {
        if (ignoreProgress) {
            return;
        }
        if (expectedLength == Long.MAX_VALUE) {
            onConnecting();
        } else if (bytesRead == expectedLength) {
            onDownloaded();
        } else {
            onDownloading(bytesRead, expectedLength);
        }
    }

    /**
     * Called when the Glide load has started.
     * At this time it is not known if the Glide will even go and use the network to fetch the image.
     */
    protected abstract void onConnecting();

    /**
     * Called when there's any progress on the download; not called when loading from cache.
     * At this time we know how many bytes have been transferred through the wire.
     */
    protected abstract void onDownloading(long bytesRead, long expectedLength);

    /**
     * Called when the bytes downloaded reach the length reported by the server; not called when loading from cache.
     * At this time it is fairly certain, that Glide either finished reading the stream.
     * This means that the image was either already decoded or saved the network stream to cache.
     * In the latter case there's more work to do: decode the image from cache and transform.
     * These cannot be listened to for progress so it's unsure how fast they'll be, best to show indeterminate progress.
     */
    protected abstract void onDownloaded();

    /**
     * Called when the Glide load has finished either by successfully loading the image or failing to load or cancelled.
     * In any case the best is to hide/reset any progress displays.
     */
    protected abstract void onDelivered();

    private void start() {
        FirebaseAppGlideModule.expect(toPathString(model), this);
        ignoreProgress = false;
        onProgress(0, Long.MAX_VALUE);
    }

    private void cleanup() {
        ignoreProgress = true;
        StorageReference model = this.model; // save in case it gets modified
        onDelivered();
        FirebaseAppGlideModule.forget(toPathString(model));
        this.model = null;
    }

    @Override
    public void onLoadStarted(Drawable placeholder) {
        super.onLoadStarted(placeholder);
        start();
    }

    @Override
    public void onResourceReady(Z resource, Transition<? super Z> transition) {
        cleanup();
        super.onResourceReady(resource, transition);
    }

    @Override
    public void onLoadFailed(@Nullable Drawable errorDrawable) {
        cleanup();
        super.onLoadFailed(errorDrawable);
    }

    @Override
    public void onLoadCleared(Drawable placeholder) {
        cleanup();
        super.onLoadCleared(placeholder);
    }
}