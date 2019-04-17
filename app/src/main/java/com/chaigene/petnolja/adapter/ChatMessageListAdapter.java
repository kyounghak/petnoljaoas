package com.chaigene.petnolja.adapter;

import android.content.Context;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.chaigene.petnolja.Constants;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.manager.AuthManager;
import com.chaigene.petnolja.manager.GlideManager;
import com.chaigene.petnolja.manager.StorageManager;
import com.chaigene.petnolja.model.FIRMessage;
import com.chaigene.petnolja.model.User;
import com.chaigene.petnolja.util.CommonUtil;
import com.chaigene.petnolja.util.UserUtil;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.chaigene.petnolja.util.CommonUtil.getFormattedTimeString;

public class ChatMessageListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final String TAG = "ChatMessageListAdapter";

    private static final int VISIBLE_THRESHOLD = 1;

    private final int VIEW_ITEM = 1;
    private final int VIEW_PROG = 0;

    private Context mContext;

    @LayoutRes
    private final int ITEM_LAYOUT_ID = R.layout.item_message;

    private User mTargetUser;
    // private StorageReference mFriendProfileImageRef;

    private RecyclerView mRecyclerView;
    private List<HashMap<String, FIRMessage>> mMessages;
    private int mLastDateCaptionPosition;
    private String mLastDateCaptionMessageId;
    private String mLastReadedMessageId;

    private int firstVisibleItem, lastVisibleItem, totalItemCount;

    private boolean mInitialDataLoaded;
    private boolean mLoading;

    public ChatMessageListAdapter(RecyclerView recyclerView, List<HashMap<String, FIRMessage>> messages) {
        Log.i(TAG, "ChatMessageListAdapter");

        mContext = recyclerView.getContext().getApplicationContext();
        mRecyclerView = recyclerView;
        mMessages = messages;

        if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {

            final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                final String TAG = "OnScrollListener";

                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    // Log.i(TAG, "onScrollStateChanged:newState:" + newState);
                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);

                    totalItemCount = linearLayoutManager.getItemCount();
                    firstVisibleItem = linearLayoutManager.findFirstVisibleItemPosition() + 1;
                    lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition() + 1;

                    /*Log.d(TAG, "totalItemCount:" + totalItemCount +
                            "/firstVisibleItem:" + firstVisibleItem +
                            "/lastVisibleItem:" + lastVisibleItem +
                            "/dx:" + dx +
                            "/dy:" + dy
                    );*/

                    if (!mLoading && totalItemCount <= (lastVisibleItem + VISIBLE_THRESHOLD)) {
                        // End has been reached
                        // if (onLoadMoreListener != null) onLoadMoreListener.onLoadMore();
                        // mLoading = true;
                    }

                    if (firstVisibleItem <= VISIBLE_THRESHOLD && dy < 0) {
                        Log.d(TAG, "onScrolled:reached:isLoading:" + isLoading());
                        if (!isLoading()) {
                            // Log.d(TAG, "onScrolled:reached");

                            // 최초 호출 되었을 때는 최초 로드된 데이타 라고 판단하고 무시해버린다.
                            if (!isInitialDataLoaded()) {
                                setInitialDataLoaded(true);
                                return;
                            }

                            if (onLoadMoreListener != null) onLoadMoreListener.onLoadMore();
                            setLoading(true);
                        }
                    }
                }
            });
        }
    }

    @Override
    public int getItemViewType(int position) {
        return mMessages.get(position) != null ? VIEW_ITEM : VIEW_PROG;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Log.i(TAG, "onCreateViewHolder");

        RecyclerView.ViewHolder holder;
        if (viewType == VIEW_ITEM) {
            View v = LayoutInflater.from(parent.getContext()).inflate(ITEM_LAYOUT_ID, parent, false);
            holder = new ChatHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_progress, parent, false);
            holder = new ProgressHolder(v);
        }
        return holder;
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (!(holder instanceof ChatHolder)) return;
        ChatHolder chatHolder = (ChatHolder) holder;
        Glide.with(mContext).clear(chatHolder.ivProfileImage);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Log.i(TAG, "onBindViewHolder:position:" + position);

        if (holder instanceof ChatHolder) {
            ChatHolder chatHolder = (ChatHolder) holder;
            FIRMessage firMessage = mMessages.get(position).values().iterator().next();

            Log.i(TAG, "onBindViewHolder:message:" + firMessage.toMap().toString());

            String dateFormat = mContext.getString(R.string.format_chat_date_caption);
            String dateCaption = CommonUtil.getFormattedTimeString(firMessage.getTimestamp(true), dateFormat);
            boolean isFirstMessageOfTheDate = isFirstMessageOfTheDate(position);
            chatHolder.setDateCaption(dateCaption, isFirstMessageOfTheDate);
            /*if (isFirstMessageOfTheDate) {
                Tasks.call(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        notifyDataSetChanged();
                        return null;
                    }
                });
            }*/

            if (mTargetUser.getId() != null) chatHolder.setNickname(mTargetUser.getNickname());

            chatHolder.setMessage(firMessage.getMessage());

            String time = getFormattedTimeString(firMessage.getTimestamp(true), "aa hh:mm");
            chatHolder.setTime(time);

            // 누가 보낸 메세지 인지를 여기서 결정
            boolean isMyMessage = firMessage.getUid().equals(AuthManager.getUserId());
            if (isMyMessage) {
                chatHolder.setIsSender(true);

                // 읽음 처리
                boolean isReaded = isReadedMessage(position);
                chatHolder.setReaded(isReaded);
            } else {
                chatHolder.setIsSender(false);

                UserUtil.downloadProfileImage(firMessage.getUid(), chatHolder.ivProfileImage).continueWith(new Continuation<Void, Void>() {
                    @Override
                    public Void then(@NonNull Task<Void> task) throws Exception {
                        return null;
                    }
                });
            }
        } else {
            // ((ProgressHolder) holder).progressBar.setIndeterminate(true);
        }
    }

    @Override
    public synchronized int getItemCount() {
        return mMessages.size();
    }

    public void updateLastDateCaption() {
        // notifyItemChanged(getLastDateCaptionPosition());
        String lastDateCaptionMessageId = getLastDateCaptionMessageId();
        int index = getIndexForKey(lastDateCaptionMessageId);
        if (index != -1) {
            // 만약 값이 존재한다면
            notifyItemChanged(index);
        } else {
            // 존재하지 않는다면 아무 것도 하지 않는다.
        }
    }

    private boolean isFirstMessageOfTheDate(int position) {
        boolean result;
        HashMap<String, FIRMessage> currentMessage = mMessages.get(position);
        long currentTimestamp = currentMessage.entrySet().iterator().next().getValue().getTimestamp(true);
        String currentDate = CommonUtil.getFormattedTimeString(currentTimestamp, "yyMMdd");

        if (position == 0) {
            Log.d(TAG, "isFirstMessageOfTheDate:previousDate:-/currentDate:-/true(position:0)");
            result = true;
        } else {
            // TODO: 로딩 중일 때는 previousMessage가 null을 반환할 수도 있다.
            HashMap<String, FIRMessage> previousMessage = mMessages.get(position - 1);
            if (previousMessage == null) return true;
            long previousTimestamp = previousMessage.entrySet().iterator().next().getValue().getTimestamp(true);
            String previousDate = CommonUtil.getFormattedTimeString(previousTimestamp, "yyMMdd");

            Log.d(TAG, "isFirstMessageOfTheDate:previousDate:" + previousDate + "/currentDate:" + currentDate + "/" + !currentDate.equals(previousDate));

            result = !currentDate.equals(previousDate);
        }

        // if (result) setLastDateCaptionPosition(position);
        if (result)
            setLastDateCaptionMessageId(currentMessage.entrySet().iterator().next().getKey());
        return result;
    }

    // 친구 정보를 입력 받아서 멤버 필드에 저장해두고 재사용한다.
    // 이 변수가 호출된다고 해서 딱히 특별한 이벤트가 발생하지는 않는다.
    public void setTargetUser(User targetUser) {
        mTargetUser = targetUser;
    }

    public synchronized void setLastDateCaptionPosition(int lastDateCaptionPosition) {
        mLastDateCaptionPosition = lastDateCaptionPosition;
    }

    private synchronized int getLastDateCaptionPosition() {
        return mLastDateCaptionPosition;
    }

    public synchronized void setLastDateCaptionMessageId(String lastDateCaptionMessageId) {
        mLastDateCaptionMessageId = lastDateCaptionMessageId;
    }

    private synchronized String getLastDateCaptionMessageId() {
        return mLastDateCaptionMessageId;
    }

    public synchronized void setLastReadedMessageId(String lastReadedMessageId) {
        mLastReadedMessageId = lastReadedMessageId;
        notifyDataSetChanged();
    }

    private synchronized String getLastReadedMessageId() {
        return mLastReadedMessageId;
    }

    private boolean isReadedMessage(int position) {
        String lastReadedMessageId = getLastReadedMessageId();
        if (lastReadedMessageId == null) {
            // Log.i(TAG, "isReadedMessage:index:-/position:" + position + "/false(null)");
            return false;
        }
        int index = getIndexForKey(lastReadedMessageId);
        // Log.i(TAG, "isReadedMessage:index:" + index + "/position:" + position + "/" + (index != -1 && index >= position));
        if (index != -1 && index >= position) {
            return true;
        }
        return false;
    }

    private int getIndexForKey(String key) {
        Log.i(TAG, "getIndexForKey:key:" + key);
        // Log.d(TAG, "getIndexForKey:mMessages:" + mMessages.toString());
        int index = 0;
        for (HashMap<String, FIRMessage> chat : mMessages) {
            // chat이 null 일 경우 로딩 중으로 간주한다.
            if (chat != null && chat.containsKey(key)) {
                return index;
            } else {
                index++;
            }
        }
        return -1;
    }

    public void showLoading() {
        Tasks.call(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                mMessages.add(0, null);
                notifyItemInserted(0);
                mRecyclerView.getLayoutManager().smoothScrollToPosition(mRecyclerView, null, 0);
                return null;
            }
        });
    }

    public void hideLoading() {
        Log.i(TAG, "hideLoading");
        setLoading(false);
        if (getItemViewType(0) == VIEW_PROG) {
            mMessages.remove(0);
            notifyItemRemoved(0);
            // 로딩 및 date caption 높이만큼 스크롤을 아래로 이동시켜준다.
            int progressHeight = CommonUtil.dpToPx(mContext, 20);
            int dateCaptionHeight = CommonUtil.dpToPx(mContext, 36);
            mRecyclerView.scrollBy(0, progressHeight + dateCaptionHeight);
        }
    }

    public synchronized boolean isLoading() {
        return mLoading;
    }

    private synchronized void setLoading(boolean isLoading) {
        Log.i(TAG, "setLoading:" + isLoading);
        mLoading = isLoading;
    }

    public boolean isInitialDataLoaded() {
        return mInitialDataLoaded;
    }

    public void setInitialDataLoaded(boolean initialDataLoaded) {
        this.mInitialDataLoaded = initialDataLoaded;
    }

    private OnLoadMoreListener onLoadMoreListener;

    public interface OnLoadMoreListener {
        void onLoadMore();
    }

    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
    }

    // 일단 무조건 Target uid는 Intent를 통해서 넘겨받았기 때문에 처음부터 존재한다.
    // 존재를 보장할 수 없는 것은 id이다.
    // id는 처음부터 있다고 판단하고 null 일 경우 업데이트를 해주자.
    @Deprecated
    private void downloadProfileImage(ImageView view) {
        String targetUid = mTargetUser.getId();
        StorageReference profileImageRef = StorageManager.getArticlePostsRef()
                .child(targetUid)
                .child(Constants.PROFILE_IMAGE_FILENAME);
        GlideManager.loadImage(profileImageRef, view, GlideManager.SCALE_TYPE_FIT_CENTER);
    }

    // View holder
    public static class ChatHolder extends RecyclerView.ViewHolder {
        public static final String TAG = "ChatHolder";

        Context mContext;

        @BindView(R.id.message_container)
        RelativeLayout mMessageContainer;

        @BindView(R.id.date_caption)
        FrameLayout mVDateCaption;

        @BindView(R.id.date_text)
        TextView mTvDateText;

        @BindView(R.id.message_content)
        RelativeLayout mVMessageContent;

        @BindView(R.id.profile_image)
        ImageView ivProfileImage;

        @BindView(R.id.nickname_text)
        TextView mTvUserId;

        @BindView(R.id.message)
        LinearLayout mMessage;

        @BindView(R.id.message_text)
        TextView mTvMessage;

        @BindView(R.id.metadata_start)
        View mTvMetadataStart;

        @BindView(R.id.metadata_end)
        View mTvMetadataEnd;

        @BindView(R.id.time_text_start)
        TextView mTvTimeStart;

        @BindView(R.id.time_text_end)
        TextView mTvTimeEnd;

        @BindView(R.id.readed_count_text_start)
        TextView mTvReadedCountStart;

        @BindView(R.id.readed_count_text_end)
        TextView mTvReadedCountEnd;

        public ChatHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            mContext = itemView.getContext();
        }

        public void setDateCaption(String date, boolean isShowing) {
            if (isShowing) {
                mTvDateText.setText(date);
                mVDateCaption.setVisibility(View.VISIBLE);
            } else mVDateCaption.setVisibility(View.GONE);
        }

        // TODO: 마지막 읽은 메시지를 계산해서 뷰를 업데이트 해줘야 한다.
        public void setReaded(boolean isReaded) {
            Log.i(TAG, "setReaded:isReaded:" + isReaded);
            if (isReaded) {
                mTvReadedCountStart.setVisibility(View.GONE);
                // mTvReadedCountEnd.setVisibility(View.GONE);
            } else {
                mTvReadedCountStart.setVisibility(View.VISIBLE);
                // mTvReadedCountEnd.setVisibility(View.VISIBLE);
            }
        }

        @SuppressWarnings("ConstantConditions")
        public void setIsSender(Boolean isSender) {
            // int color;

            if (isSender) {
                ivProfileImage.setVisibility(View.GONE);
                mTvUserId.setVisibility(View.GONE);
                // color = ContextCompat.getColor(mContext, R.color.material_green_300);
                mVMessageContent.setGravity(Gravity.END);

                mTvMetadataStart.setVisibility(View.VISIBLE);
                mTvMetadataEnd.setVisibility(View.GONE);
            } else {
                ivProfileImage.setVisibility(View.VISIBLE);
                mTvUserId.setVisibility(View.VISIBLE);

                // color = ContextCompat.getColor(mContext, R.color.material_gray_300);
                mVMessageContent.setGravity(Gravity.START);

                mTvMetadataStart.setVisibility(View.GONE);
                mTvMetadataEnd.setVisibility(View.VISIBLE);
            }

            // Chat unit 단위의 백그라운드 색상 변경
            // ((GradientDrawable) mMessage.getBackground()).setColor(color);
        }

        public void setNickname(String nickname) {
            mTvUserId.setText(nickname);
        }

        @Deprecated
        public void setProfileImage(StorageReference profileImageRef) {
            GlideManager.loadImage(profileImageRef, ivProfileImage, GlideManager.SCALE_TYPE_FIT_CENTER);
        }

        public void setMessage(String text) {
            mTvMessage.setText(text);
        }

        public void setTime(String text) {
            mTvTimeStart.setText(text);
            mTvTimeEnd.setText(text);
        }
    }

    public static class ProgressHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.progress_bar)
        View progressBar;

        public ProgressHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
