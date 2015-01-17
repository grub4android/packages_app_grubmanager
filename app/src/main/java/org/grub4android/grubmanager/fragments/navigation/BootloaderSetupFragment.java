package org.grub4android.grubmanager.fragments.navigation;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.melnykov.fab.FloatingActionButton;

import org.apache.commons.io.FileUtils;
import org.grub4android.grubmanager.R;
import org.grub4android.grubmanager.RootUtils;
import org.grub4android.grubmanager.Utils;
import org.grub4android.grubmanager.activities.MainActivity;
import org.grub4android.grubmanager.adapter.TwoLineAdapter;
import org.grub4android.grubmanager.fragments.BaseFragment;
import org.grub4android.grubmanager.models.JSONBuild;
import org.grub4android.grubmanager.models.JSONManifest;
import org.grub4android.grubmanager.updater.UpdaterTools;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

public class BootloaderSetupFragment extends BaseFragment {

    private View mToolbarContent;
    private RecyclerView mRecyclerSysinfo;
    private TwoLineAdapter mAdapterSysinfo;
    private ArrayList<TwoLineAdapter.Dataset> mAdapter;
    private TextView mToolbarSubtitile1;
    private TextView mToolbarSubtitile2;

    private TwoLineAdapter.Dataset mDatasetInstall;
    private TwoLineAdapter.Dataset mDatasetChangelog;
    private TwoLineAdapter.Dataset mDatasetUninstall;
    private TwoLineAdapter.Dataset mDatasetGRUB;
    private TwoLineAdapter.Dataset mDatasetLK;
    private TwoLineAdapter.Dataset mDatasetMultiboot;
    private JSONBuild mUpdateBuild;

    public BootloaderSetupFragment() {
        setTitle(R.string.title_bootloader_setup);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_bootloader_setup, container, false);

        // get views
        mToolbarSubtitile1 = (TextView) getActivity().findViewById(R.id.toolbar_subtitle1);
        mToolbarSubtitile2 = (TextView) getActivity().findViewById(R.id.toolbar_subtitle2);

        // datasets
        ArrayList<TwoLineAdapter.Dataset> dataset = new ArrayList<>();
        mDatasetInstall = new TwoLineAdapter.Dataset(R.string.install, R.string.loading, TwoLineAdapter.ViewType.TYPE_ITEM, android.R.id.button1);
        mDatasetChangelog = new TwoLineAdapter.Dataset(R.string.changelog, R.string.changelog_description, TwoLineAdapter.ViewType.TYPE_ITEM, android.R.id.button2);
        mDatasetUninstall = new TwoLineAdapter.Dataset(R.string.uninstall, R.string.uninstall_description, TwoLineAdapter.ViewType.TYPE_ITEM, android.R.id.button3);
        mDatasetGRUB = new TwoLineAdapter.Dataset(R.string.grub, R.string.not_installed, TwoLineAdapter.ViewType.TYPE_ITEM, 0);
        mDatasetLK = new TwoLineAdapter.Dataset(R.string.lk, R.string.not_installed, TwoLineAdapter.ViewType.TYPE_ITEM, 0);
        mDatasetMultiboot = new TwoLineAdapter.Dataset(R.string.multiboot, R.string.not_installed, TwoLineAdapter.ViewType.TYPE_ITEM, 0);

        dataset.add(new TwoLineAdapter.Dataset(R.string.setup, 0, TwoLineAdapter.ViewType.TYPE_SUBHEADER, 0));
        dataset.add(mDatasetInstall);
        dataset.add(mDatasetChangelog);
        dataset.add(mDatasetUninstall);
        dataset.add(new TwoLineAdapter.Dataset(R.string.system_info, 0, TwoLineAdapter.ViewType.TYPE_SUBHEADER, 0));
        dataset.add(mDatasetGRUB);
        dataset.add(mDatasetLK);
        dataset.add(mDatasetMultiboot);

        // list
        mRecyclerSysinfo = (RecyclerView) rootView.findViewById(R.id.recycler_view);
        mRecyclerSysinfo.setHasFixedSize(true);
        mRecyclerSysinfo.setLayoutManager(new LinearLayoutManager(getActivity()));

