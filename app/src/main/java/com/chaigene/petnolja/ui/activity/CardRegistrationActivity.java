package com.chaigene.petnolja.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.WindowManager;

import com.chaigene.petnolja.BuildConfig;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.ui.fragment.CardRegDetailFragment;

import org.greenrobot.eventbus.EventBus;

import butterknife.ButterKnife;

// ShopActivity 자체가 RootFragment와 같은 컨테이너가 되어야한다.
public class CardRegistrationActivity extends BaseActivity {
    public static final String TAG = "CardRegActivity";

    private static final String ACTION_STATUS = "action_status";
    public static final int ACTION_STATUS_INITIAL_REGISTER = 0;
    public static final int ACTION_STATUS_STANDARD_REGISTER = 1;

    public static final String FRAGMENT_TAG_COMPANY = "company";
    public static final String FRAGMENT_TAG_DETAIL = "detail";

    // private TabShopFragment mSelectedFragment;

    private int mActionStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 스크린 캡쳐 방지
        // Souce: https://stackoverflow.com/a/9822607/4729203
        if (!BuildConfig.DEBUG) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }

        setContentView(R.layout.activity_card_registration);
        ButterKnife.bind(this);
        initView();
        // EventBus.getDefault().register(this);
    }

    @Override
    protected void readIntent() {
        super.readIntent();
        mActionStatus = getIntent().getIntExtra(ACTION_STATUS, 0);
    }

    @Override
    protected void setupToolbar() {
        Log.i(TAG, "setupToolbar");
        super.setupToolbar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setToolbarTitle("결제 카드 등록");
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    /*@SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStartActivityEvent(StartActivityEvent event) {
        // mSelectedFragment.onStartActivityEvent(event);
    }*/

    @Override
    protected void initView() {
        super.initView();

        // CardRegCompanyFragment companyFragment = CardRegCompanyFragment.newInstance();
        // add(companyFragment, FRAGMENT_TAG_COMPANY);

        CardRegDetailFragment detailFragment = CardRegDetailFragment.newInstance(mActionStatus);
        add(detailFragment, FRAGMENT_TAG_DETAIL);
    }

    public void add(Fragment fragment, String tag) {
        Log.i(TAG, "add:" + fragment + " " + tag);

        if (fragment.isAdded()) {
            Log.d(TAG, "add:isAdded:true");
            return;
        }

        getSupportFragmentManager()
                .beginTransaction()
                .setTransition(R.animator.fade_in)
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                .add(R.id.fragment_container, fragment, tag)
                .addToBackStack(null)
                .commit();
    }

    public void remove(Fragment fragment) {
        Log.i(TAG, "remove:" + fragment);

        if (!fragment.isAdded()) {
            Log.d(TAG, "add:isAdded:false");
            return;
        }

        getSupportFragmentManager()
                .beginTransaction()
                .remove(fragment)
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*@Override
    public void onBackPressed() {
        if (isLoading()) {
            dismissDialog();
            FirestoreManager.getInstance().cancel();
            return;
        }
        finish();
    }*/

    // TODO: RootFragment를 사용하지 않아도 될 것 같다.
    // 그냥 프래그먼트를 삽입할 수 있는 콘테이너를 레이아웃에 설정하고 getSupportFragment를 사용하여 대체해주기로 한다.
    // 그리고 툴바 영역은 아무런 네비게이션 메뉴나 기타 메뉴가 존재하지 않는다.
    @Override
    public void onBackPressed() {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        int backStackCount = fragmentManager.getBackStackEntryCount();
        Log.i(TAG, "onBackPressed:backStackCount:" + backStackCount);

        // 프로필 설정 화면일 때는 뒤로 갈 수가 없다.
        /*final Fragment settingProfileFragment = find(FRAGMENT_TAG_SETTING_PROFILE);
        if (settingProfileFragment != null && settingProfileFragment.isVisible()) {
            // super.onBackPressed();
            return;
        }*/

        // 최초의 BaseFragment는 남겨두고 모두 제거한다.
        if (backStackCount > 1) {
            fragmentManager.popBackStack();
        } else finish();
    }

    public static Intent createIntent(Context context, int actionStatus) {
        Context c = context.getApplicationContext();
        Intent intent = new Intent().setClass(c, CardRegistrationActivity.class);
        intent.putExtra(ACTION_STATUS, actionStatus);
        return intent;
    }
}