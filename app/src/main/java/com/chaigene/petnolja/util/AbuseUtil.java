package com.chaigene.petnolja.util;

import androidx.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.chaigene.petnolja.manager.AuthManager;
import com.chaigene.petnolja.manager.DatabaseManager;
import com.chaigene.petnolja.model.FIRAbuse;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import static com.chaigene.petnolja.model.FIRAbuse.TYPE_ARTICLE;

/**
 * 어뷰징을 방지하기 위해서 유저 스스로가 유해 콘텐츠를 신고할 수 있는 기능이 담긴 유틸 클래스이다.
 * (현재는 글 신고만 가능하다.)
 */
public class AbuseUtil {
    public static final String TAG = "AbuseUtil";

    /**
     * 글을 신고한다.
     *
     * @param postId            신고하고자 하는 글 ID
     * @param targetUid         해당 글의 글쓴이 ID
     * @param targetNickname    해당 글의 글쓴이 닉네임
     * @return null 타입의 Task 객체를 반환한다.
     */
    // TODO: 가능하면 그 글의 관련된 정보를 다 집어넣는다.
    public static Task<Void> reportArticle(final String postId, final String targetUid, final String targetNickname) {
        Log.i(TAG, "reportArticle");
        ExecutorService executor = DatabaseManager.getInstance().getExecutor();
        return Tasks.call(executor, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Task<String> getUserNicknameTask = UserUtil.getUserNickname(AuthManager.getUserId());
                String reporterNickname = Tasks.await(getUserNicknameTask);
                FIRAbuse firAbuse = new FIRAbuse(
                        TYPE_ARTICLE,
                        AuthManager.getUserId(),
                        reporterNickname,
                        targetUid,
                        targetNickname,
                        postId,
                        null,
                        null,
                        null,
                        null
                );
                Task<Void> setValueTask = DatabaseManager.getAbuseAbusesRef().push().setValue(firAbuse);
                Tasks.await(setValueTask);
                return null;
            }
        }).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(@NonNull Task<Void> task) throws Exception {
                if (!task.isSuccessful()) {
                    Exception e = task.getException();
                    throw e;
                }
                return null;
            }
        });
    }

    /**
     * 댓글을 신고한다.
     *
     * @return null 타입의 Task 객체를 반환한다.
     */
    public static Task<Void> reportComment() {
        Log.i(TAG, "reportComment");
        return null;
    }

    /**
     * 사용자를 신고한다.
     *
     * @return null 타입의 Task 객체를 반환한다.
     */
    public static Task<Void> reportUser() {
        Log.i(TAG, "reportUser");
        return null;
    }

    /**
     * 채팅 방을 신고한다.
     *
     * @return null 타입의 Task 객체를 반환한다.
     */
    public static Task<Void> reportChatRoom() {
        Log.i(TAG, "reportChatRoom");
        return null;
    }

    /**
     * 채팅 메세지를 신고한다.
     *
     * @return null 타입의 Task 객체를 반환한다.
     */
    public static Task<Void> reportChatMessage() {
        Log.i(TAG, "reportChatMessage");
        return null;
    }
}