        // adapter
        mAdapterSysinfo = new TwoLineAdapter(dataset);
        mAdapterSysinfo.setOnDatasetItemClickListener(new TwoLineAdapter.OnDatasetItemClickListener() {
            @Override
            public void onClick(TwoLineAdapter.Dataset dataset) {
                if (dataset == mDatasetInstall && mUpdateBuild != null) {
                    UpdaterTools.doDownload(getActivity(), DEVICE_INFO, mUpdateBuild, new UpdaterTools.UpdateListener() {
                        @Override
                        public void onFinished() {
                            onDataReceived();
                        }
                    });
                } else if (dataset == mDatasetUninstall) {
                    MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
                    builder.accentColor(getResources().getColor(R.color.material_green));
                    builder.title(R.string.uninstall);
                    builder.content(R.string.message_sure);
                    builder.positiveText(android.R.string.yes);
                    builder.negativeText(android.R.string.no);
                    builder.callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            UpdaterTools.doUninstall(getActivity(), DEVICE_INFO, new UpdaterTools.UpdateListener() {
                                @Override
                                public void onFinished() {
                                    onDataReceived();
                                }
                            });
                        }
                    });
                    builder.show();
                }
            }
        });
        mRecyclerSysinfo.setAdapter(mAdapterSysinfo);

        // toolbar2
        mToolbarSubtitile2.setText(Build.MANUFACTURER + " " + Build.MODEL + " (" + Build.DEVICE + ")");

        // FAB
        FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchFromServer();
            }
        });

        return rootView;
    }

    @Override
    public void onDataReceived() {
        try {
            String bootPath = DEVICE_INFO.getAbsoluteBootPath(false);
            String manifestPath = bootPath + "/manifest.json";

            // get latest build
            mUpdateBuild = DEVICE_INFO.getLatestBuild();

            // set install description
            mDatasetInstall.setDescription(mUpdateBuild.getFilename());

            if (!DEVICE_INFO.isInstalled(true)) {
                // subtitle1
                mToolbarSubtitile1.setText("-");

                // install: install
                mDatasetInstall.setTitle(R.string.install);

                // hide uninstall button
                mDatasetUninstall.mHidden = true;

                // GRUB
                mDatasetGRUB.setDescription(R.string.not_installed);
                // LK
                mDatasetLK.setDescription(R.string.not_installed);
                // MULTIBOOT
                mDatasetMultiboot.setDescription(R.string.not_installed);
            } else {
                // read package checksum
                String sha1 = FileUtils.readFileToString(new File(RootUtils.copyToCache(getActivity(), bootPath + "/package.sha1"))).trim();

                // set install title
                if (!sha1.equals(mUpdateBuild.getSHA1())) {
                    // update
                    mDatasetInstall.setTitle(R.string.update);
                } else {
                    // reinstall
                    mDatasetInstall.setTitle(R.string.reinstall);
                }

                // read manifest
                File cachedManifestFile = new File(RootUtils.copyToCache(getActivity(), manifestPath));
                JSONManifest installedManifest = new JSONManifest(new JSONObject(FileUtils.readFileToString(cachedManifestFile)));

                // subtitle1
                try {
                    mToolbarSubtitile1.setText(
                            Utils.getDate(installedManifest.getTimeStamp())
                                    + "-g" + Long.toHexString(installedManifest.getRevisionHash())
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                    mToolbarSubtitile1.setText(R.string.error_occurred);
                }

                // show uninstall button
                mDatasetUninstall.mHidden = false;

                // GRUB
                try {
                    mDatasetGRUB.setDescription(installedManifest.getGRUBVersion());
                } catch (Exception e) {
                    e.printStackTrace();
                    mDatasetGRUB.setDescription(R.string.error_occurred);
                }

                // LK
                try {
                    mDatasetLK.setDescription(installedManifest.getLKVersion());
                } catch (Exception e) {
                    e.printStackTrace();
                    mDatasetLK.setDescription(R.string.error_occurred);
                }

                // MULTIBOOT
                try {
                    mDatasetMultiboot.setDescription(installedManifest.getMultibootVersion());
                } catch (Exception e) {
                    e.printStackTrace();
                    mDatasetMultiboot.setDescription(getString(R.string.error_occurred));
                }
            }

        } catch (Exception e) {
            Utils.alertError(getActivity(), R.string.error_parse_device_info, e);
        } finally {
            mAdapterSysinfo.notifyDataSetChanged();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // show toolbar content
        mToolbarContent = ((MainActivity) getActivity()).getToolbar().findViewById(R.id.toolbar_bootloader_setup);
        mToolbarContent.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // hide toolbar content
        mToolbarContent.setVisibility(View.GONE);
    }
}
