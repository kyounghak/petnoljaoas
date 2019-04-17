package com.chaigene.petnolja.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.manager.ConfigManager;
import com.chaigene.petnolja.model.Card;
import com.chaigene.petnolja.model.ErrorCode;
import com.chaigene.petnolja.ui.activity.SecurityPinActivity;
import com.chaigene.petnolja.util.CommonUtil;
import com.chaigene.petnolja.util.ShopUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.chaigene.petnolja.Constants.EXTRA_ACTION_STATUS;
import static com.chaigene.petnolja.model.Request.STATUS_SUCCESS;
import static com.chaigene.petnolja.ui.activity.SecurityPinActivity.ACTION_STATUS_REGISTER;

public class CardRegDetailFragment extends BaseFragment {
    public static final String TAG = "CardRegDetailFragment";

    public static final int ACTION_STATUS_INITIAL_REGISTER = 0;
    public static final int ACTION_STATUS_STANDARD_REGISTER = 1;

    @BindView(R.id.card_no_input_1)
    EditText mEtCardNo1;

    @BindView(R.id.card_no_input_2)
    EditText mEtCardNo2;

    @BindView(R.id.card_no_input_3)
    EditText mEtCardNo3;

    @BindView(R.id.card_no_input_4)
    EditText mEtCardNo4;

    @BindView(R.id.exp_date_month_input)
    EditText mEtExpDateMonth;

    @BindView(R.id.exp_date_year_input)
    EditText mEtExpDateYear;

    @BindView(R.id.pin_input)
    EditText mEtPin;

    @BindView(R.id.confirm_button)
    Button mBtnConfirm;

    private int mActionStatus;
    private int mCardCompany;

    // private boolean isProgressing;

    public static CardRegDetailFragment newInstance(int actionStatus) {
        CardRegDetailFragment fragment = new CardRegDetailFragment();
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
        mView = inflater.inflate(R.layout.fragment_card_reg_detail, container, false);
        ButterKnife.bind(this, mView);
        initView();
        return mView;
    }

    @Override
    protected void initView() {
        super.initView();
        setupCardNoInput();
        setupExpDateInput();
        setupPinInput();
    }

