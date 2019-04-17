package com.chaigene.petnolja.util;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.chaigene.petnolja.manager.AuthManager;
import com.chaigene.petnolja.manager.DatabaseManager;
import com.chaigene.petnolja.model.Comment;
import com.chaigene.petnolja.model.FIRComment;
import com.chaigene.petnolja.model.Post;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

public class CommentUtil {
    public static final String TAG = "CommentUtil";

    /**
     * 해당 글의 댓글 개수를 반환한다.
     *
     * @param postId
     * @return
     */
    public static Task<Integer> getCount(String postId) {
        Log.i(TAG, "getCount:postId:" + postId);

        Query query = DatabaseManager.getArticleCommentCountRef().child(postId);
        return DatabaseManager.getValue(query).continueWith(new Continuation<DataSnapshot, Integer>() {
            @Override
            public Integer then(@NonNull Task<DataSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    Exception e = task.getException();
                    Log.w(TAG, "getCount:ERROR", e);
                    throw e;
                }

                DataSnapshot dataSnapshot = task.getResult();
                if (!dataSnapshot.exists()) return 0;

                return dataSnapshot.getValue(Integer.class);
            }
        });
    }

    /**
     * 해당 글의 댓글 목록을 반환한다.
     *
     * @param postId
     * @return
     */
    public static Task<List<Comment>> getComments(@NonNull String postId) {
        Log.i(TAG, "getComments:postId:" + postId);
        Query query = DatabaseManager.getArticleCommentsRef().child(postId).orderByKey();
        return DatabaseManager.getValue(query).continueWith(new Continuation<DataSnapshot, List<Comment>>() {
            @Override
            public List<Comment> then(@NonNull Task<DataSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    Exception e = task.getException();
                    Log.w(TAG, "getComments:ERROR", e);
                    throw e;
                }

                DataSnapshot dataSnapshot = task.getResult();
                Log.d(TAG, "getComments:key:" + dataSnapshot.getKey() + "|count:" + dataSnapshot.getChildrenCount());

                List<Comment> comments = new ArrayList<>();

                // 데이타가 없을 때는 종료한다.
                if (!dataSnapshot.exists() || dataSnapshot.getChildrenCount() == 0) {
                    Log.d(TAG, "getComments:no_data");
                    return comments;
                }

                Iterable<DataSnapshot> dataSnapshots = dataSnapshot.getChildren();
                List<DataSnapshot> dataSnapshotList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshots) {
                    dataSnapshotList.add(snapshot);
                }
                for (DataSnapshot snapshot : dataSnapshotList) {
                    FIRComment firComment = DatabaseManager.getValue(snapshot, FIRComment.class);
                    // TODO: 완전한 Comment 객체를 만드는 작업이 추가되어야 한다. (현재까지는 이미 완전함)
                    Comment comment = new Comment(firComment);
                    comments.add(comment);
                }
                return comments;
            }
        });
    }

    /**
     * 해당 글의 댓글 목록을 반환한다.
     * 특정 개수만 반환하거나 infinite scroll(혹은 pagination)이 필요한 경우 이 메서드를 사용해야 한다.
     *
     * @param postId
     * @param amount
     * @param maxKey
     * @return
     */
    public static Task<List<Comment>> get(@NonNull String postId, @IntRange(from = 1) int amount, @Nullable final String maxKey) {
        Log.i(TAG, "getUserPosts:postId:" + postId + "/amount:" + amount + "/maxKey:" + maxKey);

        Query query = DatabaseManager.getArticleCommentsRef().child(postId);

        // maxKey가 존재하지 않을 경우에는 다른 query를 사용해야 한다.
        if (maxKey != null) {
            query = query.orderByKey().endAt(maxKey).limitToLast(amount + 1);
        } else {
            query = query.orderByKey().limitToLast(amount);
        }

        return DatabaseManager.getValue(query).continueWith(new Continuation<DataSnapshot, List<Comment>>() {
            @Override
            public List<Comment> then(@NonNull Task<DataSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    Exception e = task.getException();
                    Log.w(TAG, "getUserPosts:ERROR", e);
                    throw e;
                }

                DataSnapshot dataSnapshot = task.getResult();
                Log.d(TAG, "getUserPosts:key:" + dataSnapshot.getKey() + "/count:" + dataSnapshot.getChildrenCount());

                List<Comment> comments = new ArrayList<>();

                // 데이타가 없을 때는 종료한다.
                if (!dataSnapshot.exists() || dataSnapshot.getChildrenCount() == 0) {
                    Log.d(TAG, "getUserPosts:no_data");
                    return comments;
                }

                Iterable<DataSnapshot> dataSnapshots = dataSnapshot.getChildren();
                List<DataSnapshot> dataSnapshotList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshots) {
                    if (snapshot.getKey().equals(maxKey)) continue;
                    dataSnapshotList.add(snapshot);
                }
                for (DataSnapshot snapshot : dataSnapshotList) {
                    FIRComment firComment = DatabaseManager.getValue(snapshot, FIRComment.class);
                    // TODO: 완전한 Post 객체를 만드는 작업이 추가되어야 한다. (현재까지는 이미 완전함)
                    Comment comment = new Comment(firComment);
                    comments.add(comment);
                }

                return comments;
            }
        });
    }

    /**
     * 해당 글에 댓글을 삽입한다.
     *
     * @param postId
     * @param content
     * @return
     */
    public static Task<Comment> insert(final String postId, final String content) {
        Log.i(TAG, "insert:postId:" + postId + "/content:" + content);
        final String uid = AuthManager.getUserId();
        final DatabaseReference commentsRef = DatabaseManager.getArticleCommentsRef().child(postId);
        final String commentId = commentsRef.push().getKey();

        return Tasks.call(Executors.newSingleThreadExecutor(), new Callable<Comment>() {
            @Override
            public Comment call() throws Exception {
                Task<String> getUserNicknameTask = UserUtil.getUserNickname(uid);
                String nickname = Tasks.await(UserUtil.getUserNickname(uid));

                if (!getUserNicknameTask.isSuccessful()) {
                    Exception e = getUserNicknameTask.getException();
                    throw e;
                }

                // TODO: 임시 패치
                // 카카오 등으로 로그인해서 닉네임이 존재하지 않는 경우는 uid를 닉네임으로 대체한다.
                if (TextUtils.isEmpty(nickname)) nickname = uid;

                final FIRComment firComment = new FIRComment(commentId, uid, nickname, content);
                Task<Void> setCommentTask = commentsRef.child(commentId).setValue(firComment);
                Tasks.await(setCommentTask);

                if (!setCommentTask.isSuccessful()) {
                    Exception e = setCommentTask.getException();
                    throw e;
                }

                Comment comment = new Comment(firComment);
                comment.setTimestamp(CommonUtil.getTimeInMillis());
                return comment;
            }
        });
    }

    /**
     * 해당 글에 댓글을 삽입한다.
     * 댓글에 존재하는 해쉬태그 및 멘션을 함께 삽입하고자 할 경우 이 메서드를 사용해야 한다.
     *
     * @param postId
     * @param content
     * @param hashtags
     * @param mentions
     * @return
     */
    public static Task<Comment> insert(final String postId,
                                       final String content,
                                       final List<String> hashtags,
                                       final List<String> mentions) {
        Log.i(TAG, "insert:postId:" + postId + "|content:" + content);
        final String uid = AuthManager.getUserId();
        final DatabaseReference commentsRef = DatabaseManager.getArticleCommentsRef().child(postId);
        final String commentId = commentsRef.push().getKey();

        return Tasks.call(Executors.newSingleThreadExecutor(), new Callable<Comment>() {
            @Override
            public Comment call() throws Exception {
                Task<String> getUserNicknameTask = UserUtil.getUserNickname(uid);
                String nickname = Tasks.await(UserUtil.getUserNickname(uid));

                /*if (!getUserNicknameTask.isSuccessful()) {
                    Exception e = getUserNicknameTask.getException();
                    throw e;
                }*/

                // TODO: 임시 패치
                // 카카오 등으로 로그인해서 닉네임이 존재하지 않는 경우는 uid를 닉네임으로 대체한다.
                if (TextUtils.isEmpty(nickname)) nickname = uid;

                final FIRComment firComment = new FIRComment(commentId, uid, nickname, content);
                firComment.setHashtags(hashtags);
                firComment.setMentions(mentions);

                Task<Void> setCommentTask = commentsRef.child(commentId).setValue(firComment);
                Tasks.await(setCommentTask);

                /*if (!setCommentTask.isSuccessful()) {
                    Exception e = setCommentTask.getException();
                    throw e;
                }*/

                Comment comment = new Comment(firComment);
                comment.setTimestamp(CommonUtil.getTimeInMillis());
                return comment;
            }
        });
    }

    /**
     * 해당 댓글을 삭제한다.
     *
     * @param postId
     * @param commentId
     * @return
     */
    public static Task<Void> delete(final String postId, final String commentId) {
        Log.i(TAG, "delete:postId:" + postId + "|commentId:" + commentId);
        DatabaseReference commentRef = DatabaseManager.getArticleCommentsRef().child(postId).child(commentId);
        return DatabaseManager.removeValue(commentRef).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(@NonNull Task<Void> task) throws Exception {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "delete:ERROR:" + task.getException().getMessage());
                    throw task.getException();
                }
                Log.d(TAG, "delete:SUCCESS");

                // ArticleUtil의 캐쉬에서도 삭제해준다.
                Post cachedPost = OldArticleUtil.getInstance().loadCachedPost(postId);
                if (cachedPost == null) return null;
                Log.d(TAG, "delete:commentId:" + commentId);
                List<Comment> cachedComments = cachedPost.getLatestComments();
                for (Comment c : cachedPost.getLatestComments()) {
                    Log.d(TAG, "delete:comments:latestComments:loop:comment:" + c.toMap());
                }
                Iterator<Comment> iterator = cachedComments.iterator();
                //noinspection WhileLoopReplaceableByForEach
                while (iterator.hasNext()) {
                    Comment comment = iterator.next();
                    if (comment.getKey().equals(commentId)) iterator.remove();
                }
                return null;
            }
        }).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(@NonNull Task<Void> task) throws Exception {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "delete:save_to_post_cache:ERROR:", task.getException());
                    throw task.getException();
                }
                Log.d(TAG, "delete:save_to_post_cache:SUCCESS");
                return null;
            }
        });
    }
}