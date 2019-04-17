package com.chaigene.petnolja.util;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.chaigene.petnolja.manager.AuthManager;
import com.chaigene.petnolja.manager.DatabaseManager;
import com.chaigene.petnolja.model.FIRMessage;
import com.chaigene.petnolja.model.FIRRoom;
import com.chaigene.petnolja.model.FIRUserRoom;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static com.google.android.gms.tasks.Tasks.await;
import static com.chaigene.petnolja.model.FIRUserRoom.TYPE_DIRECT;

public class ChatUtil {
    private static final String TAG = "ChatUtil";

    private static volatile ChatUtil instance;

    public static ChatUtil getInstance() {
        if (instance == null) instance = new ChatUtil();
        return instance;
    }

    public static void releaseInstance() {
        if (instance != null) {
            instance.release();
            instance = null;
        }
    }

    private void release() {
        this.mTotalUnreadCount = 0;
    }

    private ChatUtil() {
        this.mTotalUnreadCount = 0;
    }

    /**
     * 새로운 채팅방을 생성한다.
     * 상대방 유저의 ID를 기준으로 검색한 뒤 방이 존재하면 해당 방 ID를 반환하고 없으면 새로 생성한다.
     *
     * @param targetUid
     * @param targetNickname
     * @return
     */
    public static Task<String> createRoom(final String targetUid, final String targetNickname) {
        Log.i(TAG, "createRoom:targetUid:" + targetUid);
        return Tasks.call(Executors.newSingleThreadExecutor(), new Callable<String>() {
            @Override
            public String call() throws Exception {
                String myUid = AuthManager.getUserId();

                // TODO: /chat/rooms/ 폴더에서 검색할 수 없는 이슈가 있다. 이유는 파이어베이스는 한가지 일치하는 변수만 검색이 가능하다.
                /*final Query myRoomsQuery = DatabaseManager.getChatUserRoomsRef().child(myUid).orderByChild("targetUid").equalTo(targetUid);
                final Query targetRoomsQuery = DatabaseManager.getChatUserRoomsRef().child(targetUid).orderByChild("targetUid").equalTo(myUid);

                Task<FIRRoom> getMyRoomTask = DatabaseManager.getChildValue(myRoomsQuery, FIRRoom.class);
                Task<FIRRoom> getTargetRoomTask = DatabaseManager.getChildValue(targetRoomsQuery, FIRRoom.class);

                try {
                    Tasks.await(getMyRoomTask);
                } catch (Exception ignored) {
                }

                // 이미 존재함.
                if (getMyRoomTask.isSuccessful()) {
                    FIRRoom myFirRoom = getMyRoomTask.getResult();
                    Log.d(TAG, "createRoom:getMyRoomTask:SUCCESS:" + myFirRoom.toMap());
                    String roomId = myFirRoom.getKey();
                    return roomId;
                }

                // 해당 유저와의 방이 존재하지 않음.
                Exception getMyRoomError = getMyRoomTask.getException();
                Log.w(TAG, "createRoom:getMyRoomTask:ERROR:" + getMyRoomError.getMessage());
                if (!(getMyRoomError instanceof DatabaseManager.NullDataException)) {
                    throw getMyRoomError;
                }

                try {
                    Tasks.await(getTargetRoomTask);
                } catch (Exception ignored) {
                }

                // 이미 존재함.
                if (getTargetRoomTask.isSuccessful()) {
                    FIRRoom targetFirRoom = getTargetRoomTask.getResult();
                    Log.d(TAG, "createRoom:getTargetRoomTask:SUCCESS:" + targetFirRoom.toMap());
                    String roomId = targetFirRoom.getKey();
                    return roomId;
                }

                // 해당 유저와의 방이 존재하지 않음.
                Exception getTargetRoomError = getTargetRoomTask.getException();
                Log.w(TAG, "createRoom:getTargetRoomTask:ERROR:" + getTargetRoomError.getMessage());
                if (!(getTargetRoomError instanceof DatabaseManager.NullDataException)) {
                    throw getTargetRoomError;
                }*/

                final Query myRoomsQuery = DatabaseManager.getChatUserRoomsRef().child(myUid).orderByChild("directUid").equalTo(targetUid);
                final Query targetRoomsQuery = DatabaseManager.getChatUserRoomsRef().child(targetUid).orderByChild("directUid").equalTo(myUid);

                Task<FIRUserRoom> getMyRoomTask = DatabaseManager.getChildValue(myRoomsQuery, FIRUserRoom.class);
                Task<FIRUserRoom> getTargetRoomTask = DatabaseManager.getChildValue(targetRoomsQuery, FIRUserRoom.class);

                try {
                    Tasks.await(getMyRoomTask);
                } catch (Exception ignored) {
                }

                // 내 채팅룸 리스트에 존재함.
                if (getMyRoomTask.isSuccessful()) {
                    FIRUserRoom myFirUserRoom = getMyRoomTask.getResult();
                    Log.d(TAG, "createRoom:getMyRoomTask:SUCCESS:" + myFirUserRoom.toMap());
                    String roomId = myFirUserRoom.getKey();
                    return roomId;
                }

                // 해당 유저와의 방이 존재하지 않음.
                Exception getMyRoomError = getMyRoomTask.getException();
                Log.w(TAG, "createRoom:getMyRoomTask:ERROR:" + getMyRoomError.getMessage());
                if (!(getMyRoomError instanceof DatabaseManager.NullDataException)) {
                    throw getMyRoomError;
                }

                try {
                    Tasks.await(getTargetRoomTask);
                } catch (Exception ignored) {
                }

                // 상대방 채팅방 리스트에 존재함.
                // TODO: 상대방 채팅방 리스트에 존재하더라도 내 채팅방 리스트에 존재하지 않기 때문에
                // 채팅방을 여는 순간 내 채팅방 리스트에 팬아웃으로 가져와서 똑같이 넣어줘야 이후에
                // 메시지를 보내더라도 크래쉬가 되지 않는다.
                if (getTargetRoomTask.isSuccessful()) {
                    FIRUserRoom targetFirUserRoom = getTargetRoomTask.getResult();
                    Log.d(TAG, "createRoom:getTargetRoomTask:SUCCESS:" + targetFirUserRoom.toMap());

                    FIRUserRoom firUserRoom = new FIRUserRoom(
                            targetFirUserRoom.getType(),
                            targetFirUserRoom.getHostUid(),
                            targetUid,
                            targetNickname,
                            null,
                            0,
                            targetFirUserRoom.getDeletedUsers()
                    );

                    String existsRoomId = targetFirUserRoom.getKey();
                    DatabaseReference newUserRoomRef = DatabaseManager.getChatUserRoomsRef().child(myUid).child(existsRoomId);
                    Task<Void> setTask = DatabaseManager.setValue(newUserRoomRef, firUserRoom);

                    Tasks.await(setTask);

                    return existsRoomId;
                }

                // 해당 유저와의 방이 존재하지 않음.
                Exception getTargetRoomError = getTargetRoomTask.getException();
                Log.w(TAG, "createRoom:getTargetRoomTask:ERROR:" + getTargetRoomError.getMessage());
                if (!(getTargetRoomError instanceof DatabaseManager.NullDataException)) {
                    throw getTargetRoomError;
                }

                // 대상 유저와 이미 존재하는 방이 없기 때문에 방을 생성한다.
                String newRoomId = DatabaseManager.getChatRoomsRef().push().getKey();

                Map<String, Object> childUpdates = new HashMap<>();

                Map<String, Boolean> members = new HashMap<>();
                members.put(myUid, true);
                members.put(targetUid, true);

                FIRRoom firRoom = new FIRRoom(TYPE_DIRECT, myUid, members);
                childUpdates.put("chat/rooms/" + newRoomId, firRoom);

                // TODO: 탈퇴된 유저에게는 이전 단계에서 애초에 메세지를 보낼 수 없게 막아야 한다.
                FIRUserRoom firUserRoom = new FIRUserRoom(
                        TYPE_DIRECT,
                        myUid,
                        targetUid,
                        targetNickname,
                        null,
                        0,
                        null
                );
                childUpdates.put(CommonUtil.format("chat/user-rooms/%s/%s", myUid, newRoomId), firUserRoom);

                // target uid 부분에는 넣을 필요가 없다. 내가 채팅방을 생성했고 메시지를 전송하기 전에는 내 채팅방 리스트에만 보여져야 하기 때문이다.
                // childUpdates.put(CommonUtil.format("chat/user-rooms/%s/%s", targetUid, roomId), room);

                Task<Void> updateTask = DatabaseManager.updateChildren(childUpdates);

                await(updateTask);
                return newRoomId;
            }
        });
    }

