package com.chaigene.petnolja.util;

import android.net.Uri;
import androidx.annotation.NonNull;
import android.util.Log;

import com.chaigene.petnolja.manager.AuthManager;
import com.chaigene.petnolja.manager.DatabaseManager;
import com.chaigene.petnolja.manager.StorageManager;
import com.chaigene.petnolja.model.FIRPost;
import com.chaigene.petnolja.model.Post;
import com.chaigene.petnolja.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_ALL;
import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_FEED;
import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_TALENT;
import static com.chaigene.petnolja.Constants.DATABASE_PATH_ARTICLE;
import static com.chaigene.petnolja.Constants.DATABASE_PATH_ARTICLE_POSTS;

/**
 * 글과 관련된 모든 기능을 할 수 있는 유틸이다.
 */
public class ArticleUtil {
    public static final String TAG = "OldArticleUtil";

    /**
     * 판다즈 앱은 내장디비를 사용하지 않기 때문에 캐쉬화를 오직 메모리를 통해서만 구현한다.
     * 한번 로드되어진 글은 이 객체에 저장되고 재사용되어진다.
     */
    private List<Post> mCachedPosts;

    /**
     * 글의 캐쉬화를 위해 싱글톤을 사용한다.
     */
    private static volatile ArticleUtil mInstance;

    public static ArticleUtil getInstance() {
        if (mInstance == null) mInstance = new ArticleUtil();
        return mInstance;
    }

    public static void releaseInstance() {
        if (mInstance != null) {
            mInstance.release();
            mInstance = null;
        }
    }

    private void release() {
        this.mCachedPosts = null;
    }

    private ArticleUtil() {
        this.mCachedPosts = new ArrayList<>();
    }

    /**
     * 완성된 형태의 Post를 서버로부터 받아오고 메모리에 캐쉬화한다.
     *
     * @param postId    대상 글 ID
     * @param isRefresh 캐쉬값이 아닌 서버의 최신 값을 가져오기 위한 flag
     * @return null 타입의 Task 객체를 반환한다.
     */
    // TODO: 완성된 형태의 Post를 서버로부터 받아오고 메모리에 저장한다.
    public static Task<Post> getPost(final String postId, boolean isRefresh) {
        Log.i(TAG, "getUserPosts:" + postId);

        if (!isRefresh) {
            Post cachedPost = getInstance().loadCachedPost(postId);
            if (cachedPost != null) {
                return Tasks.forResult(cachedPost);
            }
        }

        return Tasks.call(Executors.newSingleThreadExecutor(), () -> {
            // TODO: 내가 라이크를 했는지 여부가 담겨야 한다.
            DatabaseReference postsRef = DatabaseManager.getArticlePostsRef().child(postId);
            Task<FIRPost> getPostTask = DatabaseManager.getValue(postsRef, FIRPost.class);

            FIRPost firPost = Tasks.await(getPostTask);
            Post post = new Post(firPost);

            Task<User> getUserTask = UserUtil.getUser(firPost.getUid());
            User user = Tasks.await(getUserTask);

            if (!getUserTask.isSuccessful()) {
                Exception e = getUserTask.getException();
                throw e;
            }
            post.setUser(user);
            // TODO: isLike, commentCount 값이 들어가야 함.
            getInstance().saveCachedPost(post);
            return post;
        });
    }

    // https://goo.gl/Yk3STe
    // Child에서 a라는 Key값의 Value를 기준으로 정렬을 하고
    // 해당 노드의 Key값이 d이며 a의 Value가 b인 노드를 검색한다.
    // key:d/value:{a=b}
    // final Query query = DatabaseManager.getArticlePostsRef().orderByChild("a").equalTo("b", "d");

    // https://goo.gl/scTvux
    // Child에서 a라는 Key값의 Value를 기준으로 정렬을 하고
    // a의 Value가 b인 노드를 검색한다.
    // key:d/value:{a=b}
    // final Query query = DatabaseManager.getArticlePostsRef().orderByChild("a").equalTo("b");


