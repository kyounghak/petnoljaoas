package com.chaigene.petnolja.ui.view;

import android.graphics.Rect;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

public class GridSpaceItemDecoration extends RecyclerView.ItemDecoration {
    private int spanCount;
    private int spacing;

    public GridSpaceItemDecoration(int spanCount, int spacing) {
        this.spanCount = spanCount;
        this.spacing = spacing;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        // item position
        int position = parent.getChildAdapterPosition(view);
        // item column
        int column = position % spanCount;

        // outRect.left = spacing - column * spacing / spanCount;
        // outRect.right = (column + 1) * spacing / spanCount;
        // top edge
        // if (position < spanCount) {
        //     outRect.top = spacing;
        // }
        // item bottom
        // outRect.bottom = spacing;

        // 최상단에는 무조건 마진을 넣는다.
        outRect.left = column * spacing / spanCount;
        outRect.right = spacing - (column + 1) * spacing / spanCount;
        // Item top margin
        // if (position >= spanCount) outRect.top = spacing;
        // if (position < spanCount) outRect.top = spacing;
        outRect.top = spacing;
    }
}