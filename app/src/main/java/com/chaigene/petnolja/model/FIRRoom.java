package com.chaigene.petnolja.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class FIRRoom extends FIRObject {
    @Exclude
    public static final int TYPE_DIRECT = 0;
    @Exclude
    public static final int TYPE_GROUP = 1;

    int type;
    String hostUid;
    Map<String, Boolean> members;

    public FIRRoom() {
        // Default constructor required for calls to DataSnapshot.getValue(Post.class)
    }

    public FIRRoom(int type, String hostUid, Map<String, Boolean> members) {
        this.type = type;
        this.hostUid = hostUid;
        this.members = members;
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

    public Map<String, Boolean> getMembers() {
        return members;
    }

    public void setMembers(Map<String, Boolean> members) {
        this.members = members;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("type", type);
        result.put("hostUid", hostUid);
        result.put("members", members);
        return result;
    }
}