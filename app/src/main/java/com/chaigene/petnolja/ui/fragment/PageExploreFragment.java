package com.chaigene.petnolja.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.chaigene.petnolja.R;
import com.chaigene.petnolja.ui.activity.SearchActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.chaigene.petnolja.Constants.ARTICLE_SCOPE_ALL;
import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_ALL;
import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_FEED;
import static com.chaigene.petnolja.Constants.ARTICLE_TYPE_TALENT;

public class PageExploreFragment extends ChildFragment {
    public static final String TAG = "PageExploreFragment";

    private static final String FRAGMENT_TAG_FEED = String.valueOf(ARTICLE_TYPE_FEED);
    private static final String FRAGMENT_TAG_TALENT = String.valueOf(ARTICLE_TYPE_TALENT);

    @BindView(R.id.refresh_layout)
    SwipeRefreshLayout mRefreshLayout;

    @BindView(R.id.tab_layout)
    TabLayout mTabLayout;

    private TabArticleFragment mSelectedFragment;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.i(TAG, "onAttach");
    }

    // onCreate ->

    // onCreateView ->

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.i(TAG, "onActivityCreated");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "onStop");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.i(TAG, "onDestroyView");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i(TAG, "onDetach");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.page_fragment_explore, container, false);
        ButterKnife.bind(this, mView);
        initView();
        mRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    final String TAG = "OnRefreshListener";

                    @Override
                    public void onRefresh() {
                        Log.d(TAG, "onRefresh");
                        mRefreshLayout.setRefreshing(false);
                        if (mSelectedFragment != null) mSelectedFragment.onRefresh();
                    }
                }
        );
        return mView;
    }

    public void refresh() {
        Log.d(TAG, "refresh");
        // mRefreshLayout.setRefreshing(true);
        /*getArticles(true).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(@NonNull Task<Void> task) throws Exception {
                mRefreshLayout.setRefreshing(false);
                if (!task.isSuccessful()) {
                    Log.w(TAG, "getArticles:ERROR");
                    return null;
                }
                Log.d(TAG, "getArticles:SUCCESS");
                return null;
            }
        });*/
    }

    @Override
    protected void initView() {
        super.initView();
        // setupScollContainer();
        setupTabs();
    }

    private void setupScollContainer() {
        /*mNsScrollContainer.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView nestedScrollView, int i, int i1, int i2, int i3) {

            }
        });*/
    }

    private void setupTabs() {
        mTabLayout.addTab(mTabLayout.newTab().setText("ALL"), true);
        mTabLayout.addTab(mTabLayout.newTab().setText("PANDA"));

        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    showFeedTab();
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
        showFeedTab();
    }

    private void showFeedTab() {
        Log.i(TAG, "showFeedTab");

        Fragment feedFragment = find(FRAGMENT_TAG_FEED);
        if (feedFragment == null) {
            feedFragment = TabArticleFragment.newInstance(ARTICLE_SCOPE_ALL, ARTICLE_TYPE_ALL, null);
            add(feedFragment, FRAGMENT_TAG_FEED);
        } else {
            show(feedFragment);
        }

        Fragment talentFragment = find(FRAGMENT_TAG_TALENT);
        if (talentFragment != null) hide(talentFragment);

        mSelectedFragment = (TabArticleFragment) feedFragment;
    }

    private void showTalentTab() {
        Log.i(TAG, "showTalentTab");

        Fragment talentFragment = find(FRAGMENT_TAG_TALENT);
        if (talentFragment == null) {
            talentFragment = TabArticleFragment.newInstance(ARTICLE_SCOPE_ALL, ARTICLE_TYPE_TALENT, null);
            add(talentFragment, FRAGMENT_TAG_TALENT);
        } else {
            show(talentFragment);
        }

        Fragment feedFragment = find(FRAGMENT_TAG_FEED);
        if (feedFragment != null) hide(feedFragment);

        mSelectedFragment = (TabArticleFragment) talentFragment;
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

    /*@OnClick(R.id.search_container)
    void startSearchFragment() {
        SearchFragment searchFragment = SearchFragment.newInstance();
        getRootFragment().add(searchFragment);
    }*/

    @OnClick(R.id.search_container)
    void startSearchActivity() {
        Intent intent = createIntent(SearchActivity.class);
        startActivity(intent);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        Log.i(TAG, "onCreateOptionsMenu");

        if (isPrimaryFragment()) {
            // inflater.inflate(R.menu.menu_fragment_profile, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // int itemId = item.getItemId();
        return super.onOptionsItemSelected(item);
    }
}