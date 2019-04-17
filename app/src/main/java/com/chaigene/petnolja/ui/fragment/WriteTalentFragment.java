package com.chaigene.petnolja.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.chaigene.petnolja.R;
import com.chaigene.petnolja.ui.activity.WriteActivity;
import com.chaigene.petnolja.util.CommonUtil;

import java.util.List;
import java.util.concurrent.Callable;

import butterknife.BindView;
import butterknife.ButterKnife;

public class WriteTalentFragment extends BaseFragment {
    public static final String TAG = "WriteTalentFragment";

    @BindView(R.id.product_title_input)
    EditText mEtProductTitleInput;

    @BindView(R.id.product_price_input)
    EditText mEtProductPriceInput;

    @BindView(R.id.shipping_price_input)
    EditText mEtShippingPriceInput;

    @BindView(R.id.product_services_input)
    EditText mEtProductServicesInput;

    public static WriteTalentFragment newInstance() {
        WriteTalentFragment fragment = new WriteTalentFragment();

        // Bundle args = new Bundle();
        // fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView:savedInstanceState:" + savedInstanceState);
        mView = inflater.inflate(R.layout.fragment_write_talent, container, false);
        ButterKnife.bind(this, mView);

        initView();

        return mView;
    }

    // 글 수정일 때 해야하는 작업은 이미지 다운로드 및 콘텐츠 삽입하기
    protected void initView() {
        /*if (!isModify()) {

        } else {
            // mEtContentInput.setText(mPost.getContent());
        }*/

        mEtProductPriceInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.i(TAG, "afterTextChanged:s:" + s.toString());
                if (!TextUtils.isEmpty(s.toString())) {
                    mEtProductPriceInput.removeTextChangedListener(this);
                    String result = CommonUtil.format("%,d", Long.parseLong(s.toString().replace(",", "")));
                    mEtProductPriceInput.setText(result);
                    mEtProductPriceInput.setSelection(result.length());
                    mEtProductPriceInput.addTextChangedListener(this);
                }
            }
        });

        mEtShippingPriceInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.i(TAG, "afterTextChanged:s:" + s.toString());
                if (!TextUtils.isEmpty(s.toString())) {
                    mEtShippingPriceInput.removeTextChangedListener(this);
                    String result = CommonUtil.format("%,d", Long.parseLong(s.toString().replace(",", "")));
                    mEtShippingPriceInput.setText(result);
                    mEtShippingPriceInput.setSelection(result.length());
                    mEtShippingPriceInput.addTextChangedListener(this);
                }
            }
        });
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        Log.i(TAG, "onHiddenChanged:hidden:" + hidden);
        if (!hidden) return;

        String productTitle = mEtProductTitleInput.getText().toString().trim();
        setProductTitle(productTitle);

        String productPrice = mEtProductPriceInput.getText().toString().trim().replace(",", "");
        setProductPrice(productPrice);

        String shippingPrice = mEtShippingPriceInput.getText().toString().trim().replace(",", "");
        setShippingPrice(shippingPrice);

        String productServices = mEtProductServicesInput.getText().toString().trim();
        setProductServices(productServices);
    }

    @Override
    public void onPause() {
        super.onPause();
        String productTitle = mEtProductTitleInput.getText().toString().trim();
        setProductTitle(productTitle);

        String productPrice = mEtProductPriceInput.getText().toString().trim().replace(",", "");
        setProductPrice(productPrice);

        String shippingPrice = mEtShippingPriceInput.getText().toString().trim().replace(",", "");
        setShippingPrice(shippingPrice);

        String productServices = mEtProductServicesInput.getText().toString().trim();
        setProductServices(productServices);
    }

    @Override
    public void onResume() {
        super.onResume();
        mEtProductTitleInput.setText(getProductTitle());
        mEtProductPriceInput.setText(getProductPrice());
        mEtShippingPriceInput.setText(getShippingPrice());
        mEtProductServicesInput.setText(getProductServices());
    }

    private void setArticleType(int articleType) {
        ((WriteActivity) getActivity()).setArticleType(articleType);
    }

    private int getArticleType() {
        return ((WriteActivity) getActivity()).getArticleType();
    }

    public void setImageUris(List<Uri> imageUris) {
        ((WriteActivity) getActivity()).setImageUris(imageUris);
    }

    public List<Uri> getImageUris() {
        return ((WriteActivity) getActivity()).getImageUris();
    }

    public void setContent(String content) {
        ((WriteActivity) getActivity()).setContent(content);
    }

    public String getContent() {
        return ((WriteActivity) getActivity()).getContent();
    }

    public void setHashtags(List<String> hashtags) {
        ((WriteActivity) getActivity()).setHashtags(hashtags);
    }

    public List<String> getHashtags() {
        return ((WriteActivity) getActivity()).getHashtags();
    }

    public void setMentions(List<String> mentions) {
        ((WriteActivity) getActivity()).setMentions(mentions);
    }

    public List<String> getMentions() {
        return ((WriteActivity) getActivity()).getMentions();
    }

    public int getProductType() {
        return ((WriteActivity) getActivity()).getProductType();
    }

    public void setProductType(int productType) {
        ((WriteActivity) getActivity()).setProductType(productType);
    }

    public void setProductTitle(String productTitle) {
        ((WriteActivity) getActivity()).setProductTitle(productTitle);
    }

    public String getProductTitle() {
        return ((WriteActivity) getActivity()).getProductTitle();
    }

    public void setProductPrice(String productPrice) {
        ((WriteActivity) getActivity()).setProductPrice(productPrice);
    }

    public String getProductPrice() {
        return ((WriteActivity) getActivity()).getProductPrice();
    }

    public void setShippingPrice(String shippingPrice) {
        ((WriteActivity) getActivity()).setShippingPrice(shippingPrice);
    }

    public String getShippingPrice() {
        return ((WriteActivity) getActivity()).getShippingPrice();
    }

    public void setProductServices(String productServices) {
        ((WriteActivity) getActivity()).setProductServices(productServices);
    }

    public String getProductServices() {
        return ((WriteActivity) getActivity()).getProductServices();
    }

    private void insertArticle() {
        CommonUtil.clearFocus(mEtProductTitleInput);
        CommonUtil.clearFocus(mEtProductPriceInput);
        CommonUtil.clearFocus(mEtShippingPriceInput);
        CommonUtil.clearFocus(mEtProductServicesInput);
        CommonUtil.hideKeyboard(getActivity());

        String productTitle = mEtProductTitleInput.getText().toString().trim();
        String productPrice = mEtProductPriceInput.getText().toString().trim().replace(",", "");
        String shippingPrice = mEtShippingPriceInput.getText().toString().trim().replace(",", "");
        String productServices = mEtProductServicesInput.getText().toString().trim();

        if (TextUtils.isEmpty(productTitle)) {
            CommonUtil.showSnackbar(getActivity(), "상품의 제목을 입력해주세요.");
            mEtProductTitleInput.requestFocus();
            CommonUtil.delayCall(new Callable() {
                @Override
                public Object call() throws Exception {
                    CommonUtil.showKeyboard(getActivity());
                    return null;
                }
            }, 1000);
            return;
        }

        if (TextUtils.isEmpty(productPrice)) {
            CommonUtil.showSnackbar(getActivity(), "상품의 가격을 입력해주세요.");
            mEtProductPriceInput.requestFocus();
            CommonUtil.delayCall(new Callable() {
                @Override
                public Object call() throws Exception {
                    CommonUtil.showKeyboard(getActivity());
                    return null;
                }
            }, 1000);
            return;
        }

        int productPriceInt = CommonUtil.toInt(productPrice);
        if (productPriceInt < 5000) {
            CommonUtil.showSnackbar(getActivity(), "가격은 5,000원 이상만 입력 가능합니다.");
            mEtProductPriceInput.requestFocus();
            CommonUtil.delayCall(new Callable() {
                @Override
                public Object call() throws Exception {
                    CommonUtil.showKeyboard(getActivity());
                    return null;
                }
            }, 1000);
            return;
        }

        if (TextUtils.isEmpty(shippingPrice)) {
            shippingPrice = "0";
        }

        setProductTitle(productTitle);
        setProductPrice(productPrice);
        setShippingPrice(shippingPrice);
        setProductServices(productServices);

        ((WriteActivity) getActivity()).updateArticle(
                getArticleType(),
                getImageUris(),
                getContent(),
                getHashtags(),
                getHashtags(),
                getProductType(),
                getProductTitle(),
                getProductPrice(),
                getShippingPrice(),
                getProductServices()
        );
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, CommonUtil.format("onActivityResult:requestCode:%d/resultCode:%d", requestCode, resultCode));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        getToolbar().getMenu().clear();
        inflater.inflate(R.menu.menu_fragment_write_talent, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_confirm: {
                insertArticle();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}