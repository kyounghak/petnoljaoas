package com.chaigene.petnolja.manager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Parcel;
import androidx.annotation.Nullable;
import android.util.Log;

import com.chaigene.petnolja.util.OldArticleUtil;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AdditionalUserInfo;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.kakao.auth.Session;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.LogoutResponseCallback;
import com.chaigene.petnolja.util.UserUtil;
import com.nhn.android.naverlogin.OAuthLogin;

import java.util.concurrent.Executors;

@SuppressWarnings("WeakerAccess")
public class AuthManager {
    public static final String TAG = "AuthManager";

    private static AuthManager mInstance;
    // private static FirebaseAuth mAuth;
    // private FirebaseAuth.AuthStateListener mAuthStateListener;

    public static synchronized AuthManager getInstance() {
        Log.i(TAG, "getInstance:instance:" + mInstance);
        if (mInstance == null) mInstance = new AuthManager();
        return mInstance;
    }

    private AuthManager() {
    }

    public static FirebaseAuth getAuth() {
        // return (mAuth == null) ? FirebaseAuth.getInstance() : mAuth;
        return FirebaseAuth.getInstance();
    }

    /*public void initAuthStateListener() {
        Log.i(TAG, "initAuthStateListener");

        // 이미 존재할 때는 중복 실행하지 않는다.
        if (mAuthStateListener != null) return;

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                Log.d(TAG, "onAuthStateChanged");

                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getId());
                } else {
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };
        getAuth().addAuthStateListener(mAuthStateListener);
    }*/

    /*public void releaseAuthStateListener() {
        Log.i(TAG, "releaseAuthStateListener:auth_state_listener:" + mAuthStateListener);

        getAuth().removeAuthStateListener(mAuthStateListener);
        mAuthStateListener = null;
    }*/

    public static boolean isSignedIn() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        return auth.getCurrentUser() != null;
    }

    public static Task<AuthResult> signInAnonymously() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        return auth.signInAnonymously();
    }

    public static boolean isAnonymous() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        return auth.getCurrentUser().isAnonymous();
    }

    // TODO: 페이스북에서도 로그아웃 한다.
    public static void signOut(Context context) {
        Context c = context.getApplicationContext();
        FirebaseAuth.getInstance().signOut();
        UserManagement.requestLogout(new LogoutResponseCallback() {
            @Override
            public void onCompleteLogout() {
                Log.d(TAG, "signOut:kakao_signout_complete");
                Session.getCurrentSession().close();
            }
        });
        OAuthLogin oAuthLogin = OAuthLogin.getInstance();
        boolean isSuccessDeleteToken = oAuthLogin.logoutAndDeleteToken(c);
        if (!isSuccessDeleteToken) {
            // 서버에서 토큰 삭제에 실패했어도 클라이언트에 있는 토큰은 삭제되어 로그아웃된 상태입니다.
            // 클라이언트에 토큰 정보가 없기 때문에 추가로 처리할 수 있는 작업은 없습니다.
            Log.d(TAG, "signOut:naver:errorCode:" + oAuthLogin.getLastErrorCode(c));
            Log.d(TAG, "signOut:naver:errorDesc:" + oAuthLogin.getLastErrorDesc(c));
        }
        UserUtil.releaseInstance();
        OldArticleUtil.releaseInstance();
    }

    @Nullable
    public static FirebaseUser getUser() {
        return getAuth().getCurrentUser();
    }

    @Nullable
    public static String getUserId() {
        if (getUser() == null) return null;
        // return "qYQg53Wz59dB8yY8BRleUEjyif43";
        return getUser().getUid();
    }

    @Nullable
    public static String getNickname() {
        if (getUser() == null) return null;
        return getUser().getDisplayName();
    }

    @Nullable
    public static String getEmail() {
        if (getUser() == null) return null;
        return getUser().getEmail();
    }

    @Nullable
    public static String getPhoneNumber() {
        if (getUser() == null) {
            Log.i(TAG, "getPhoneNumber:null");
            return null;
        }
        String number = getUser().getPhoneNumber();
        Log.d(TAG, "getPhoneNumber:" + number);
        return number;
    }

    public static Task<Void> updatePhoneNumber(PhoneAuthCredential credential) {
        Log.i(TAG, "updatePhoneNumber");
        return getUser().updatePhoneNumber(credential);
    }

    public static Task<Void> reload() {
        return getUser().reload().continueWith(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            return task.getResult();
        });
    }

    /**
     * Migrates a user token from the legacy Firebase SDK, making that user the current user
     * in the new Firebase SDK.
     * <p>
     * This works as follows:
     * <ol>
     * <li>Looks up the legacy auth token.</li>
     * <li>Sends the legacy token to a Firebase server to exchange it for a new auth token.</li>
     * <li>Uses the new auth token to log in the user.</li>
     * <li>Removes the legacy auth token from the device.</li>
     * </ol>
     * <p>
     * If a user is already logged in with the new Firebase SDK, then the legacy auth token will be
     * removed, but the logged in user will not be affected.
     * <p>
     * If the Firebase server determines that the legacy auth token is invalid, it will be removed
     * and the user will not be logged in.
     */
    @Deprecated
    public Task<AuthResult> migrate(final String persistenceKey) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            return Tasks.forResult((AuthResult) new MigratorAuthResult(currentUser));
        }

        String legacyToken = "1234";
        if (legacyToken == null) {
            // If there's no legacy token, just return null.
            return Tasks.forResult((AuthResult) new MigratorAuthResult(null));
        }

        // Otherwise, exchange the token.
        return exchangeToken(legacyToken)
                .continueWithTask(task -> {
                    if (task.getResult() == null) return Tasks.forResult(null);
                    return FirebaseAuth.getInstance().signInWithCustomToken(task.getResult());
                }).continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        try {
                            throw task.getException();
                        } catch (FirebaseWebRequestException e) {
                            if (e.getHttpStatusCode() == 400 || e.getHttpStatusCode() == 403) {
                            }
                            return task;
                        }
                    }
                    return task;
                });
    }

    @Deprecated
    private Task<String> exchangeToken(final String legacyToken) {
        if (legacyToken == null) return Tasks.forResult(null);

        return Tasks.call(Executors.newCachedThreadPool(), () -> {
            int responseCode = 400;
            if (responseCode != 200) {
                throw new FirebaseWebRequestException("Error !", 400);
            }
            return "token";
        });
    }

    @Deprecated
    private static class FirebaseWebRequestException extends FirebaseException {
        private final int httpStatusCode;

        public FirebaseWebRequestException(String message, int httpStatusCode) {
            this.httpStatusCode = httpStatusCode;
        }

        public int getHttpStatusCode() {
            return httpStatusCode;
        }
    }

    @Deprecated
    @SuppressLint("ParcelCreator")
    private static class MigratorAuthResult implements AuthResult {
        private final FirebaseUser user;

        public MigratorAuthResult(FirebaseUser user) {
            this.user = user;
        }

        @Override
        public FirebaseUser getUser() {
            return user;
        }

        @Override
        public AdditionalUserInfo getAdditionalUserInfo() {
            return null;
        }

        @Override
        public AuthCredential getCredential() {
            return null;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {

        }
    }

    public static class AuthException extends Exception {
        public AuthException() {
        }

        public AuthException(String detailMessage) {
            super(detailMessage);
        }
    }
}