package com.chaigene.petnolja.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

import static com.chaigene.petnolja.Constants.STORAGE_REGION_TOKYO;

@IgnoreExtraProperties
public class FIRNotification extends FIRObject {

    @Exclude
    public static final int TYPE_LIKE = 0;
    @Exclude
    public static final int TYPE_COMMENT = 1;
    @Exclude
    public static final int TYPE_FOLLOW = 2;
    @Exclude
    public static final int TYPE_FOLLOW_REQUEST = 3;
    @Exclude
    public static final int TYPE_FOLLOW_ACCEPT = 4;
    @Exclude
    public static final int TYPE_SHOP = 5;
    @Exclude
    public static final int TYPE_MENTION_ARTICLE = 6;
    @Exclude
    public static final int TYPE_MENTION_COMMENT = 7;

    // 100 이상부터는 알림탭에서 생성되지 않는 push들.
    @Exclude
    public static final int TYPE_CHAT_MESSAGE = 100;

    // TODO: RecyclerView의 아이템으로 넣는 것이 아니라 상위 레이아웃 요소로 삽입하는 것이 좋을 것 같다.
    // 글쓰기가 완료되면 Broadcast로 쏘아주는 것이 좋을지 생각해본다.
    // 인스타의 경우에는 해당 탭 페이지가 reveal 될 때 한번 더 체크하는 것으로 보인다. (왜 그렇게 하지?)
    // db에 저장하면 편하겠지만 일단은 메모리에 저장하도록 하자. ArticleUtil에 저장한다.
    // setArticleInserting(boolean isInserting) 함수를 만들어서 true/false로 보이고 가린다.
    // 여러개의 글을 올릴 경우는 어떻게 처리해야 할까?

    @Exclude
    public static final int TYPE_ARTICLE_INSERT_COMPLETED = 101;

    private int type;
    private String targetUid;
    private String targetNickname;
    private String postId;
    private String content;
    private String photoName;

    @Deprecated
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
    private boolean checked;

    /*public FIRNotification(String uid, String message, int type) {
        this.type = type;
        this.timestamp = ServerValue.TIMESTAMP;
    }*/

    public FIRNotification() {
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPhotoName() {
        return photoName;
    }

    public void setPhotoName(String photoName) {
        this.photoName = photoName;
    }

    @Deprecated
    public Map<String, Boolean> getRegions() {
        return regions;
    }

    @Deprecated
    @Exclude
    public void setDefaultRegions() {
        Map<String, Boolean> regions = new HashMap<>();
        regions.put(STORAGE_REGION_TOKYO, true);
        this.regions = regions;
    }

    @Deprecated
    public void setRegions(Map<String, Boolean> regions) {
        this.regions = regions;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getChatRoomId() {
        return chatRoomId;
    }

    public void setChatRoomId(String chatRoomId) {
        this.chatRoomId = chatRoomId;
    }

    public String getChatMessage() {
        return chatMessage;
    }

    public void setChatMessage(String chatMessage) {
        this.chatMessage = chatMessage;
    }

    public int getShopType() {
        return shopType;
    }

    public void setShopType(int shopType) {
        this.shopType = shopType;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getOrderName() {
        return orderName;
    }

    public void setOrderName(String orderName) {
        this.orderName = orderName;
    }

    public int getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(int orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductTitle() {
        return productTitle;
    }

    public void setProductTitle(String productTitle) {
        this.productTitle = productTitle;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(int totalPrice) {
        this.totalPrice = totalPrice;
    }

    public boolean isAutoFinalized() {
        return isAutoFinalized;
    }

    public void setAutoFinalized(boolean autoFinalized) {
        isAutoFinalized = autoFinalized;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setTimestamp(Object timestamp) {
        this.timestamp = timestamp;
    }

    // ClassCastException: java.util.Collections$UnmodifiableMap cannot be cast to java.lang.Long
    public Object getTimestamp() {
        return timestamp;
    }

    @Exclude
    public Long getTimestamp(Boolean isLong) {
        if (timestamp instanceof Long) return (Long) timestamp;
        else return null;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("type", type);
        result.put("targetUid", targetUid);
        result.put("targetNickname", targetNickname);
        result.put("postId", postId);
        result.put("photoName", photoName);
        result.put("regions", regions);
        result.put("commentId", commentId);
        result.put("comment", comment);
        result.put("chatRoomId", chatRoomId);
        result.put("chatMessage", chatMessage);
        result.put("shopType", shopType);
        result.put("orderId", orderId);
        result.put("orderName", orderName);
        result.put("orderStatus", orderStatus);
        result.put("productId", productId);
        result.put("productTitle", productTitle);
        result.put("quantity", quantity);
        result.put("totalPrice", totalPrice);
        result.put("isAutoFinalized", isAutoFinalized);
        result.put("message", message);
        result.put("timestamp", timestamp);
        result.put("checked", checked);
        return result;
    }
}