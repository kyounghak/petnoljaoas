package com.chaigene.petnolja.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.adapter.ChatRoomListAdapter;
import com.chaigene.petnolja.manager.AuthManager;
import com.chaigene.petnolja.manager.DatabaseManager;
import com.chaigene.petnolja.model.FIRUserRoom;
import com.chaigene.petnolja.model.UserRoom;
import com.chaigene.petnolja.util.CommonUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ChatRoomActivity extends BaseActivity {
    public static final String TAG = "ChatRoomActivity";

    @BindView(R.id.room_list)
    RecyclerView mRvRoomList;

    private List<UserRoom> mRooms;
    private ChatRoomListAdapter mAdapter;

    private Query mRoomListQuery;
    private ChildEventListener mRoomListChildEventListener;
    private ValueEventListener mRoomListValueEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);
        ButterKnife.bind(this);
        setupRecyclerView();
        setupRealtimeRoomList();
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setToolbarTitle(R.string.title_activity_chat_room);
    }

    private void setupRecyclerView() {
        Log.i(TAG, "setupRecyclerView");

        mRvRoomList.setLayoutManager(new LinearLayoutManager(this));

        // Source: http://stackoverflow.com/a/28828749/4729203
        mRvRoomList.setHasFixedSize(true);

        // Divider
        DividerItemDecoration divider = new DividerItemDecoration(getApplicationContext(), DividerItemDecoration.VERTICAL);
        divider.setDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.shape_divider_chat_room));
        mRvRoomList.addItemDecoration(divider);

        mRooms = new ArrayList<>();
        mAdapter = new ChatRoomListAdapter(this, mRooms, mRvRoomList);
        mAdapter.setOnItemClickListener(new ChatRoomListAdapter.OnItemClickListener() {
            final String TAG = "OnItemClickListener";

            @Override
            public void onItemClick(int position) {
                Log.i(TAG, "onItemClick");
                // roomId가 null이 발생하는 이슈는 절대 발생해서는 안된다.
                String targetUid = mRooms.get(position).getDirectUid();
                String roomId = mRooms.get(position).getKey();
                Intent intent = ChatActivity.createIntent(getApplicationContext(), targetUid, roomId);
                startActivity(intent);
            }

            @Override
            public void onItemDeleteClick(int position) {
                deleteRoom(position);
            }
        });
        mRvRoomList.setAdapter(mAdapter);
    }

    // TODO: 2가지 방법이 있다. 첫번째는 매뉴얼 새로고침을 넣는 것.
    // 두번째는 채팅방 목록을 실시간으로 업데이트 하는 것이다.
    // 일단 두번째 방법으로 작업한다.
    private void setupRealtimeRoomList() {
        Log.i(TAG, "setupRealtimeRoomList");
        // TODO: 일단은 limit을 두지 않고 모든 방을 불러온다.
        // 결론적으로는 limit을 적용하고 infinite scroll을 적용해야 한다.
        // 앞으로 구현해야할 이슈는 last update를 기준으로 정렬을 할 텐데
        // history 영역에 속해있던 값이 realtime 영역으로 재배열 되면 history에서 값을 검색해서 삭제시켜주는게 시급하다.

        // 그냥 100개 정도 리스트에서 벗어나는 오래된 채팅방은 삭제시켜버리자.
        mRoomListQuery = DatabaseManager.getChatUserRoomsRef().child(AuthManager.getUserId()).orderByChild("updatedTimestamp");
        mRoomListQuery.addChildEventListener(new ChildEventListener() {
            final String TAG = "ChildEventListener";

            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Log.i(TAG, "onChildAdded:key:" + dataSnapshot.getKey() + "|value:" + dataSnapshot.getValue().toString());

                // TODO: 추가가 되는대로 getValue를 통해 방 정보를 가져온다.
                // final String roomId = dataSnapshot.getKey();
                final FIRUserRoom firUserRoom = DatabaseManager.getValue(dataSnapshot, FIRUserRoom.class);
                final UserRoom room = new UserRoom(firUserRoom);

                // TODO: add 일 수도 있고 set 일 수도 있다.
                mRooms.add(0, room);
                mAdapter.notifyItemInserted(0);

                // TODO: 그냥 functions에서 lastMessage 값을 넣어준다.
                // Deprecated
                /*Query query = DatabaseManager.getChatMessagesRef().child(roomId).orderByChild("timestamp").limitToLast(1);
                DatabaseManager.getValue(query, FIRMessage.class).continueWith(new Continuation<FIRMessage, Void>() {
                    @Override
                    public Void then(@NonNull Task<FIRMessage> task) throws Exception {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, task.getException());
                        }
                        FIRMessage firMessage = task.getResult();
                        Log.d(TAG, "SUCCESS:message:" + firMessage.toMap().toString());

                        int index = mRooms.indexOf(room);
                        mRooms.get(index).setLastMessage(firMessage.getMessage());

                        mAdapter.notifyItemChanged(index);
                        return null;
                    }
                });*/
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Log.i(TAG, "onChildChanged:key:" + dataSnapshot.getKey() + "|value:" + dataSnapshot.getValue().toString());

                String roomId = dataSnapshot.getKey();
                int index = 0;
                boolean isExists = false;
                for (UserRoom room : mRooms) {
                    Log.d(TAG, "onChildChanged:loop:room:" + room.toMap());
                    if (room.getKey().equals(roomId)) {
                        isExists = true;
                        break;
                    }
                    index++;
                }

                if (!isExists) {
                    Log.w(TAG, "onChildChanged:item_for_the_key_is_not_exist");
                    return;
                }

                final FIRUserRoom newFirUserRoom = DatabaseManager.getValue(dataSnapshot, FIRUserRoom.class);
                final UserRoom newRoom = new UserRoom(newFirUserRoom);
                mRooms.set(index, newRoom);
                mAdapter.notifyDataSetChanged();
                // mAdapter.notifyItemChanged(index);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        mRoomListQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void releaseRealtimeRoomList() {
        // TODO: ...
    }

    /*private int getIndexForKey(String key) {
        int index = 0;
        for (UserRoom room : mRooms) {
            // chat이 null 일 경우 로딩 중으로 간주한다.
            if (chat != null && chat.containsKey(key)) {
                return index;
            } else {
                index++;
            }
        }
        return -1;
        // throw new IllegalArgumentException("Key not found");
    }*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // MenuInflater inflater = getMenuInflater();
        // inflater.inflate(R.menu.menu_activity_chat_list, menu);
        // return true;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_new_room:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // TODO: ChatUtil로 이동
    private void deleteRoom(final int position) {
        Log.i(TAG, "deleteRoom:position:" + position);
        String myUid = AuthManager.getUserId();

        // mRooms 해당 인덱스의 값이 지워지지 않는 이상 아래의 코드는 정상적으로 값이 반환된다.
        String roomId = mRooms.get(position).getKey();

        // TODO: friendUid의 값이 없으면 방을 지울 수가 없다. => X
        // 방을 지울 때는 기본적으로 내 것만 지운다.

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put(CommonUtil.format("chat/user-rooms/%s/%s", myUid, roomId), null);

        // childUpdates.put("chats/mRooms/" + targetRoomId, null);
        // TODO: last_readed_messages는 나와 상대방이 모두 방을 삭제했을 때만 최종적으로 삭제되어야 한다.
        // childUpdates.put("chats/last_readed_messages/" + targetRoomId + "/" + myUid + "/" + targetRoomId, null);

        // TODO: messages의 내용도 지우는 로직은 조금 복잡하다.
        // chats/users에서 친구의 uid가 존재하는지 확인한다.
        // 나의 uid는 지금 지울 것이기 때문에 신경쓰지 않아도 된다.

        // 만약 친구의 uid가 이미 지워진 상태라면
        // mRooms / messages / last_readed_messages 모두 삭제 처리한다.

        // 결과적으로 봤을 때는 chats/users에서 친구의 uid가 존재하는지를 먼저 확인하고
        // 존재할 경우 chats/users의 나의 uid만 삭제하고
        // 존재하지 않을 경우 updateChildren을 통해서 일괄 삭제한다.

        DatabaseManager.updateChildren(childUpdates).addOnCompleteListener(new OnCompleteListener<Void>() {
            final String TAG = "OnCompleteListener";

            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (!task.isSuccessful()) {
                    Log.e(TAG, "deleteRoom:ERROR", task.getException());
                }
                Log.d(TAG, "deleteRoom:SUCCESS");
                mRooms.remove(position);
                mAdapter.notifyItemRemoved(position);
            }
        });
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
