package com.chaigene.petnolja.ui.fragment;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.messaging.RemoteMessage;
import com.chaigene.petnolja.ui.activity.BaseActivity;
import com.chaigene.petnolja.ui.activity.ChildActivity;

import static com.chaigene.petnolja.Constants.EXTRA_REMOTE_MESSAGE;

public class BaseFragment extends Fragment {
    private static final String TAG = "BaseFragment";

    public static final int RESULT_CANCELED = 0;
    public static final int RESULT_OK = -1;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    /*private OnFragmentInteractionListener mListener;*/

    ViewGroup mVGLoading;
    protected ProgressDialog mProgressDialog;

    @Nullable
    protected View mView;

    // TODO: 이 값은 요청한 개수보다 적은 양의 값이 반환되었을 때
    // 최초의 값에 도달했는지 여부를 설정하기 위한 필드이다.
    private boolean mLastDataReached;

    protected synchronized boolean isLastDataReached() {
        return mLastDataReached;
    }

    protected synchronized void setLastDataReached(boolean lastDataReached) {
        this.mLastDataReached = lastDataReached;
    }

    protected void checkInitialDataReached(DataSnapshot dataSnapshot, int requestedLimit) {
        int count = (int) dataSnapshot.getChildrenCount();
        Log.d(TAG, "checkInitialDataReached:count:" + count + "|requestedLimit:" + requestedLimit + "|" + (count < requestedLimit));
        if (count < requestedLimit) setLastDataReached(true);
        else setLastDataReached(false);
    }

