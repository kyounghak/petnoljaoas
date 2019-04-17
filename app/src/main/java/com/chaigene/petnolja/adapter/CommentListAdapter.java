package com.chaigene.petnolja.adapter;

import android.content.Context;
import android.graphics.Typeface;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.chaigene.petnolja.R;
import com.chaigene.petnolja.hashtag.AtSignHelper;
import com.chaigene.petnolja.hashtag.HashTagHelper;
import com.chaigene.petnolja.model.Comment;
import com.chaigene.petnolja.util.CommonUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

// TODO: Infinite loading 구현 필요함.
public class CommentListAdapter extends RecyclerView.Adapter<CommentListAdapter.CommentViewHolder> implements HashTagHelper.OnHashTagClickListener, AtSignHelper.OnAtSignClickListener {
    public static final String TAG = "CommentListAdapter";

    private List<Comment> mComments = new ArrayList<>();
    private Context mContext;
    private Context mAppContext;

    public CommentListAdapter(Context context, List<Comment> comments) {
        // Log.i(TAG, "CommentListAdapter");

        this.mContext = context;
        this.mAppContext = context.getApplicationContext();
        this.mComments = comments;
    }

    @Override
    public CommentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.i(TAG, "onCreateViewHolder");
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        CommentViewHolder vh = new CommentViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(final CommentViewHolder holder, final int position) {
        Log.i(TAG, "onBindViewHolder");

        final Comment comment = mComments.get(position);

        HashTagHelper hashTagHelper = CommonUtil.createDefaultHashTagHelper(mContext, this);
        hashTagHelper.handle(holder.tvContentText);
        AtSignHelper atSignHelper = CommonUtil.createDefaultAtSignHelper(mContext, this);
        atSignHelper.handle(holder.tvContentText);

        holder.setContentText(comment.getNickname(), comment.getContent());

        // String date = CommonUtil.getFormattedTimeString(comment.getTimestamp(true), "MM/dd hh:mm aa");
        String date = CommonUtil.getTimeAgo(mContext, comment.getTimestamp(true));
        holder.tvDateText.setText(date);
    }

