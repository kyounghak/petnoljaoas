package com.chaigene.petnolja.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.adapters.RecyclerSwipeAdapter;
import com.daimajia.swipe.util.Attributes;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.model.UserRoom;
import com.chaigene.petnolja.util.CommonUtil;
import com.chaigene.petnolja.util.UserUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ChatRoomListAdapter extends RecyclerSwipeAdapter<ChatRoomListAdapter.RoomViewHolder> {
    public static final String TAG = "ChatRoomListAdapter";

    private final int VIEW_ITEM = 0;
    private final int VIEW_LOADING = 1;
    private static final int INFINITE_SCROLL_VISIBLE_THRESHOLD = 1;

    private Context mContext;
    private Context mAppContext;

    private List<UserRoom> mRooms = new ArrayList<>();
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mManager;
    private static OnItemClickListener mOnItemClickListener;

    public ChatRoomListAdapter(Context context, List<UserRoom> rooms, RecyclerView recyclerView) {
        Log.i(TAG, "ArticleListAdapter");

        this.mContext = context;
        this.mAppContext = context.getApplicationContext();
        this.mRooms = rooms;
        this.mRecyclerView = recyclerView;
        this.mManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        setMode(Attributes.Mode.Single);

        // setupInfiniteScroll();
    }

    @Override
    public int getItemCount() {
        return mRooms.size();
    }

    @Override
    public RoomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Log.i(TAG, "onCreateViewHolder");
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_room, parent, false);
        return new RoomViewHolder(itemView);
    }

    @Override
    public void onViewRecycled(RoomViewHolder holder) {
        super.onViewRecycled(holder);
        // if (!(holder instanceof RoomViewHolder)) return;
        // RoomViewHolder roomViewHolder = holder;
        Glide.with(mContext).clear(holder.ivProfileImage);
    }

    @Override
    public void onBindViewHolder(RoomViewHolder holder, int position) {
        Log.i(TAG, "onBindViewViewHolder:position:" + position);

        UserRoom room = mRooms.get(position);

        UserUtil.downloadProfileImage(room.getDirectUid(), holder.ivProfileImage).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(@NonNull Task<Void> task) throws Exception {
                return null;
            }
        });

        // String nickname = !room.isUserDeleted() ? room.getDirectNickname() : "(알 수 없음)";
        String nickname = room.getDirectNickname();
        holder.tvNickname.setText(nickname);

        holder.tvLastMessage.setText(room.getLastMessage());

        /*String dateFormat = mContext.getString(R.string.format_chat_room_time);
        String time = CommonUtil.getFormattedTimeString(room.getUpdatedTimestamp(true), dateFormat);*/

        int unreadCount = room.getUnreadCount();
        if (unreadCount > 0) {
            holder.tvUnreadCount.setVisibility(View.VISIBLE);
        } else {
            holder.tvUnreadCount.setVisibility(View.GONE);
        }
        holder.tvUnreadCount.setText(String.valueOf(unreadCount));

        String time = CommonUtil.getTimeAgo(mContext, room.getUpdatedTimestamp(true));
        holder.tvTime.setText(time);
    }

    /*private void downloadProfileImage(@NonNull ImageView view, @NonNull String uid) {
        StorageReference postsRef = StorageManager.getUsersRef().child(uid).child(Constants.PROFILE_IMAGE_FILENAME);
        GlideManager.loadImage(postsRef, view);
    }*/

    @Override
    public int getSwipeLayoutResourceId(int position) {
        return R.id.swipe_container;
    }

    // ViewHolder
    public static class RoomViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.swipe_container)
        SwipeLayout vgSwipeContainer;

        @BindView(R.id.delete_room_button)
        LinearLayout btnDeleteRoom;

        @BindView(R.id.room_container)
        RelativeLayout vgRoomContainer;

        @BindView(R.id.profile_image)
        ImageView ivProfileImage;

        @BindView(R.id.nickname_text)
        TextView tvNickname;

        @BindView(R.id.last_message_text)
        TextView tvLastMessage;

        @BindView(R.id.unread_count_text)
        TextView tvUnreadCount;

        @BindView(R.id.time_text)
        TextView tvTime;

        public RoomViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            vgSwipeContainer.setShowMode(SwipeLayout.ShowMode.PullOut);
            vgSwipeContainer.setClickToClose(true);
            /*vgSwipeContainer.addSwipeListener(new SimpleSwipeListener() {
                @Override
                public void onOpen(SwipeLayout layout) {
                }
            });*/
            vgRoomContainer.setOnClickListener(this);
            btnDeleteRoom.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mOnItemClickListener == null) return;
            switch (v.getId()) {
                case R.id.room_container:
                    mOnItemClickListener.onItemClick(getAdapterPosition());
                    break;
                case R.id.delete_room_button:
                    mOnItemClickListener.onItemDeleteClick(getAdapterPosition());
                    break;
            }
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position);

        void onItemDeleteClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        mOnItemClickListener = l;
    }
}