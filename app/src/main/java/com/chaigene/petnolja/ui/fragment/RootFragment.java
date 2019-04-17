package com.chaigene.petnolja.ui.fragment;

import android.os.Bundle;
import androidx.annotation.IntRange;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.chaigene.petnolja.R;
import com.chaigene.petnolja.event.StartActivityEvent;
import com.chaigene.petnolja.ui.helper.InitialFragmentHelper;

import org.greenrobot.eventbus.EventBus;

import static com.chaigene.petnolja.Constants.EXTRA_PAGE_FRAGMENT;
import static com.chaigene.petnolja.Constants.PAGE_EXPLORE;
import static com.chaigene.petnolja.Constants.PAGE_NOTIFICATION;
import static com.chaigene.petnolja.Constants.PAGE_GALLERY;
import static com.chaigene.petnolja.Constants.PAGE_HOME;
import static com.chaigene.petnolja.Constants.PAGE_PROFILE;

// TODO: RootFragment
public class RootFragment extends BaseFragment {
    private static final String TAG = "RootFragment";

    private int mPageIndex;
    private ChildFragment mBaseFragment;
    private InitialFragmentHelper mInitialFragmentHelper;

    // 아무런 기본 페이지를 띄우고 싶지 않을 때는 -1을 넘겨주도록 한다.
    public static RootFragment newInstance(@IntRange(from = 0, to = 4) int pageFragment) {
        Log.i(TAG, "newInstance");

        if (pageFragment < 0 || pageFragment > 4)
            throw new IllegalArgumentException("Unexpected page fragment index retrived.");

        RootFragment rootFragment = new RootFragment();

        Bundle args = new Bundle();
        args.putInt(EXTRA_PAGE_FRAGMENT, pageFragment);
        rootFragment.setArguments(args);

        return rootFragment;
    }

    /*public static RootFragment newInstance(InitialFragmentHelper helper) {
        Log.i(TAG, "newInstance");

        RootFragment rootFragment = new RootFragment();

        Bundle args = new Bundle();
        args.putSerializable(EXTRA_CHILD_FRAGMENT, helper);
        rootFragment.setArguments(args);

        return rootFragment;
    }*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void readBundle(@Nullable Bundle bundle) {
        super.readBundle(bundle);
        Log.i(TAG, "readBundle");

        if (mBaseFragment != null) return;

        /*mInitialFragmentHelper = (InitialFragmentHelper) bundle.getSerializable(EXTRA_CHILD_FRAGMENT);
        if (mInitialFragmentHelper != null) {
            mBaseFragment = mInitialFragmentHelper.onInit();
        }*/

        if (bundle == null) return;

        mPageIndex = bundle.getInt(EXTRA_PAGE_FRAGMENT);
        switch (mPageIndex) {
            case PAGE_HOME:
                mBaseFragment = new PageHomeFragment();
                break;
            case PAGE_EXPLORE:
                mBaseFragment = new PageExploreFragment();
                break;
            case PAGE_GALLERY:
                mBaseFragment = new PageGalleryFragment();
                break;
            case PAGE_NOTIFICATION:
                mBaseFragment = new PageNotificationFragment();
                break;
            case PAGE_PROFILE:
                mBaseFragment = new PageProfileFragment();
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView:savedInstanceState:" + savedInstanceState);
        mView = inflater.inflate(R.layout.fragment_root, container, false);
        initView();
        return mView;
    }

    @Override
    protected void initView() {
        Log.i(TAG, "initView");
        super.initView();
        if (mBaseFragment != null) {
            add(mBaseFragment);
        } else {
            EventBus.getDefault().post(new StartActivityEvent(this));
        }
    }

    /*public void add(Fragment fragment) {
        Log.i(TAG, "add:" + fragment);

        if (fragment.isAdded()) {
            Log.d(TAG, "add:isAdded:true");
            return;
        }

        getChildFragmentManager()
                .beginTransaction()
                .add(R.id.root_container, fragment)
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }*/

    public void add(Fragment fragment) {
        add(fragment, false);
    }

    public void add(Fragment fragment, boolean isAnimated) {
        Log.i(TAG, "add:" + fragment);

        if (fragment.isAdded()) {
            Log.d(TAG, "add:isAdded:true");
            return;
        }

        FragmentTransaction transaction = getChildFragmentManager()
                .beginTransaction();

        if (isAnimated) {
            transaction
                    .setTransition(R.animator.fade_in)
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out);
        }

        transaction
                .add(R.id.root_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    public void pop() {
        Log.i(TAG, "pop");
        getChildFragmentManager().popBackStack();
    }

    public Fragment getPrimaryFragment() {
        Fragment primaryFragment = getChildFragmentManager().findFragmentById(R.id.root_container);
        Log.i(TAG, "getPrimaryFragment:" + primaryFragment);
        return primaryFragment;
    }

    public int getPageIndex() {
        return mPageIndex;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        // Log.i(TAG, "setUserVisibleHint:" + isVisibleToUser);
    }

    @Nullable
    public ChildFragment getBaseFragment() {
        return mBaseFragment;
    }
}