package com.chaigene.petnolja.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.chaigene.petnolja.BuildConfig;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.adapter.CardListAdapter;
import com.chaigene.petnolja.model.Card;
import com.chaigene.petnolja.ui.dialog.DialogConfirmFragment;
import com.chaigene.petnolja.util.CommonUtil;
import com.chaigene.petnolja.util.ShopUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.chaigene.petnolja.Constants.CARD_REG_ACTIVITY;
import static com.chaigene.petnolja.Constants.COUNT_SHOP_CARD;
import static com.chaigene.petnolja.Constants.COUNT_SHOP_ORDER;
import static com.chaigene.petnolja.Constants.EXTRA_TARGET_CARD;
import static com.chaigene.petnolja.ui.activity.CardRegistrationActivity.ACTION_STATUS_STANDARD_REGISTER;

public class CardActivity extends BaseActivity {
    public static final String TAG = "CardActivity";

    private static final String ACTION_STATUS = "action_status";
    public static final int ACTION_STATUS_DEFAULT = 0;
    public static final int ACTION_STATUS_SELECT = 1;

    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;

    private int mActionStatus;

    private LinearLayoutManager mManager;
    private CardListAdapter mAdapter;
    private List<Card> mCards;

    private boolean mProgressing;

    public boolean isProgressing() {
        return mProgressing;
    }

    public void setProgressing(boolean progressing) {
        this.mProgressing = progressing;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 스크린 캡쳐 방지
        // Souce: https://stackoverflow.com/a/9822607/4729203
        if (!BuildConfig.DEBUG) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }

