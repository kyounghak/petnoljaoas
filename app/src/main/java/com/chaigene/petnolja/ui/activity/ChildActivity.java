package com.chaigene.petnolja.ui.activity;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.util.Log;

import com.chaigene.petnolja.R;
import com.chaigene.petnolja.ui.fragment.RootFragment;

// ChildActivity 자체가 RootFragment와 같은 컨테이너가 되어야한다.
public class ChildActivity extends BaseActivity {
    public static final String TAG = "ChildActivity";

    RootFragment mRootFragment;
    // InitialFragmentHelper mInitialFragmentHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child);
        initView();
    }

    @Override
    protected void readIntent() {
        super.readIntent();
        // mInitialFragmentHelper = (InitialFragmentHelper) getIntent().getSerializableExtra(EXTRA_HELPER);
    }

    @Override
    protected void initView() {
        super.initView();
        // mRootFragment = RootFragment.newInstance(PAGE_NONE_FRAGMENT, mChildFragment);
        mRootFragment = new RootFragment();
        add(mRootFragment);
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
    }

    public void add(Fragment fragment) {
        Log.i(TAG, "add:" + fragment);

        if (fragment.isAdded()) {
            Log.d(TAG, "add:isAdded:true");
            return;
        }

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.root_container, fragment)
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }

    public void pop() {
        Log.i(TAG, "pop");
        getSupportFragmentManager().popBackStack();
    }

    /*@Override
    public void onBackPressed() {
        int backStackCount = getSupportFragmentManager().getBackStackEntryCount();
        Log.i(TAG, "onBackPressed:backStackCount:" + backStackCount);

        FragmentManager fragmentManager = getSupportFragmentManager();
        int childBackStackCount = fragmentManager.getBackStackEntryCount();
        Log.i(TAG, "onBackPressed:childBackStackCount:" + childBackStackCount);

        // 최초의 BaseFragment는 남겨두고 모두 제거한다.
        if (childBackStackCount > 1) {
            fragmentManager.popBackStack();
        } else finish();
    }*/

    @Override
    public void onBackPressed() {
        int backStackCount = getSupportFragmentManager().getBackStackEntryCount();
        Log.i(TAG, "onBackPressed:backStackCount:" + backStackCount);

        FragmentManager childFragmentManager = mRootFragment.getChildFragmentManager();
        int childBackStackCount = childFragmentManager.getBackStackEntryCount();
        Log.i(TAG, "onBackPressed:childBackStackCount:" + childBackStackCount);

        // 최초의 BaseFragment는 남겨두고 모두 제거한다.
        if (childBackStackCount > 1) {
            childFragmentManager.popBackStack();
        } else finish();
    }

    // TODO: 여기서 어떤 프래그먼트를 초기로 보여줄지를 결정한다.
    // 그러나 프래그먼트 자체를 인텐트로 넘기는 것은 불가능하다.
    // 일단 여기에서는 받은 값을 그대로 RootFragment에 전달해준다.
    /*public static Intent createIntent(Context context, InitialFragmentHelper helper) {
        Context c = context.getApplicationContext();
        Intent intent = new Intent().setClass(c, ChildActivity.class);
        intent.putExtra(EXTRA_HELPER, helper);
        return intent;
    }*/

    /*public static Intent createIntent(Context context, final ChildFragment childFragment) {
        Context c = context.getApplicationContext();
        Intent intent = new Intent().setClass(c, ChildActivity.class);
        intent.putExtra(EXTRA_HELPER, new InitialFragmentHelper() {
            @Override
            public ChildFragment onInit() {
                return childFragment;
            }
        });
        return intent;
    }*/
}