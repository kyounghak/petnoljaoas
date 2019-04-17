package com.chaigene.petnolja.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;

import android.util.Log;

import com.chaigene.petnolja.manager.FirestoreManager;
import com.chaigene.petnolja.manager.TasksManager;
import com.chaigene.petnolja.model.Quota;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.iid.FirebaseInstanceId;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.manager.AuthManager;
import com.chaigene.petnolja.util.CommonUtil;
import com.chaigene.petnolja.util.NotificationUtil;
import com.chaigene.petnolja.util.UserUtil;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import static com.chaigene.petnolja.Constants.ACTION_STATUS_ARTICLE;
import static com.chaigene.petnolja.Constants.ACTION_STATUS_DEFAULT;
import static com.chaigene.petnolja.Constants.ACTION_STATUS_PROFILE;
import static com.chaigene.petnolja.Constants.ACTION_STATUS_SHOP;
import static com.chaigene.petnolja.Constants.EXTRA_ACTION_STATUS;
import static com.chaigene.petnolja.Constants.EXTRA_ORDER_ID;
import static com.chaigene.petnolja.Constants.EXTRA_SHOP_TYPE;
import static com.chaigene.petnolja.Constants.EXTRA_TARGET_POST_ID;
import static com.chaigene.petnolja.Constants.EXTRA_TARGET_ROOM_ID;
import static com.chaigene.petnolja.Constants.EXTRA_TARGET_USER_ID;
import static com.chaigene.petnolja.Constants.RC_SIGN_IN;
import static com.chaigene.petnolja.model.Quota.FIELD_DATE;
import static com.chaigene.petnolja.model.Quota.FIELD_MONTH;
import static com.chaigene.petnolja.model.Quota.FIELD_SHOP_ID;
import static com.chaigene.petnolja.model.Quota.FIELD_YEAR;
import static com.chaigene.petnolja.util.CommonUtil.PLAY_SERVICES_RESOLUTION_REQUEST;

public class LauncherActivity extends BaseActivity {
    public static final String TAG = "LauncherActivity";

    private static final int RC_STORAGE_PERMS = 101;

    private int mActionStatus;
    private String mTargetPostId;
    private String mTargetUserId;
    private int mTargetShopType;
    private String mTargetOrderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // SHA1: 99:94:0E:A1:E2:D3:17:2F:8E:EE:FD:A7:4D:4D:20:4A:7A:63:3A:A9
        /*byte[] sha1 = {
                (byte) 0x99,
                (byte) 0x94,
                (byte) 0x0E,
                (byte) 0xA1,
                (byte) 0xE2,
                (byte) 0xD3,
                (byte) 0x17,
                (byte) 0x2F,
                (byte) 0x8E,
                (byte) 0xEE,
                (byte) 0xFD,
                (byte) 0xA7,
                (byte) 0x4D,
                (byte) 0x4D,
                (byte) 0x20,
                (byte) 0x4A,
                (byte) 0x7A,
                (byte) 0x63,
                (byte) 0x3A,
                (byte) 0xA9
        };
        Log.i(TAG, "sha1_to_keyhash:" + Base64.encodeToString(sha1, Base64.NO_WRAP));*/

        // Deprecated
        // Launcher에서는 반드시 Manifest의 Theme를 통해 Drawable을 사용해야 앱을 최초 실행했을 때 Launcher가 보이게 된다.
        // setContentView(R.layout.activity_launcher);

        // checkPermissions();

        // 구글 플레이 서비스를 실행한다.
        boolean isPlayServicesAvailable = CommonUtil.checkPlayServices(this);
        if (!isPlayServicesAvailable) return;

