package com.chaigene.petnolja.ui.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.chaigene.petnolja.R;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.chaigene.petnolja.Constants.EXTRA_TOOLBAR_TITLE;
import static com.chaigene.petnolja.Constants.EXTRA_URL;

public class SettingWebViewFragment extends ChildFragment {
    public static final String TAG = "SettingWebViewFragment";

    @BindView(R.id.web_view)
    WebView mWebView;

    private String mToolbarTitle;
    private String mUrl;

    public static SettingWebViewFragment newInstance(String toolbarTitle, String url) {
        SettingWebViewFragment fragment = new SettingWebViewFragment();

        Bundle args = new Bundle();
        args.putString(EXTRA_TOOLBAR_TITLE, toolbarTitle);
        args.putString(EXTRA_URL, url);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    protected void readBundle(@Nullable Bundle bundle) {
        super.readBundle(bundle);
        mToolbarTitle = bundle.getString(EXTRA_TOOLBAR_TITLE);
        mUrl = bundle.getString(EXTRA_URL);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView:savedInstanceState:" + savedInstanceState);
        mView = inflater.inflate(R.layout.fragment_setting_web_view, container, false);
        ButterKnife.bind(this, mView);
        initView();
        return mView;
    }

    @SuppressLint("SetJavaScriptEnabled")
    protected void initView() {
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // mWebView.loadUrl("javascript:execDaumPostcode()");
            }
        });
        mWebView.loadUrl(mUrl);
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        setToolbarTitle(mToolbarTitle);
        setToolbarTitleAlign(Gravity.CENTER_HORIZONTAL);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (isPrimaryFragment()) {
            getToolbar().setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
            getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
                final String TAG = "OnClickListener";

                @Override
                public void onClick(View v) {
                    Log.i(TAG, "onClick");
                    finish();
                }
            });
        }
    }

    private void finish() {
        // 임시 조취.
        getActivity().onBackPressed();
    }
}