    /**
     * 채팅 메세지를 전송한다.
     *
     * @param targetRoomId
     * @param targetUid
     * @param message
     * @return
     */
    public static Task<Void> sendMessage(String targetRoomId, String targetUid, String message) {
        Log.i(TAG, "sendMessage");

        String uid = AuthManager.getUserId();
        FIRMessage firMessage = new FIRMessage(uid, message, FIRMessage.TYPE_MESSAGE);
        String key = DatabaseManager.getChatMessagesRef().child(targetRoomId).push().getKey();
        Map<String, Object> childUpdates = new HashMap<>();
        Map<String, Object> messageValues = firMessage.toMap();
        childUpdates.put(CommonUtil.format("chat/messages/%s/%s", targetRoomId, key), messageValues);
        childUpdates.put(CommonUtil.format("chat/user-messages/%s/%s/%s", uid, targetRoomId, key), messageValues);
        childUpdates.put(CommonUtil.format("chat/user-messages/%s/%s/%s", targetUid, targetRoomId, key), messageValues);

        return DatabaseManager.updateChildren(childUpdates);
    }

    // TODO: MainActivity가 onResume이 될 때마다 체크해야 함.
    private int mTotalUnreadCount;

    /**
     * 캐쉬에 읽지 않은 모든 채팅 알림 개수를 저장한다.
     *
     * @param unreadCount
     */
    public synchronized void saveTotalUnreadCount(int unreadCount) {
        this.mTotalUnreadCount = unreadCount;
    }

    /**
     * 캐쉬에서 읽지 않은 모든 채팅 알림 개수를 반환한다.
     *
     * @return
     */
    public synchronized int loadTotalUnreadCount() {
        return mTotalUnreadCount;
    }

    // 실시간으로 채팅 메시지 노티가 오면 아래 메서드를 실행해줘야 함.
    public synchronized void increaseTotalUnreadCount() {
        int count = loadTotalUnreadCount();
        count++;
        saveTotalUnreadCount(count);
    }

    // 사실상 사용할 일 없음.
    public synchronized void decreaseTotalUnreadCount() {
        int count = loadTotalUnreadCount();
        if (count > 0) count--;
        else count = 0;
        saveTotalUnreadCount(count);
    }

    /**
     * 캐쉬에 읽지 않은 모든 채팅 알림 개수를 초기화한다.
     */
    public synchronized void clearTotalUnreadCount() {
        saveTotalUnreadCount(0);
    }
}
