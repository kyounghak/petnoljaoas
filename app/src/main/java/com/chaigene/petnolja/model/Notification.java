package com.chaigene.petnolja.model;

import java.util.Map;

public class Notification extends FIRNotification {

    /*private int type;
    private String targetUid;
    private String targetNickname;
    private String postId;
    private String photoName;
    private Map<String, Boolean> regions;
    private String commentId;
    private String comment;
    private String chatRoomId;
    private String chatMessage;

    // Shop
    private int shopType;
    private String orderId;
    private String orderName;
    private int orderStatus;
    private String productId;
    private String productTitle;
    private int quantity;
    private int totalPrice;
    private boolean isAutoFinalized;

    private String message;
    private Object timestamp;
    private boolean checked;*/

    public Notification(FIRNotification firNotification) {
        setKey(firNotification.getKey());
        setType(firNotification.getType());
        setTargetUid(firNotification.getTargetUid());
        setTargetNickname(firNotification.getTargetNickname());
        setPostId(firNotification.getPostId());
        setContent(firNotification.getContent());
        setPhotoName(firNotification.getPhotoName());
        setRegions(firNotification.getRegions());
        setCommentId(firNotification.getCommentId());
        setComment(firNotification.getComment());
        setChatRoomId(firNotification.getChatRoomId());
        setChatMessage(firNotification.getChatMessage());
        setShopType(firNotification.getShopType());
        setOrderId(firNotification.getOrderId());
        setOrderName(firNotification.getOrderName());
        setOrderStatus(firNotification.getOrderStatus());
        setProductId(firNotification.getProductId());
        setProductTitle(firNotification.getProductTitle());
        setQuantity(firNotification.getQuantity());
        setTotalPrice(firNotification.getTotalPrice());
        setAutoFinalized(firNotification.isAutoFinalized());
        setMessage(firNotification.getMessage());
        setTimestamp(firNotification.getTimestamp(true));
        setChecked(firNotification.isChecked());
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> result = super.toMap();
        result.put("key", key);
        return result;
    }
}
