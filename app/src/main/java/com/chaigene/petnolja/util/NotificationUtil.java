package com.chaigene.petnolja.util;

import androidx.annotation.NonNull;
import android.util.Log;

import com.chaigene.petnolja.manager.TasksManager;
import com.chaigene.petnolja.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.iid.FirebaseInstanceId;
import com.chaigene.petnolja.BuildConfig;
import com.chaigene.petnolja.manager.AuthManager;
import com.chaigene.petnolja.manager.DatabaseManager;
import com.chaigene.petnolja.manager.FirestoreManager;
import com.chaigene.petnolja.model.FIRNotification;
import com.chaigene.petnolja.model.Notification;
import com.chaigene.petnolja.model.Token;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_ALL;
import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_FEED;
import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_TALENT;
import static com.chaigene.petnolja.model.Token.FIELD_TOKEN;
import static com.chaigene.petnolja.model.Token.FIELD_VERSION_CODE;
import static com.chaigene.petnolja.model.Token.FIELD_VERSION_NAME;

public class NotificationUtil {
    private static final String TAG = "NotificationUtil";

    private int mUncheckedCount;

    private static NotificationUtil instance;

    public static synchronized NotificationUtil getInstance() {
        if (instance == null) instance = new NotificationUtil();
        return instance;
    }

    public static synchronized void releaseInstance() {
        if (instance != null) {
            instance.release();
            instance = null;
        }
    }

    private void release() {
        this.mUncheckedCount = 0;
    }

    private NotificationUtil() {
        this.mUncheckedCount = 0;
    }

    /**
     * FCM을 통해 받은 Map 형태의 data를 Notification 모델 객체로 파싱한다.
     *
     * @param data
     * @return
     */
    public static Notification parse(Map<String, String> data) {

        String regions = data.get("regions");
        Map<String, Boolean> regionsMap = new HashMap<>();
        if (regions != null) {
            if (regions.contains("|")) {
                String[] regionsArr = regions.split("|");
                List<String> regionsList = Arrays.asList(regionsArr);
                for (String region : regionsList) {
                    regionsMap.put(region, true);
                }
            } else {
                regionsMap.put(regions, true);
            }
        }

        FIRNotification firNotification = new FIRNotification();
        firNotification.setKey(data.get("key"));
        firNotification.setType(CommonUtil.toInt(data.get("type")));
        firNotification.setTargetUid(data.get("targetUid"));
        firNotification.setTargetNickname(data.get("targetNickname"));
        firNotification.setPostId(data.get("postId"));
        firNotification.setContent(data.get("content"));
        firNotification.setPhotoName(data.get("photoName"));
        firNotification.setRegions(regionsMap);
        firNotification.setCommentId(data.get("commentId"));
        firNotification.setComment(data.get("comment"));
        firNotification.setChatRoomId(data.get("chatRoomId"));
        firNotification.setChatMessage(data.get("chatMessage"));
        firNotification.setShopType(CommonUtil.toInt(data.get("shopType")));
        firNotification.setOrderId(data.get("orderId"));
        firNotification.setOrderName(data.get("orderName"));
        firNotification.setOrderStatus(CommonUtil.toInt(data.get("orderStatus")));
        firNotification.setProductId(data.get("productId"));
        firNotification.setProductTitle(data.get("productTitle"));
        firNotification.setQuantity(CommonUtil.toInt(data.get("quantity")));
        firNotification.setTotalPrice(CommonUtil.toInt(data.get("totalPrice")));
        firNotification.setAutoFinalized(Boolean.valueOf(data.get("isAutoFinalized")));
        firNotification.setTimestamp(CommonUtil.toLong("timestamp"));
        firNotification.setMessage(data.get("message"));
        Notification notification = new Notification(firNotification);

        Log.i(TAG, "parse:notification:" + notification.toMap());
        return notification;
    }

