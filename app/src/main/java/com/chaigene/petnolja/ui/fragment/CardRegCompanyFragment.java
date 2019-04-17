package com.chaigene.petnolja.ui.fragment;

import android.os.Bundle;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.chaigene.petnolja.R;
import com.chaigene.petnolja.ui.activity.CardRegistrationActivity;
import com.chaigene.petnolja.util.CommonUtil;

import java.util.concurrent.Callable;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.chaigene.petnolja.Constants.EXTRA_ACTION_STATUS;
import static com.chaigene.petnolja.ui.activity.CardRegistrationActivity.FRAGMENT_TAG_DETAIL;

// TODO: 로그인을 하지 않은 상태에서 DB에 접근하는 것이 안전한가?
public class CardRegCompanyFragment extends BaseFragment {
    public static final String TAG = "CardRegCompanyFragment";

    public static final int ACTION_STATUS_INITIAL_REGISTER = 0;

    @BindView(R.id.company_button_00)
    View mBtnCompany00;
    @BindView(R.id.company_button_01)
    View mBtnCompany01;
    @BindView(R.id.company_button_02)
    View mBtnCompany02;
    @BindView(R.id.company_button_03)
    View mBtnCompany03;
    @BindView(R.id.company_button_04)
    View mBtnCompany04;
    @BindView(R.id.company_button_05)
    View mBtnCompany05;
    @BindView(R.id.company_button_06)
    View mBtnCompany06;
    @BindView(R.id.company_button_07)
    View mBtnCompany07;
    @BindView(R.id.company_button_08)
    View mBtnCompany08;
    @BindView(R.id.company_button_09)
    View mBtnCompany09;
    @BindView(R.id.company_button_10)
    View mBtnCompany10;

    private View[] mCompanyButtons;

    private int mActionStatus;
    private int mCardCompany;

    // private boolean isProgressing;

    public static CardRegCompanyFragment newInstance(int actionStatus) {
        CardRegCompanyFragment fragment = new CardRegCompanyFragment();
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_ACTION_STATUS, actionStatus);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void readBundle(@Nullable Bundle bundle) {
        super.readBundle(bundle);
        mActionStatus = bundle.getInt(EXTRA_ACTION_STATUS);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView:savedInstanceState:" + savedInstanceState);
        mView = inflater.inflate(R.layout.fragment_card_reg_company, container, false);
        ButterKnife.bind(this, mView);

        mCompanyButtons = new View[]{
                mBtnCompany00,
                mBtnCompany01,
                mBtnCompany02,
                mBtnCompany03,
                mBtnCompany04,
                mBtnCompany05,
                mBtnCompany06,
                mBtnCompany07,
                mBtnCompany08,
                mBtnCompany09,
                mBtnCompany10
        };
        initView();

        return mView;
    }

    @Override
    protected void initView() {
        super.initView();
        setupCompanyButtons();
    }

    private void setupCompanyButtons() {
        for (int i = 0; i <= 10; i++) {
            View targetButton = mCompanyButtons[i];
            Log.i(TAG, "setupCompanyButtons:loop:index:" + i + "|targetButton:" + CommonUtil.getResourceName(targetButton));
            targetButton.setOnClickListener(null);
            final int companyIndex = i;
            targetButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onCompanyButtonClick(companyIndex);
                }
            });
        }
    }

    private void onCompanyButtonClick(int companyIndex) {
        Log.i(TAG, "onCompanyButtonClick:companyIndex:" + companyIndex);
        // if (isProgressing()) return;
        this.mCardCompany = companyIndex;
        showCardRegDetailFragment();
    }

    // TODO: 자연스럽게 뒤로 갈수 있게 Company 프래그먼트를 삭제하지 않는다.
    void showCardRegDetailFragment() {
        CardRegDetailFragment detailFragment = CardRegDetailFragment.newInstance(mActionStatus);
        ((CardRegistrationActivity) getActivity()).add(detailFragment, FRAGMENT_TAG_DETAIL);
        CommonUtil.delayCall(new Callable() {
            @Override
            public Object call() throws Exception {
                // ((CardRegistrationActivity) getActivity()).remove(CardRegCompanyFragment.this);
                return null;
            }
        }, 200, true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
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

    void finish() {
        getActivity().onBackPressed();
    }
}