        startApp();
    }

    // TODO: 현재의 인텐트 값을 그대로 MainActivity에 넘겨준다.
    @Override
    protected void readIntent() {
        super.readIntent();

        // 특정 액티비티로 바로 넘어가기(Null인 경우는 존재하지 않는다)

        // if (getIntent().getAction() == null) return;

        // switch (getIntent().getAction()) {
            /*case NotificationUtil.ACTION_SHOW_CONTENTS:
                startActivity(new Intent(this, ContentsActivity.class));
            case NotificationUtil.ACTION_SHOW_EVENT:
                startActivity(new Intent(this, EventActivity.class));*/
        // }

        mActionStatus = getIntent().getIntExtra(EXTRA_ACTION_STATUS, 0);
        mTargetPostId = getIntent().getStringExtra(EXTRA_TARGET_POST_ID);
        mTargetUserId = getIntent().getStringExtra(EXTRA_TARGET_USER_ID);
        mTargetShopType = getIntent().getIntExtra(EXTRA_SHOP_TYPE, 0);
        mTargetOrderId = getIntent().getStringExtra(EXTRA_ORDER_ID);

        if (mActionStatus == ACTION_STATUS_DEFAULT) return;

        if (mActionStatus == ACTION_STATUS_ARTICLE) {

        }

        if (mActionStatus == ACTION_STATUS_PROFILE) {

        }

        if (mActionStatus == ACTION_STATUS_SHOP) {
            // Intent in = ShopActivity.createIntent(this, mTargetShopType, mTargetOrderId);
            // startActivity(in);
        }

        // Handle possible data accompanying notification message.
        /*if (getIntent().getExtras() != null) {
            for (String key : getIntent().getExtras().keySet()) {
                Object value = getIntent().getExtras().get(key);
                Log.d(TAG, "readIntent:key:" + key + "|value:" + value);
            }
        }*/

        getIntent().removeExtra(EXTRA_ACTION_STATUS);
        getIntent().removeExtra(EXTRA_TARGET_POST_ID);
        getIntent().removeExtra(EXTRA_TARGET_USER_ID);
        getIntent().removeExtra(EXTRA_SHOP_TYPE);
        getIntent().removeExtra(EXTRA_ORDER_ID);
        getIntent().removeExtra(EXTRA_TARGET_ROOM_ID);
    }

    private void checkToken() {
        Log.i(TAG, "checkToken");

        if (!AuthManager.isSignedIn()) {
            return;
        }

        final String token = FirebaseInstanceId.getInstance().getToken();
        if (token == null) {
            Log.d(TAG, "checkToken:token_is_null");
            return;
        }

        TasksManager.call((Callable<Void>) () -> {
            Task<Boolean> isStableTokenExistsTask = NotificationUtil.isStableTokenExists(token);
            Task<Boolean> isUserExistsTask = UserUtil.isExists(AuthManager.getUserId());
            Task<Void> tasks = Tasks.whenAll(isStableTokenExistsTask, isUserExistsTask);

            Tasks.await(tasks);

            // 가입을 완료하지 않은 유저라면 유저정보 입력 페이지를 보여준다.
            // if (!isUserExistsTask.isSuccessful()) {
            //     Log.w(TAG, "checkToken:isUserExistsTask:ERROR:");
            // }

            boolean isUserExists = isUserExistsTask.getResult();
            if (!isUserExists) {
                return null;
            }

            // 가입을 완료하지 않은 유저라면 토큰을 저장해서도 안된다.
            // if (!isTokenExistsTask.isSuccessful()) {
            //     Log.w(TAG, "checkToken:isTokenExistsTask:ERROR:");
            //     return null;
            // }

            boolean isStableTokenExists = isStableTokenExistsTask.getResult();
            if (!isStableTokenExists) {
                // 현재 디바이스에 대한 토큰값이 서버에 존재하지 않으면 삽입해준다.
                Task<Void> insertStableTokenTask = NotificationUtil.insertStableToken(token);
                Tasks.await(insertStableTokenTask);
            }
            return null;
        }).continueWith((Continuation<Void, Void>) task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "checkToken:ERROR:", task.getException());
                return null;
            }
            return null;
        });
    }

    @AfterPermissionGranted(RC_STORAGE_PERMS)
    protected void checkPermissions() {
        Log.d(TAG, "checkPermissions");
        String[] perm = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (!EasyPermissions.hasPermissions(this, perm)) {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_storage_perm), RC_STORAGE_PERMS, perm);
            return;
        }
        startApp();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, CommonUtil.format("onActivityResult:requestCode:%d|resultCode:%d", requestCode, resultCode));
        if (resultCode == PLAY_SERVICES_RESOLUTION_REQUEST) {
            boolean isPlayServicesAvailable = CommonUtil.checkPlayServices(this);
            if (isPlayServicesAvailable) startApp();
        }
    }

    private void startApp() {
        Log.d(TAG, "startApp");

        /*FunctionsManager.hello("world!").continueWith(new Continuation<String, Void>() {
            @Override
            public Void then(@NonNull Task<String> task) throws Exception {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "hello:ERROR:", task.getException());
                    return null;
                }
                Log.d(TAG, "hello:SUCCESS:" + task.getResult());
                return null;
            }
        });*/

        checkToken();

        // Background service를 시작한다.
        // CoreService.startService(this);

        // For test.
        // AuthManager.signOut(this);

        // TODO: isSignedIn이 true를 반환할지라도 유저 기본정보가 존재하지 않는다면 LoginActivity를 실행시킨다.
        // HOTFIX: 로그인이 되어있지 않으면 익명 로그인을 시도한다.

        /*boolean isSignedIn = AuthManager.isSignedIn();
        AuthManager.getAuth().signInAnonymously().continueWith((Continuation<AuthResult, Void>) task -> {
            if (!task.isSuccessful()) {
                return null;
            }
            AuthResult authResult = task.getResult();
            // AdditionalUserInfo userInfo = authResult.getAdditionalUserInfo();
            // Map<String, Object> profile = userInfo.getProfile();
            // String providerId = userInfo.getProviderId();
            // String username = userInfo.getUsername();
            // AuthCredential credential = authResult.getCredential();
            // FirebaseUser firebaseUser = authResult.getUser();
            // String currentUserId = firebaseUser.getUid();
            // boolean isAnonymous = firebaseUser.isAnonymous();
            return null;
        });*/

        /*WriteBatch batch = FirestoreManager.batch();
        for (int i = 1; i <= 31; i++) {
            String shopId = "maone";
            int year = 2019;
            int month = 3;
            int date = i;
            int min = 0;
            int max = 10;
            int occupied = min + (int) (Math.random() * ((max - min) + 1));
            Quota quota = new Quota(shopId, year, month, date, occupied);
            batch.set(FirestoreManager.getHotelBookingQuotasRef().document(), quota);
        }
        for (int i = 1; i <= 30; i++) {
            String shopId = "maone";
            int year = 2019;
            int month = 4;
            int date = i;
            int min = 0;
            int max = 10;
            int occupied = min + (int) (Math.random() * ((max - min) + 1));
            Quota quota = new Quota(shopId, year, month, date, occupied);
            batch.set(FirestoreManager.getHotelBookingQuotasRef().document(), quota);
        }
        for (int i = 1; i <= 31; i++) {
            String shopId = "maone";
            int year = 2019;
            int month = 5;
            int date = i;
            int min = 0;
            int max = 10;
            int occupied = min + (int) (Math.random() * ((max - min) + 1));
            Quota quota = new Quota(shopId, year, month, date, occupied);
            batch.set(FirestoreManager.getHotelBookingQuotasRef().document(), quota);
        }
        batch.commit().continueWith(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "ERROR");
                return null;
            }
            Log.i(TAG, "SUCCESS");
            return null;
        });*/

        /*loadQuotas(2019, 3).continueWith(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "loadQuotas:ERROR", task.getException());
                return null;
            }
            List<Quota> quotas = task.getResult();
            Log.i(TAG, "loadQuotas:SUCCESS:quotas:" + quotas);
            return null;
        });*/

        if (AuthManager.isSignedIn()) {
            startMainActivity();
        } else {
            TasksManager.call((Callable<Void>) () -> {
                Tasks.await(AuthManager.signInAnonymously());
                startMainActivity();
                return null;
            }).addOnFailureListener(e -> {
                Log.w(TAG, "signInAnonymously:ERROR", e);
            });
        }

        /*Intent in;
        if (AuthManager.isSignedIn()) {

            // TODO: 앱을 실행할 때마다 네트워크를 통해서 유저 프로필 존재 여부를 확인한다면
            // 오프라인에서는 메인 액티비티를 아에 실행할 수 없을 것이다.
            // 오프라인에서 실행하더라도 현재 로그인이 된 상태라고 반환할 것이다.
            // 가입을 완료 안하더라도 메인 액티비티가 떠도 상관은 없다.
            // 하지만 메인 액티비티가 떠 있는 상태에서 다시 네트워크가 돌아오면 문제가 된다.
            // 프리퍼런스에 가입이 완료 안된 유저라면 값을 저장해두는 것이 좋을 것 같다.
            // isSignupCompleted
            // LoginActivity에서 onSuccessSignIn가 호출되었을 때
            // UserUtil의 isExists 메서드를 통해서 가입 완료 상태를 파악한다.
            // Signout을 할 때는 프리퍼런스의 값을 삭제해줘야 한다.
            // 카카오나 페이스북으로 로그인 했을 때는 사실 액션이 로그인인지 가입인지 파악하기 힘들다.
            // 모든 상황을 고려해봤을 때

            in = createIntent(OldMainActivity.class);
            in.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            in.putExtra(EXTRA_ACTION_STATUS, mActionStatus);
            in.putExtra(EXTRA_TARGET_POST_ID, mTargetPostId);
            in.putExtra(EXTRA_TARGET_USER_ID, mTargetUserId);
            in.putExtra(EXTRA_SHOP_TYPE, mTargetShopType);
            in.putExtra(EXTRA_ORDER_ID, mTargetOrderId);
            startActivity(in);
            finish();
        } else {
            in = createIntent(LoginActivity.class);
            in.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivityForResult(in, RC_SIGN_IN);
            finish();
        }*/
    }

    private Task<List<Quota>> loadQuotas(int year, int month) {
        return TasksManager.call(() -> {
            Query quotasRef = FirestoreManager.getHotelBookingQuotasRef()
                    .whereEqualTo(FIELD_SHOP_ID, "maone")
                    .whereEqualTo(FIELD_YEAR, year)
                    .whereEqualTo(FIELD_MONTH, month)
                    .orderBy(FIELD_DATE, Query.Direction.ASCENDING);
            List<Quota> quotas = Tasks.await(FirestoreManager.getInstance().get(quotasRef, Quota.class));
            return quotas;
        });
    }

    private void startMainActivity() {
        /*Intent in;
        in = createIntent(LoginActivity.class);
        in.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivityForResult(in, RC_SIGN_IN);
        finish();*/

        Intent in;
        in = createIntent(CalendarActivity.class);
        in.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivityForResult(in, RC_SIGN_IN);
        finish();

        /*in = createIntent(OldMainActivity.class);
        in.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        in.putExtra(EXTRA_ACTION_STATUS, mActionStatus);
        in.putExtra(EXTRA_TARGET_POST_ID, mTargetPostId);
        in.putExtra(EXTRA_TARGET_USER_ID, mTargetUserId);
        in.putExtra(EXTRA_SHOP_TYPE, mTargetShopType);
        in.putExtra(EXTRA_ORDER_ID, mTargetOrderId);
        startActivity(in);
        finish();*/
    }

    /*private void recordUserProperties() {
        // String uid = ConfigManager.getInstance(this).getUserId();
        final String nickname = ConfigManager.getInstance(this).getNickname();
        String email = ConfigManager.getInstance(this).getEmail();

        Tasks.call(Executors.newSingleThreadExecutor(), new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                if (nickname == null) {
                    Task<OldUser> getUesrTask = UserUtil.getOldUser(AuthManager.getUserId());
                    OldUser user = Tasks.await(getUesrTask);

                    if (!getUesrTask.isSuccessful()) {
                        // TODO: Record nothing.
                        return null;
                    }
                }
                return null;
            }
        });
    }*/
}