    /**
     * 현재 로그인한 유저의 StableToken이 서버에 존재하는지 여부를 반환한다.
     * StableToken이란 디바이스 ID(token) 및 앱 버전을 담고있어서 토큰별로 다른 형태의 FCM Push를 제공할 때 사용된다.
     *
     * @param token
     * @return
     */
    public static Task<Boolean> isStableTokenExists(String token) {
        Log.i(TAG, "isStableTokenExists");
        com.google.firebase.firestore.Query tokensRef = FirestoreManager.getNotiStableTokensRef().whereEqualTo(FIELD_TOKEN, token);
        return FirestoreManager.getInstance().get(tokensRef).continueWith(task -> {
            if (!task.isSuccessful()) {
                Exception e = task.getException();
                throw e;
            }
            QuerySnapshot snapshot = task.getResult();
            return !snapshot.isEmpty();
        });
    }

    /**
     * 현재 로그인한 유저의 StableToken을 서버로부터 반환한다.
     * StableToken이란 디바이스 ID(token) 및 앱 버전을 담고있어서 토큰별로 다른 형태의 FCM Push를 제공할 때 사용된다.
     *
     * @param token
     * @return
     */
    public static Task<Token> getStableToken(String token) {
        // Log.i(TAG, "getStableToken");
        com.google.firebase.firestore.Query tokensRef = FirestoreManager.getNotiStableTokensRef().whereEqualTo(FIELD_TOKEN, token);
        return FirestoreManager.getInstance().get(tokensRef, Token.class).continueWith(task -> {
            if (!task.isSuccessful()) {
                Exception e = task.getException();
                Log.w(TAG, "getStableToken:ERROR:" + e.getMessage());
                throw e;
            }
            List<Token> tokens = task.getResult();
            Token stableToken;
            if (tokens.isEmpty()) {
                stableToken = null;
            } else {
                stableToken = tokens.iterator().next();
            }
            Log.i(TAG, "getStableToken:" + (stableToken != null ? stableToken.toMap() : null));
            return stableToken;
        });
    }

    /**
     * 현재 로그인한 유저의 StableToken을 서버에 삽입한다.
     * StableToken이란 디바이스 ID(token) 및 앱 버전을 담고있어서 토큰별로 다른 형태의 FCM Push를 제공할 때 사용된다.
     *
     * @param token
     * @return
     */
    public static Task<Void> insertStableToken(String token) {
        Token newToken = new Token(token, BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME);
        return insertStableToken(newToken);
    }

    /**
     * 현재 로그인한 유저의 StableToken을 서버에 업데이트한다.
     * StableToken이란 디바이스 ID(token) 및 앱 버전을 담고있어서 토큰별로 다른 형태의 FCM Push를 제공할 때 사용된다.
     *
     * @param token
     * @return
     */
    public static Task<Void> insertStableToken(Token token) {
        Log.i(TAG, "insertStableToken:token:" + token);
        DocumentReference tokenRef = FirestoreManager.getNotiStableTokensRef().document();
        return FirestoreManager.getInstance().set(tokenRef, token);
    }

    /**
     * 현재 로그인한 유저의 StableToken을 서버에 업데이트한다.
     * StableToken이란 디바이스 ID(token) 및 앱 버전을 담고있어서 토큰별로 다른 형태의 FCM Push를 제공할 때 사용된다.
     *
     * @param tokenId
     * @param versionCode
     * @param versionName
     * @return
     */
    public static Task<Void> updateStableToken(String tokenId, int versionCode, String versionName) {
        DocumentReference tokenRef = FirestoreManager.getNotiStableTokensRef().document(tokenId);
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put(FIELD_VERSION_CODE, versionCode);
        childUpdates.put(FIELD_VERSION_NAME, versionName);
        return FirestoreManager.getInstance().update(tokenRef, childUpdates);
    }

