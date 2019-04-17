package com.chaigene.petnolja.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.chaigene.petnolja.manager.TasksManager;
import com.chaigene.petnolja.model.User;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.adapter.ChatMessageListAdapter;
import com.chaigene.petnolja.event.StartActivityEvent;
import com.chaigene.petnolja.manager.AuthManager;
import com.chaigene.petnolja.manager.DatabaseManager;
import com.chaigene.petnolja.model.FIRMessage;
import com.chaigene.petnolja.ui.fragment.ChildFragment;
import com.chaigene.petnolja.ui.fragment.ProfileFragment;
import com.chaigene.petnolja.ui.fragment.RootFragment;
import com.chaigene.petnolja.util.ChatUtil;
import com.chaigene.petnolja.util.CommonUtil;
import com.chaigene.petnolja.util.UserUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.chaigene.petnolja.Constants.EXTRA_TARGET_ROOM_ID;
import static com.chaigene.petnolja.Constants.EXTRA_TARGET_USER;
import static com.chaigene.petnolja.Constants.EXTRA_TARGET_USER_ID;
import static com.chaigene.petnolja.model.UserRoom.FIELD_UNREAD_COUNT;

public class ChatActivity extends BaseActivity {
    public static String TAG = "ChatActivity";

    @BindView(R.id.message_input)
    EditText mEtMessageEdit;

    @BindView(R.id.send_button)
    Button mBtnSend;

    @BindView(R.id.messages_list)
    RecyclerView mRvMessageList;

    private ChildFragment mTargetFragment;

    // 둘 중에 하나가 null이라면 asyncTask 메서드에서 각각 채워준다.
    private String mTargetUid;
    private User mTargetUser;
    private String mTargetRoomId;

    private String mLastReadedMessageId;

    private Query mRealtimeQuery;
    private ChildEventListener mRealtimeChildEventListener;
    private ValueEventListener mRealtimeValueEventListener;

    private LinearLayoutManager mManager;
    private ChatMessageListAdapter mAdapter;

    private List<HashMap<String, FIRMessage>> mMessages;

    private static final int CHAT_LIMIT_AMOUNT = 20;

    private boolean mFinishing;

    public synchronized boolean isFinishing() {
        return mFinishing;
    }

    public synchronized void setFinishing(boolean finishing) {
        this.mFinishing = finishing;
    }

    private boolean mInitializedChatRoom;

    public synchronized boolean isInitializeChatRoom() {
        return mInitializedChatRoom;
    }

    public synchronized void setInitializedChatRoom(boolean initializedChatRoom) {
        this.mInitializedChatRoom = initializedChatRoom;
    }

    private boolean mInitialDataReached;

    public synchronized boolean isInitialDataReached() {
        return mInitialDataReached;
    }

    public synchronized void setInitialDataReached(boolean initialDataReached) {
        this.mInitialDataReached = initialDataReached;
    }