    /**
     * 완성된 형태의 Post 목록을 서버로부터 받아오고 메모리에 캐쉬화한다.
     * 탐색 탭(MainActivity에서 2번째 탭)에서 불러와지는 판다즈에 올라오는 전체 글에 대한 메서드이다.
     * 전체, 일반글, 판매글 3가지 형태의 글 목록을 불러올 수 있다.
     *
     * @param articleType 불러오고자 하는 게시글 타입
     *                    ARTICLE_TYPE_ALL=전체, ARTICLE_TYPE_FEED=일반글, ARTICLE_TYPE_TALENT=판매글
     * @param amount      불러올 목록 수
     * @param maxKey      Infinite scroll(or pagination)을 위해 기준점이 되는 글 ID를 의미한다.
     *                    null이 아닐 경우 해당 글 ID 이후(더 오래된 글)의 목록을 반환한다.
     * @return 완성된 Post 객체 목록이 담긴 Task 객체를 반환한다.
     */
    public static Task<List<Post>> getPosts(final int articleType, final int amount, final String maxKey) {
        Log.i(TAG, "getPosts:articleType:" + articleType + "|amount:" + amount + "|maxKey:" + maxKey);

        return Tasks.call(Executors.newSingleThreadExecutor(), () -> {
            Query query = null;
            if (articleType == ARTICLE_TYPE_ALL) {
                query = DatabaseManager.getArticlePostsRef();
            } else if (articleType == ARTICLE_TYPE_FEED) {
                query = DatabaseManager.getArticlePostsFeedRef();
            } else if (articleType == ARTICLE_TYPE_TALENT) {
                query = DatabaseManager.getArticlePostsTalentRef();
            }

            if (maxKey != null) {
                query = query.orderByKey().endAt(maxKey).limitToLast(amount + 1);
            } else {
                query = query.orderByKey().limitToLast(amount);
            }

            Task<DataSnapshot> getTask = DatabaseManager.getValue(query);
            DataSnapshot dataSnapshot = Tasks.await(getTask);

            /*if (!getTask.isSuccessful()) {
                Exception e = getTask.getException();
                Log.w(TAG, "getPosts:getTask:ERROR", e);
                throw e;
            }*/

            Log.d(TAG, "getPosts:key:" + dataSnapshot.getKey() + "|count:" + dataSnapshot.getChildrenCount());

            List<Post> posts = new ArrayList<>();

            // 데이타가 없을 때는 종료한다.
            if (!dataSnapshot.exists() || dataSnapshot.getChildrenCount() == 0) {
                Log.d(TAG, "getPosts:no_data");
                return posts;
            }

            Iterable<DataSnapshot> dataSnapshots = dataSnapshot.getChildren();
            List<DataSnapshot> dataSnapshotList = new ArrayList<>();
            for (DataSnapshot snapshot : dataSnapshots) {
                if (snapshot.getKey().equals(maxKey)) continue;
                dataSnapshotList.add(snapshot);
            }
            Collections.reverse(dataSnapshotList);
            for (DataSnapshot snapshot : dataSnapshotList) {
                FIRPost firPost = snapshot.getValue(FIRPost.class);
                firPost.setKey(snapshot.getKey());

                int index = dataSnapshotList.indexOf(snapshot);
                Log.d(TAG, "getPosts:loop:index:" + index + "|child:" + snapshot.toString());
                Log.d(TAG, "getPosts:loop:index:" + index + "|firPost:" + firPost.toMap());

                // TODO: 완전한 Post 객체를 만드는 작업이 추가되어야 한다.
                Post post = new Post(firPost);

                Task<User> getUserTask = UserUtil.getUser(firPost.getUid());
                // OldUser user = Tasks.await(getUserTask, 100000, TimeUnit.MILLISECONDS);
                User user = Tasks.await(getUserTask);
                if (!getUserTask.isSuccessful()) {
                    Exception e = getUserTask.getException();
                    Log.w(TAG, "getPosts:getUserTask:ERROR", e);
                    throw e;
                }
                post.setUser(user);
                // TODO: 좋아요를 클릭했는지 여부, 댓글 개수
                posts.add(post);
                getInstance().saveCachedPost(post);
            }
            return posts;
        });
    }

    public static Task<List<Post>> getPostList(int amount, String cursor) {
        Log.i(TAG, "getPostList:amount:" + amount + "|cursor:" + cursor);

        Query query = FirebaseDatabase.getInstance().getReference().child("posts");

        query = query.orderByKey();
        if (cursor != null) {
            query = query.endAt(cursor);
            amount = amount + 1;
        }
        query = query.limitToLast(amount);

        return DatabaseManager.getValue(query).continueWith(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "getPostList:ERROR:" + task.getException().getMessage());
                throw task.getException();
            }
            DataSnapshot dataSnapshot = task.getResult();

            List<Post> posts = new ArrayList<>();

            // 데이타가 없을 때는 종료한다.
            if (!dataSnapshot.exists() || dataSnapshot.getChildrenCount() == 0) {
                Log.d(TAG, "getPostList:no_data");
                return posts;
            }

