package com.chaigene.petnolja.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.chaigene.petnolja.R;
import com.chaigene.petnolja.ui.view.NoLastDividerItemDecoration;
import com.chaigene.petnolja.util.CommonUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DialogListFragment extends AppCompatDialogFragment {
    public static final String TAG = "DialogListFragment";

    private static String EXTRA_ITEMS = "extra_items";
    private static String EXTRA_LISTENER = "extra_listener";

    private View mView;

    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;

    private ListDialogAdapter mAdapter;
    private List<String> mItems;
    private OnListItemSelectListener mOnItemSelectListener;

    public static DialogListFragment newInstance(@NonNull String[] items, @NonNull OnListItemSelectListener listener) {
        Log.i(TAG, "newInstance");
        DialogListFragment dialogListFragment = new DialogListFragment();

        Bundle args = new Bundle();
        args.putStringArray(EXTRA_ITEMS, items);
        args.putParcelable(EXTRA_LISTENER, listener);
        dialogListFragment.setArguments(args);

        return dialogListFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        readBundle(getArguments());
    }

    protected void readBundle(@Nullable Bundle bundle) {
        Log.i(TAG, "readBundle");
        // if (mBaseFragment != null) return;
        String[] items = bundle.getStringArray(EXTRA_ITEMS);
        mItems = Arrays.asList(items);
        mOnItemSelectListener = bundle.getParcelable(EXTRA_LISTENER);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_dialog_list, container, false);
        ButterKnife.bind(this, mView);

        LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        NoLastDividerItemDecoration decoration = new NoLastDividerItemDecoration(getActivity(), manager.getOrientation());
        decoration.setDrawable(CommonUtil.getDrawable(getActivity(), R.drawable.shape_divider_list_dialog));
        mRecyclerView.addItemDecoration(decoration);
        mRecyclerView.setLayoutManager(manager);

        mAdapter = new ListDialogAdapter(getActivity(), mItems);
        mRecyclerView.setAdapter(mAdapter);

        return mView;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return super.onCreateDialog(savedInstanceState);
    }

    private class ListDialogAdapter extends RecyclerView.Adapter<ViewHolder> {
        public static final String TAG = "ListDialogAdapter";

        Context c;
        List<String> items = new ArrayList<>();

        ListDialogAdapter(Context context, List items) {
            this.c = context.getApplicationContext();
            this.items = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Log.i(TAG, "onCreateViewHolder");
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list_dialog, parent, false);
            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Log.i(TAG, "onBindViewHolder");
            String item = items.get(position);
            holder.title.setText(item);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.title)
        TextView title;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mOnItemSelectListener == null) return;
            mOnItemSelectListener.onSelect(getAdapterPosition());
            dismiss();
        }
    }

    private interface OnListItemSelectListener extends Parcelable {
        void onSelect(int index);
    }

    public static class OnItemSelectListener implements OnListItemSelectListener {

        @Override
        public void onSelect(int index) {
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
        }
    }
}
