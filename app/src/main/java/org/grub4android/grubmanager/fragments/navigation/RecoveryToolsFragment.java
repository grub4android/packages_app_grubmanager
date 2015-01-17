package org.grub4android.grubmanager.fragments.navigation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.grub4android.grubmanager.R;
import org.grub4android.grubmanager.fragments.BaseFragment;

public class RecoveryToolsFragment extends BaseFragment {

    public RecoveryToolsFragment() {
        setTitle(R.string.title_recovery_tools);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_item_twoline, container, false);
    }

    @Override
    public void onDataReceived() {
    }
}
