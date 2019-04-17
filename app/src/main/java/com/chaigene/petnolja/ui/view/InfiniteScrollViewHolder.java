package com.chaigene.petnolja.ui.view;

import android.view.View;

import com.airbnb.lottie.LottieAnimationView;
import com.chaigene.petnolja.R;

import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;

public class InfiniteScrollViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.progress_bar)
    LottieAnimationView progressBar;

    public InfiniteScrollViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }
}
