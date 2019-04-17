package com.chaigene.petnolja.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.ServerValue;

@IgnoreExtraProperties
public class FIRAbuse extends FIRObject {
    @Exclude
    public static final int TYPE_USER = 0;
    @Exclude
    public static final int TYPE_ARTICLE = 1;
    @Exclude
    public static final int TYPE_COMMENT = 2;
    @Exclude
    public static final int TYPE_CHAT_ROOM = 3;
    @Exclude
    public static final int TYPE_CHAT_MESSAGE = 4;

    int type;
    String reporterUid;
    String reporterNickname;
    String targetUid;
    String targetNickname;
    String postId;
    String commentId;
    String chatRoomId;
    String chatMessageId;
    String reasonMessage;
    Object timestamp;

    public FIRAbuse() {
        // Default constructor required for calls to DataSnapshot.getValue(Post.class)
        this.timestamp = ServerValue.TIMESTAMP;
    }

    public FIRAbuse(int type,
                    String reporterUid,
                    String reporterNickname,
                    String targetUid,
                    String targetNickname,
                    String postId,
                    String commentId,
                    String chatRoomId,
                    String chatMessageId,
                    String reasonMessage) {
        this.type = type;
        this.reporterUid = reporterUid;
        this.reporterNickname = reporterNickname;
        this.targetUid = targetUid;
        this.targetNickname = targetNickname;
        this.postId = postId;
        this.commentId = commentId;
        this.chatRoomId = chatRoomId;
        this.chatMessageId = chatMessageId;
        this.reasonMessage = reasonMessage;
        this.timestamp = ServerValue.TIMESTAMP;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getReporterUid() {
        return reporterUid;
    }

    public void setReporterUid(String reporterUid) {
        this.reporterUid = reporterUid;
    }

    public String getReporterNickname() {
        return reporterNickname;
    }

    public void setReporterNickname(String reporterNickname) {
        this.reporterNickname = reporterNickname;
    }

    public String getTargetUid() {
        return targetUid;
    }

    public void setTargetUid(String targetUid) {
        this.targetUid = targetUid;
    }

    public String getTargetNickname() {
        return targetNickname;
    }

    public void setTargetNickname(String targetNickname) {
        this.targetNickname = targetNickname;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getChatRoomId() {
        return chatRoomId;
    }

    public void setChatRoomId(String chatRoomId) {
        this.chatRoomId = chatRoomId;
    }

    public String getChatMessageId() {
        return chatMessageId;
    }

    public void setChatMessageId(String chatMessageId) {
        this.chatMessageId = chatMessageId;
    }

    public String getReasonMessage() {
        return reasonMessage;
    }

    public void setReasonMessage(String reasonMessage) {
        this.reasonMessage = reasonMessage;
    }

    public Object getTimestamp() {
        return timestamp;
    }

    @Exclude
    public Long getTimestamp(boolean isLong) {
        if (timestamp instanceof Long) return (Long) timestamp;
        else return null;
    }

    public void setTimestamp(Object timestamp) {
        this.timestamp = timestamp;
    }
}