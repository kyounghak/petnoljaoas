package com.chaigene.petnolja.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.MenuItem;

import com.chaigene.petnolja.R;
import com.chaigene.petnolja.event.StartActivityEvent;
import com.chaigene.petnolja.manager.FirestoreManager;
import com.chaigene.petnolja.ui.fragment.TabShopFragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.chaigene.petnolja.Constants.EXTRA_ORDER_ID;
import static com.chaigene.petnolja.Constants.EXTRA_SHOP_TYPE;
import static com.chaigene.petnolja.Constants.SHOP_TYPE_BUY;
import static com.chaigene.petnolja.Constants.SHOP_TYPE_SELL;

// ShopActivity 자체가 RootFragment와 같은 컨테이너가 되어야한다.
public class ShopActivity extends BaseActivity {
    public static final String TAG = "ShopActivity";

    private static final String FRAGMENT_TAG_BUY = String.valueOf(SHOP_TYPE_BUY);
    private static final String FRAGMENT_TAG_SELL = String.valueOf(SHOP_TYPE_SELL);

    // private static final String ACTION_STATUS = "action_status";
    // public static final int ACTION_STATUS_DEFAULT = 0;
    // public static final int ACTION_STATUS_SELECT = 1;

    @BindView(R.id.swipe_layout)
    SwipeRefreshLayout mSwipeLayout;

    @BindView(R.id.tab_layout)
    TabLayout mTabLayout;

    private int mTargetShopType;
    private String mTargetOrderId;
    private TabShopFragment mSelectedFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);
        ButterKnife.bind(this);
        initView();
        EventBus.getDefault().register(this);

        mSwipeLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    final String TAG = "OnRefreshListener";

                    @Override
                    public void onRefresh() {
                        Log.d(TAG, "onRefresh");
                        mSwipeLayout.setRefreshing(false);
                        if (mSelectedFragment != null) mSelectedFragment.onRefresh();
                    }
                }
        );
    }

    @Override
    protected void readIntent() {
        super.readIntent();
        mTargetShopType = getIntent().getIntExtra(EXTRA_SHOP_TYPE, 0);
        mTargetOrderId = getIntent().getStringExtra(EXTRA_ORDER_ID);
        Log.i(TAG, "readIntent:shopType:" + mTargetShopType + "|targetOrderId:" + (mTargetOrderId != null ? mTargetOrderId : "null"));
        /*if (TextUtils.isEmpty(mProductId) ||
                TextUtils.isEmpty(mProductTitle) ||
                TextUtils.isEmpty(mProductPrice) ||
                TextUtils.isEmpty(mCoverPhoto) ||
                TextUtils.isEmpty(mSellerId) ||
                TextUtils.isEmpty(mSellerNickname)) {
            Toast.makeText(getApplicationContext(), "주문정보에 오류가 있습니다. 잠시후 다시 시도해주세요.", Toast.LENGTH_LONG).show();
            finish();
        }*/
        getIntent().removeExtra(EXTRA_SHOP_TYPE);
        getIntent().removeExtra(EXTRA_ORDER_ID);
    }

    @Override
    protected void setupToolbar() {
        Log.i(TAG, "setupToolbar");
        super.setupToolbar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getToolbar().setBackgroundResource(android.R.color.white);
        setToolbarTitle(R.string.title_activity_shop);
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStartActivityEvent(StartActivityEvent event) {
        // mSelectedFragment.onStartActivityEvent(event);
    }

    @Override
    protected void initView() {
        super.initView();
        // setupSearchInput();
        setupTabs();
    }

    private void setupTabs() {
        mTabLayout.addTab(mTabLayout.newTab().setText("구매관리"), mTargetShopType == SHOP_TYPE_BUY);
        mTabLayout.addTab(mTabLayout.newTab().setText("판매관리"), mTargetShopType == SHOP_TYPE_SELL);

        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    showBuyTab();
                }
                if (tab.getPosition() == 1) {
                    showSellTab();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        if (mTargetShopType == SHOP_TYPE_BUY) {
            showBuyTab();
        }
        if (mTargetShopType == SHOP_TYPE_SELL) {
            showSellTab();
        }
    }

    private void showBuyTab() {
        Log.i(TAG, "showBuyTab");

        Fragment buyFragment = find(FRAGMENT_TAG_BUY);
        if (buyFragment == null) {
            // buyFragment = TabShopFragment.newInstance(SHOP_TYPE_BUY);
            buyFragment = TabShopFragment.newInstance(SHOP_TYPE_BUY, mTargetOrderId);
            mTargetShopType = SHOP_TYPE_BUY;
            mTargetOrderId = null;
            add(buyFragment, FRAGMENT_TAG_BUY);
        } else {
            show(buyFragment);
        }

        Fragment saleFragment = find(FRAGMENT_TAG_SELL);
        if (saleFragment != null) hide(saleFragment);

        mSelectedFragment = (TabShopFragment) buyFragment;
        // Log.d(TAG, "showBuyTab:mSelectedFragment:" + mSelectedFragment);
    }

    private void showSellTab() {
        Log.i(TAG, "showSellTab");

        Fragment saleFragment = find(FRAGMENT_TAG_SELL);
        if (saleFragment == null) {
            saleFragment = TabShopFragment.newInstance(SHOP_TYPE_SELL, mTargetOrderId);
            mTargetShopType = SHOP_TYPE_BUY;
            mTargetOrderId = null;
            add(saleFragment, FRAGMENT_TAG_SELL);
        } else {
            show(saleFragment);
        }

        Fragment buyFragment = find(FRAGMENT_TAG_BUY);
        if (buyFragment != null) hide(buyFragment);

        mSelectedFragment = (TabShopFragment) saleFragment;
    }

    private Fragment find(String tag) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
        Log.i(TAG, "find:tag:" + tag + "|result:" + fragment);
        return fragment;
    }

    protected void add(Fragment fragment, String tag) {
        Log.i(TAG, "add:" + fragment + " " + tag);

        if (fragment.isAdded()) {
            Log.d(TAG, "add:isAdded:true");
            return;
        }

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, fragment, tag)
                .addToBackStack(null)
                .commit();
    }

    private void show(Fragment fragment) {
        Log.i(TAG, "show:" + fragment);

        getSupportFragmentManager()
                .beginTransaction()
                .show(fragment)
                .commit();
    }

    private void hide(Fragment fragment) {
        Log.i(TAG, "hide:" + fragment);

        getSupportFragmentManager()
                .beginTransaction()
                .hide(fragment)
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

    @Override
    public void onBackPressed() {
        if (isLoading()) {
            dismissDialog();
            FirestoreManager.getInstance().cancel();
            return;
        }
        finish();
    }

    public static Intent createIntent(Context context, int shopType) {
        Context c = context.getApplicationContext();
        Intent intent = new Intent().setClass(c, ShopActivity.class);
        intent.putExtra(EXTRA_SHOP_TYPE, shopType);
        return intent;
    }

    public static Intent createIntent(Context context, int shopType, String orderId) {
        Context c = context.getApplicationContext();
        Intent intent = new Intent().setClass(c, ShopActivity.class);
        intent.putExtra(EXTRA_SHOP_TYPE, shopType);
        intent.putExtra(EXTRA_ORDER_ID, orderId);
        return intent;
    }
}