    /**
     * 현재 로그인한 유저의 StableToken을 서버에서 삭제한다.
     * StableToken이란 디바이스 ID(token) 및 앱 버전을 담고있어서 토큰별로 다른 형태의 FCM Push를 제공할 때 사용된다.
     *
     * @return
     */
    public static Task<Void> deleteStableToken() {
        String token = FirebaseInstanceId.getInstance().getToken();
        return TasksManager.call(() -> {
            String tokenId = Tasks.await(getStableToken(token)).getId();
            DocumentReference tokenRef = FirestoreManager.getNotiStableTokensRef().document(tokenId);
            Tasks.await(FirestoreManager.getInstance().delete(tokenRef));
            return null;
        });
    }

    /**
     * 현재 로그인한 유저의 Token이 서버에 존재하는지 여부를 반환한다.
     * Token은 StableToken과는 다르게 단순한 디바이스 ID 값을 의미한다.
     *
     * @return
     */
    @Deprecated
    public static Task<Boolean> isTokenExists() {
        Log.i(TAG, "isTokenExists");

        if (!AuthManager.isSignedIn()) {
            Log.w(TAG, "isTokenExists:not_signed_in");
            return Tasks.forException(new AuthManager.AuthException("User is not signed in."));
        }

        String token = FirebaseInstanceId.getInstance().getToken();

        if (token == null) {
            Log.w(TAG, "isTokenExists:token_is_null");
            return Tasks.forException(new RuntimeException("Token doesn't exists on the client."));
        }

        DocumentReference userRef = FirestoreManager.getUsersRef().document(AuthManager.getUserId());
        return FirestoreManager.getInstance().get(userRef, User.class).continueWith(task -> {
            if (!task.isSuccessful()) {
                Exception e = task.getException();
                Log.w(TAG, "isTokenExists:get_token:ERROR", e);
                throw e;
            }
            User user = task.getResult();
            return user.getTokens().contains(token);
        });
    }

