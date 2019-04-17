package com.chaigene.petnolja.ui.activity;

import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

import com.chaigene.petnolja.R;

import butterknife.ButterKnife;

import static com.chaigene.petnolja.Constants.SHOP_TYPE_BUY;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";

    // 메인 탭 4개에 대한 상수는 어떠한 변수명으로 선언해둘 것인지 고민해야한다.
    // 해당 상수를 재사용할 일이 있을 것인지도 고민해야한다.
    private static final String FRAGMENT_TAG_HOME = String.valueOf(SHOP_TYPE_BUY);
    // private static final String FRAGMENT_TAG_SELL = String.valueOf(SHOP_TYPE_SELL);
    // private static final String FRAGMENT_TAG_SELL = String.valueOf(SHOP_TYPE_SELL);
    // private static final String FRAGMENT_TAG_SELL = String.valueOf(SHOP_TYPE_SELL);

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = item -> {
        switch (item.getItemId()) {
            case R.id.navigation_home:

                return true;
            case R.id.navigation_dashboard:

                return true;
            case R.id.navigation_notifications:

                return true;
        }
        return false;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        // initView();

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    /*private void showBuyTab() {
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
    }*/

    /*private void showSellTab() {
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
    }*/

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
}