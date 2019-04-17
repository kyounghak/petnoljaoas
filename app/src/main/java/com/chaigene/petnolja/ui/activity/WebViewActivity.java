package com.chaigene.petnolja.ui.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.chaigene.petnolja.R;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.chaigene.petnolja.Constants.EXTRA_URL;

public class WebViewActivity extends BaseActivity {

    @BindView(R.id.webview)
    WebView mWebView;

    private String mUrl;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        ButterKnife.bind(this);
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
    protected void readIntent() {
        super.readIntent();
        mUrl = getIntent().getStringExtra(EXTRA_URL);
        /*if (TextUtils.isEmpty(mProductId) ||
                TextUtils.isEmpty(mProductTitle) ||
                TextUtils.isEmpty(mProductPrice) ||
                TextUtils.isEmpty(mCoverPhoto) ||
                TextUtils.isEmpty(mSellerId) ||
                TextUtils.isEmpty(mSellerNickname)) {
            Toast.makeText(getApplicationContext(), "주문정보에 오류가 있습니다. 잠시후 다시 시도해주세요.", Toast.LENGTH_LONG).show();
            finish();
        }*/
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setToolbarTitle("주소검색");
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_CANCELED);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static Intent createIntent(Context context, String url) {
        Context c = context.getApplicationContext();
        Intent intent = new Intent().setClass(c, WebViewActivity.class);
        intent.putExtra(EXTRA_URL, url);
        return intent;
    }
}