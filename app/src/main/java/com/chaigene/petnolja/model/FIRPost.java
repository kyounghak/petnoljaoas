package com.chaigene.petnolja.model;

import androidx.annotation.Nullable;
import android.util.Log;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.ServerValue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.chaigene.petnolja.Constants.STORAGE_REGION_TOKYO;

// TODO: 글을 올리고 난 뒤 아이디를 변경하면 이전 아이디를 먼저 보여줘야 할 것인가?
// 만약 아이디를 변경했을 때 본인이 쓴글이 1000개 정도 된다면 변경할 때마다 일괄적으로 변경할 것인가?
// 일단은 구아이디를 보여주고 글이 로드되었을 때 변경되었는지 검사를 하고 업데이트를 해준다.

@IgnoreExtraProperties
public class FIRPost extends FIRObject implements Serializable {
    private static final String TAG = "FIRPost";

    int type;
    String uid;
    String nickname;
    String content;
    int likeCount = 0;
    int saveCount = 0;
    // 팔로워가 여기 왜 있을까.
    Map<String, Boolean> followers = new HashMap<>();
    Map<String, Boolean> likes = new HashMap<>();
    Map<String, Boolean> saves = new HashMap<>();
    Map<String, String> photos = new HashMap<>();
    Map<String, Boolean> regions = new HashMap<>();
    Map<String, Boolean> hashtags = new HashMap<>();
    Map<String, Boolean> mentions = new HashMap<>();

    int productType;
    String productTitle;
    String productPrice;
    String shippingPrice;
    String productServices;

    List<Comment> latestComments = new ArrayList<>();
    Object timestamp;

    public FIRPost() {
        // Default constructor required for calls to DataSnapshot.getValue(Post.class)
        this.timestamp = ServerValue.TIMESTAMP;
    }

    public FIRPost(int type, String uid, String nickname, String content, List<String> photos) {
        this.type = type;
        this.uid = uid;
        this.nickname = nickname;
        this.content = content;
        setPhotos(photos);
        setDefaultRegions();
        this.timestamp = ServerValue.TIMESTAMP;
    }

    // 반드시 photos 인자가 존재해야 객체를 생성할 수 있다.
    public FIRPost(int type, String uid, String nickname, String content, Map<String, String> photos) {
        this.type = type;
        this.uid = uid;
        this.nickname = nickname;
        this.content = content;
        this.photos = photos;
        setDefaultRegions();
        this.timestamp = ServerValue.TIMESTAMP;
    }