    public void checkInitialDataReached(DataSnapshot dataSnapshot, int requestedLimit) {
        int count = (int) dataSnapshot.getChildrenCount();
        Log.d(TAG, "checkInitialDataReached:count:" + count + "|requestedLimit:" + requestedLimit + "|" + (count < requestedLimit));
        if (count < requestedLimit) setInitialDataReached(true);
        else setInitialDataReached(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ButterKnife.bind(this);

        if (isFinishing()) return;

        showLoadingDialog();
        asyncTask().continueWith((Continuation<Void, Void>) task -> {
            if (!task.isSuccessful()) {
                dismissDialog();
                Log.w(TAG, "asyncTask:ERROR:", task.getException());
                finish();
                return null;
            }
            String nickname = !mTargetUser.isDeleted() ? mTargetUser.getNickname() : "(알 수 없음)";
            setToolbarTitle(nickname);
            setupRecyclerView();
            setupMessageInput();
            setupRealtimeLatestMessages();
            setupLastReadedMessagesListener();

            setInitializedChatRoom(true);

            dismissDialog();
            return null;
        });
    }

    @Override
    protected void readIntent() {
        super.readIntent();
        // TODO: targetUser가 null을 받아왔다면 액티비티를 종료해야 한다.
        mTargetUid = getIntent().getStringExtra(EXTRA_TARGET_USER_ID);
        mTargetUser = (User) getIntent().getSerializableExtra(EXTRA_TARGET_USER);
        mTargetRoomId = getIntent().getStringExtra(EXTRA_TARGET_ROOM_ID);
        Log.i(TAG, "readIntent:targetUser:" + (mTargetUser != null ? mTargetUser.toMap() : "null") + "|targetRoomId:" + mTargetRoomId);
        if ((mTargetUid != null && mTargetUid.equals(AuthManager.getUserId()) || (mTargetUser != null && mTargetUser.getId().equals(AuthManager.getUserId())))) {
            Toast.makeText(getApplicationContext(), "자기 자신에게는 메시지를 전송할 수 없습니다.", Toast.LENGTH_LONG).show();
            setFinishing(true);
            finish();
        }
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setToolbarTitle(mTargetUser != null ? mTargetUser.getNickname() : getString(R.string.anonymous));
    }

    private Task<Void> asyncTask() {
        Log.i(TAG, "asyncTask");
        return TasksManager.call(() -> {
            // 절대 발생해서는 안되는 상황.
            if (mTargetUid == null && mTargetUser == null) finish();

            if (mTargetUser == null) {
                Task<User> getUserTask = UserUtil.getUser(mTargetUid);
                User user = Tasks.await(getUserTask);
                if (!getUserTask.isSuccessful()) {
                    Log.w(TAG, "getUserTask:ERROR:", getUserTask.getException());
                    throw getUserTask.getException();
                }
                mTargetUser = user;
            }

            if (mTargetUid == null) {
                mTargetUid = mTargetUser.getId();
            }

            // null 체크를 해서 null 일 경우 방을 새로 생성한다.
            if (mTargetRoomId == null) {
                Task<String> createRoomTask = ChatUtil.createRoom(mTargetUser.getId(), mTargetUser.getNickname());
                mTargetRoomId = Tasks.await(createRoomTask);
                Log.d(TAG, "createRoom:roomId:" + mTargetRoomId);
                if (!createRoomTask.isSuccessful()) {
                    Log.w(TAG, "createRoom:ERROR:", createRoomTask.getException());
                    throw createRoomTask.getException();
                }
            }
            return null;
        });
    }

    // 메시지를 전송한다.
    private void sendMessage() {
        Log.i(TAG, "sendMessage");
        String message = mEtMessageEdit.getText().toString();
        mEtMessageEdit.setText("");
        ChatUtil.sendMessage(mTargetRoomId, mTargetUser.getId(), message).continueWith((Continuation<Void, Void>) task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "sendMessage:ERROR", task.getException());
                return null;
            }
            Log.d(TAG, "sendMessage:SUCCESS");
            return null;
        });
    }

    // TODO: 사실은 받은 메시지가 내 메시지일 경우에는 서버에 기록하지 않아도 된다.
    private void setLastReadedMessage(String messageId) {
        String uid = AuthManager.getUserId();
        DatabaseReference lastReadedMessageRef = DatabaseManager.getChatLastReadedMessagesRef()
                .child(mTargetRoomId)
                .child(uid);
        DatabaseReference userRoomRef = DatabaseManager.getChatUserRoomsRef()
                .child(uid)
                .child(mTargetRoomId)
                .child(FIELD_UNREAD_COUNT);
        Map<String, Object> childUpdates = new HashMap<>();
        String lastReadedMessagePath = DatabaseManager.getPath(lastReadedMessageRef);
        String userRoomPath = DatabaseManager.getPath(userRoomRef);
        childUpdates.put(lastReadedMessagePath, messageId);
        childUpdates.put(userRoomPath, 0);
        DatabaseManager.updateChildren(childUpdates).continueWith((Continuation<Void, Void>) task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "setLastReadedMessage:ERROR", task.getException());
            }
            return null;
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // 비동기 테스크가 끝났을 때만 호출이 가능하다.
        if (isInitializeChatRoom()) {
            setupRealtimeLatestMessages();
            setupLastReadedMessagesListener();
        }
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        // 비동기 테스크가 끝났을 때만 호출이 가능하다.
        if (isInitializeChatRoom()) {
            releaseRealtimeLatestMessages();
            releaseLastReadedMessagesListener();
        }
        EventBus.getDefault().unregister(this);
    }

    private void setupRecyclerView() {
        mMessages = new ArrayList<>();
        mManager = new LinearLayoutManager(this);
        mRvMessageList.setLayoutManager(mManager);
        mRvMessageList.setHasFixedSize(false);
        mRvMessageList.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom) scrollToBottom(mAdapter.getItemCount());
        });

        mAdapter = new ChatMessageListAdapter(mRvMessageList, mMessages);
        mAdapter.setTargetUser(mTargetUser);
        mAdapter.setOnLoadMoreListener(new ChatMessageListAdapter.OnLoadMoreListener() {
            final String TAG = "OnLoadMoreListener";

            @Override
            public void onLoadMore() {
                Log.i(TAG, "onLoadMore");

                if (isInitialDataReached()) {
                    mAdapter.hideLoading();
                    return;
                }

                // 로딩을 보여준다.
                mAdapter.showLoading();
                loadMoreMessages().continueWith((Continuation<Void, Void>) task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, task.getException());
                    }
                    mAdapter.hideLoading();
                    return null;
                });
            }
        });
        mAdapter.registerAdapterDataObserver(
                new RecyclerView.AdapterDataObserver() {
                    final String TAG = "AdapterDataObserver";

                    @Override
                    public void onItemRangeInserted(int positionStart, int itemCount) {
                        /*Log.i(TAG, "onItemRangeInserted:" +
                                "adapterItemCount:" + mAdapter.getItemCount() +
                                "/insertedItem:" + (positionStart + 1) +
                                "/itemCount:" + itemCount);*/
                        // positionStart가 0이면 로딩 뷰로 간주한다.
                        // if (positionStart == 0) return;

                        // 마지막에 삽입됬을 때만 스크롤을 이동한다.
                        // if (mAdapter.getItemCount() != (positionStart + 1)) return;
                        // scrollToBottom(mAdapter.getItemCount());
                    }
                }
        );
        mRvMessageList.setAdapter(mAdapter);
    }

    private void setupMessageInput() {
        Log.i(TAG, "setupMessageInput");
        mEtMessageEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Log.i(TAG, "afterTextChanged:s:'" + s.toString() + "'");
                if (s.toString().trim().length() == 0) mBtnSend.setEnabled(false);
                else mBtnSend.setEnabled(true);
            }
        });
        mBtnSend.setOnClickListener(v -> sendMessage());
        CommonUtil.hideKeyboard(this);
    }

    private void setupRealtimeLatestMessages() {
        Log.i(TAG, "setupRealtimeLatestMessages");

        if (mTargetRoomId == null) return;

        String uid = AuthManager.getUserId();
        mRealtimeQuery = DatabaseManager.getChatUserMessagesRef().child(uid).child(mTargetRoomId).limitToLast(CHAT_LIMIT_AMOUNT);
        mRealtimeChildEventListener = new ChildEventListener() {
            final String TAG = "ChildEventListener";

            // 초기 값도 onChildAdded 통해서만 retrieve 받는다.
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildKey) {
                // Log.i(TAG, "onChildAdded");

                FIRMessage firMessage = dataSnapshot.getValue(FIRMessage.class);
                Log.d(TAG, "onChildAdded:message:" + firMessage.toMap().toString());

                HashMap<String, FIRMessage> chat = new HashMap<>();
                chat.put(dataSnapshot.getKey(), dataSnapshot.getValue(FIRMessage.class));

                int index = 0;
                if (previousChildKey != null) {
                    int existIndex = getIndexForKey(mMessages, previousChildKey);
                    if (existIndex != -1) index = existIndex + 1;
                }

                Log.i(TAG, "onChildAdded:index:" + index);

                // TODO: set은 해당 인덱스에 이미 값이 존재할 때
                // add는 해당 인덱스에 값이 있으면 기존 인덱스를 밀어낸다.
                // 기본적으로 onChildAdded는 기존에 없는 값이 추가되었을 때 호출된다.

                // TODO: 다른 액티비티가 완전히 덮어버릴 때 어차피
                // 현재 리스너가 떨어져나가기 때문에
                // 아래처럼 해주는게 맞지 않을까?
                // mRvMessageList.setAdapter(null);
                // 아니면 아래를 호출하거나
                // mMessages.clear();
                // mAdapter.notifyDataSetChanged();
                // 하지만 액티비티가 전환되었다는 이유만으로 기존 채팅이 매번 초기화되면 유저 사용성에 굉장히 불편이 초래된다.
                // 따라서 기존값을 replace 하는 작업이 나을 것 같다.

                if (mMessages.size() <= index) {
                    mMessages.add(index, chat);
                    mAdapter.notifyItemInserted(index);
                } else {
                    mMessages.set(index, chat);
                    mAdapter.notifyItemChanged(index);
                }
            }

            // Child 요소가 추가되거나 삭제되면 무조건 호출된다.
            // 하지만 최초 값을 불러올 때는 기존의 값이 없기 때문에 호출되지 않는다.
            // 그리고 50개의 값만 불러왔다면 새로운 값이 추가되면 최초 인덱스의 값이 삭제되기 때문에
            // onChildRemoved도 같이 호출된다.
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildKey) {
                Log.i(TAG, "onChildChanged");
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Log.i(TAG, "onChildRemoved");
                // mMessageMap.remove(dataSnapshot.getKey());
            }

            // 순서를 임의로 바꾸지 않는 이상 호출되지 않는다.
            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildKey) {
                Log.i(TAG, "onChildMoved");
            }

            // 에러가 발생했을 경우에만 호출된다.
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "onCancelled", databaseError.toException());
            }
        };
        mRealtimeValueEventListener = new ValueEventListener() {
            final String TAG = "ValueEventListener";

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                checkInitialDataReached(dataSnapshot, CHAT_LIMIT_AMOUNT);

                int totalCount = (int) dataSnapshot.getChildrenCount();
                int index = 0;
                String lastMessageKey = null;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    if (index == totalCount - 1) {
                        lastMessageKey = snapshot.getKey();
                    }
                    index++;
                }
                if (lastMessageKey != null) setLastReadedMessage(lastMessageKey);

                scrollToBottom(mAdapter.getItemCount() - 1);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };
        mRealtimeQuery.addChildEventListener(mRealtimeChildEventListener);
        mRealtimeQuery.addValueEventListener(mRealtimeValueEventListener);
    }

    private void releaseRealtimeLatestMessages() {
        mRealtimeQuery.removeEventListener(mRealtimeChildEventListener);
        mRealtimeQuery.removeEventListener(mRealtimeValueEventListener);
    }

    // TODO: mFriendUid가 존재한다는 것을 보장할 수가 없다.
    // onStop에서 리스너를 떼어내야함.
    private void setupLastReadedMessagesListener() {
        Log.i(TAG, "setupLastReadedMessagesListener");

        // mTargetRoomId가 null이 될 수 있는가?
        if (mTargetRoomId == null) return;

        DatabaseReference ref = DatabaseManager.getChatLastReadedMessagesRef().child(mTargetRoomId).child(mTargetUser.getId());
        ref.addValueEventListener(new ValueEventListener() {
            final String TAG = "ValueEventListener";

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Log.i(TAG, "onDataChange");

                mLastReadedMessageId = dataSnapshot.getValue(String.class);
                Log.d(TAG, "onDataChange:mLastReadedMessageId:" + mLastReadedMessageId);

                mAdapter.setLastReadedMessageId(mLastReadedMessageId);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.i(TAG, "onCancelled");
            }
        });
    }

    private void releaseLastReadedMessagesListener() {
    }

    private Task<Void> loadMoreMessages() {
        final TaskCompletionSource tcs = new TaskCompletionSource();

        // TODO: mMessages에서 가장 마지막 값(실제로는 0번의 인덱스)의 키값을 가져와야한다.
        Map.Entry<String, FIRMessage> startEntry = mMessages.get(0).entrySet().iterator().next();
        final String key = startEntry.getKey();
        double timestamp = (double) startEntry.getValue().getTimestamp(true);
        Log.d(TAG, "loadMoreMessages:key:" + key + "/timestamp:" + timestamp);

        // startAt이나 endAt의 첫번째 파라미터는 orderBy에 의존된다.
        // startAt이나 endAt의 두번째 파라미터는 원하는 child의 key 값이다.
        String uid = AuthManager.getUserId();
        final Query query = DatabaseManager.getChatUserMessagesRef().child(uid).child(mTargetRoomId)
                .orderByChild("timestamp").endAt(timestamp, key).limitToLast(CHAT_LIMIT_AMOUNT + 1);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            final String TAG = "ValueEventListener";

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                checkInitialDataReached(dataSnapshot, CHAT_LIMIT_AMOUNT + 1);

                int totalCount = (int) dataSnapshot.getChildrenCount();
                // 로딩 중일 때는 강제로 index를 1부터 시작한다.
                int index = mAdapter.isLoading() ? 1 : 0;
                // int index = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    if (snapshot.getKey().equals(key)) {
                        // Log.d(TAG, "onDataChange:KEY_EXISTS:index:" + index);
                        totalCount--;
                        continue;
                    }

                    HashMap<String, FIRMessage> chat = new HashMap<>();
                    chat.put(snapshot.getKey(), snapshot.getValue(FIRMessage.class));

                    int existIndex = getIndexForKey(mMessages, snapshot.getKey());
                    if (existIndex != -1) {
                        // 이미 존재할 경우 아무 것도 하지 않는다.
                    } else {
                        // 현재 객체를 검색할 수 없기 때문에 해당 인덱스에 값을 밀어넣는다.
                        // 기존의 객체를 뒤로 밀려나게 된다.
                        mMessages.add(index, chat);
                    }
                    index++;
                }
                // TODO: notifyItemRangeInserted를 사용하게 되면 date caption을 갱신할 수가 없게 되는 이슈가 발생한다.
                // 하지만 그렇다고 notifyDataSetChanged 사용해버리면 스크롤 값을 잃어버린다.
                // 기존에 존재하던 값들 최상단에 값들이 추가되었기 때문에 notifyDataSetChanged를 호출하면 값을 잃어버릴 수 밖에 없다.

                // 0번 인덱스는 항상 로딩으로 인식한다.
                mAdapter.notifyItemRangeInserted(1, totalCount);
                mAdapter.notifyItemChanged(totalCount + 1);

                final int scrollPosition = index + 1;
                // int progressHeight = CommonUtil.dpToPx(getApplicationContext(), 20);
                // int dateCaptionHeight = CommonUtil.dpToPx(getApplicationContext(), 36);
                // ((LinearLayoutManager) mRvMessageList.getLayoutManager()).scrollToPositionWithOffset(scrollPosition, dateCaptionHeight);

                Tasks.call((Callable<Void>) () -> {
                    // int dateCaptionHeight = CommonUtil.dpToPx(getApplicationContext(), 36);
                    // ((LinearLayoutManager) mRvMessageList.getLayoutManager()).scrollToPositionWithOffset(scrollPosition, dateCaptionHeight);
                    // mRvMessageList.findViewHolderForAdapterPosition(finalIndex).itemView.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                    return null;
                });

                // 가장 최상단에는 date caption이 항상 존재한다.
                mRvMessageList.scrollToPosition(scrollPosition);
                tcs.setResult(null);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                tcs.setException(databaseError.toException());
            }
        });
        return tcs.getTask();
    }

    private int getIndexForKey(List<HashMap<String, FIRMessage>> messages, String key) {
        int index = 0;
        for (HashMap<String, FIRMessage> chat : messages) {
            // chat이 null 일 경우 로딩 중으로 간주한다.
            if (chat != null && chat.containsKey(key)) {
                return index;
            } else {
                index++;
            }
        }
        return -1;
        // throw new IllegalArgumentException("Key not found");
    }

    // LinearLayoutManager에서 스크롤이 가능하게 해주는 메서드
    private void scrollToBottom(final int position) {
        Log.i(TAG, "scrollToBottom:position:" + position);
        if (position < 0) return;
        if (mRvMessageList.getScrollState() == RecyclerView.SCROLL_STATE_DRAGGING) return;
        mManager.smoothScrollToPosition(mRvMessageList, null, position);
    }

    /*private Task<Void> updateTargetUserInfo() {
        DatabaseReference usersRef = DatabaseManager.getUserUsersRef().child(mTargetUser.getKey());
        return DatabaseManager.getValue(usersRef, FIRUser.class)
                .continueWith(new Continuation<FIRUser, Void>() {
                    @Override
                    public Void then(@NonNull Task<FIRUser> task) throws Exception {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, task.getException());
                        }

                        FIRUser firUser = task.getResult();
                        Log.d(TAG, "updateTargetUserInfo:firUser:" + firUser.toMap().toString());

                        mTargetUser.setNickname(firUser.getNickname());
                        return null;
                    }
                });
    }*/

    // mTargetFragment를 BaseFragment에 넣는 것도 좋을 것 같다.
    void startProfileWindow() {
        Log.i(TAG, "startProfileWindow");
        mTargetFragment = ProfileFragment.newInstance(mTargetUid);
        startChildActivity();
    }

    protected void startChildActivity() {
        Intent intent = createIntent(ChildActivity.class);
        startActivity(intent);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStartActivityEvent(StartActivityEvent event) {
        Log.i(TAG, "onStartActivityEvent");
        RootFragment rootFragment = event.rootFragment;
        rootFragment.add(mTargetFragment);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_activity_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_show_profile: {
                startProfileWindow();
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static Intent createIntent(Context context, String targetUid) {
        Context c = context.getApplicationContext();
        Intent intent = new Intent().setClass(c, ChatActivity.class);
        intent.putExtra(EXTRA_TARGET_USER_ID, targetUid);
        return intent;
    }

    // 액티비티를 먼저 띄우고 유저의 정보를 받아오는 것이 이상적일 것 같다. 다만, 이미 유저의 정보가 있다면 다시 받아올 필요가 없다.
    public static Intent createIntent(Context context, String targetUid, String targetRoomId) {
        Context c = context.getApplicationContext();
        Intent intent = new Intent().setClass(c, ChatActivity.class);
        intent.putExtra(EXTRA_TARGET_USER_ID, targetUid);
        intent.putExtra(EXTRA_TARGET_ROOM_ID, targetRoomId);
        return intent;
    }

    public static Intent createIntent(Context context, User targetUser, String targetRoomId) {
        Context c = context.getApplicationContext();
        Intent intent = new Intent().setClass(c, ChatActivity.class);
        intent.putExtra(EXTRA_TARGET_USER, targetUser);
        intent.putExtra(EXTRA_TARGET_ROOM_ID, targetRoomId);
        return intent;
    }

    public static Intent createIntent(Context context, User targetUser) {
        Context c = context.getApplicationContext();
        Intent intent = new Intent().setClass(c, ChatActivity.class);
        intent.putExtra(EXTRA_TARGET_USER, targetUser);
        return intent;
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