        setContentView(R.layout.activity_card);
        ButterKnife.bind(this);
        setupRecyclerView();
        showLoadingDialog();
        getCards(true).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(@NonNull Task<Void> task) throws Exception {
                dismissDialog();
                if (!task.isSuccessful()) {
                    Log.w(TAG, "ERROR:");
                }
                return null;
            }
        });
    }

    /*@Override
    public void onRefresh() {
        super.onRefresh();
        mOrders.clear();
        mAdapter.notifyDataSetChanged();
        showLoadingDialog();
        getOrders().continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(@NonNull Task<Void> task) throws Exception {
                dismissDialog();
                if (!task.isSuccessful()) {
                    Log.w(TAG, "ERROR:");
                }
                return null;
            }
        });
    }*/

    @Override
    protected void readIntent() {
        super.readIntent();

        mActionStatus = getIntent().getIntExtra(ACTION_STATUS, 0);

        /*mProductId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        mProductTitle = getIntent().getStringExtra(EXTRA_PRODUCT_TITLE);
        mProductPrice = getIntent().getStringExtra(EXTRA_PRODUCT_PRICE);
        mCoverPhoto = getIntent().getStringExtra(EXTRA_COVER_PHOTO);
        mRegions = getIntent().getStringArrayListExtra(EXTRA_TARGET_REGIONS);
        mQuantity = getIntent().getIntExtra(EXTRA_QUANTITY, 0);
        mShippingPrice = getIntent().getStringExtra(EXTRA_SHIPPING_PRICE);
        mSellerId = getIntent().getStringExtra(EXTRA_SELLER_ID);
        mSellerNickname = getIntent().getStringExtra(EXTRA_SELLER_NICKNAME);
        mCardId = getIntent().getStringExtra(EXTRA_CARD_ID);
        mInstallment = getIntent().getIntExtra(EXTRA_INSTALLMENT, 0);

        if (TextUtils.isEmpty(mProductId) ||
                TextUtils.isEmpty(mProductTitle) ||
                TextUtils.isEmpty(mProductPrice) ||
                TextUtils.isEmpty(mCoverPhoto) ||
                TextUtils.isEmpty(mSellerId) ||
                TextUtils.isEmpty(mSellerNickname) ||
                TextUtils.isEmpty(mCardId)) {
            Toast.makeText(getApplicationContext(), "주문정보에 오류가 있습니다. 잠시후 다시 시도해주세요.", Toast.LENGTH_LONG).show();
            finish();
        }*/
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setToolbarTitle("결제 카드");
    }

    @Override
    protected void initView() {
        super.initView();
        // setupSecurityPinInput();
        // scrambleDigitButtons();
    }

    private void setupRecyclerView() {
        Log.i(TAG, "setupRecyclerView");

        mManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mManager);

        // Source: http://stackoverflow.com/a/28828749/4729203
        mRecyclerView.setHasFixedSize(true);
        // false로 하게 되면 하단 부분이 짤리게 된다.
        mRecyclerView.setNestedScrollingEnabled(true);

        // Divider
        DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        divider.setDrawable(CommonUtil.getDrawable(this, R.drawable.shape_divider_notification));
        mRecyclerView.addItemDecoration(divider);

        mCards = new ArrayList<>();
        mAdapter = new CardListAdapter(this, mCards, mRecyclerView);
        mAdapter.setOnItemClickListener(new CardListAdapter.OnItemClickListener() {
            final String TAG = "OnItemClickListener";

            @Override
            public void onItemClick(final Card card) {
                Log.i(TAG, "onItemClick");
                if (!card.getIsPrimary()) {
                    DialogConfirmFragment confirmDialog = DialogConfirmFragment.newInstance(
                            "선택하신 카드를 주결제카드로 설정하시겠습니까?",
                            new DialogConfirmFragment.OnSelectListener() {
                                @Override
                                public void onConfirm() {
                                    super.onConfirm();
                                    showLoadingDialog();
                                    ShopUtil.setPrimaryCard(card.getId()).continueWith(new Continuation<Card, Void>() {
                                        @Override
                                        public Void then(@NonNull Task<Card> task) throws Exception {
                                            dismissDialog();
                                            if (!task.isSuccessful()) {
                                                Exception e = task.getException();
                                                Log.w(TAG, "changeStatus:ERROR:" + e.getMessage());
                                            }
                                            onDeny();
                                            return null;
                                        }
                                    });
                                }

                                @Override
                                public void onDeny() {
                                    super.onDeny();
                                    Intent in = new Intent();
                                    in.putExtra(EXTRA_TARGET_CARD, card);
                                    setResult(RESULT_OK, in);
                                    finish();
                                }
                            }
                    );
                    confirmDialog.show(getSupportFragmentManager(), null);
                } else {
                    Intent in = new Intent();
                    in.putExtra(EXTRA_TARGET_CARD, card);
                    setResult(RESULT_OK, in);
                    finish();
                }
            }
        });
        mAdapter.setOnLoadMoreListener(new CardListAdapter.OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                Log.i(TAG, "onLoadMore");

                // 로딩을 보여준다.
                getCards(false).continueWith(new Continuation<Void, Void>() {
                    @Override
                    public Void then(@NonNull Task<Void> task) throws Exception {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "onLoadMore:getCards:ERROR");
                        }
                        mAdapter.setLoading(false);
                        return null;
                    }
                });
            }
        });
        mRecyclerView.setAdapter(mAdapter);
    }

    /*private void startArticleFragment(Post post) {
        ArticleFragment articleFragment = ArticleFragment.newInstance(post);
        ((PageNotificationFragment) getParentFragment()).getRootFragment().add(articleFragment);
    }*/

    /*public void refresh() {
        Log.d(TAG, "refresh");
        mSRLContainer.setRefreshing(true);
        getArticles(true).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(@NonNull Task<Void> task) throws Exception {
                mSRLContainer.setRefreshing(false);
                if (!task.isSuccessful()) {
                    Log.w(TAG, "getArticles:ERROR");
                    return null;
                }
                Log.d(TAG, "getArticles:SUCCESS");
                return null;
            }
        });
    }*/

    private Task<Void> getCards(boolean isRefresh) {
        Log.i(TAG, "getCards");

        if (isRefresh) {
            mCards.clear();
            mAdapter.notifyDataSetChanged();
        }

        DocumentSnapshot cursor = null;
        if (!mCards.isEmpty()) cursor = mCards.get(mCards.size() - 1).getDocumentSnapshot();
        Task<List<Card>> getCardListTask = ShopUtil.getCardList(
                COUNT_SHOP_ORDER,
                cursor
        );
        return getCardListTask.continueWith(new Continuation<List<Card>, Void>() {
            @Override
            public Void then(@NonNull Task<List<Card>> task) throws Exception {
                mAdapter.hideLoading();

                if (!task.isSuccessful()) {
                    // ERROR
                    Exception e = task.getException();
                    Log.w(TAG, "getCards:ERROR:" + e.getMessage());
                    throw e;
                }

                List<Card> cards = task.getResult();

                if (cards.size() < COUNT_SHOP_CARD) mAdapter.setLastDataReached(true);

                for (Card card : cards) {
                    Log.d(TAG, "getCards:card:" +
                            card.getDocumentSnapshot().getId() +
                            "=>" +
                            card.getDocumentSnapshot().getData()
                    );
                    mCards.add(card);
                    int currentIndex = mCards.indexOf(card);
                    mAdapter.notifyItemInserted(currentIndex);
                }
                return null;
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, CommonUtil.format("onActivityResult:requestCode:%d|resultCode:%d", requestCode, resultCode));
        switch (requestCode) {
            case CARD_REG_ACTIVITY:
                if (resultCode == RESULT_OK) {
                    showLoadingDialog();
                    getCards(true).continueWith(new Continuation<Void, Void>() {
                        @Override
                        public Void then(@NonNull Task<Void> task) throws Exception {
                            dismissDialog();
                            if (!task.isSuccessful()) {
                                // ERROR
                            }
                            return null;
                        }
                    });
                }

                if (resultCode == RESULT_CANCELED) {

                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_activity_card, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_CANCELED);
                finish();
                return true;
            case R.id.action_reg:
                Intent in = CardRegistrationActivity.createIntent(this, ACTION_STATUS_STANDARD_REGISTER);
                startActivityForResult(in, CARD_REG_ACTIVITY);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static Intent createIntent(Context context, int actionStatus) {
        Context c = context.getApplicationContext();
        Intent intent = new Intent().setClass(c, CardActivity.class);
        intent.putExtra(ACTION_STATUS, actionStatus);
        return intent;
    }
}