package com.chaigene.petnolja.util;

import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import com.chaigene.petnolja.manager.AuthManager;
import com.chaigene.petnolja.manager.DatabaseManager;
import com.chaigene.petnolja.manager.FirestoreManager;
import com.chaigene.petnolja.manager.GlideManager;
import com.chaigene.petnolja.manager.StorageManager;
import com.chaigene.petnolja.manager.TasksManager;
import com.chaigene.petnolja.model.FIRUser;
import com.chaigene.petnolja.model.PrivateInfo;
import com.chaigene.petnolja.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import static com.chaigene.petnolja.Constants.PROFILE_IMAGE_FILENAME;
import static com.chaigene.petnolja.model.User.FIELD_EMAIL;
import static com.chaigene.petnolja.model.User.FIELD_NICKNAME;

public class UserUtil {
    private static final String TAG = "UserUtil";

    /**
     * 판다즈 앱은 내장디비를 사용하지 않기 때문에 캐쉬화를 오직 메모리를 통해서만 구현한다.
     * 한번 로드되어진 유저는 이 객체에 저장되고 재사용되어진다.
     */
    private List<User> mCachedUsers;

    private static volatile UserUtil mInstance;

    public static UserUtil getInstance() {
        if (mInstance == null) mInstance = new UserUtil();
        return mInstance;
    }

    public static void releaseInstance() {
        if (mInstance != null) {
            mInstance.release();
            mInstance = null;
        }
    }

    private void release() {
        this.mCachedUsers = null;
    }

    private UserUtil() {
        this.mCachedUsers = new ArrayList<>();
    }

    /**
     * 유저 정보를 업데이트 한다.
     * OldUser 객체가 아닌 FIRUser 객체를 넣어줘야 한다.
     *
     * @param firUser
     * @return
     */
    // FIXME
    @Deprecated
    public static Task<Void> update(FIRUser firUser) {
        DatabaseReference usersRef = DatabaseManager.getUserUsersRef().child(firUser.getKey());
        Map<String, Object> firUserMap = firUser.toMap();
        firUserMap.values().removeAll(Collections.singleton(null));
        // 3가지 값은 임의로 수정할 수 없고 functions에서만 수정이 가능하다.
        firUserMap.remove("followingCount");
        firUserMap.remove("followerCount");
        firUserMap.remove("purchaseCount");
        return DatabaseManager.updateChildren(usersRef, firUserMap);
    }

    /**
     * 유저 정보를 업데이트 한다.
     * FIXME: User 객체가 아닌 FIRUser 객체를 넣어줘야 한다.
     *
     * @param user
     * @return
     */
    public static Task<Void> update(User user) {
        DocumentReference userRef = FirestoreManager.getUsersRef().document(user.getId());
        Map<String, Object> userMap = user.toMap();
        userMap.values().removeAll(Collections.singleton(null));
        return FirestoreManager.getInstance().set(userRef, userMap, SetOptions.merge());
        /*DatabaseReference usersRef = DatabaseManager.getUserUsersRef().child(firUser.getKey());
        Map<String, Object> firUserMap = firUser.toMap();
        firUserMap.values().removeAll(Collections.singleton(null));
        // 3가지 값은 임의로 수정할 수 없고 functions에서만 수정이 가능하다.
        firUserMap.remove("followingCount");
        firUserMap.remove("followerCount");
        firUserMap.remove("purchaseCount");
        return DatabaseManager.updateChildren(usersRef, firUserMap);*/
    }

    /**
     * 해당 유저 ID가 존재하는지 여부를 반환한다.
     * Firebase Auth에 해당 uid가 존재하는지 여부는 알수가 없다.
     * 이 메서드는 RTDB에 해당 uid가 존재하는지를 반환하는 메서드이다.
     *
     * @param userId
     * @return
     */
    public static Task<Boolean> isExists(String userId) {
        Log.i(TAG, "isExists:userId:" + userId);
        Task<DocumentSnapshot> getUserTask = FirestoreManager.getUsersRef().document(userId).get();

        DocumentReference userRef = FirestoreManager.getUsersRef().document(userId);
        return FirestoreManager.getInstance().get(userRef, User.class).continueWith(task -> {
            if (!task.isSuccessful()) {
                Exception e = task.getException();
                Log.d(TAG, "isExists:ERROR:", e);
                throw e;
            }
            User user = task.getResult();
            boolean result = user != null && !TextUtils.isEmpty(user.getNickname());
            Log.d(TAG, "isExists:SUCCESS:result:" + result);
            return result;
        });
    }

