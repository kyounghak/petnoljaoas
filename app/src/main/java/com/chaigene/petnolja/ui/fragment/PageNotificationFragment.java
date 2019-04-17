package com.chaigene.petnolja.ui.fragment;

import android.os.Bundle;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.chaigene.petnolja.R;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_ALL;
import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_FEED;
import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_TALENT;

public class PageNotificationFragment extends ChildFragment {
    public static final String TAG = "PageNotiFragment";

    private static final String FRAGMENT_TAG_ALL = String.valueOf(ARTICLE_TYPE_ALL);
    private static final String FRAGMENT_TAG_FEED = String.valueOf(ARTICLE_TYPE_FEED);
    private static final String FRAGMENT_TAG_TALENT = String.valueOf(ARTICLE_TYPE_TALENT);

    @BindView(R.id.tab_layout)
    TabLayout mTabLayout;

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        Log.i(TAG, "setUserVisibleHint:" + isVisibleToUser);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.page_fragment_notification, container, false);
        ButterKnife.bind(this, mView);
        initView();
        return mView;
    }

    @Override
    protected void initView() {
        super.initView();
        setupTabs();
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        getToolbar().setBackgroundResource(android.R.color.white);
    }

    private void setupTabs() {
        mTabLayout.addTab(mTabLayout.newTab().setText("ALL"), true);
        mTabLayout.addTab(mTabLayout.newTab().setText("PANDA"));

        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    showAllTab();
                }
                if (tab.getPosition() == 1) {
                    showTalentTab();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        showAllTab();
        // showTalentTab();
    }

    private void showAllTab() {
        Log.i(TAG, "showAllTab");

        Fragment allFragment = find(FRAGMENT_TAG_ALL);
        if (allFragment == null) {
            add(TabNotificationFragment.newInstance(ARTICLE_TYPE_ALL), FRAGMENT_TAG_ALL);
        } else {
            show(allFragment);
        }

        Fragment talentFragment = find(FRAGMENT_TAG_TALENT);
        if (talentFragment != null) hide(talentFragment);
    }

    /*@Deprecated
    private void showFeedTab() {
        Log.i(TAG, "showFeedTab");

        Fragment feedFragment = find(FRAGMENT_TAG_FEED);
        if (feedFragment == null) {
            add(TabNotificationFragment.newInstance(ARTICLE_TYPE_FEED), FRAGMENT_TAG_FEED);
        } else {
            show(feedFragment);
        }

        Fragment talentFragment = find(FRAGMENT_TAG_TALENT);
        if (talentFragment != null) hide(talentFragment);
    }*/

    private void showTalentTab() {
        Log.i(TAG, "showTalentTab");

        Fragment talentFragment = find(FRAGMENT_TAG_TALENT);
        if (talentFragment == null) {
            add(TabNotificationFragment.newInstance(ARTICLE_TYPE_TALENT), FRAGMENT_TAG_TALENT);
        } else {
            show(talentFragment);
        }

        Fragment feedFragment = find(FRAGMENT_TAG_FEED);
        if (feedFragment != null) hide(feedFragment);
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