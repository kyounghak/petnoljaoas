package com.chaigene.petnolja.model;

import java.util.Map;

public class UserRoom extends FIRUserRoom {

    public static final String FIELD_UNREAD_COUNT = "unreadCount";

    public UserRoom(FIRUserRoom firUserRoom) {
        this.key = firUserRoom.getKey();
        this.type = firUserRoom.getType();
        this.directUid = firUserRoom.getDirectUid();
        this.directNickname = firUserRoom.getDirectNickname();
        this.lastMessage = firUserRoom.getLastMessage();
        this.unreadCount = firUserRoom.getUnreadCount();
        this.updatedTimestamp = firUserRoom.getUpdatedTimestamp();
    }

    // For debug
    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> result = super.toMap();
        result.put("key", key);
        return result;
    }
}