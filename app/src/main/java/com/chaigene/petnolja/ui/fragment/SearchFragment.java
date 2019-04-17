package com.chaigene.petnolja.ui.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.chaigene.petnolja.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.chaigene.petnolja.Constants.SEARCH_TYPE_HASHTAG;
import static com.chaigene.petnolja.Constants.SEARCH_TYPE_USER;

public class SearchFragment extends ChildFragment {
    public static final String TAG = "SearchFragment";

    private static final String FRAGMENT_TAG_USER = String.valueOf(SEARCH_TYPE_USER);
    private static final String FRAGMENT_TAG_HASHTAG = String.valueOf(SEARCH_TYPE_HASHTAG);

    @BindView(R.id.search_input)
    EditText mEtSearchInput;

    @BindView(R.id.tab_layout)
    TabLayout mTabLayout;

    private TabSearchFragment mSelectedFragment;

    public static SearchFragment newInstance() {
        SearchFragment fragment = new SearchFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log.i(TAG, "onCreateView:savedInstanceState:" + savedInstanceState);
        mView = inflater.inflate(R.layout.fragment_search, container, false);
        ButterKnife.bind(this, mView);
        initView();
        return mView;
    }

    @Override
    protected void initView() {
        super.initView();
        setupSearchInput();
        setupTabs();
    }

    // Source: https://stackoverflow.com/a/35268540/4729203
    private void setupSearchInput() {
        mEtSearchInput.addTextChangedListener(new TextWatcher() {
            final String TAG = "TextWatcher";
            Handler handler = new Handler(Looper.getMainLooper());
            Runnable workRunnable;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(final Editable s) {
                handler.removeCallbacks(workRunnable);
                workRunnable = new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "afterTextChanged:s:" + s.toString());
                        String searchText = s.toString().trim();
                        Log.d(TAG, "afterTextChanged:mSelectedFragment:" + mSelectedFragment);
                        if (mSelectedFragment != null) mSelectedFragment.onSearchTextChanged(searchText);
                    }
                };
                handler.postDelayed(workRunnable, 500);
            }
        });
    }

    @OnClick(R.id.cancel_button)
    void cancel() {
        getActivity().finish();
    }


    private void setupTabs() {
        mTabLayout.addTab(mTabLayout.newTab().setText("이용자"), true);
        mTabLayout.addTab(mTabLayout.newTab().setText("게시물"));

        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    showUserTab();
                }
                if (tab.getPosition() == 1) {
                    showHashtagTab();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        showUserTab();
    }

    private void showUserTab() {
        Log.i(TAG, "showUserTab");

        Fragment userFragment = find(FRAGMENT_TAG_USER);
        if (userFragment == null) {
            userFragment = TabSearchFragment.newInstance(SEARCH_TYPE_USER);
            add(userFragment, FRAGMENT_TAG_USER);
        } else {
            show(userFragment);
        }

        Fragment hashtagFragment = find(FRAGMENT_TAG_HASHTAG);
        if (hashtagFragment != null) hide(hashtagFragment);

        mSelectedFragment = (TabSearchFragment) userFragment;
        Log.d(TAG, "showUserTab:mSelectedFragment:" + mSelectedFragment);

        String searchText = mEtSearchInput.getText().toString().trim();
        if (mSelectedFragment != null) mSelectedFragment.onSearchTextChanged(searchText);
    }

    private void showHashtagTab() {
        Log.i(TAG, "showHashtagTab");

        Fragment hashtagFragment = find(FRAGMENT_TAG_HASHTAG);
        if (hashtagFragment == null) {
            hashtagFragment = TabSearchFragment.newInstance(SEARCH_TYPE_HASHTAG);
            add(hashtagFragment, FRAGMENT_TAG_HASHTAG);
        } else {
            show(hashtagFragment);
        }

        Fragment userFragment = find(FRAGMENT_TAG_USER);
        if (userFragment != null) hide(userFragment);

        mSelectedFragment = (TabSearchFragment) hashtagFragment;

        String searchText = mEtSearchInput.getText().toString().trim();
        if (mSelectedFragment != null) mSelectedFragment.onSearchTextChanged(searchText);
    }

    private Fragment find(String tag) {
        Fragment fragment = getChildFragmentManager().findFragmentByTag(tag);
        Log.i(TAG, "find:tag:" + tag + "/result:" + fragment);
        return fragment;
    }

    protected void add(Fragment fragment, String tag) {
        Log.i(TAG, "add:" + fragment + " " + tag);

        if (fragment.isAdded()) {
            Log.d(TAG, "add:isAdded:true");
            return;
        }

        getChildFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, fragment, tag)
                .addToBackStack(null)
                .commit();
    }

    private void show(Fragment fragment) {
        Log.i(TAG, "show:" + fragment);

        getChildFragmentManager()
                .beginTransaction()
                .show(fragment)
                .commit();
    }

    private void hide(Fragment fragment) {
        Log.i(TAG, "hide:" + fragment);

        getChildFragmentManager()
                .beginTransaction()
                .hide(fragment)
                .commit();
    }
}
