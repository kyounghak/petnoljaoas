package com.chaigene.petnolja.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Firebase와 통신하기 위한 VO와 Presentation용 VO는 구분시킨다.
public class Post extends FIRPost {
    private static final String TAG = "Post";

    User user;
    boolean isLike;
    int commentCount = 0;

    public Post(FIRPost firPost) {
        this.key = firPost.getKey();
        this.type = firPost.getType();
        this.content = firPost.getContent();
        this.likeCount = firPost.getLikeCount();
        this.saveCount = firPost.getSaveCount();
        this.followers = firPost.getFollowers();
        this.likes = firPost.getLikes();
        this.saves = firPost.getSaves();
        this.photos = firPost.getPhotos();
        this.regions = firPost.getRegions();
        this.hashtags = firPost.getHashtags();
        this.mentions = firPost.getMentions();
        this.productTitle = firPost.getProductTitle();
        this.productPrice = firPost.getProductPrice();
        this.shippingPrice = firPost.getShippingPrice();
        this.productServices = firPost.getProductServices();
        this.latestComments = firPost.getLatestComments();
        this.timestamp = firPost.getTimestamp();
        this.user = null;
        this.isLike = false;
        this.commentCount = 0;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCoverPhoto() {
        return getPhotosList().iterator().next();
    }

    public List<String> getPhotosList() {
        List<String> photosArray = new ArrayList<>();
        int count = photos.size();
        for (int i = 0; i < count; i++) {
            String key = String.valueOf(i);
            photosArray.add(photos.get(key));
        }
        return photosArray;
    }

    public List<String> getRegionsList() {
        ArrayList<String> regionsList = new ArrayList<>(regions.keySet());
        return regionsList;
    }

    /*public void setPhotos(Map<String, String> photos) {
        ArrayList<String> photosArray = new ArrayList<>();
        int count = photos.size();
        for (int i = 0; i < count; i++) {
            String key = String.valueOf(i);
            photosArray.add(photos.getUserPosts(key));
        }
        this.photos = photosArray;
    }*/

    // Setter를 다음과 같이 변경할 수도 있다.
    /*public void setPhotos(ArrayList<String> photos) {
        Log.i(TAG, "setPhotos:photos:" + photos.toString());
        HashMap<String, String> photosMap = new HashMap<>();
        int index = 0;
        for (String photo : photos) {
            photosMap.put(String.valueOf(index), photo);
            index++;
        }
        this.photos = photosMap;
    }*/

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isLike() {
        return isLike;
    }

    public void setLike(boolean like) {
        isLike = like;
    }

    @Override
    public String getUid() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setUid(String uid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setNickname(String nickname) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNickname() {
        throw new UnsupportedOperationException();
    }

    // Only for debug
    public Map<String, Object> toMap() {
        Map<String, Object> result = super.toMap();
        result.remove("uid");
        result.remove("nickname");
        result.put("key", key);
        result.put("user", user != null ? user.toMap() : null);
        result.put("isLike", isLike);
        result.put("commentCount", commentCount);
        return result;
    }
}