    @Override
    public int getItemCount() {
        return mComments.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void onHashTagClicked(String hashTag) {
        Log.i(TAG, "onHashTagClicked:hashTag:" + hashTag);
        if (mOnItemClickListener != null) mOnItemClickListener.onItemHashtagClick(hashTag);
    }

    @Override
    public void onAtSignClicked(String atSign) {
        Log.i(TAG, "onAtSignClicked:atSign:" + atSign);
        if (mOnItemClickListener != null) mOnItemClickListener.onItemAtSignClick(atSign);
    }

    public class CommentViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener {

        @BindView(R.id.comment_container)
        public ViewGroup vgCommentContainer;

        /*@BindView(R.id.guide_anchor)
        public View vGuideAnchor;*/

        @BindView(R.id.nickname_overlay)
        public View vNicknameOverlay;

        @BindView(R.id.comment_text)
        public TextView tvContentText;

        @BindView(R.id.date_text)
        public TextView tvDateText;

        public CommentViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            vgCommentContainer.setOnClickListener(this);
            vNicknameOverlay.setOnClickListener(this);

            vgCommentContainer.setOnLongClickListener(this);
            vNicknameOverlay.setOnLongClickListener(this);
            tvContentText.setOnLongClickListener(this);
            tvDateText.setOnLongClickListener(this);
        }

        private void setContentText(String nickname, String content) {
            Log.i(TAG, "setContentText:nickname:" + nickname + "|content:" + content);

            int highlightLength = nickname.length();
            CharSequence result = nickname + "  " + content;
            SpannableStringBuilder builder = new SpannableStringBuilder(result);
            builder.setSpan(new StyleSpan(Typeface.BOLD), 0, highlightLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            tvContentText.setText(builder);

            // CharSequence spannedText = tvContentText.getText();
            // Spannable spannable = (Spannable) spannedText;
            // CharacterStyle[] spans = spannable.getSpans(0, spannedText.length(), CharacterStyle.class);
            /*int spanStart = spannable.getSpanStart(spans[0]);
            int spanEnd = spannable.getSpanEnd(spans[0]);
            CharSequence spanText = spannedText.subSequence(spanStart, spanEnd);*/
            // Log.d(TAG, "showCommentMentionGuide:spanText:" + spanText);

            TextPaint paint = new TextPaint();
            paint.setAntiAlias(true);
            paint.setTextSize(tvContentText.getTextSize());

            StaticLayout tempLayout = new StaticLayout(
                    nickname,
                    paint,
                    10000,
                    Layout.Alignment.ALIGN_NORMAL,
                    1f,
                    0f,
                    false
            );
            int lineCount = tempLayout.getLineCount();
            float textWidth = 0;
            for (int i = 0; i < lineCount; i++) {
                textWidth += tempLayout.getLineWidth(i);
            }

            int textHeight = tempLayout.getHeight();

            Log.d(TAG, "setContentText:textWidth:" + textWidth);
            vNicknameOverlay.setLayoutParams(new FrameLayout.LayoutParams(Math.round(textWidth), textHeight));

            /*CommonUtil.appendHighlightText(tvContentText, nickname, nickname, new ClickableSpan() {
                final String TAG = "ClickableSpan";

                @Override
                public void onClick(View widget) {
                    Log.d(TAG, "onClick");
                    if (mOnItemClickListener != null)
                        mOnItemClickListener.onItemNicknameClick(comment.getUserId(), position);
                }

                @Override
                public void updateDrawState(TextPaint ds) {
                    ds.setUnderlineText(false);
                }
            });*/
        }

        @Override
        public void onClick(View view) {
            if (mOnItemClickListener == null) return;
            switch (view.getId()) {
                case R.id.comment_container:
                    // mOnItemClickListener.onItemClick(v, getAdapterPosition());
                    break;
                case R.id.nickname_overlay:
                    if (mOnItemClickListener != null) {
                        Comment comment = mComments.get(getAdapterPosition());
                        mOnItemClickListener.onItemNicknameClick(comment.getUid(), getAdapterPosition());
                    }
                    break;
                /*case R.id.nickname_text:
                    int position = getAdapterPosition();
                    Post post = mPosts.get(position);
                    String uid = post.getOldUser().getKey();
                    mOnItemClickListener.onItemNicknameClick(uid, position);
                    break;*/
            }
        }

        @Override
        public boolean onLongClick(View v) {

            String viewId = CommonUtil.getResourceName(v);
            Log.d(TAG, "onLongClick:viewId:" + viewId);

            if (mOnItemLongClickListener == null) return false;
            switch (v.getId()) {
                case R.id.comment_container:
                case R.id.comment_text:
                case R.id.date_text:
                    mOnItemLongClickListener.onItemLongClick(getAdapterPosition());
                    return true;
                case R.id.nickname_overlay:
                    String mention = CommonUtil.format("@%s", mComments.get(getAdapterPosition()).getNickname());
                    mOnItemLongClickListener.onItemNicknameLongClick(mention);
                    return true;
            }
            return false;
        }
    }

    private OnItemClickListener mOnItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(View view, int position);

        void onItemNicknameClick(String uid, int position);

        void onItemHashtagClick(String hashtag);

        void onItemAtSignClick(String atSign);
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        mOnItemClickListener = l;
    }

    private OnItemLongClickListener mOnItemLongClickListener;

    public interface OnItemLongClickListener {
        void onItemLongClick(int position);

        void onItemNicknameLongClick(String mention);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener l) {
        mOnItemLongClickListener = l;
    }

    /*private OnLoadMoreListener onLoadMoreListener;

    public interface OnLoadMoreListener {
        void onLoadMore();
    }

    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
    }*/
}