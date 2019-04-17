package com.chaigene.petnolja.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class FIRUserRoom extends FIRObject {
    @Exclude
    public static final int TYPE_DIRECT = 0;
    @Exclude
    public static final int TYPE_GROUP = 1;

    int type;
    String hostUid;
    String directUid;
    String directNickname;
    String lastMessage;
    int unreadCount;
    // TODO: deletedUsers는 rooms가 가지고 있어야 한다.
    // rooms에서 값이 변경되면 user-rooms로 팬아웃 시켜준다.
    Map<String, Boolean> deletedUsers;
    Object updatedTimestamp;

    public FIRUserRoom() {
        // Default constructor required for calls to DataSnapshot.getValue(FIRUserRoom.class)
    }

    public FIRUserRoom(int type,
                       String hostUid,
                       String directUid,
                       String directNickname,
                       String lastMessage,
                       int unreadCount,
                       Map<String, Boolean> deletedUsers) {
        this.type = type;
        this.hostUid = hostUid;
        this.directUid = directUid;
        this.directNickname = directNickname;
        this.lastMessage = lastMessage;
        this.unreadCount = unreadCount;
        this.deletedUsers = deletedUsers;
        this.updatedTimestamp = ServerValue.TIMESTAMP;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getHostUid() {
        return hostUid;
    }

    public void setHostUid(String hostUid) {
        this.hostUid = hostUid;
    }

    public String getDirectUid() {
        return directUid;
    }

    public void setDirectUid(String directUid) {
        this.directUid = directUid;
    }

    public String getDirectNickname() {
        return directNickname;
    }

    public void setDirectNickname(String directNickname) {
        this.directNickname = directNickname;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public Map<String, Boolean> getDeletedUsers() {
        return deletedUsers;
    }

    public void setDeletedUsers(Map<String, Boolean> deletedUsers) {
        this.deletedUsers = deletedUsers;
    }

    public Object getUpdatedTimestamp() {
        return updatedTimestamp;
    }

    @Exclude
    public Long getUpdatedTimestamp(Boolean isLong) {
        if (updatedTimestamp instanceof Long) return (Long) updatedTimestamp;
        else return null;
    }

    public void setUpdatedTimestamp(Object updatedTimestamp) {
        this.updatedTimestamp = updatedTimestamp;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("type", type);
        result.put("hostUid", hostUid);
        result.put("directUid", directUid);
        result.put("directNickname", directNickname);
        result.put("lastMessage", lastMessage);
        result.put("unreadCount", unreadCount);
        result.put("updatedTimestamp", updatedTimestamp);
        return result;
    }
}