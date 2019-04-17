package com.chaigene.petnolja.ui.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.storage.StorageReference;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.image.glide.GlideApp;
import com.chaigene.petnolja.image.glide.targets.ProgressTarget;
import com.chaigene.petnolja.manager.StorageManager;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GlideActivity extends BaseActivity {

    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;

    private LinearLayoutManager mManager;
    private ProgressAdapter mAdapter;
    private List<StorageReference> mImageRefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_glide);
        ButterKnife.bind(this);

        mManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mManager);

        mRecyclerView.setHasFixedSize(true);

        // gs://panda-server-tokyo-test/article/posts/-Kvmfnx5WkYGpcoWHH_W/19ae2d47-9277-49f8-8dd1-389f92ac396f.jpg
        // gs://panda-server-tokyo-test/article/posts/-L201jy5revqsEAg_xE5/0cabae62-e2aa-4272-a409-837f88c06e1d.jpg
        // gs://panda-server-tokyo-test/RWU.png
        mImageRefs = Arrays.asList(
                // StorageManager.getArticlePostsRef(STORAGE_REGION_TOKYO).child("-Kvmfnx5WkYGpcoWHH_W").child("19ae2d47-9277-49f8-8dd1-389f92ac396f.jpg"),
                StorageManager.getArticlePostsRef().child("-L201jy5revqsEAg_xE5").child("0cabae62-e2aa-4272-a409-837f88c06e1d.jpg"),
                StorageManager.getRef().child("table.png"),
                StorageManager.getRef().child("RWU.png"),
                StorageManager.getRef().child("RWU.png")
        );
        mAdapter = new ProgressAdapter(mImageRefs);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        setToolbarTitle("Glide Test");
    }

    private static class ProgressViewHolder extends ViewHolder {
        private final Context context;
        private final ImageView image;
        private final TextView text;
        private final ProgressBar progress;
        /**
         * Cache target because all the views are tied to this view holder.
         */
        private final ProgressTarget<Bitmap> target;

        ProgressViewHolder(View root) {
            super(root);
            context = root.getContext();
            image = root.findViewById(R.id.image);
            text = root.findViewById(R.id.text);
            progress = root.findViewById(R.id.progress);
            image.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    bind(target.getModel());
                }
            });
            target = new MyProgressTarget<>(context, new BitmapImageViewTarget(image), progress, image, text);
        }

        void bind(StorageReference imageRef) {
            target.setModel(imageRef);
            GlideApp.with(context)
                    .asBitmap()
                    .placeholder(R.drawable.github_232_progress)
                    .load(imageRef)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .centerCrop()
                    .into(target);
        }
    }

    /**
     * Demonstrates 3 different ways of showing the progress:
     * <ul>
     * <li>Update a full fledged progress bar</li>
     * <li>Update a text view to display size/percentage</li>
     * <li>Update the placeholder via Drawable.level</li>
     * </ul>
     * This last one is tricky: the placeholder that Glide sets can be used as a progress drawable
     * without any extra Views in the view hierarchy if it supports levels via <code>usesLevel="true"</code>
     * or <code>level-list</code>.
     *
     * @param <Z> automatically match any real Glide target so it can be used flexibly without reimplementing.
     */
    @SuppressLint("SetTextI18n") // text set only for debugging
    private static class MyProgressTarget<Z> extends ProgressTarget<Z> {
        public static final String TAG = "MyProgressTarget";

        private final TextView text;
        private final ProgressBar progress;
        private final ImageView image;

        public MyProgressTarget(Context context, Target<Z> target, ProgressBar progress, ImageView image, TextView text) {
            super(context, target);
            this.progress = progress;
            this.image = image;
            this.text = text;
        }

        @Override
        public float getGranualityPercentage() {
            return 0.1f; // this matches the format string for #text below
        }

        @Override
        protected void onConnecting() {
            Log.i(TAG, "onConnecting");
            progress.setIndeterminate(true);
            progress.setVisibility(View.VISIBLE);
            image.setImageLevel(0);
            text.setVisibility(View.VISIBLE);
            text.setText("connecting");
        }

        @Override
        protected void onDownloading(long bytesRead, long expectedLength) {
            Log.i(TAG, "onDownloading:this:" + this);
            progress.setIndeterminate(false);
            progress.setProgress((int) (100 * bytesRead / expectedLength));
            image.setImageLevel((int) (10000 * bytesRead / expectedLength));
            text.setText(String.format(Locale.ROOT, "downloading %.2f/%.2f MB %.1f%%",
                    bytesRead / 1e6, expectedLength / 1e6, 100f * bytesRead / expectedLength));
        }

        @Override
        protected void onDownloaded() {
            Log.i(TAG, "onDownloaded");
            progress.setIndeterminate(true);
            image.setImageLevel(10000);
            text.setText("decoding and transforming");
        }

        @Override
        protected void onDelivered() {
            Log.i(TAG, "onDelivered");
            progress.setVisibility(View.INVISIBLE);
            image.setImageLevel(0); // reset ImageView default
            text.setVisibility(View.INVISIBLE);
        }
    }

    private static class ProgressAdapter extends Adapter<ProgressViewHolder> {
        private final List<StorageReference> models;

        public ProgressAdapter(List<StorageReference> models) {
            this.models = models;
        }

        @Override
        public ProgressViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.github_232_item, parent, false);
            return new ProgressViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ProgressViewHolder holder, int position) {
            holder.bind(models.get(position));
        }

        @Override
        public int getItemCount() {
            return models.size();
        }
    }
}