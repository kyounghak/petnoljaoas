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
public class ViralUtil {
    public static final String TAG = "ViralUtil";

    public static Task<Void> logHashtag() {

        return null;
    }

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
}