    private void setupCardNoInput() {
        mEtCardNo1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() == 4) mEtCardNo2.requestFocus();
                updateConfirmBtn();
            }
        });
        mEtCardNo2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() == 4) mEtCardNo3.requestFocus();
                updateConfirmBtn();
            }
        });
        mEtCardNo3.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() == 4) mEtCardNo4.requestFocus();
                updateConfirmBtn();
            }
        });
        mEtCardNo4.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() == 4) mEtExpDateMonth.requestFocus();
                updateConfirmBtn();
            }
        });
    }

    private void setupExpDateInput() {
        mEtExpDateMonth.setFilters(new InputFilter[]{new InputFilter() {
            final String TAG = "InputFilter";
            final int MIN_VALUE = 1;
            final int MAX_VALUE = 12;

            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                try {
                    Log.i(TAG, "filter:source:" + source +
                            "|start:" + start +
                            "|end:" + end +
                            "|dest:" + dest.toString() +
                            "|dstart:" + dstart +
                            "|dend:" + dend
                    );
                    // return null;
                    // if (dest.toString().length() == 1) return null;

                    // Remove the string out of destination that is to be replaced
                    String newVal = dest.toString().substring(0, dstart) + dest.toString().substring(dend, dest.toString().length());
                    Log.i(TAG, "filter:newVal1:" + newVal);

                    // Add the new string in
                    newVal = newVal.substring(0, dstart) + source.toString() + newVal.substring(dstart, newVal.length());
                    Log.i(TAG, "filter:newVal2:" + newVal);

                    int input = Integer.parseInt(newVal);

                    if (dstart == 0) {
                        if (isInRange(0, 1, input)) return null;
                    } else {
                        if (isInRange(MIN_VALUE, MAX_VALUE, input)) return null;
                    }
                } catch (NumberFormatException nfe) {
                }
                return "";
            }

            private boolean isInRange(int a, int b, int c) {
                return b > a ? c >= a && c <= b : c >= b && c <= a;
            }
        }});
        mEtExpDateYear.setFilters(new InputFilter[]{new InputFilter() {
            final int MIN_VALUE = 17;
            final int MAX_VALUE = 99;

            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                try {
                    // Remove the string out of destination that is to be replaced
                    String newVal = dest.toString().substring(0, dstart) + dest.toString().substring(dend, dest.toString().length());
                    // Add the new string in
                    newVal = newVal.substring(0, dstart) + source.toString() + newVal.substring(dstart, newVal.length());
                    int input = Integer.parseInt(newVal);

                    if (dstart == 0) {
                        if (isInRange(1, 9, input)) return null;
                    } else {
                        if (isInRange(MIN_VALUE, MAX_VALUE, input)) return null;
                    }
                } catch (NumberFormatException nfe) {
                }
                return "";
            }

            private boolean isInRange(int a, int b, int c) {
                return b > a ? c >= a && c <= b : c >= b && c <= a;
            }
        }});
        mEtExpDateMonth.addTextChangedListener(new TextWatcher() {
            final String originalHint = mEtExpDateMonth.getHint().toString();

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (TextUtils.isEmpty(editable.toString().trim())) {
                    mEtExpDateMonth.setHint(originalHint);
                } else {
                    mEtExpDateMonth.setHint(null);
                }
                if (editable.length() == 2) mEtExpDateYear.requestFocus();
                updateConfirmBtn();
            }
        });
        mEtExpDateYear.addTextChangedListener(new TextWatcher() {
            final String originalHint = mEtExpDateYear.getHint().toString();

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (TextUtils.isEmpty(editable.toString().trim())) {
                    mEtExpDateYear.setHint(originalHint);
                } else {
                    mEtExpDateYear.setHint(null);
                }
                if (editable.length() == 2) mEtPin.requestFocus();
                updateConfirmBtn();
            }
        });
    }

    private void setupPinInput() {
        mEtPin.addTextChangedListener(new TextWatcher() {
            final String originalHint = mEtPin.getHint().toString();

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (TextUtils.isEmpty(editable.toString().trim())) {
                    mEtPin.setHint(originalHint);
                } else {
                    mEtPin.setHint(null);
                }
                updateConfirmBtn();
            }
        });
    }

    private void updateConfirmBtn() {
        Log.i(TAG, "updateConfirmBtn");
        boolean isEnabled = true;
        if (mEtCardNo1.getText().toString().trim().length() < 4) {
            isEnabled = false;
        }
        if (mEtCardNo2.getText().toString().trim().length() < 4) {
            isEnabled = false;
        }
        if (mEtCardNo3.getText().toString().trim().length() < 4) {
            isEnabled = false;
        }
        if (mEtCardNo4.getText().toString().trim().length() < 4) {
            isEnabled = false;
        }
        if (mEtExpDateMonth.getText().toString().trim().length() < 2) {
            isEnabled = false;
        }
        if (mEtExpDateYear.getText().toString().trim().length() < 2) {
            isEnabled = false;
        }
        if (mEtPin.getText().toString().trim().length() < 2) {
            isEnabled = false;
        }
        mBtnConfirm.setEnabled(isEnabled);
    }

    // ShopUtil을 통해 request 해야 함.
    @OnClick(R.id.confirm_button)
    void confirm() {
        Log.i(TAG, "confirm");
        // TODO: 결제비밀번호를 입력하는 화면을 보여준다.
        // 카드를 등록할 때는 무조건 결제비밀번호를 입력해야 하는지가 의문.

        String cardNo1 = mEtCardNo1.getText().toString().trim();
        String cardNo2 = mEtCardNo2.getText().toString().trim();
        String cardNo3 = mEtCardNo3.getText().toString().trim();
        String cardNo4 = mEtCardNo4.getText().toString().trim();

        String expDateMM = mEtExpDateMonth.getText().toString().trim();
        String expDateYY = mEtExpDateYear.getText().toString().trim();

        String pin = mEtPin.getText().toString().trim();

        boolean isPrimary;
        if (mActionStatus == ACTION_STATUS_INITIAL_REGISTER) {
            isPrimary = true;
        } else {
            isPrimary = false;
        }

        showLoadingDialog();

        boolean isFakePaymentMode = ConfigManager.getInstance(getContext()).isFakePaymentMode();
        Task<Card> insertCardTask;
        if (!isFakePaymentMode) {
            insertCardTask = ShopUtil.insertCard(
                    cardNo1,
                    cardNo2,
                    cardNo3,
                    cardNo4,
                    expDateMM,
                    expDateYY,
                    pin,
                    isPrimary
            );
        } else {
            insertCardTask = ShopUtil.fakeInsertCard(
                    cardNo1,
                    cardNo2,
                    cardNo3,
                    cardNo4,
                    expDateMM,
                    expDateYY,
                    pin,
                    isPrimary
            );
        }
        insertCardTask.continueWith(new Continuation<Card, Void>() {
            @Override
            public Void then(@NonNull Task<Card> task) throws Exception {
                dismissDialog();
                if (!task.isSuccessful()) {
                    Log.w(TAG, "confirm:insertCard:ERROR:" + task.getException());
                    return null;
                }
                Card resCard = task.getResult();
                /*if (resCard.getStatus() == STATUS_REG_FAIL) {
                    CommonUtil.showSnackbar(getActivity(), "카드정보를 잘못 입력하셨거나 사용할 수 없는 카드입니다. 다시 확인하시고 시도해주세요.");
                    return null;
                }*/
                if (resCard.getResponseStatus() != STATUS_SUCCESS) {
                    switch (resCard.getResponseCode()) {
                        case ErrorCode.CARD_NOT_ALLOWED_CARD_COMPANY:
                            CommonUtil.showSnackbar(getActivity(), "취급하지 않는 카드사입니다.");
                            break;
                        case ErrorCode.CARD_DIFFRENT_USERNAME:
                            CommonUtil.showSnackbar(getActivity(), "인증된 정보와 카드의 명의자가 다릅니다.");
                            break;
                        case ErrorCode.CARD_INVALID_NO:
                            CommonUtil.showSnackbar(getActivity(), "유효하지않은 카드번호를 입력하셨습니다.");
                            break;
                        default:
                            String errorMessage = resCard.getResponseMessage() != null ?
                                    resCard.getResponseMessage() :
                                    "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
                            CommonUtil.showSnackbar(getActivity(), errorMessage);
                            break;
                    }
                    return null;
                }
                onRegisterFinish();
                return null;
            }
        });
    }

    private void onRegisterFinish() {
        if (mActionStatus == ACTION_STATUS_INITIAL_REGISTER) {
            Intent in = SecurityPinActivity.createIntent(getContext(), ACTION_STATUS_REGISTER);
            startActivity(in);
            getActivity().setResult(RESULT_OK);
            getActivity().finish();
        }
        if (mActionStatus == ACTION_STATUS_STANDARD_REGISTER) {
            getActivity().setResult(RESULT_OK);
            getActivity().finish();
        }
    }

    /*void showCardRegDetailFragment() {
        // LoginSignupBeginFragment signupBeginFragment = LoginSignupBeginFragment.newInstance();
        // ((CardRegistrationActivity) getActivity()).add(signupBeginFragment, FRAGMENT_TAG_SIGNUP_BEGIN);
        *//*CommonUtil.delayCall(new Callable() {
            @Overrideasdfasdf
            public Object call() throws Exception {
                // ((CardRegistrationActivity) getActivity()).remove(CardRegCompanyFragment.this);
                return null;
            }
        }, 200, true);*//*
    }*/

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