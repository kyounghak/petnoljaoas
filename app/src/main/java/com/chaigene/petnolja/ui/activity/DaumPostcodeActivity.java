package com.chaigene.petnolja.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.chaigene.petnolja.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DaumPostcodeActivity extends BaseActivity {

    @BindView(R.id.webview)
    WebView mWebView;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daum_postcode);
        ButterKnife.bind(this);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.addJavascriptInterface(new DaumPostcodeInterface(), "Android");
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                mWebView.loadUrl("javascript:execDaumPostcode()");
            }
        });
        mWebView.loadUrl("https://pandazmaker.com/daum.html");
        // mWebView.loadUrl("http://awsk.cafe24.com/privates/panda/daum.html");
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

    private class DaumPostcodeInterface {
        @JavascriptInterface
        public void processData(String data) {
            Bundle extra = new Bundle();
            Intent intent = new Intent();
            extra.putString("address", data);
            intent.putExtras(extra);
            setResult(RESULT_OK, intent);
            finish();
        }
    }
}