            Iterable<DataSnapshot> dataSnapshots = dataSnapshot.getChildren();
            List<DataSnapshot> dataSnapshotList = new ArrayList<>();
            for (DataSnapshot snapshot : dataSnapshots) {
                if (snapshot.getKey().equals(cursor)) continue;
                dataSnapshotList.add(snapshot);
            }
            Collections.reverse(dataSnapshotList);
            for (DataSnapshot snapshot : dataSnapshotList) {
                Post post = snapshot.getValue(Post.class);
                post.setKey(snapshot.getKey());
            }
            return posts;
        });
    }

    /**
     * 해당 유저의 완성된 형태의 Post 목록을 서버로부터 받아오고 메모리에 캐쉬화한다.
     * 프로필 페이지에 보이는 유저가 올린 글에 대한 메서드이다.
     * 전체, 일반글, 판매글 3가지 형태의 글 목록을 불러올 수 있다.
     *
     * @param targetUid   대상 유저 ID
     * @param articleType 불러오고자 하는 게시글 타입
     *                    ARTICLE_TYPE_ALL=전체, ARTICLE_TYPE_FEED=일반글, ARTICLE_TYPE_TALENT=판매글
     * @param amount      불러올 목록 수
     * @param maxKey      Infinite scroll(or pagination)을 위해 기준점이 되는 글 ID를 의미한다.
     *                    null이 아닐 경우 해당 글 ID 이후(더 오래된 글)의 목록을 반환한다.
     * @return 완성된 Post 객체 목록이 담긴 Task 객체를 반환한다.
     */
    public static Task<List<Post>> getUserPosts(final String targetUid, final int articleType, final int amount, final String maxKey) {
        Log.i(TAG, "getUserPosts:targetUid:" + targetUid + "/amount:" + amount + "/maxKey:" + maxKey);

        return Tasks.call(Executors.newSingleThreadExecutor(), () -> {
            Query query = null;
            if (articleType == ARTICLE_TYPE_ALL) {
                query = DatabaseManager.getArticleUserPostsRef().child(targetUid);
            } else if (articleType == ARTICLE_TYPE_FEED) {
                query = DatabaseManager.getArticleUserPostsFeedRef().child(targetUid);
            } else if (articleType == ARTICLE_TYPE_TALENT) {
                query = DatabaseManager.getArticleUserPostsTalentRef().child(targetUid);
            }

            // maxKey가 존재하지 않을 경우에는 다른 query를 사용해야 한다.
            if (maxKey != null) {
                query = query.orderByKey().endAt(maxKey).limitToLast(amount + 1);
            } else {
                query = query.orderByKey().limitToLast(amount);
            }

            Task<DataSnapshot> getTask = DatabaseManager.getValue(query);
            DataSnapshot dataSnapshot = Tasks.await(getTask);

            if (!getTask.isSuccessful()) {
                Exception e = getTask.getException();
                Log.w(TAG, "getTask:ERROR", e);
                throw e;
            }

            Log.d(TAG, "getUserPosts:key:" + dataSnapshot.getKey() + "|count:" + dataSnapshot.getChildrenCount());

            List<Post> posts = new ArrayList<>();

            // 데이타가 없을 때는 종료한다.
            if (!dataSnapshot.exists() || dataSnapshot.getChildrenCount() == 0) {
                Log.d(TAG, "getUserPosts:no_data");
                return posts;
            }

            Iterable<DataSnapshot> dataSnapshots = dataSnapshot.getChildren();
            List<DataSnapshot> dataSnapshotList = new ArrayList<>();
            for (DataSnapshot snapshot : dataSnapshots) {
                if (snapshot.getKey().equals(maxKey)) continue;
                dataSnapshotList.add(snapshot);
            }
            Collections.reverse(dataSnapshotList);
            for (DataSnapshot snapshot : dataSnapshotList) {
                FIRPost firPost = snapshot.getValue(FIRPost.class);
                firPost.setKey(snapshot.getKey());
                // TODO: 완전한 Post 객체를 만드는 작업이 추가되어야 한다.
                // 현재 User가 없는 상태
                Post post = new Post(firPost);

                Task<User> getUserTask = UserUtil.getUser(firPost.getUid());
                // OldUser user = Tasks.await(getUserTask, 100000, TimeUnit.MILLISECONDS);
                User user = Tasks.await(getUserTask);
                if (!getUserTask.isSuccessful()) {
                    Exception e = getUserTask.getException();
                    Log.w(TAG, "getUserTask:ERROR", e);
                    throw e;
                }
                post.setUser(user);
                // TODO: 좋아요를 클릭했는지 여부, 댓글 개수
                posts.add(post);
                getInstance().saveCachedPost(post);
            }
            return posts;
        });
    }

    /**
     * 해당 유저가 찜한 완성된 형태의 Post 목록을 서버로부터 받아오고 메모리에 캐쉬화한다.
     * 프로필 페이지에 보이는 유저가 찜한 글에 대한 메서드이다.
     *
     * @param targetUid 대상 유저 ID
     * @param amount    불러올 목록 수
     * @param maxKey    Infinite scroll(or pagination)을 위해 기준점이 되는 글 ID를 의미한다.
     *                  null이 아닐 경우 해당 글 ID 이후(더 오래된 글)의 목록을 반환한다.
     * @return 완성된 Post 객체 목록이 담긴 Task 객체를 반환한다.
     */
    public static Task<List<Post>> getUserSaves(final String targetUid, final int amount, final String maxKey) {
        Log.i(TAG, "getUserSaves:targetUid:" + targetUid + "|amount:" + amount + "|maxKey:" + maxKey);

        return Tasks.call(Executors.newSingleThreadExecutor(), () -> {
            Query query = DatabaseManager.getArticleUserSavesRef().child(targetUid);

            // maxKey가 존재하지 않을 경우에는 다른 query를 사용해야 한다.
            if (maxKey != null) {
                query = query.orderByKey().endAt(maxKey).limitToLast(amount + 1);
            } else {
                query = query.orderByKey().limitToLast(amount);
            }

            Task<DataSnapshot> getTask = DatabaseManager.getValue(query);
            DataSnapshot dataSnapshot = Tasks.await(getTask);

            Log.d(TAG, "getUserSaves:key:" + dataSnapshot.getKey() + "|count:" + dataSnapshot.getChildrenCount());

            List<Post> posts = new ArrayList<>();

            // 데이타가 없을 때는 종료한다.
            if (!dataSnapshot.exists() || dataSnapshot.getChildrenCount() == 0) {
                Log.d(TAG, "getUserSaves:no_data");
                return posts;
            }

            Iterable<DataSnapshot> dataSnapshots = dataSnapshot.getChildren();
            List<DataSnapshot> dataSnapshotList = new ArrayList<>();
            for (DataSnapshot snapshot : dataSnapshots) {
                if (snapshot.getKey().equals(maxKey)) continue;
                dataSnapshotList.add(snapshot);
            }
            Collections.reverse(dataSnapshotList);
            for (DataSnapshot snapshot : dataSnapshotList) {
                FIRPost firPost = snapshot.getValue(FIRPost.class);
                firPost.setKey(snapshot.getKey());
                // TODO: 완전한 Post 객체를 만드는 작업이 추가되어야 한다.
                // 현재 User가 없는 상태
                Post post = new Post(firPost);

                Task<User> getUserTask = UserUtil.getUser(firPost.getUid());
                // OldUser user = Tasks.await(getUserTask, 100000, TimeUnit.MILLISECONDS);
                User user = Tasks.await(getUserTask);

                post.setUser(user);
                // TODO: 좋아요를 클릭했는지 여부, 댓글 개수
                posts.add(post);
                getInstance().saveCachedPost(post);
            }
            return posts;
        });
    }

    /**
     * 해당 유저의 타임라인의 완성된 형태의 Post 목록을 서버로부터 받아오고 메모리에 캐쉬화한다.
     * 타임라인 탭(MainActivity에서 첫번째 탭)에 보이는 유저가 올린 글에 대한 메서드이다.
     *
     * @param amount    불러올 목록 수
     * @param maxKey    Infinite scroll(or pagination)을 위해 기준점이 되는 글 ID를 의미한다.
     *                  null이 아닐 경우 해당 글 ID 이후(더 오래된 글)의 목록을 반환한다.
     * @param isRefresh 캐쉬값이 아닌 서버의 최신 값을 가져오기 위한 flag (아직 구현 못함)
     * @return 완성된 Post 객체 목록이 담긴 Task 객체를 반환한다.
     */
    public static Task<List<Post>> getTimelinePosts(final int amount, final String maxKey, boolean isRefresh) {
        Log.i(TAG, "getTimelinePosts:amount:" + amount + "|maxKey:" + maxKey);

        return Tasks.call(Executors.newSingleThreadExecutor(), () -> {
            Query query = DatabaseManager.getArticleTimelineRef().child(AuthManager.getUserId());

            // maxKey가 존재하지 않을 경우에는 다른 query를 사용해야 한다.
            if (maxKey != null) {
                query = query.orderByKey().endAt(maxKey).limitToLast(amount + 1);
            } else {
                query = query.orderByKey().limitToLast(amount);
            }

            Task<DataSnapshot> getTimelineTask = DatabaseManager.getValue(query);
            DataSnapshot dataSnapshot = Tasks.await(getTimelineTask);

            if (!getTimelineTask.isSuccessful()) {
                Exception e = getTimelineTask.getException();
                Log.w(TAG, "getTimelineTask:ERROR", e);
                throw e;
            }

            Log.d(TAG, "getTimelinePosts:key:" + dataSnapshot.getKey() + "|count:" + dataSnapshot.getChildrenCount());

            List<Post> posts = new ArrayList<>();

            // 데이타가 없을 때는 종료한다.
            if (!dataSnapshot.exists() || dataSnapshot.getChildrenCount() == 0) {
                Log.d(TAG, "getTimelinePosts:no_data");
                return posts;
            }

            Iterable<DataSnapshot> dataSnapshots = dataSnapshot.getChildren();
            List<DataSnapshot> dataSnapshotList = new ArrayList<>();
            for (DataSnapshot snapshot : dataSnapshots) {
                if (snapshot.getKey().equals(maxKey)) continue;
                dataSnapshotList.add(snapshot);
            }
            Collections.reverse(dataSnapshotList);
            for (DataSnapshot snapshot : dataSnapshotList) {
                FIRPost firPost = snapshot.getValue(FIRPost.class);
                firPost.setKey(snapshot.getKey());
                // TODO: 완전한 Post 객체를 만드는 작업이 추가되어야 한다.
                // 현재 User가 없는 상태
                Post post = new Post(firPost);

                Task<User> getUserTask = UserUtil.getUser(firPost.getUid(), true);
                // OldUser user = Tasks.await(getUserTask, 100000, TimeUnit.MILLISECONDS);
                User user = Tasks.await(getUserTask);
                post.setUser(user);
                // TODO: 좋아요를 클릭했는지 여부, 댓글 개수
                posts.add(post);
                getInstance().saveCachedPost(post);
            }
            return posts;
        }).continueWith(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "getTimelinePosts:ERROR:", task.getException());
                throw task.getException();
            }
            List<Post> posts = task.getResult();
            return posts;
        });
    }

    /**
     * 일반 글을 삽입한다.
     *
     * @param type
     * @param imageUris
     * @param content
     * @param hashtags
     * @param mentions
     * @return
     */
    public static Task<Void> insert(final int type,
                                    final List<Uri> imageUris,
                                    final String content,
                                    final List<String> hashtags,
                                    final List<String> mentions) {
        // TODO: 피드인지 재능인지에 따라서 다른 멤버 객체가 사용된다.

        final String postId = DatabaseManager.getRef()
                .child(DATABASE_PATH_ARTICLE)
                .child(DATABASE_PATH_ARTICLE_POSTS)
                .push().getKey();

        StorageReference postsRef = StorageManager.getArticlePostsRef().child(postId);

        // 닉네임을 가져오는 작업은 사실 서버에서 처리해도 된다 (트리거 중복 호출 이슈 존재함)
        final Task<String> getUserNicknameTask = UserUtil.getUserNickname(AuthManager.getUserId());
        final Task<List<String>> putFilesTask = StorageManager.putFilesFix(postsRef, imageUris);

        return Tasks.whenAll(getUserNicknameTask, putFilesTask).continueWithTask(task -> {
            if (!getUserNicknameTask.isSuccessful()) {
                Log.w(TAG, "getUserTask:ERROR:", getUserNicknameTask.getException());
            }
            Log.d(TAG, "getUserTask:SUCCESS");

            if (!putFilesTask.isSuccessful()) {
                Log.w(TAG, "putFilesTask:ERROR:", putFilesTask.getException());
                throw putFilesTask.getException();
            }
            Log.d(TAG, "putFilesTask:SUCCESS");

            String nickname = getUserNicknameTask.getResult();

            List<String> filenames = putFilesTask.getResult();
            Log.d(TAG, "putFilesTask:filenames:" + filenames.toString());

            // 새로운 글에 대한 객체 생성.
            FIRPost firPost = new FIRPost(type, AuthManager.getUserId(), nickname, content, filenames);

            // Hashtag (반드시 필요한가?)
            firPost.setHashtags(hashtags);
            firPost.setMentions(mentions);

            DatabaseReference postsRef1 = DatabaseManager.getArticlePostsRef().child(postId);
            return DatabaseManager.setValue(postsRef1, firPost);
        });
    }

    /**
     * 판매글을 삽입한다.
     *
     * @param type
     * @param imageUris
     * @param content
     * @param hashtags
     * @param mentions
     * @param productType
     * @param productTitle
     * @param productPrice
     * @param shippingPrice
     * @param productServices
     * @return
     */
    public static Task<Void> insert(final int type,
                                    List<Uri> imageUris,
                                    final String content,
                                    final List<String> hashtags,
                                    final List<String> mentions,
                                    final int productType,
                                    final String productTitle,
                                    final String productPrice,
                                    final String shippingPrice,
                                    final String productServices) {

        // TODO: 피드인지 재능인지에 따라서 다른 멤버 객체가 사용된다.

        final String postId = DatabaseManager.getRef()
                .child(DATABASE_PATH_ARTICLE)
                .child(DATABASE_PATH_ARTICLE_POSTS)
                .push().getKey();

        StorageReference postsRef = StorageManager.getArticlePostsRef().child(postId);

        // 닉네임을 가져오는 작업은 사실 서버에서 처리해도 된다 (트리거 중복 호출 이슈 존재함)
        final Task<String> getUserNicknameTask = UserUtil.getUserNickname(AuthManager.getUserId());
        final Task<List<String>> putFilesTask = StorageManager.putFilesFix(postsRef, imageUris);

        return Tasks.whenAll(getUserNicknameTask, putFilesTask).continueWithTask(task -> {
            if (!getUserNicknameTask.isSuccessful()) {
                Log.w(TAG, "getUserTask:ERROR:", getUserNicknameTask.getException());
            }
            Log.d(TAG, "getUserTask:SUCCESS");

            if (!putFilesTask.isSuccessful()) {
                Log.w(TAG, "putFilesTask:ERROR:", putFilesTask.getException());
                throw putFilesTask.getException();
            }
            Log.d(TAG, "putFilesTask:SUCCESS");

            String nickname = getUserNicknameTask.getResult();

            List<String> filenames = putFilesTask.getResult();
            Log.d(TAG, "putFilesTask:filenames:" + filenames.toString());

            // 새로운 글에 대한 객체 생성.
            FIRPost firPost = new FIRPost(
                    type,
                    AuthManager.getUserId(),
                    nickname,
                    content,
                    filenames,
                    productType,
                    productTitle,
                    productPrice,
                    shippingPrice,
                    productServices
            );

            // Hashtag (반드시 필요한가?)
            firPost.setHashtags(hashtags);

            Log.d(TAG, "insert:firPost:" + firPost.toMap());

            DatabaseReference postsRef1 = DatabaseManager.getArticlePostsRef().child(postId);
            return DatabaseManager.setValue(postsRef1, firPost);
        });
    }

    /**
     * 일반글을 수정한다.
     *
     * @param postId
     * @param type
     * @param content
     * @param hashtags
     * @param mentions
     * @return
     */
    public static Task<Void> modify(final String postId,
                                    final int type,
                                    final String content,
                                    final List<String> hashtags,
                                    final List<String> mentions) {
        return modify(
                postId,
                type,
                content,
                hashtags,
                mentions,
                null,
                null,
                null,
                null,
                null);
    }

    /**
     * 판매글을 수정한다.
     *
     * @param postId
     * @param type
     * @param content
     * @param hashtags
     * @param mentions
     * @param productType
     * @param productTitle
     * @param productPrice
     * @param shippingPrice
     * @param productServices
     * @return
     */
    public static Task<Void> modify(final String postId,
                                    final Integer type,
                                    final String content,
                                    final List<String> hashtags,
                                    final List<String> mentions,
                                    final Integer productType,
                                    final String productTitle,
                                    final String productPrice,
                                    final String shippingPrice,
                                    final String productServices) {
        DatabaseReference postsRef = DatabaseManager.getArticlePostsRef().child(postId);
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("type", type);
        childUpdates.put("content", content);
        Map<String, Object> hashtagMap = new HashMap<>();
        for (String hashtag : hashtags) hashtagMap.put(hashtag, true);
        childUpdates.put("hashtags", hashtagMap);
        Map<String, Object> mentionMap = new HashMap<>();
        for (String mention : mentions) mentionMap.put(mention, true);
        childUpdates.put("mentions", mentionMap);
        childUpdates.put("productType", productType);
        childUpdates.put("productTitle", productTitle);
        childUpdates.put("productPrice", productPrice);
        childUpdates.put("shippingPrice", shippingPrice);
        childUpdates.put("productServices", productServices);
        childUpdates.values().removeAll(Collections.singleton(null));
        return DatabaseManager.updateChildren(postsRef, childUpdates);
    }

    public static Task<Void> delete(String postId) {
        DatabaseReference postRef = DatabaseManager.getArticlePostsRef().child(postId);
        return DatabaseManager.removeValue(postRef);
    }

    public static Task<Void> delete(final Post post) {
        Log.i(TAG, "delete:post:" + (post != null ? post.toMap() : null));
        final String postId = post.getKey();
        DatabaseReference postRef = DatabaseManager.getArticlePostsRef().child(postId);
        return DatabaseManager.removeValue(postRef).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            StorageReference folderRef = StorageManager.getArticlePostsRef().child(postId);
            List<String> filenames = post.getPhotosList();
            return StorageManager.delete(folderRef, filenames);
        });
    }

    // TODO: 이미 좋아하고 있다면 어떻게 할까?
    // 값을 넣기 전에 이미 좋아요클 클릭했는지 여부를 확인하고 값을 넣는다.
    // TODO: 서버에서 확인하도록 변경.

    /**
     * 해당 글에 좋아요를 한다.
     *
     * @param postId
     * @return
     */
    public static Task<Integer> like(final String postId) {
        DatabaseReference postsRef = DatabaseManager.getArticlePostsRef().child(postId);
        final String uid = AuthManager.getUserId();
        final AtomicReference<FIRPost> atomicFirPost = new AtomicReference<>();
        final AtomicBoolean isAlreadyLiked = new AtomicBoolean(false);
        return DatabaseManager.runTransaction(postsRef, new DatabaseManager.TransactionHandler() {
            final String TAG = "TransactionHandler";

            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Log.i(TAG, "doTransaction:mutableData:" + mutableData.toString());
                // doTransaction은 key를 반환해주지 않는다.
                FIRPost firPost = DatabaseManager.getValue(mutableData, postId, FIRPost.class);
                Log.d(TAG, "doTransaction:firPost:key:" + (firPost != null ? firPost.getKey() : null));
                Log.d(TAG, "doTransaction:firPost:" + (firPost != null ? firPost.toMap() : null));
                if (firPost == null) {
                    return Transaction.success(mutableData);
                }
                // 이미 좋아요 하고 있다면 그냥 성공했다고 반환해준다.
                if (firPost.getLikes().containsKey(uid)) {
                    isAlreadyLiked.set(true);
                    atomicFirPost.set(firPost);
                    return Transaction.success(mutableData);
                }
                firPost.setLikeCount(firPost.getLikeCount() + 1);
                firPost.getLikes().put(uid, true);
                mutableData.setValue(firPost);
                atomicFirPost.set(firPost);
                return Transaction.success(mutableData);
            }
        }).continueWith(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "like:ERROR:", task.getException());
                throw task.getException();
            }
            FIRPost newFirPost = atomicFirPost.get();
            Log.d(TAG, "like:SUCCESS:newFirPost:" + (newFirPost != null ? newFirPost.toMap() : null));
            int likeCount;
            if (newFirPost == null) {
                likeCount = 0;
            } else {
                /*if (!isAlreadyLiked.get()) {
                    newFirPost.setLikeCount(newFirPost.getLikeCount() + 1);
                }*/
                ArticleUtil.getInstance().saveCachedPost(newFirPost);
                likeCount = newFirPost.getLikeCount();
            }
            return likeCount;
        });
    }

    // 문제는 cachedPost에 값을 업데이트 해줘야 된다는 점이다.

    /**
     * 해당 글의 좋아요를 취소한다.
     *
     * @param postId
     * @return
     */
    public static Task<Integer> unlike(final String postId) {
        DatabaseReference postsRef = DatabaseManager.getArticlePostsRef().child(postId);
        final String uid = AuthManager.getUserId();
        final AtomicReference<FIRPost> atomicFirPost = new AtomicReference<>();
        final AtomicBoolean isAlreadyLiked = new AtomicBoolean(true);
        return DatabaseManager.runTransaction(postsRef, mutableData -> {
            FIRPost firPost = DatabaseManager.getValue(mutableData, postId, FIRPost.class);
            Log.d(TAG, "doTransaction:firPost:key:" + (firPost != null ? firPost.getKey() : null));
            Log.d(TAG, "doTransaction:firPost:" + (firPost != null ? firPost.toMap() : null));
            if (firPost == null) {
                return Transaction.success(mutableData);
            }
            // 이미 언라이크 상태라면 그냥 성공했다고 반환해준다.
            if (!firPost.getLikes().containsKey(uid)) {
                isAlreadyLiked.set(false);
                atomicFirPost.set(firPost);
                return Transaction.success(mutableData);
            }
            firPost.setLikeCount(firPost.getLikeCount() != 0 ? firPost.getLikeCount() - 1 : 0);
            firPost.getLikes().remove(uid);
            mutableData.setValue(firPost);
            atomicFirPost.set(firPost);
            return Transaction.success(mutableData);
        }).continueWith(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "unlike:ERROR:", task.getException());
                throw task.getException();
            }
            Log.d(TAG, "unlike:SUCCESS:");
            int likeCount;
            if (atomicFirPost.get() == null) {
                likeCount = 0;
            } else {
                FIRPost newFirPost = atomicFirPost.get();
                /*if (isAlreadyLiked.get() && newFirPost.getLikeCount() != 0) {
                    newFirPost.setLikeCount(newFirPost.getLikeCount() - 1);
                }*/
                ArticleUtil.getInstance().saveCachedPost(newFirPost);
                likeCount = newFirPost.getLikeCount();
            }
            return likeCount;
        });
    }

    /**
     * 해당 글을 찜한다.
     *
     * @param postId
     * @return
     */
    public static Task<Integer> save(final String postId) {
        DatabaseReference postRef = DatabaseManager.getArticlePostsRef().child(postId);
        final String uid = AuthManager.getUserId();
        final AtomicReference<FIRPost> atomicFirPost = new AtomicReference<>();
        final AtomicBoolean isAlreadySaved = new AtomicBoolean(false);
        return DatabaseManager.runTransaction(postRef, new DatabaseManager.TransactionHandler() {
            final String TAG = "TransactionHandler";

            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Log.i(TAG, "doTransaction:mutableData:" + mutableData.toString());
                // doTransaction은 key를 반환해주지 않는다.
                FIRPost firPost = DatabaseManager.getValue(mutableData, postId, FIRPost.class);
                Log.d(TAG, "doTransaction:firPost:key:" + (firPost != null ? firPost.getKey() : null));
                Log.d(TAG, "doTransaction:firPost:" + (firPost != null ? firPost.toMap() : null));
                if (firPost == null) {
                    return Transaction.success(mutableData);
                }
                // 이미 좋아요 하고 있다면 그냥 성공했다고 반환해준다.
                if (firPost.getSaves().containsKey(uid)) {
                    isAlreadySaved.set(true);
                    atomicFirPost.set(firPost);
                    return Transaction.success(mutableData);
                }
                firPost.setSaveCount(firPost.getSaveCount() + 1);
                firPost.getSaves().put(uid, true);
                mutableData.setValue(firPost);
                atomicFirPost.set(firPost);
                return Transaction.success(mutableData);
            }
        }).continueWith(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "save:ERROR:", task.getException());
                throw task.getException();
            }
            FIRPost newFirPost = atomicFirPost.get();
            Log.d(TAG, "save:SUCCESS:newFirPost:" + (newFirPost != null ? newFirPost.toMap() : null));
            int saveCount;
            if (newFirPost == null) {
                saveCount = 0;
            } else {
                /*if (!isAlreadyLiked.get()) {
                    newFirPost.setLikeCount(newFirPost.getLikeCount() + 1);
                }*/
                ArticleUtil.getInstance().saveCachedPost(newFirPost);
                saveCount = newFirPost.getSaveCount();
            }
            return saveCount;
        });
    }

    /**
     * 해당 글의 찜을 취소한다.
     *
     * @param postId
     * @return
     */
    public static Task<Integer> unsave(final String postId) {
        DatabaseReference postsRef = DatabaseManager.getArticlePostsRef().child(postId);
        final String uid = AuthManager.getUserId();
        final AtomicReference<FIRPost> atomicFirPost = new AtomicReference<>();
        final AtomicBoolean isAlreadySaved = new AtomicBoolean(true);
        return DatabaseManager.runTransaction(postsRef, mutableData -> {
            FIRPost firPost = DatabaseManager.getValue(mutableData, postId, FIRPost.class);
            Log.d(TAG, "doTransaction:firPost:key:" + (firPost != null ? firPost.getKey() : null));
            Log.d(TAG, "doTransaction:firPost:" + (firPost != null ? firPost.toMap() : null));
            if (firPost == null) {
                return Transaction.success(mutableData);
            }
            // 이미 언라이크 상태라면 그냥 성공했다고 반환해준다.
            if (!firPost.getSaves().containsKey(uid)) {
                isAlreadySaved.set(false);
                atomicFirPost.set(firPost);
                return Transaction.success(mutableData);
            }
            firPost.setSaveCount(firPost.getSaveCount() != 0 ? firPost.getSaveCount() - 1 : 0);
            firPost.getSaves().remove(uid);
            mutableData.setValue(firPost);
            atomicFirPost.set(firPost);
            return Transaction.success(mutableData);
        }).continueWith(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "unsave:ERROR:", task.getException());
                throw task.getException();
            }
            Log.d(TAG, "unsave:SUCCESS:");
            int saveCount;
            if (atomicFirPost.get() == null) {
                saveCount = 0;
            } else {
                FIRPost newFirPost = atomicFirPost.get();
                /*if (isAlreadyLiked.get() && newFirPost.getLikeCount() != 0) {
                    newFirPost.setLikeCount(newFirPost.getLikeCount() - 1);
                }*/
                ArticleUtil.getInstance().saveCachedPost(newFirPost);
                saveCount = newFirPost.getSaveCount();
            }
            return saveCount;
        });
    }

    // TODO: 캐쉬에 해당 Post가 저장되어 있지 않다면 아무 작업도 하지 않는다.
    // 캐쉬에 기존 객체가 존재할 때만 덮어씌우는 메서드이다.
    public synchronized void saveCachedPost(FIRPost newFirPost) {
        // Log.i(TAG, "saveCachedPost:newFirPost:" + newFirPost.toMap());
        String postId = newFirPost.getKey();
        Post cachedPost = loadCachedPost(postId);
        if (cachedPost == null) {
            Log.w(TAG, "saveCachedPost:cached_post_not_exists_for_target_post_id");
            return;
        }
        Post newPost = new Post(newFirPost);
        newPost.setUser(cachedPost.getUser());
        newPost.setLike(cachedPost.isLike());
        newPost.setCommentCount(cachedPost.getCommentCount());
        newPost.setLatestComments(cachedPost.getLatestComments());
        saveCachedPost(newPost);
    }

    /**
     * 완성된 형태의 Post 객체를 메모리에 저장한다.
     * 반드시 완성된 형태의 모델 객체만 메모리에 저장한다 (Post => o, FIRPost => x)
     *
     * @param newPost 완성된 형태의 Post 객체
     */
    public synchronized void saveCachedPost(Post newPost) {
        // Log.i(TAG, "saveCachedPost:newPost:" + newPost.toMap());
        boolean isExists = false;
        // Log.d(TAG, "saveCachedPost:cachedPost:" + mCachedPosts);
        for (Post cachedPost : mCachedPosts) {
            Log.d(TAG, "saveCachedPost:loop:cachedPost:" + (cachedPost != null ? cachedPost.toMap() : null));
            if (newPost.getKey().equals(cachedPost.getKey())) {
                isExists = true;
                int index = mCachedPosts.indexOf(cachedPost);
                mCachedPosts.set(index, newPost);
                break;
            }
        }
        if (!isExists) mCachedPosts.add(newPost);
    }

    /**
     * 캐쉬에 저장된 Post 객체를 반환한다.
     *
     * @param postId
     * @return
     */
    public synchronized Post loadCachedPost(@NonNull String postId) {
        // Log.i(TAG, "loadCachedPost:cachedPosts:" + mCachedPosts.toString());
        for (Post post : mCachedPosts) {
            // Log.i(TAG, "loadCachedPost:loop:post:" + post.toMap());
            if (postId.equals(post.getKey())) {
                return post;
            }
        }
        return null;
    }
}