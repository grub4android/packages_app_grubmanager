package org.grub4android.grubmanager.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.RequestHandle;
import com.melnykov.fab.FloatingActionButton;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.grub4android.grubmanager.Pointer;
import org.grub4android.grubmanager.R;
import org.grub4android.grubmanager.RootUtils;
import org.grub4android.grubmanager.Utils;
import org.grub4android.grubmanager.adapter.TwoLineAdapter;
import org.grub4android.grubmanager.models.JSONBuild;
import org.grub4android.grubmanager.models.JSONDeviceInfo;
import org.grub4android.grubmanager.models.JSONManifest;
import org.grub4android.grubmanager.updater.UpdaterClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UpdateActivity extends ActionBarActivity {
    public boolean mCurrentlyInstalled = false;
    private RecyclerView mRecyclerSysinfo;
    private TwoLineAdapter mAdapterSysinfo;
    private ArrayList<TwoLineAdapter.Dataset> mAdapter;
    private UpdateHandler mUpdateHandler;
    private JSONBuild mUpdateBuild = null;
    private TextView mToolbarSubtitile1;
    private TextView mToolbarSubtitile2;

    private TwoLineAdapter.Dataset mDatasetInstall;
    private TwoLineAdapter.Dataset mDatasetChangelog;
    private TwoLineAdapter.Dataset mDatasetUninstall;
    private TwoLineAdapter.Dataset mDatasetGRUB;
    private TwoLineAdapter.Dataset mDatasetLK;
    private TwoLineAdapter.Dataset mDatasetMultiboot;

    private JSONDeviceInfo mDeviceInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update);

        // init busybox
        RootUtils.initBusybox(this);

        // get views
        mToolbarSubtitile1 = (TextView) findViewById(R.id.toolbar_subtitle1);
        mToolbarSubtitile2 = (TextView) findViewById(R.id.toolbar_subtitle2);

        // toolbar
        android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

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
        mRecyclerSysinfo = (RecyclerView) findViewById(R.id.my_recycler_view);
        mRecyclerSysinfo.setHasFixedSize(true);
        mRecyclerSysinfo.setLayoutManager(new LinearLayoutManager(this));

        // adapter
        mAdapterSysinfo = new TwoLineAdapter(dataset);
        mAdapterSysinfo.setOnDatasetItemClickListener(new TwoLineAdapter.OnDatasetItemClickListener() {
            @Override
            public void onClick(TwoLineAdapter.Dataset dataset) {
                if (dataset == mDatasetInstall && mUpdateBuild != null) {
                    doDownload(mUpdateBuild);
                } else if (dataset == mDatasetUninstall && mDeviceInfo != null) {
                    new AlertDialog.Builder(UpdateActivity.this).setTitle("Uninstall").setMessage("Are you sure?").setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            doUninstall();
                        }
                    }).setNegativeButton(android.R.string.no, null).show();
                }
            }
        });
        mRecyclerSysinfo.setAdapter(mAdapterSysinfo);

        // updateHandler
        mUpdateHandler = new UpdateHandler(Looper.getMainLooper());

        // FAB
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdaterClient.clearCache();
                updateUI();
            }
        });

        // update UI
        updateUI();
    }

    private void updateUI_loadDeviceInfo() {
        UpdaterClient.getDeviceInfo(this, new UpdaterClient.DeviceInfoReceivedCallback() {
            @Override
            public void onDeviceInfoReceived(JSONDeviceInfo deviceInfo, Exception eUC) {
                // check for exception
                if (eUC != null) {
                    Toast.makeText(UpdateActivity.this, getString(R.string.error_occurred) + ": " + eUC.getMessage(), Toast.LENGTH_LONG).show();
                    return;
                }

                // check device info
                mDeviceInfo = deviceInfo;
                if (deviceInfo == null) {
                    Toast.makeText(UpdateActivity.this, getString(R.string.error_occurred), Toast.LENGTH_LONG).show();
                    return;
                }

                try {
                    String mountPoint = deviceInfo.getBootloaderMountpoint(false);
                    String bootPath = mountPoint + "/" + mDeviceInfo.getBootPath();
                    String manifestPath = bootPath + "/manifest.json";

                    // get latest build
                    mUpdateBuild = deviceInfo.getLatestBuild();

                    // set install description
                    mDatasetInstall.setDescription(mUpdateBuild.getFilename());

                    if (!RootUtils.exists(manifestPath)) {
                        mCurrentlyInstalled = false;

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
                        mCurrentlyInstalled = true;

                        // read package checksum
                        String sha1 = FileUtils.readFileToString(new File(RootUtils.copyToCache(UpdateActivity.this, bootPath + "/package.sha1"))).trim();

                        // set install title
                        if (!sha1.equals(mUpdateBuild.getSHA1())) {
                            // update
                            mDatasetInstall.setTitle(R.string.update);
                        } else {
                            // reinstall
                            mDatasetInstall.setTitle(R.string.reinstall);
                        }

                        // read manifest
                        File cachedManifestFile = new File(RootUtils.copyToCache(UpdateActivity.this, manifestPath));
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
                    e.printStackTrace();
                    Toast.makeText(UpdateActivity.this, getString(R.string.error_occurred) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
                } finally {
                    mAdapterSysinfo.notifyDataSetChanged();
                }
            }
        });
    }

    private void updateUI() {
        // toolbar2
        mToolbarSubtitile2.setText(Build.MANUFACTURER + " " + Build.MODEL + " (" + Build.DEVICE + ")");

        // install: loading
        mDatasetInstall.setTitle(R.string.install);
        mDatasetInstall.setDescription(R.string.loading);

        // uninstall: hidden
        mDatasetUninstall.mHidden = true;

        // sysinfo: loading
        mDatasetGRUB.setDescription(R.string.loading);
        mDatasetLK.setDescription(R.string.loading);
        mDatasetMultiboot.setDescription(R.string.loading);

        // notify
        mAdapterSysinfo.notifyDataSetChanged();

        UpdaterClient.getDeviceList(this, new UpdaterClient.DeviceListReceivedCallback() {
            @Override
            public void onDeviceListReceived(List<String> devices, Exception eUC) {
                // check for exception
                if (eUC != null) {
                    Toast.makeText(UpdateActivity.this, getString(R.string.error_occurred) + ": " + eUC.getMessage(), Toast.LENGTH_LONG).show();
                    return;
                }

                // check device info
                if (devices == null || !devices.contains(Build.DEVICE)) {
                    Toast.makeText(UpdateActivity.this, R.string.device_not_supported, Toast.LENGTH_LONG).show();
                    mRecyclerSysinfo.setVisibility(View.GONE);
                    return;
                }
                mRecyclerSysinfo.setVisibility(View.VISIBLE);

                // load deviceinfo
                updateUI_loadDeviceInfo();
            }
        });
    }

    private void doUninstall() {
        // create dialog
        mUpdateHandler.mDialog = ProgressDialog.show(UpdateActivity.this, null, null, true, true);
        mUpdateHandler.mDialog.setMessage("Uninstalling");
        mUpdateHandler.obtainMessage(UpdateHandler.MESSAGE_SET_CANCABLE, false).sendToTarget();
        mUpdateHandler.mDialog.setOnCancelListener(null);

        new Thread() {
            @Override
            public void run() {
                try {
                    // restore backups
                    RootUtils.restoreBackup("lk.img", mDeviceInfo.mDeviceInfo.getString("lk_installation_partition"));

                    // delete manifest
                    RootTools.deleteFileOrDirectory(mDeviceInfo.getAbsoluteBootPath(true), false);

                    mUpdateHandler.obtainMessage(UpdateHandler.MESSAGE_FINISH).sendToTarget();
                } catch (Exception e) {
                    mUpdateHandler.obtainMessage(UpdateHandler.MESSAGE_EXCEPTION, e).sendToTarget();
                }
            }
        }.start();
    }

    private void doDownload(final JSONBuild updateBuild) {
        // get filename
        try {
            mUpdateHandler.mFilename = updateBuild.getFilename();
        } catch (JSONException e) {
            mUpdateHandler.obtainMessage(UpdateHandler.MESSAGE_EXCEPTION, null).sendToTarget();
            return;
        }

        if (mUpdateHandler.mIsRunning)
            return;

        // create dialog
        mUpdateHandler.mIsRunning = true;
        mUpdateHandler.mDialog = ProgressDialog.show(UpdateActivity.this, null, null, true, true);
        mUpdateHandler.obtainMessage(UpdateHandler.MESSAGE_SETTEXT, "\"Downloading " + mUpdateHandler.mFilename).sendToTarget();
        mUpdateHandler.mUpdateBuild = updateBuild;

        // start download
        final RequestHandle request = UpdaterClient.downloadPackage(mUpdateHandler.mFilename, new FileAsyncHttpResponseHandler(UpdateActivity.this) {
            @Override
            public void onSuccess(int statusCode, Header[] headers, File response) {
                // compare checksum
                try {
                    FileInputStream fis = new FileInputStream(response);
                    byte data[] = org.apache.commons.codec.digest.DigestUtils.sha1(fis);
                    fis.close();
                    char sha1Chars[] = Hex.encodeHex(data);
                    String sha1 = String.valueOf(sha1Chars);

                    if (!sha1.equals(updateBuild.getSHA1())) {
                        throw new Exception("Invalid checksum");
                    }
                } catch (Exception e) {
                    mUpdateHandler.obtainMessage(UpdateHandler.MESSAGE_EXCEPTION, e).sendToTarget();
                    return;
                }

                // extract
                doExtract(response);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                mUpdateHandler.obtainMessage(UpdateHandler.MESSAGE_EXCEPTION, new Exception("HTTP-Statuscode: " + statusCode)).sendToTarget();
            }
        });

        // cancel listener
        mUpdateHandler.mDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                request.cancel(true);
            }
        });
    }

    private void doExtract(final File file) {
        // set title
        mUpdateHandler.obtainMessage(UpdateHandler.MESSAGE_SETTEXT, "Extracting " + mUpdateHandler.mFilename).sendToTarget();

        // delete directory if it exists
        final File dir = new File(getCacheDir().getPath() + "/installation_package");
        try {
            if (dir.exists())
                FileUtils.deleteDirectory(dir);
        } catch (IOException e) {
            mUpdateHandler.obtainMessage(UpdateHandler.MESSAGE_EXCEPTION, e).sendToTarget();
            return;
        }
        dir.mkdirs();

        try {
            final Pointer<Boolean> userAbort = new Pointer<>(false);
            final Command cmd = RootUtils.unzip(file.getAbsolutePath(), dir.getAbsolutePath(), new RootUtils.CommandFinished() {
                @Override
                public void commandFinished(int exitcode) {
                    try {
                        if (exitcode != 0)
                            throw new Exception("exitCode=" + exitcode);

                        doInstall(dir);

                    } catch (Exception e) {
                        if (!userAbort.val)
                            // unzip error or terminated
                            mUpdateHandler.obtainMessage(UpdateHandler.MESSAGE_EXCEPTION, e).sendToTarget();
                    }
                }
            }, false);

            // cancel listener
            mUpdateHandler.mDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    userAbort.val = true;
                    cmd.terminate();
                }
            });
        } catch (Exception e) {
            mUpdateHandler.obtainMessage(UpdateHandler.MESSAGE_EXCEPTION, e).sendToTarget();
        }
    }

    private void doInstall(final File dir) {
        // set title
        mUpdateHandler.obtainMessage(UpdateHandler.MESSAGE_SETTEXT, "Installing " + mUpdateHandler.mFilename).sendToTarget();

        // configure dialog
        mUpdateHandler.obtainMessage(UpdateHandler.MESSAGE_SET_CANCABLE, false).sendToTarget();
        mUpdateHandler.mDialog.setOnCancelListener(null);

        try {
            String lkPart = mDeviceInfo.mDeviceInfo.getString("lk_installation_partition");

            // backup important files
            if (!RootUtils.backupExists("lk.img") || !mCurrentlyInstalled)
                RootUtils.doBackup(lkPart, "lk.img");

            // install package
            RootUtils.installPackage(UpdateActivity.this, dir + "/install.sh", mDeviceInfo.getAbsoluteBootPath(true), dir.getAbsolutePath(), mUpdateHandler.mUpdateBuild.getSHA1(), lkPart, new RootUtils.CommandFinished() {
                @Override
                public void commandFinished(int exitcode) {
                    try {
                        if (exitcode != 0)
                            throw new Exception("exitCode=" + exitcode);

                        // we're done here
                        mUpdateHandler.obtainMessage(UpdateHandler.MESSAGE_FINISH).sendToTarget();
                    } catch (Exception e) {
                        mUpdateHandler.obtainMessage(UpdateHandler.MESSAGE_EXCEPTION, e).sendToTarget();
                    }
                }
            });
        } catch (Exception e) {
            mUpdateHandler.obtainMessage(UpdateHandler.MESSAGE_EXCEPTION, e).sendToTarget();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_update, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class UpdateHandler extends Handler {
        public static final int MESSAGE_EXCEPTION = 0;
        public static final int MESSAGE_SETTEXT = 2;
        public static final int MESSAGE_SET_CANCABLE = 3;
        public static final int MESSAGE_FINISH = 4;
        public ProgressDialog mDialog = null;
        public JSONBuild mUpdateBuild = null;
        public String mFilename = null;

        public boolean mIsRunning = false;

        public UpdateHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case MESSAGE_EXCEPTION:
                    // print exception
                    String exceptionText = "";
                    Exception e = (Exception) msg.obj;
                    if (e != null) {
                        e.printStackTrace();
                        exceptionText = ": " + e.getMessage();
                    }

                    // dismiss dialog
                    if (mDialog != null) mDialog.dismiss();

                    // show error
                    Toast.makeText(UpdateActivity.this, getString(R.string.error_occurred) + exceptionText, Toast.LENGTH_LONG).show();

                    mIsRunning = false;

                    // update UI
                    updateUI();
                    break;

                case MESSAGE_SETTEXT:
                    String s = (String) msg.obj;
                    if (mDialog != null) mDialog.setMessage(s);
                    break;

                case MESSAGE_SET_CANCABLE:
                    Boolean b = (Boolean) msg.obj;
                    if (mDialog != null) mDialog.setCancelable(b);
                    break;

                case MESSAGE_FINISH:
                    // dismiss dialog
                    if (mDialog != null) mDialog.dismiss();

                    mIsRunning = false;

                    // update UI
                    updateUI();
                    break;
            }
        }
    }
}
