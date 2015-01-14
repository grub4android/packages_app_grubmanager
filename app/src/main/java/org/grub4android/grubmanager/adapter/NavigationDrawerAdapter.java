package org.grub4android.grubmanager.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.grub4android.grubmanager.R;

import java.util.List;

public class NavigationDrawerAdapter extends RecyclerView.Adapter<NavigationDrawerAdapter.ViewHolder> {
    private List<Item> mDataset;
    private View.OnClickListener mOnClickListener = null;

    public NavigationDrawerAdapter(List<Item> myDataset) {
        mDataset = myDataset;
    }

    public void setOnClickListener(View.OnClickListener mOnClickListener) {
        this.mOnClickListener = mOnClickListener;
    }

    @Override
    public NavigationDrawerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                 int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_navbar, parent, false);
        ViewHolder vh = new ViewHolder(v);

        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.mTextView.setText(mDataset.get(position).mTitle);

        holder.mRootView.setId(mDataset.get(position).mID);
        holder.mRootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnClickListener != null) mOnClickListener.onClick(v);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView mTextView;
        public View mRootView;

        public ViewHolder(View v) {
            super(v);

            mRootView = ((ViewGroup) v).getChildAt(0);
            mTextView = (TextView) v.findViewById(android.R.id.text1);
        }
    }

    public static class Item {
        public int mTitle;
        public int mID;

        public Item(int title, int id) {
            mTitle = title;
            mID = id;
        }
    }
}