    /**
     * 해당 닉네임이 존재하는지 여부를 반환한다.
     *
     * @param nickname
     * @return
     */
    // FIXME
    @Deprecated
    public static Task<Boolean> isNicknameExists(String nickname) {
        Log.i(TAG, "isNicknameExists:nickname:" + nickname);
        Query query = FirestoreManager.getUsersRef().whereEqualTo(FIELD_NICKNAME, nickname);
        return FirestoreManager.getInstance().get(query).continueWith(task -> {
            if (!task.isSuccessful()) {
                Exception e = task.getException();
                Log.d(TAG, "isNicknameExists:ERROR:", e);
                throw e;
            }
            QuerySnapshot querySnapshot = task.getResult();
            boolean result = !querySnapshot.isEmpty();
            Log.d(TAG, "isNicknameExists:SUCCESS:result:" + result);
            return result;
        });
    }

    /**
     * 해당 닉네임을 기준으로 완전한 형태의 OldUser 객체를 반환한다.
     * 해당하는 유저가 존재하지 않을 경우 null을 반환한다.
     *
     * @param nickname
     * @return
     */
    public static Task<User> getUserByNickname(final String nickname) {
        Log.i(TAG, "getUserByNickname:nickname:" + nickname);
        return TasksManager.call(() -> {
            Task<String> getUidByNicknameTask = getUserIdByNickname(nickname);
            String userId = Tasks.await(getUidByNicknameTask);
            if (userId == null) return null;

            Task<User> getUserTask = getUser(userId);
            User user = Tasks.await(getUserTask);

            Log.d(TAG, "getUserByNickname:SUCCESS:user:" + user);
            return user;
        }).continueWith(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "getUserByNickname:ERROR:", task.getException());
                throw task.getException();
            }
            return task.getResult();
        });
    }

    /**
     * 해당 이메일을 기준으로 완전한 형태의 OldUser 객체를 반환한다.
     * 해당하는 유저가 존재하지 않을 경우 null을 반환한다.
     *
     * @param email
     * @return
     */
    public static Task<User> getUserByEmail(final String email) {
        Log.i(TAG, "getUserByEmail:email:" + email);
        return TasksManager.call(() -> {
            Task<String> getUserIdByEmailTask = getUserIdByEmail(email);
            String userId = Tasks.await(getUserIdByEmailTask);
            if (userId == null) return null;

            Task<User> getUserTask = getUser(userId);
            User user = Tasks.await(getUserTask);

            Log.d(TAG, "getUserByEmail:SUCCESS:user:" + user);
            return user;
        }).continueWith(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "getUserByEmail:ERROR:", task.getException());
                throw task.getException();
            }
            return task.getResult();
        });
    }

    /**
     * 해당 닉네임을 기준으로 유저의 ID를 반환한다.
     * 해당하는 유저가 존재하지 않을 경우 null을 반환한다.
     *
     * @param nickname
     * @return
     */
    public static Task<String> getUserIdByNickname(String nickname) {
        Log.i(TAG, "getUserIdByNickname:nickname:" + nickname);
        Query usersQuery = FirestoreManager.getUsersRef().whereEqualTo(FIELD_NICKNAME, nickname);
        return FirestoreManager.getInstance().get(usersQuery).continueWith(task -> {
            if (!task.isSuccessful()) {
                Exception e = task.getException();
                Log.w(TAG, "getUserIdByNickname:search_nickname:ERROR", e);
                throw e;
            }
            Log.d(TAG, "getUserIdByNickname:search_nickname:SUCCESS");

            QuerySnapshot querySnapshot = task.getResult();
            if (querySnapshot.isEmpty()) {
                Log.d(TAG, "getUserIdByNickname:search_nickname:no_data");
                return null;
            }

            if (querySnapshot.size() <= 0) return null;
            String userId = querySnapshot.getDocuments().iterator().next().getId();
            Log.d(TAG, "getUserIdByNickname:search_nickname:userId:" + userId);
            return userId;
        });
    }

    /**
     * 해당 이메일을 기준으로 유저의 ID를 반환한다.
     * 해당하는 유저가 존재하지 않을 경우 null을 반환한다.
     *
     * @param email
     * @return
     */
    // FIXME
    @Deprecated
    public static Task<String> getOldUserIdByEmail(String email) {
        Log.i(TAG, "getOldUserIdByEmail:email:" + email);
        com.google.firebase.database.Query usersQuery = DatabaseManager.getUserUsersRef().orderByChild("email").equalTo(email);
        return DatabaseManager.getValue(usersQuery).continueWith(task -> {
            if (!task.isSuccessful()) {
                Exception e = task.getException();
                Log.w(TAG, "getOldUserIdByEmail:search_email:ERROR", e);
                throw e;
            }
            Log.d(TAG, "getOldUserIdByEmail:search_email:SUCCESS");

            DataSnapshot dataSnapshot = task.getResult();
            if (!dataSnapshot.exists()) return null;

            if (dataSnapshot.getChildrenCount() <= 0) return null;
            String uid = dataSnapshot.getChildren().iterator().next().getKey();
            Log.d(TAG, "getOldUserIdByEmail:uid:" + uid);
            return uid;
        });
    }

    /**
     * 해당 이메일을 기준으로 유저의 ID를 반환한다.
     * 해당하는 유저가 존재하지 않을 경우 null을 반환한다.
     *
     * @param email
     * @return
     */
    public static Task<String> getUserIdByEmail(String email) {
        Log.i(TAG, "getUserIdByEmail:email:" + email);
        Query usersQuery = FirestoreManager.getUsersRef().whereEqualTo(FIELD_EMAIL, email);
        return FirestoreManager.getInstance().get(usersQuery).continueWith(task -> {
            if (!task.isSuccessful()) {
                Exception e = task.getException();
                Log.w(TAG, "getUserIdByEmail:search_email:ERROR", e);
                throw e;
            }
            Log.d(TAG, "getUserIdByEmail:search_email:SUCCESS");

            QuerySnapshot querySnapshot = task.getResult();
            if (querySnapshot.isEmpty()) return null;
            if (querySnapshot.size() <= 0) return null;
            String userId = querySnapshot.getDocuments().iterator().next().getId();
            Log.d(TAG, "getOldUserIdByEmail:userId:" + userId);
            return userId;
        });
    }

    /**
     * 완전한 유저 객체를 반환한다.
     *
     * @param uid
     * @return
     */
    /*@Deprecated
    public static Task<OldUser> getOldUser(String uid) {
        // Log.i(TAG, "getOldUser:" + uid);
        return getOldUser(uid, false);
    }*/

    /**
     * 완전한 유저 객체를 반환한다.
     *
     * @param userId
     * @return
     */
    public static Task<User> getUser(String userId) {
        // Log.i(TAG, "getUser:" + uid);
        return getUser(userId, false);
    }

    /**
     * 완전한 OldUser 객체를 반환한다.
     *
     * @param uid
     * @param isForceRefresh
     * @return
     */
    /*@Deprecated
    public static Task<OldUser> getOldUser(String uid, boolean isForceRefresh) {
        // Log.i(TAG, "getOldUser:" + uid + "/isForceRefresh:" + isForceRefresh);
        if (!isForceRefresh) {
            OldUser cachedUser = getInstance().loadOldCachedUser(uid);
            if (cachedUser != null) {
                return Tasks.forResult(cachedUser);
            }
        }

        final Task<Boolean> isFollowingTask = isFollowing(uid);
        final Task<FIRUser> getUserTask = DatabaseManager.getValue(DatabaseManager.getUserUsersRef().child(uid), FIRUser.class);
        return Tasks.whenAll(isFollowingTask, getUserTask).continueWith(task -> {
            if (!isFollowingTask.isSuccessful()) throw isFollowingTask.getException();
            if (!getUserTask.isSuccessful()) {
                Exception getUserError = getUserTask.getException();
                if (getUserError instanceof DatabaseManager.NullDataException) {
                    return null;
                }
                throw getUserTask.getException();
            }
            boolean isFollowing = isFollowingTask.getResult();
            FIRUser firUser = getUserTask.getResult();
            OldUser user = new OldUser(firUser, isFollowing);
            getInstance().saveOldCachedUser(user);
            return user;
        });
    }*/

    /**
     * FIXME: 완전한 User 객체를 반환한다.
     *
     * @param userId
     * @param isForceRefresh
     * @return
     */
    public static Task<User> getUser(String userId, boolean isForceRefresh) {
        // Log.i(TAG, "getUser:" + uid + "|isForceRefresh:" + isForceRefresh);
        if (!isForceRefresh) {
            User cachedUser = getInstance().loadCachedUser(userId);
            if (cachedUser != null) return Tasks.forResult(cachedUser);
        }
        DocumentReference userRef = FirestoreManager.getUsersRef().document(userId);
        return FirestoreManager.getInstance().get(userRef, User.class).continueWith(task -> {
            User user = task.getResult();
            getInstance().saveCachedUser(user);
            return user;
        });
    }

    /**
     * 로그인한 유저가 휴대폰 인증을 했는지 여부를 반환한다.
     *
     * @return
     */
    public static Task<Boolean> isPhoneVerified() {
        return isPhoneVerified(AuthManager.getUserId());
    }

    /**
     * 해당 유저가 휴대폰 인증을 했는지 여부를 반환한다.
     *
     * @param uid
     * @return
     */
    public static Task<Boolean> isPhoneVerified(String uid) {
        Log.i(TAG, "isPhoneVerified");
        return getPrivateInfo(uid).continueWith(task -> {
            if (!task.isSuccessful()) {
                Exception e = task.getException();
                Log.w(TAG, "isPhoneVerified:ERROR:" + e.getMessage());
                throw e;
            }
            PrivateInfo privateInfo = task.getResult();
            boolean result = privateInfo != null && privateInfo.getPhone() != null;
            Log.d(TAG, "isPhoneVerified:" + result);
            return result;
        });
    }

    /**
     * 로그인한 유저의 휴대폰 인증을 초기화한다.
     * 휴대폰 인증에는 실명, 생년월일, 성별, 휴대폰 번호가 포함된다.
     * 따라서 이 메서드가 호출되면 4가지 값이 초기화 된다.
     *
     * @return
     */
    public static Task<Void> resetPhoneVerification() {
        Log.i(TAG, "resetPhoneVerification");
        Map<String, Object> privateInfoMap = new HashMap<>();
        privateInfoMap.put(PrivateInfo.FIELD_USERNAME, null);
        privateInfoMap.put(PrivateInfo.FIELD_BIRTHDAY, null);
        privateInfoMap.put(PrivateInfo.FIELD_SEX, null);
        privateInfoMap.put(PrivateInfo.FIELD_PHONE, null);
        DocumentReference privateInfoRef = FirestoreManager.getUserPrivateInfosRef().document(AuthManager.getUserId());
        return FirestoreManager.getInstance().set(privateInfoRef, privateInfoMap, SetOptions.merge());
    }

    public static Task<String> getBrithday() {
        return getBrithday(AuthManager.getUserId());
    }

    /**
     * 해당 유저의 개인정보에서 생년월일을 반환한다.
     *
     * @param userId
     * @return
     */
    public static Task<String> getBrithday(String userId) {
        // Log.i(TAG, "getBrithday:" + uid);
        return getPrivateInfo(userId).continueWith(task -> {
            if (!task.isSuccessful()) {
                Exception e = task.getException();
                throw e;
            }
            PrivateInfo privateInfo = task.getResult();
            return privateInfo.getBirthday();
        });
    }

    /**
     * 로그인한 유저의 개인정보를 업데이트 한다.
     *
     * @param privateInfo
     * @return
     */
    public static Task<Void> updatePrivateInfo(PrivateInfo privateInfo) {
        Log.i(TAG, "updatePrivateInfo:privateInfo:" + privateInfo.toMap());
        Map<String, Object> privateInfoMap = privateInfo.toMap();
        privateInfoMap.remove("id");
        privateInfoMap.values().removeAll(Collections.singleton(null));
        DocumentReference privateInfoRef = FirestoreManager.getUserPrivateInfosRef().document(AuthManager.getUserId());
        return FirestoreManager.getInstance().set(privateInfoRef, privateInfoMap, SetOptions.merge());
    }

    /**
     * 해당 유저의 개인정보를 반환한다.
     *
     * @param uid
     * @return
     */
    public static Task<PrivateInfo> getPrivateInfo(String uid) {
        Log.i(TAG, "getPrivateInfo");
        DocumentReference privateInfoRef = FirestoreManager.getUserPrivateInfosRef().document(uid);
        return FirestoreManager.getInstance().get(privateInfoRef, PrivateInfo.class);
    }

    /**
     * 현재 로그인한 유저의 개인정보를 삭제한다.
     *
     * @return
     */
    public static Task<Void> deletePrivateInfo() {
        return deletePrivateInfo(AuthManager.getUserId());
    }

    /**
     * 해당 유저의 개인정보를 삭제한다.
     *
     * @param uid
     * @return
     */
    public static Task<Void> deletePrivateInfo(String uid) {
        Log.i(TAG, "getPrivateInfo");
        DocumentReference privateInfoRef = FirestoreManager.getUserPrivateInfosRef().document(uid);
        return FirestoreManager.getInstance().delete(privateInfoRef);
    }

    /*@Deprecated
    public static Task<Void> clearSignatureAndRegions(String uid) {
        Map<String, Object> childUpdates = new HashMap<>();
        DatabaseReference userRef = DatabaseManager.getUserUsersRef().child(uid);
        String signaturePath = DatabaseManager.getPath(userRef, "signature");
        String regionsPath = DatabaseManager.getPath(userRef, "regions");
        childUpdates.put(signaturePath, null);
        childUpdates.put(regionsPath, null);
        return DatabaseManager.updateChildren(childUpdates);
    }*/

    /**
     * 해당 유저 프로필 사진의 signature를 저장한다.
     * signature는 Glide의 캐쉬 control을 위해 필요한 값이다.
     *
     * @param uid
     * @param signature
     * @return
     */
    public static Task<Void> setSignature(String uid, String signature) {
        Log.i(TAG, "setSignature:uid:" + uid + ":signature:" + signature);
        return DatabaseManager.getUserUsersRef().child(uid).child("signature").setValue(signature);
    }

    /**
     * 해당 유저의 프로필 이미지를 다운로드 한다.
     *
     * @param userId
     * @param imageView
     * @return
     */
    public static Task<Void> downloadProfileImage(final String userId, final ImageView imageView) {
        Log.i(TAG, "downloadProfileImage:uid:" + userId);
        return getUser(userId).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "downloadProfileImage:getOldUser:ERROR:" + task.getException().getMessage());
                throw task.getException();
            }
            User user = task.getResult();
            Log.w(TAG, "downloadProfileImage:getOldUser:user:" + user.toMap());
            String signature = user.getSignature();
            return downloadProfileImage(userId, signature, imageView);
        }).continueWith(task -> null);
    }

    /**
     * 해당 유저의 프로필 이미지를 다운로드 한다.
     *
     * @param uid
     * @param signature
     * @param imageView
     * @return
     */
    // FIXME
    public static Task<Void> downloadProfileImage(String uid, String signature, ImageView imageView) {
        Log.i(TAG, "downloadProfileImage");
        StorageReference usersRef = StorageManager.getUsersRef().child(uid).child(PROFILE_IMAGE_FILENAME);
        return GlideManager.loadImageWithSignature(usersRef, signature, imageView).continueWith(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "downloadProfileImage:ERROR:" + task.getException());
                return null;
            }
            return null;
        });
    }

    // TODO: Cache에 있는 값을 재사용할 것인가?
    // 애초에 getUser를 했을 때만 cachedUser에 해당 유저 객체가 존재하게 된다.
    // 따라서 cachedUser에 값이 존재한다는 것은 이미 이전에 isFollowing 메서드가 호출된 적이 있다는 의미이다.
    // 따라서 중복 호출을 방지하기 위해서 cache를 재사용해도 상관 없을 것 같다.

    /**
     * 해당 유저를 팔로잉 하고 있는지 여부를 반환한다.
     *
     * @param targetUid
     * @return
     */
    /*@Deprecated
    public static Task<Boolean> isFollowing(final String targetUid) {
        // Log.i(TAG, "isFollowing:targetUid:" + targetUid);

        // 로그인이 아닌 상태에서도 이 메서드를 호출할 수 있다.
        if (!AuthManager.isSignedIn()) return Tasks.forResult(false);

        // targetUid가 본인의 uid와 동일할 경우에는 false 반환
        String uid = AuthManager.getUserId();
        if (targetUid.equals(uid)) {
            return Tasks.forResult(false);
        }

        DatabaseReference followersRef = DatabaseManager.getUserFollowFollowersRef().child(targetUid).child(uid);
        return DatabaseManager.getBoolean(followersRef).continueWith(task -> {
            if (!task.isSuccessful()) {
                Exception e = task.getException();
                // NullDataException이라면 팔로잉을 하지 않고 있다는 뜻이기 때문에 throw를 해서는 안된다.
                if (e instanceof DatabaseManager.NullDataException) {
                    // Log.d(TAG, "isFollowing:SUCCESS:false");
                    return false;
                }
                // Log.w(TAG, "isFollowing:ERROR:", e);
                throw e;
            }
            boolean isFollowing = task.getResult();
            // Log.d(TAG, "isFollowing:SUCCESS:" + isFollowing);
            User cachedUser = getInstance().loadOldCachedUser(targetUid);
            if (cachedUser != null) {
                cachedUser.setFollowing(true);
                getInstance().saveOldCachedUser(cachedUser);
            }
            return isFollowing;
        });
    }*/
    @Deprecated
    public static Task<Boolean> isFollower(String targetUid) {
        return null;
    }

    // 팔로우를 하고 난 뒤에 cache 된 유저 리스트를 업데이트 해줘야 한다.
    // 만약 cache 된 유저가 없다면 어떻게 해야할까?

    /**
     * 해당 유저를 팔로우 한다.
     *
     * @param targetUid
     * @return
     */
    // FIXME
    /*@Deprecated
    public static Task<Void> follow(final String targetUid) {
        Log.i(TAG, "follow:" + targetUid);
        DatabaseReference followersRef = DatabaseManager.getUserFollowFollowersRef().child(targetUid).child(AuthManager.getUserId());
        *//*HashMap<String, Boolean> followMap = new HashMap<>();
        followMap.put(AuthManager.getUserId(), true);*//*
        return DatabaseManager.setValue(followersRef, true).continueWith((Continuation<Void, Void>) task -> {
            if (!task.isSuccessful()) {
                Exception e = task.getException();
                // Log.w(TAG, "follow");
                throw e;
            }
            OldUser cachedUser = getInstance().loadOldCachedUser(targetUid);
            if (cachedUser != null) {
                cachedUser.setFollowing(true);
                getInstance().saveOldCachedUser(cachedUser);
            }
            return null;
        }).continueWith(task -> {
            if (!task.isSuccessful()) {
                Exception e = task.getException();
                Log.w(TAG, "follow:ERROR", e);
                throw e;
            }
            return null;
        });
    }*/

    /**
     * 해당 유저를 언팔로우 한다.
     *
     * @param targetUid
     * @return
     */
    // FIXME
    /*@Deprecated
    public static Task<Void> unfollow(final String targetUid) {
        Log.i(TAG, "unfollow:" + targetUid);
        DatabaseReference followersRef = DatabaseManager.getUserFollowFollowersRef().child(targetUid).child(AuthManager.getUserId());
        return followersRef.removeValue().continueWith((Continuation<Void, Void>) task -> {
            if (!task.isSuccessful()) {
                Exception e = task.getException();
                // Log.w(TAG, "unfollow", e);
                throw e;
            }
            OldUser cachedUser = getInstance().loadOldCachedUser(targetUid);
            if (cachedUser != null) {
                cachedUser.setFollowing(false);
                getInstance().saveOldCachedUser(cachedUser);
            }
            return null;
        }).continueWith(task -> {
            if (!task.isSuccessful()) {
                Exception e = task.getException();
                Log.w(TAG, "follow:ERROR", e);
                throw e;
            }
            return null;
        });
    }*/

    /**
     * 해당 유저를 팔로우 하고 있는 유저의 목록을 반환한다.
     *
     * @param userId
     * @param amount
     * @param maxKey
     * @return
     */
    @Deprecated
    public static Task<List<User>> getFollowers(final String userId, final int amount, final String maxKey) {
        return Tasks.call(Executors.newSingleThreadExecutor(), () -> {
            com.google.firebase.database.Query query = DatabaseManager.getUserFollowFollowersRef().child(userId);
            if (maxKey != null) {
                query = query.orderByKey().endAt(maxKey).limitToLast(amount + 1);
            } else {
                query = query.orderByKey().limitToLast(amount);
            }

            Task<DataSnapshot> getTask = DatabaseManager.getValue(query);

            DataSnapshot dataSnapshot = null;
            try {
                dataSnapshot = Tasks.await(getTask);
            } catch (Exception ignored) {
            }

            if (!getTask.isSuccessful()) {
                Exception e = getTask.getException();
                Log.w(TAG, "getFollowers:getTask:ERROR", e);
                throw e;
            }

            Log.d(TAG, "getFollowers:key:" + dataSnapshot.getKey() + "|count:" + dataSnapshot.getChildrenCount());

            List<User> followers = new ArrayList<>();

            // 데이타가 없을 때는 종료한다.
            if (!dataSnapshot.exists() || dataSnapshot.getChildrenCount() == 0) {
                Log.d(TAG, "getFollowers:no_data");
                return followers;
            }

            Iterable<DataSnapshot> dataSnapshots = dataSnapshot.getChildren();
            List<DataSnapshot> dataSnapshotList = new ArrayList<>();
            for (DataSnapshot snapshot : dataSnapshots) {
                if (snapshot.getKey().equals(maxKey)) continue;
                dataSnapshotList.add(snapshot);
            }
            Collections.reverse(dataSnapshotList);
            for (DataSnapshot snapshot : dataSnapshotList) {
                String followerUserId = snapshot.getKey();

                // Log.d(TAG, "getFollowers:loop:followerUserId:" + followerUserId);

                Task<User> getUserTask = getUser(followerUserId);
                User follower = null;
                try {
                    follower = Tasks.await(getUserTask);
                } catch (Exception ignored) {
                }

                if (!getUserTask.isSuccessful()) {
                    Exception e = getUserTask.getException();
                    Log.w(TAG, "getFollowers:getUserTask:ERROR", e);
                    throw e;
                }

                // int index = dataSnapshotList.indexOf(snapshot);
                // Log.d(TAG, "getPosts:loop:index:" + index + "|child:" + snapshot.toString());
                // Log.d(TAG, "getPosts:loop:index:" + index + "|firPost:" + firPost.toMap());

                getInstance().saveCachedUser(follower);

                followers.add(follower);
            }
            return followers;
        });
    }

    /**
     * 해당 유저를 팔로잉 하고 있는 유저의 목록을 반환한다.
     *
     * @param userId
     * @param amount
     * @param maxKey
     * @return
     */
    @Deprecated
    public static Task<List<User>> getFollowings(final String userId, final int amount, final String maxKey) {
        return Tasks.call(Executors.newSingleThreadExecutor(), () -> {
            com.google.firebase.database.Query query = DatabaseManager.getUserFollowFollowingsRef().child(userId);
            if (maxKey != null) {
                query = query.orderByKey().endAt(maxKey).limitToLast(amount + 1);
            } else {
                query = query.orderByKey().limitToLast(amount);
            }

            Task<DataSnapshot> getTask = DatabaseManager.getValue(query);

            DataSnapshot dataSnapshot = null;
            try {
                dataSnapshot = Tasks.await(getTask);
            } catch (Exception ignored) {
            }

            if (!getTask.isSuccessful()) {
                Exception e = getTask.getException();
                Log.w(TAG, "getFollowings:getTask:ERROR", e);
                throw e;
            }

            Log.d(TAG, "getFollowings:key:" + dataSnapshot.getKey() + "|count:" + dataSnapshot.getChildrenCount());

            List<User> followings = new ArrayList<>();

            // 데이타가 없을 때는 종료한다.
            if (!dataSnapshot.exists() || dataSnapshot.getChildrenCount() == 0) {
                Log.d(TAG, "getFollowings:no_data");
                return followings;
            }

            Iterable<DataSnapshot> dataSnapshots = dataSnapshot.getChildren();
            List<DataSnapshot> dataSnapshotList = new ArrayList<>();
            for (DataSnapshot snapshot : dataSnapshots) {
                if (snapshot.getKey().equals(maxKey)) continue;
                dataSnapshotList.add(snapshot);
            }
            Collections.reverse(dataSnapshotList);
            for (DataSnapshot snapshot : dataSnapshotList) {
                String followingUserId = snapshot.getKey();

                Task<User> getUserTask = getUser(followingUserId);
                User following = null;
                try {
                    following = Tasks.await(getUserTask);
                } catch (Exception ignored) {
                }

                if (!getUserTask.isSuccessful()) {
                    Exception e = getUserTask.getException();
                    Log.w(TAG, "getFollowings:getUserTask:ERROR", e);
                    throw e;
                }
                Log.d(TAG, "getFollowings:getUserTask:SUCCESS");

                getInstance().saveCachedUser(following);

                followings.add(following);
            }
            return followings;
        });
    }

    /**
     * 해당 유저 ID를 기준으로 유저의 닉네임을 반환한다.
     * 캐쉬에서 존재하는지 검색해보고 없으면 서버에서 검색해서 반환해준다.
     *
     * @param uid
     * @return
     */
    public static Task<String> getUserNickname(String uid) {
        User cachedUser = getInstance().loadCachedUser(uid);
        if (cachedUser != null) {
            return Tasks.forResult(cachedUser.getNickname());
        }

        return getUser(uid).continueWith(task -> {
            if (!task.isSuccessful()) {
                // ERROR
                return null;
            }
            User user = task.getResult();
            String nickname = user.getNickname();
            return nickname;
        });
    }

    // FIXME: 완성된 형태의 VO만 메모리에 저장한다 (User => o, FIRUser => x)
    public synchronized void saveCachedUser(User newUser) {
        boolean isExists = false;
        for (User user : mCachedUsers) {
            if (newUser.getId().equals(user.getId())) {
                isExists = true;
                int index = mCachedUsers.indexOf(user);
                mCachedUsers.set(index, newUser);
                // Log.d(TAG, "saveOldCachedUser:saved:cachedUsers:" + mCachedOldUsers.toString());
                break;
            }
        }
        if (!isExists) {
            mCachedUsers.add(newUser);
            // Log.d(TAG, "saveOldCachedUser:saved:cachedUsers:" + mCachedOldUsers.toString());
        }
    }

    public synchronized User loadCachedUser(@NonNull String userId) {
        // Log.i(TAG, "loadOldCachedUser:uid:" + uid);
        for (User user : mCachedUsers) {
            if (userId.equals(user.getId())) {
                // Log.d(TAG, "loadOldCachedUser:loaded:user:" + user.toMap());
                return user;
            }
        }
        return null;
    }
}