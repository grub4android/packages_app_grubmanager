package org.grub4android.grubmanager.fragments.navigation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.grub4android.grubmanager.R;
import org.grub4android.grubmanager.fragments.BaseFragment;

public class OperatingSystemsFragment extends BaseFragment {

    public OperatingSystemsFragment() {
        setTitle(R.string.title_operating_systems);
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
