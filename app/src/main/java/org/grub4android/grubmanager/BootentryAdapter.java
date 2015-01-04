package org.grub4android.grubmanager;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.grub4android.grubmanager.models.Bootentry;

import java.util.List;

public class BootentryAdapter extends RecyclerView.Adapter<BootentryAdapter.ViewHolder> {
    private List<Bootentry> mDataset;

    // Provide a suitable constructor (depends on the kind of dataset)
    public BootentryAdapter(List<Bootentry> myDataset) {
        mDataset = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public BootentryAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                          int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_bootentry, parent, false);
        ViewHolder vh = new ViewHolder(v);

        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.mTextViewPrimary.setText(mDataset.get(position).mTitle);
        holder.mTextViewSecondary.setText(mDataset.get(position).mDesription);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView mTextViewPrimary;
        public TextView mTextViewSecondary;

        public ViewHolder(View v) {
            super(v);

            mTextViewPrimary = (TextView) v.findViewById(R.id.textPrimary);
            mTextViewSecondary = (TextView) v.findViewById(R.id.textSecondary);
        }
    }
}