    /**
     * 현재 로그인한 유저의 Token 및 StableToken을 서버에 저장한다.
     *
     * @param token
     * @return
     */
    public static Task<Void> insertToken(final String token) {
        Log.i(TAG, "insertToken:token:" + token);
        if (!AuthManager.isSignedIn()) {
            return Tasks.forException(new AuthManager.AuthException("Not signed in."));
        }
        return NotificationUtil.getStableToken(token).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            Token stableToken = task.getResult();
            if (stableToken == null) {
                return insertStableToken(token);
            } else {
                return updateStableToken(stableToken.getId(), BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME);
            }
        });
    }

    /**
     * 현재 로그인한 유저의 서버상의 토큰을 삭제한다.
     *
     * @return
     */
    @Deprecated
    public static Task<Void> deleteToken() {
        Log.i(TAG, "deleteToken");
        String token = FirebaseInstanceId.getInstance().getToken();
        return DatabaseManager.getUserUsersRef().child(AuthManager.getUserId()).child("tokens").child(token).removeValue();
    }

    /**
     * 현재 로그인한 유저의 알림 목록을 반환한다.
     *
     * @param articleType
     * @param amount
     * @param maxKey
     * @return
     */
    public static Task<List<Notification>> getNotifications(int articleType, int amount, final String maxKey) {
        Log.i(TAG, "getNotifications:articleType:" + articleType + "|amount:" + amount + "|maxKey:" + maxKey);

        Query query = null;
        if (articleType == ARTICLE_TYPE_ALL) {
            query = DatabaseManager.getNotificationUserNotisRef().child(AuthManager.getUserId());
        } else if (articleType == ARTICLE_TYPE_FEED) {
            query = DatabaseManager.getNotificationUserNotisFeedRef().child(AuthManager.getUserId());
        } else if (articleType == ARTICLE_TYPE_TALENT) {
            query = DatabaseManager.getNotificationUserNotisTalentRef().child(AuthManager.getUserId());
        }

        // maxKey가 존재하지 않을 경우에는 다른 query를 사용해야 한다.
        if (maxKey != null) {
            query = query.orderByKey().endAt(maxKey).limitToLast(amount + 1);
        } else {
            query = query.orderByKey().limitToLast(amount);
        }

        return DatabaseManager.getValue(query).continueWith(task -> {
            if (!task.isSuccessful()) {
                Exception e = task.getException();
                Log.w(TAG, "getNotifications:ERROR", e);
                throw e;
            }

            List<Notification> notifications = new ArrayList<>();

            DataSnapshot dataSnapshot = task.getResult();
            Log.d(TAG, "getNotifications:key:" + dataSnapshot.getKey() + "|count:" + dataSnapshot.getChildrenCount());

            // 데이타가 없을 때는 종료한다.
            if (!dataSnapshot.exists() || dataSnapshot.getChildrenCount() == 0) {
                Log.d(TAG, "getNotifications:no_data");
                return notifications;
            }

            Iterable<DataSnapshot> dataSnapshots = dataSnapshot.getChildren();
            List<DataSnapshot> dataSnapshotList = new ArrayList<>();
            for (DataSnapshot snapshot : dataSnapshots) {
                if (snapshot.getKey().equals(maxKey)) continue;
                dataSnapshotList.add(snapshot);
            }
            Collections.reverse(dataSnapshotList);
            for (DataSnapshot snapshot : dataSnapshotList) {
                Log.d(TAG, "getNotifications:loop:snapshot:" + snapshot.getKey() + " => " + snapshot.toString());
                FIRNotification firNotification = snapshot.getValue(FIRNotification.class);
                firNotification.setKey(snapshot.getKey());
                Notification notification = new Notification(firNotification);
                notifications.add(notification);
            }

            return notifications;
        });
    }

    /**
     * 서버의 해당 알림을 읽음 처리한다.
     *
     * @param notificationId
     * @return
     */
    public static Task<Void> check(final String notificationId) {
        Log.i(TAG, "check:notificationId:" + notificationId);
        DatabaseReference notisRef = DatabaseManager.getNotificationUserNotisRef();
        return notisRef.child(AuthManager.getUserId()).child(notificationId).child("checked").setValue(true);
    }

    @Deprecated
    public static Task<Void> check(final String notificationId, int articleType) {
        Log.i(TAG, "check:notificationId:" + notificationId + "|articleType:" + articleType);
        DatabaseReference notisRef = null;
        if (articleType == ARTICLE_TYPE_ALL) {
            notisRef = DatabaseManager.getNotificationUserNotisRef();
        } else if (articleType == ARTICLE_TYPE_FEED) {
            notisRef = DatabaseManager.getNotificationUserNotisFeedRef();
        } else if (articleType == ARTICLE_TYPE_TALENT) {
            notisRef = DatabaseManager.getNotificationUserNotisTalentRef();
        }
        return notisRef.child(AuthManager.getUserId()).child(notificationId).child("checked").setValue(true);
    }

    /**
     * 서버의 모든 알림을 읽음 처리한다.
     *
     * @return
     */
    public static Task<Void> checkAll() {
        Log.i(TAG, "checkAll");
        return Tasks.call(Executors.newSingleThreadExecutor(), (Callable<Void>) () -> {
            String myUid = AuthManager.getUserId();
            Query query = DatabaseManager.getNotificationUserNotisRef().child(myUid).orderByChild("checked").equalTo(false);
            Task<DataSnapshot> getUnchckedListTask = DatabaseManager.getValue(query);
            DataSnapshot dataSnapshot = Tasks.await(getUnchckedListTask);

            // 데이타가 없을 때는 종료한다.
            if (!dataSnapshot.exists() || dataSnapshot.getChildrenCount() == 0) {
                Log.d(TAG, "checkAll:no_data");
                return null;
            }

            Map<String, Object> childUpdates = new HashMap<>();
            Iterable<DataSnapshot> dataSnapshots = dataSnapshot.getChildren();
            for (DataSnapshot snapshot : dataSnapshots) {
                String notificationId = snapshot.getKey();
                DatabaseReference userNotisRef = DatabaseManager.getNotificationUserNotisRef();
                String userNotisPath = DatabaseManager.getPath(userNotisRef);
                String path = CommonUtil.format("%s/%s/%s/checked", userNotisPath, myUid, notificationId);
                childUpdates.put(path, true);
            }
            Task<Void> updateUserNotisTask = DatabaseManager.updateChildren(childUpdates);
            Tasks.await(updateUserNotisTask);
            return null;
        }).continueWith(task -> {
            if (!task.isSuccessful()) {
                Exception e = task.getException();
                Log.w(TAG, "checkAll:updateUserNotisTask:ERROR:" + e.getMessage());
                throw e;
            }
            return null;
        });
    }

    /**
     * 서버로부터 읽지 않은 알림 개수를 반환한다.
     *
     * @return
     */
    // TODO: 어느 시점에서 호출해야 하는가?
    public static Task<Integer> getUncheckedCount() {
        Log.i(TAG, "getUncheckedCount");
        Query query = DatabaseManager.getNotificationUserNotisRef().child(AuthManager.getUserId()).orderByChild("checked").equalTo(false);
        return DatabaseManager.getValue(query).continueWith(task -> {
            int count = 0;
            if (!task.isSuccessful()) {
                // ERROR
                return count;
            }
            DataSnapshot dataSnapshot = task.getResult();
            count = (int) dataSnapshot.getChildrenCount();
            return count;
        });
    }

    /**
     * 해당 노티피케이션을 삭제한다.
     * 현재는 debug 모드에서만 지원된다.
     *
     * @param notificationId
     * @return
     */
    public static Task<Void> delete(final String notificationId) {
        Log.i(TAG, "delete:notificationId:" + notificationId);
        String uid = AuthManager.getUserId();
        DatabaseReference userNotisRef = DatabaseManager.getNotificationUserNotisRef().child(uid).child(notificationId);
        DatabaseReference userNotisTalentRef = DatabaseManager.getNotificationUserNotisTalentRef().child(uid).child(notificationId);

        Map<String, Object> childUpdates = new HashMap<>();

        String userNotisPath = DatabaseManager.getPath(userNotisRef);
        String userNotisTalentPath = DatabaseManager.getPath(userNotisTalentRef);

        childUpdates.put(userNotisPath, null);
        childUpdates.put(userNotisTalentPath, null);

        return DatabaseManager.updateChildren(childUpdates).continueWith(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "delete:ERROR:" + task.getException().getMessage());
                throw task.getException();
            }
            Log.d(TAG, "delete:SUCCESS");
            return null;
        });
    }

    /**
     * 캐쉬에 읽지 않은 알림 개수를 저장한다.
     *
     * @param uncheckedCount
     */
    public synchronized void saveUncheckedCount(int uncheckedCount) {
        this.mUncheckedCount = uncheckedCount;
    }

    /**
     * 캐쉬에서 읽지 않은 알림 개수를 반환한다.
     *
     * @return
     */
    public synchronized int loadUncheckedCount() {
        return mUncheckedCount;
    }

    /**
     * 캐쉬에 읽지 않은 알림 개수를 1 증가시킨다.
     */
    public synchronized void increaseUncheckedCount() {
        int count = loadUncheckedCount();
        count++;
        saveUncheckedCount(count);
    }

    /**
     * 캐쉬에서 읽지 않은 알림 개수를 1 감소시킨다.
     */
    public synchronized void decreaseUncheckedCount() {
        int count = loadUncheckedCount();
        if (count > 0) count--;
        else count = 0;
        saveUncheckedCount(count);
    }

    /**
     * 캐쉬에 읽지 않은 알림 개수를 초기화한다.
     */
    public synchronized void clearUncheckedCount() {
        saveUncheckedCount(0);
    }
}
