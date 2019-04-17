package com.chaigene.petnolja.ui.fragment;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.util.Log;

public class TabFragment extends BaseFragment {
    public static final String TAG = "TabFragment";

    protected void startFragment(Fragment fragment) {
        Log.i(TAG, "startFragment");
        Fragment parentFragment = getParentFragment();
        if (parentFragment != null) {
            if (parentFragment instanceof ChildFragment) {
                ((ChildFragment) parentFragment).getRootFragment().add(fragment);
            } else {
                try {
                    throw new IllegalAccessException("Parent fragment should be instance of ChildFragment.");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        } else {
            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            fragmentManager
                    .beginTransaction()
                    .add(android.R.id.content, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    public void onRefresh() {

    }
}