    public FIRPost(int type,
                   String uid,
                   String nickname,
                   String content,
                   List<String> photos,
                   int productType,
                   String productTitle,
                   String productPrice,
                   String shippingPrice,
                   String productServices) {
        this.type = type;
        this.uid = uid;
        this.nickname = nickname;
        this.content = content;
        setPhotos(photos);
        setDefaultRegions();
        this.productType = productType;
        this.productTitle = productTitle;
        this.productPrice = productPrice;
        this.shippingPrice = shippingPrice;
        this.productServices = productServices;
        this.timestamp = ServerValue.TIMESTAMP;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, Boolean> getFollowers() {
        return followers;
    }

    public void setFollowers(Map<String, Boolean> followers) {
        this.followers = followers;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public int getSaveCount() {
        return saveCount;
    }

    public void setSaveCount(int saveCount) {
        this.saveCount = saveCount;
    }

    public Map<String, Boolean> getLikes() {
        return likes;
    }

    public void setLikes(Map<String, Boolean> likes) {
        this.likes = likes;
    }

    public Map<String, Boolean> getSaves() {
        return saves;
    }

    public void setSaves(Map<String, Boolean> saves) {
        this.saves = saves;
    }

    public Map<String, String> getPhotos() {
        return photos;
    }

    // Setter를 다음과 같이 변경할 수도 있다.
    public void setPhotos(List<String> photos) {
        // Log.i(TAG, "setPhotos:photos:" + photos.toString());
        HashMap<String, String> photosMap = new HashMap<>();
        int index = 0;
        for (String photo : photos) {
            photosMap.put(String.valueOf(index), photo);
            index++;
        }
        this.photos = photosMap;
    }

    // 함수명이 중복될 경우 반드시 @Exclude 어노테이션을 사용해야 한다.
    @Exclude
    public void setPhotos(Map<String, String> photos) {
        this.photos = photos;
    }

    public Map<String, Boolean> getRegions() {
        return regions;
    }

    @Exclude
    public String[] getRegionsArray() {
        return regions.keySet().toArray(new String[0]);
    }

    @Exclude
    public void setDefaultRegions() {
        Map<String, Boolean> regions = new HashMap<>();
        regions.put(STORAGE_REGION_TOKYO, true);
        this.regions = regions;
    }

    public void setRegions(Map<String, Boolean> regions) {
        this.regions = regions;
    }

    public Map<String, Boolean> getHashtags() {
        return hashtags;
    }

    @SuppressWarnings("unchecked")
    public void setHashtags(Object hashtags) {
        Log.i(TAG, "setHashtags");

        if (hashtags == null) {
            this.hashtags = null;
            return;
        }

        if (hashtags instanceof List) {
            Log.d(TAG, "setHashtags:hashtags:" + hashtags);

            List<Boolean> hashtagsList = (List<Boolean>) hashtags;
            Map<String, Boolean> hashtagsMap = new HashMap<>();
            int index = 0;
            for (Boolean value : hashtagsList) {
                if (value != null) {
                    hashtagsMap.put(String.valueOf(index), value);
                }
                index++;
            }
            this.hashtags = hashtagsMap;
        }

        if (hashtags instanceof Map) {
            this.hashtags = (Map<String, Boolean>) hashtags;
        }
    }

    @Exclude
    public void setHashtags(@Nullable List<String> hashtags) {
        // Log.i(TAG, "setPhotos:photos:" + hashtags.toString());
        if (hashtags != null) {
            HashMap<String, Boolean> hashtagMap = new HashMap<>();
            for (String hashtag : hashtags) {
                hashtagMap.put(hashtag, true);
            }
            this.hashtags = hashtagMap;
        } else {
            this.hashtags = null;
        }
    }

    public Map<String, Boolean> getMentions() {
        return mentions;
    }

    public void setMentions(Map<String, Boolean> mentions) {
        this.mentions = mentions;
    }

    @Exclude
    public void setMentions(@Nullable List<String> mentions) {
        // Log.i(TAG, "setMentions:photos:" + mentions.toString());
        if (mentions != null) {
            HashMap<String, Boolean> mentionMap = new HashMap<>();
            for (String mention : mentions) {
                mentionMap.put(mention, true);
            }
            this.mentions = mentionMap;
        } else {
            this.mentions = null;
        }
    }

    public int getProductType() {
        return productType;
    }

    public void setProductType(int productType) {
        this.productType = productType;
    }

    public String getProductTitle() {
        return productTitle;
    }

    public void setProductTitle(String productTitle) {
        this.productTitle = productTitle;
    }

    public String getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(String productPrice) {
        this.productPrice = productPrice;
    }

    public String getProductServices() {
        return productServices;
    }

    public void setProductServices(String productServices) {
        this.productServices = productServices;
    }

    public String getShippingPrice() {
        return shippingPrice;
    }

    public void setShippingPrice(String shippingPrice) {
        this.shippingPrice = shippingPrice;
    }

    public List<Comment> getLatestComments() {
        return latestComments;
    }

    public void setLatestComments(HashMap<String, FIRComment> commentsMap) {
        List<Comment> comments = new ArrayList<>();
        for (Map.Entry<String, FIRComment> commentEntry : commentsMap.entrySet()) {
            Comment comment = new Comment();
            comment.setKey(commentEntry.getKey());
            comment.setUid(commentEntry.getValue().getUid());
            comment.setNickname(commentEntry.getValue().getNickname());
            comment.setContent(commentEntry.getValue().getContent());
            comment.setTimestamp(commentEntry.getValue().getTimestamp(true));

            comments.add(comment);
        }
        this.latestComments = comments;
    }

    @Exclude
    public void setLatestComments(List<Comment> latestComments) {
        this.latestComments = latestComments;
    }

    public Object getTimestamp() {
        return timestamp;
    }

    @Exclude
    public Long getTimestamp(boolean isLong) {
        if (timestamp instanceof Long) return (Long) timestamp;
        else return null;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("uid", uid);
        result.put("nickname", nickname);
        result.put("content", content);
        result.put("likeCount", likeCount);
        result.put("saveCount", saveCount);
        result.put("likes", likes);
        result.put("saves", saves);
        result.put("photos", photos);
        result.put("regions", regions);
        result.put("hashtags", hashtags);
        result.put("mentions", mentions);
        result.put("productTitle", productTitle);
        result.put("productPrice", productPrice);
        result.put("shippingPrice", shippingPrice);
        result.put("productServices", productServices);
        result.put("latestComments", latestComments);
        result.put("timestamp", timestamp);

        return result;
    }
}