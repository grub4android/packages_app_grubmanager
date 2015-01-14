package org.grub4android.grubmanager.adapter;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.grub4android.grubmanager.R;

import java.util.List;

public class TwoLineAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public List<Dataset> mDataset;
    private OnDatasetItemClickListener mOnDatasetItemClickListener = null;

    // Provide a suitable constructor (depends on the kind of dataset)
    public TwoLineAdapter(List<Dataset> dataset) {
        mDataset = dataset;
    }

    public void setOnDatasetItemClickListener(OnDatasetItemClickListener l) {
        mOnDatasetItemClickListener = l;
    }

    private RecyclerView.ViewHolder onCreateViewHolder_Item(ViewGroup parent) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_twoline, parent, false);
        RecyclerView.ViewHolder vh = new ViewHolder_Item(v);

        return vh;
    }

    private RecyclerView.ViewHolder onCreateViewHolder_Subheader(ViewGroup parent) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_twoline_subheader, parent, false);
        RecyclerView.ViewHolder vh = new ViewHolder_Subheader(v);

        return vh;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                      int viewType) {
        ViewType type = ViewType.values()[viewType];
        switch (type) {
            case TYPE_ITEM:
                return onCreateViewHolder_Item(parent);
            case TYPE_SUBHEADER:
                return onCreateViewHolder_Subheader(parent);

            default:
                throw new IllegalArgumentException("invalid viewtype: " + viewType);
        }
    }

    public void onBindViewHolder_Item(ViewHolder_Item holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        if (mDataset.get(position).mTitle > 0)
            holder.mTextViewPrimary.setText(mDataset.get(position).mTitle);
        else holder.mTextViewPrimary.setText(mDataset.get(position).mTitleStr);

        if (mDataset.get(position).mDescription > 0)
            holder.mTextViewSecondary.setText(mDataset.get(position).mDescription);
        else holder.mTextViewSecondary.setText(mDataset.get(position).mDescriptionStr);

        if (position == getItemCount() - 1 || mDataset.get(position + 1).mType != ViewType.TYPE_SUBHEADER)
            holder.mRootView.setBackgroundColor(Color.TRANSPARENT);

        holder.mRootView.setTag(mDataset.get(position));
        holder.mRootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dataset dataset = (Dataset) v.getTag();
                if (mOnDatasetItemClickListener != null)
                    mOnDatasetItemClickListener.onClick(dataset);
            }
        });
        holder.mRootView.setVisibility(mDataset.get(position).mHidden == false ? View.VISIBLE : View.GONE);
    }

    public void onBindViewHolder_Subheader(ViewHolder_Subheader holder, int position) {
        if (mDataset.get(position).mTitle > 0)
            holder.mTextViewTitle.setText(mDataset.get(position).mTitle);
        else holder.mTextViewTitle.setText(mDataset.get(position).mTitleStr);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ViewHolder_Item) {
            onBindViewHolder_Item((ViewHolder_Item) holder, position);
        } else if (holder instanceof ViewHolder_Subheader) {
            onBindViewHolder_Subheader((ViewHolder_Subheader) holder, position);
        } else
            throw new IllegalArgumentException("Invalid Viewholder: " + holder.getClass().getCanonicalName());
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mDataset.get(position).mType.ordinal();
    }

    public static enum ViewType {
        TYPE_SUBHEADER,
        TYPE_ITEM
    }

    public static interface OnDatasetItemClickListener {
        public abstract void onClick(Dataset dataset);
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder_Item extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView mTextViewPrimary;
        public TextView mTextViewSecondary;
        public View mRootView;

        public ViewHolder_Item(View v) {
            super(v);

            mRootView = ((ViewGroup) v).getChildAt(0);
            mTextViewPrimary = (TextView) v.findViewById(R.id.textPrimary);
            mTextViewSecondary = (TextView) v.findViewById(R.id.textSecondary);
        }
    }

    public static class ViewHolder_Subheader extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView mTextViewTitle;
        public View mRootView;

        public ViewHolder_Subheader(View v) {
            super(v);

            mRootView = ((ViewGroup) v).getChildAt(0);
            mTextViewTitle = (TextView) v.findViewById(android.R.id.text1);
        }
    }

    public static class Dataset {
        public ViewType mType;
        public int mId;
        public boolean mHidden = false;
        private int mTitle;
        private String mTitleStr;
        private int mDescription;
        private String mDescriptionStr;

        public Dataset(int title, int description, ViewType type, int id) {
            mTitle = title;
            mDescription = description;
            mType = type;
            mId = id;
        }

        public void setTitle(String title) {
            mTitle = -1;
            mTitleStr = title;
        }

        public void setTitle(int title) {
            mTitle = title;
            mTitleStr = null;
        }

        public void setDescription(String description) {
            mDescription = -1;
            mDescriptionStr = description;
        }

        public void setDescription(int description) {
            mDescription = description;
            mDescriptionStr = null;
        }
    }
}