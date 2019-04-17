package com.chaigene.petnolja.ui.fragment;

import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.chaigene.petnolja.ui.activity.ChildActivity;
import com.chaigene.petnolja.ui.activity.OldMainActivity;

// ChildFragment는 RootFragment 하위에만 Attatch 될 수 있다.
public class ChildFragment extends BaseFragment {
    public static final String TAG = "ChildFragment";

    private boolean mInitializedAsyncTask;

    protected synchronized boolean isInitializedAsyncTask() {
        return mInitializedAsyncTask;
    }

    protected synchronized void setInitializedAsyncTask(boolean initializedAsyncTask) {
        // Log.i(TAG, "setInitializedAsyncTask");
        this.mInitializedAsyncTask = initializedAsyncTask;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupStackListener();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    // 일단 부모 액티비티가 OldMainActivity 일 경우에만 invalidate 한다.
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        Log.i(TAG, "setUserVisibleHint:" + isVisibleToUser);

        if (isVisibleToUser) {
            if (getView() != null) {
                if (getActivity() instanceof OldMainActivity) {
                    int currentPage = ((OldMainActivity) getActivity()).getViewPager().getCurrentItem();
                    Log.d(TAG, "setUserVisibleHint:currentPage:" + currentPage);

                    try {
                        if (currentPage == getRootPageIndex()) {
                            ActivityCompat.invalidateOptionsMenu(getActivity());
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }

                if (getActivity() instanceof ChildActivity) {
                    // TODO: ...
                }
            }
        }
    }

    // TODO: 이 메서드가 ChildActivity에서도 동일한 효과를 가져다주는지가 의문이다.
    protected boolean isPrimaryFragment() {
        // Log.i(TAG, "isPrimaryFragment");
        if (getUserVisibleHint()) {
            if (getView() != null) {
                if (getActivity() instanceof OldMainActivity) {
                    int currentPage = ((OldMainActivity) getActivity()).getViewPager().getCurrentItem();
                    // Log.d(TAG, "isPrimaryFragment:currentPage:" + currentPage);
                    try {
                        if (currentPage == getRootPageIndex()) {
                            // Log.i(TAG, "isPrimaryFragment:true");
                            return true;
                        }
                    } catch (IllegalAccessException e) {
                        // It will never happen.
                        e.printStackTrace();
                    }
                }

                if (getActivity() instanceof ChildActivity) {
                    Fragment primaryFragment = getRootFragment().getPrimaryFragment();
                    // Log.i(TAG, "isPrimaryFragment:primaryFragment:" + primaryFragment);
                    // Log.i(TAG, "isPrimaryFragment:this:" + this);
                    boolean isPrimary = primaryFragment.equals(this);
                    Log.i(TAG, "isPrimaryFragment:" + isPrimary);
                    return isPrimary;
                }
            }
        }
        // Log.i(TAG, "isPrimaryFragment:false");
        return false;
    }

    // 부모 액티비티가 OldMainActivity 일 경우에만 유효한 메서드.
    // 이 메서드는 절대로 현재 클래스에서만 호출할 수 있다.
    private int getRootPageIndex() throws IllegalAccessException {
        if (!(getActivity() instanceof OldMainActivity)) {
            throw new IllegalAccessException("");
        }
        int rootPageIndex = getRootFragment().getPageIndex();
        // Log.i(TAG, "getRootPageIndex:" + rootPageIndex);
        return rootPageIndex;
    }

    protected RootFragment getRootFragment() {
        return (RootFragment) getParentFragment();
    }

    private void setupStackListener() {
        Log.i(TAG, "setupStackListener");
        getFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            final String TAG = FragmentManager.OnBackStackChangedListener.class.getSimpleName();

            @Override
            public void onBackStackChanged() {
                // Log.i(TAG, "onBackStackChanged");
                ChildFragment.this.onBackStackChanged();
            }
        });
        getChildFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            final String TAG = FragmentManager.OnBackStackChangedListener.class.getSimpleName();

            @Override
            public void onBackStackChanged() {
                // Log.i(TAG, "onBackStackChanged");
                ChildFragment.this.onBackStackChanged();
            }
        });
    }

    protected void onBackStackChanged() {
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        Log.i(TAG, "onCreateOptionsMenu:this:" + this);

        // 가장 최상단의 프래그먼트라면 툴바의 메뉴를 모두 초기화한다.
        if (isPrimaryFragment()) {
            Log.i(TAG, "onCreateOptionsMenu:this:" + this + "|isPrimaryFragment:" + isPrimaryFragment());
            // Always clear.
            getToolbar().setNavigationIcon(null);
            getToolbar().setNavigationOnClickListener(null);
            getToolbar().getMenu().clear();
        }
    }

    // TODO: 메뉴가 보여질 때마다 계속 호출된다고 한다.
    // setHasOptionsMenu(true)를 할 경우 해당 프래그먼트가 가려지고 보일 때 무조건 계속 호출된다.
    // (가려질 때 호출되는건 ChildFragment의 setUserVisibleHint 메서드에 의한 것으로 보인다.)
    // Ref: https://developer.android.com/reference/android/app/Activity.html#onPrepareOptionsMenu(android.view.Menu)
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        Log.i(TAG, "onPrepareOptionsMenu:this:" + this);
        super.onPrepareOptionsMenu(menu);
        // onPrepareOptionsMenu는 백스택에 있을 때도 호출된다.
        // 따라서 보이지 않을 때는 호출되어서는 안된다.
        // Ref: https://stackoverflow.com/a/41862150/4729203
        if (!isPrimaryFragment()) {
            Log.i(TAG, "onPrepareOptionsMenu:this:" + this + "|isVisible:false");
            return;
        }
        resetToolbar();
        setupToolbar();
    }
}