    public BaseFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }*/

        readBundle(getArguments());

        // activate fragment menu
        setHasOptionsMenu(true);
    }

    protected void readBundle(@Nullable Bundle bundle) {
        if (bundle == null) return;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        View view = super.onCreateView(inflater, container, savedInstanceState);
        // resetToolbar();
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Log.i(TAG, "onActivityCreated");
        // if (this instanceof ChildFragment) resetToolbar();
    }

    protected void initView() {
        // setupToolbar();
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        /*if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }*/
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Log.i(TAG, "onAttach");
        /*if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }*/
    }

    @Override
    public void onDetach() {
        super.onDetach();
        /*mListener = null;*/
    }

    @Override
    public void onDestroy() {
        dismissDialog();
        super.onDestroy();
    }

    @Override
    public void onResume() {
        // Log.i(TAG, "onResume");
        super.onResume();
        // startFCMReceiver();
        // initViewPagerListener();
    }

    @Override
    public void onPause() {
        // Log.i(TAG, "onPause");
        super.onPause();
        // stopFCMReceiver();
        // releaseViewPagerListener();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public interface OnAttatchListener {
        void onAttatch(String tag);
    }

    protected OnAttatchListener mOnAttatchListener;

    public void setOnAttatchListener(OnAttatchListener l) {
        this.mOnAttatchListener = l;
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        return super.onCreateAnimation(transit, enter, nextAnim);
    }

    protected void setupToolbar() {
        Log.i(TAG, "setupToolbar");
        // mToolbar.setTitleTextColor ( getResources ().getColor ( R.color.main_title ) );
        // setSupportActionBar ( mToolbar );

        // ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        // actionBar.setDisplayHomeAsUpEnabled(true);
        // actionBar.setHomeAsUpIndicator(R.drawable.ic_action_back);


        // getToolbar().setTitleTextColor(CommonUtil.getColor(this, android.R.color.black));
        //toolbar.setNavigationIcon(R.drawable.ic_menu_white);
        // setSupportActionBar(mToolbar);
        // setToolbarTitle(null);
    }

    protected void resetToolbar() {
        Log.i(TAG, "resetToolbar");
        ((BaseActivity) getActivity()).resetToolbar();
    }

    protected ActionBar getSupportActionBar() {
        return ((BaseActivity) getActivity()).getSupportActionBar();
    }

    protected Toolbar getToolbar() {
        // Log.i(TAG, "getToolbar");
        return ((BaseActivity) getActivity()).getToolbar();
    }

    public void setToolbarTitle(@StringRes int titleResId) {
        setToolbarTitle(getString(titleResId));
    }

    public void setToolbarTitle(String title) {
        // Log.i(TAG, "setToolbarTitle:title:" + title);
        ((BaseActivity) getActivity()).setToolbarTitle(title);
    }

    public void setToolbarTitleAlign(int horizontalGravity) {
        ((BaseActivity) getActivity()).setToolbarTitleAlign(horizontalGravity);
    }

    public void showToolbarIcon() {
        ((BaseActivity) getActivity()).showToolbarIcon();
    }

    // ProgressDialog
    /*public void showLoadingDialog(@StringRes int stringResource) {
        showLoadingDialog(mContext.getString(stringResource));
    }*/

    /*public void showLoadingDialog(String message) {
        if (mProgressDialog != null) mProgressDialog.setMessage(message);
        showLoadingDialog();
    }*/

    public void setView(View view) {
        this.mView = view;
    }

    /*public void initViewPagerListener() {
        Log.i(TAG, "initViewPagerListener");
        *//*((OldMainActivity) getActivity()).setOnViewPagerChangeListener(new OldMainActivity.OnViewPagerChangeListener() {
            @Override
            public void OnPageSelected(int position) {
                Log.i(TAG, "OnPageSelected:" + position);
            }
        });*//*

        if (getUserVisibleHint()) {
            Log.d(TAG, "getUserVisibleHint:true");
            // ((OldMainActivity) getActivity()).setOnViewPagerChangeListener(this);
        } else {
            Log.d(TAG, "getUserVisibleHint:false");
        }
    }*/

    /*public void releaseViewPagerListener() {
        Log.i(TAG, "releaseViewPagerListener");
        ((OldMainActivity) getActivity()).removeOnViewPagerChangeListener();
    }*/

    public void showLoadingDialog() {
        // Log.i(TAG, "showLoadingDialog");
        ((BaseActivity) getActivity()).showLoadingDialog();
    }

    public void dismissDialog() {
        // Log.i(TAG, "dismissDialog");
        ((BaseActivity) getActivity()).dismissDialog();
    }

    public boolean isProgressDialogShowing() {
        return mProgressDialog != null && mProgressDialog.isShowing();
    }

    // MESSAGING_EVENT
    private FCMReceiver mFCMReceiver;

    protected void startFCMReceiver() {
        Log.i(TAG, "startFCMReceiver");
        if (mFCMReceiver != null) return;
        mFCMReceiver = new FCMReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.pwdr.nacky.intent.action.MESSAGING_EVENT");
        // Higher numbers have a higher priority.
        intentFilter.setPriority(1);
        getActivity().registerReceiver(mFCMReceiver, intentFilter);
    }

    protected void stopFCMReceiver() {
        Log.i(TAG, "stopFCMReceiver");
        if (mFCMReceiver == null) return;
        getActivity().unregisterReceiver(mFCMReceiver);
        mFCMReceiver = null;
    }

    private class FCMReceiver extends BroadcastReceiver {
        final String TAG = "(F)FCMReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive:fragment:" + BaseFragment.this + "|listener:" + mOnMessageReceiveListener);
            RemoteMessage remoteMessage = intent.getParcelableExtra(EXTRA_REMOTE_MESSAGE);
            if (mOnMessageReceiveListener != null)
                mOnMessageReceiveListener.onReceive(remoteMessage);
            // abortBroadcast를 하게 되면 다른 모든 프래그먼트에서 호출되지 않는다.
            // abortBroadcast();
        }
    }

    private OnMessageReceiveListener mOnMessageReceiveListener;

    public interface OnMessageReceiveListener {
        void onReceive(RemoteMessage remoteMessage);
    }

    public void setOnMessageReceiveListener(OnMessageReceiveListener l) {
        this.mOnMessageReceiveListener = l;
        Log.i(TAG, "setOnMessageReceiveListener:fragment:" + BaseFragment.this + "|listener:" + mOnMessageReceiveListener);
    }

    protected Intent createIntent(Class activityClass) {
        return new Intent().setClass(getContext().getApplicationContext(), activityClass);
    }

    protected void startChildActivity() {
        Intent intent = createIntent(ChildActivity.class);
        startActivity(intent);
    }
}