package com.chaigene.petnolja.image.glide.targets;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.request.target.Target;
import com.chaigene.petnolja.ui.view.CircleProgressBar;

public class ArticleProgressTarget<Z> extends ProgressTarget<Z> {
    public static final String TAG = "ArticleProgressTarget";

    private final ImageView sourceImageView;
    private final CircleProgressBar progressView;

    public ArticleProgressTarget(Context context,
                                 Target<Z> target,
                                 ImageView sourceImageView,
                                 CircleProgressBar progressView) {
        super(context, target);
        this.sourceImageView = sourceImageView;
        this.progressView = progressView;
    }

    @Override
    public float getGranualityPercentage() {
        return 0.1f;
    }

    @Override
    protected void onConnecting() {
        Log.i(TAG, "onConnecting");
        // sourceImageView.setImageLevel(0);
        // progressTextView.setVisibility(View.VISIBLE);
        // progressTextView.setText("connecting");
        progressView.setVisibility(View.VISIBLE);
        progressView.setProgress(0);
    }

    @Override
    protected void onDownloading(long bytesRead, long expectedLength) {
        Log.i(TAG, "onDownloading");
        // sourceImageView.setImageLevel((int) (10000 * bytesRead / expectedLength));
        // progressTextView.setText(String.format(Locale.ROOT, "downloading %.2f/%.2f MB %.1f%%",
        // bytesRead / 1e6, expectedLength / 1e6, 100f * bytesRead / expectedLength));
        float progress = 100 * bytesRead / expectedLength;
        progressView.setProgress(progress);
    }

    @Override
    protected void onDownloaded() {
        Log.i(TAG, "onDownloaded");
        // sourceImageView.setImageLevel(10000);
        // progressTextView.setText("decoding and transforming");
        progressView.setProgress(100);
    }

    @Override
    protected void onDelivered() {
        Log.i(TAG, "onDelivered");
        // sourceImageView.setImageLevel(0); // reset ImageView default
        // progressTextView.setVisibility(View.INVISIBLE);
        progressView.setVisibility(View.INVISIBLE);
        progressView.setProgress(0);
    }
}