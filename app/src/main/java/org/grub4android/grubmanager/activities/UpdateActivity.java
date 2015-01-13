package org.grub4android.grubmanager.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.RequestHandle;
import com.melnykov.fab.FloatingActionButton;
import com.stericson.RootShell.execution.Command;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.grub4android.grubmanager.Pointer;
import org.grub4android.grubmanager.R;
import org.grub4android.grubmanager.RootUtils;
import org.grub4android.grubmanager.Utils;
import org.grub4android.grubmanager.adapter.TwoLineAdapter;
import org.grub4android.grubmanager.models.MountInfo;
import org.grub4android.grubmanager.updater.UpdaterClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

public class UpdateActivity extends ActionBarActivity {
    private RecyclerView mRecyclerSysinfo;
    private TwoLineAdapter mAdapterSysinfo;
    private ArrayList<TwoLineAdapter.Dataset> mAdapter;
    private UpdateHandler mUpdateHandler;
    private String mBootPath;
    private JSONObject mUpdateBuild = null;
    private TextView mToolbarSubtitile1;
    private TextView mToolbarSubtitile2;

    private TwoLineAdapter.Dataset mDatasetInstall;
    private TwoLineAdapter.Dataset mDatasetChangelog;
    private TwoLineAdapter.Dataset mDatasetUninstall;
    private TwoLineAdapter.Dataset mDatasetGRUB;
    private TwoLineAdapter.Dataset mDatasetLK;
    private TwoLineAdapter.Dataset mDatasetMultiboot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update);

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
        mDatasetInstall = new TwoLineAdapter.Dataset(getString(R.string.install), getString(R.string.loading), TwoLineAdapter.ViewType.TYPE_ITEM, android.R.id.button1);
        mDatasetChangelog = new TwoLineAdapter.Dataset("Changelog", "See what's new in this version", TwoLineAdapter.ViewType.TYPE_ITEM, android.R.id.button2);
        mDatasetUninstall = new TwoLineAdapter.Dataset("Uninstall", "Revert to original bootloader", TwoLineAdapter.ViewType.TYPE_ITEM, android.R.id.button3);
        mDatasetGRUB = new TwoLineAdapter.Dataset("GRUB", "2.02~beta2-8d4abea", TwoLineAdapter.ViewType.TYPE_ITEM, 0);
        mDatasetLK = new TwoLineAdapter.Dataset("LK", "0.5-a76337b", TwoLineAdapter.ViewType.TYPE_ITEM, 0);
        mDatasetMultiboot = new TwoLineAdapter.Dataset("Multiboot", "0.1-6791079", TwoLineAdapter.ViewType.TYPE_ITEM, 0);

        dataset.add(new TwoLineAdapter.Dataset("Setup", null, TwoLineAdapter.ViewType.TYPE_SUBHEADER, 0));
        dataset.add(mDatasetInstall);
        dataset.add(mDatasetChangelog);
        dataset.add(mDatasetUninstall);
        dataset.add(new TwoLineAdapter.Dataset("System info", null, TwoLineAdapter.ViewType.TYPE_SUBHEADER, 0));
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
                }

            }
        });
        mRecyclerSysinfo.setAdapter(mAdapterSysinfo);

        mUpdateHandler = new UpdateHandler(Looper.getMainLooper());

        // FAB
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUI();
            }
        });

        updateUI();
    }

    private void updateUI() {
        // install: loading
        mDatasetInstall.mTitle = getString(R.string.install);
        mDatasetInstall.mDescription = getString(R.string.loading);

        // uninstall: disabled

        // sysinfo: loading
        mDatasetGRUB.mDescription = getString(R.string.loading);
        mDatasetLK.mDescription = getString(R.string.loading);
        mDatasetMultiboot.mDescription = getString(R.string.loading);

        // fetch all data
        UpdaterClient.fetchData(new UpdaterClient.RequestDoneCallback() {
            @Override
            public void onRequestDone(boolean success) {
                if (!success)
                    Toast.makeText(UpdateActivity.this, getString(R.string.error_occurred), Toast.LENGTH_LONG).show();

                // INSTALL
                try {
                    // set update info
                    JSONArray builds = (JSONArray) UpdaterClient.mDeviceInfo.get("builds");
                    mUpdateBuild = (JSONObject) builds.get(0); // TODO: get latest build, not first
                    mDatasetInstall.mDescription = mUpdateBuild.getString("filename");

                    // get boot partition mountpoint
                    String mountPoint = null;
                    int[] majmin = RootUtils.getBootloaderPartMajorMinor(UpdaterClient.mDeviceInfo.getString("grub_boot_partition_name"));
                    ArrayList<MountInfo> mountInfo = RootUtils.getMountInfo();
                    for (MountInfo i : mountInfo) {
                        if (i.getMajor() == majmin[0] && i.getMinor() == majmin[1] && i.getRoot().equals("/")) {
                            mountPoint = i.getMountPoint();
                            break;
                        }
                    }
                    if (mountPoint == null) {
                        Toast.makeText(UpdateActivity.this, "We need to mount grubPart!", Toast.LENGTH_SHORT).show();
                        throw new Exception();
                    }

                    // get manifest
                    String bootPath = mBootPath = mountPoint + "/" + UpdaterClient.mDeviceInfo.getString("grub_boot_path_prefix");
                    String installedManifestPath = bootPath + "/manifest.json";
                    if (!RootUtils.exists(installedManifestPath)) {
                        // toolbar1
                        mToolbarSubtitile1.setText("Installed: -");

                        // install: install
                        mDatasetInstall.mTitle = getString(R.string.install);

                        // hide uninstall button
                        mDatasetUninstall.mHidden = true;

                        // GRUB
                        mDatasetGRUB.mDescription = getString(R.string.not_installed);
                        // LK
                        mDatasetLK.mDescription = getString(R.string.not_installed);
                        // MULTIBOOT
                        mDatasetMultiboot.mDescription = getString(R.string.not_installed);
                    } else {
                        // copy checksum to cache
                        String cachedChecksumPath = getCacheDir() + "/installed_package.sha1";
                        RootUtils.copyToCache(bootPath + "/package.sha1", cachedChecksumPath);
                        String sha1 = Utils.getStringFromFile(new File(cachedChecksumPath)).trim();

                        Log.e("G4A", sha1 + "==" + mUpdateBuild.getString("checksum_sha1"));
                        // set title
                        if (!sha1.equals(mUpdateBuild.getString("checksum_sha1"))) {
                            // install: update
                            mDatasetInstall.mTitle = getString(R.string.update);
                        } else {
                            // install: reinstall
                            mDatasetInstall.mTitle = "Reinstall";
                        }

                        // copy manifest to cache
                        String cachedManifestPath = getCacheDir() + "/installed_manifest.json";
                        RootUtils.copyToCache(installedManifestPath, cachedManifestPath);
                        File cachedManifestFile = new File(cachedManifestPath);

                        // read manifest from cache
                        JSONObject installedManifest = new JSONObject(Utils.getStringFromFile(cachedManifestFile));
                        cachedManifestFile.delete();
                        JSONObject versions = (JSONObject) installedManifest.get("versions");

                        // toolbar1
                        try {
                            mToolbarSubtitile1.setText("Installed: "
                                            + Utils.getDate(installedManifest.getLong("timestamp"))
                                            + "-g" + Long.toHexString(installedManifest.getLong("revision_hash"))
                            );
                        } catch (Exception e) {
                            e.printStackTrace();
                            mToolbarSubtitile1.setText("Installed: " + getString(R.string.error_occurred));
                        }


                        // show uninstall button
                        mDatasetUninstall.mHidden = false;

                        // GRUB
                        try {
                            mDatasetGRUB.mDescription = versions.getString("grub");
                        } catch (Exception e) {
                            e.printStackTrace();
                            mDatasetGRUB.mDescription = getString(R.string.error_occurred);
                        }

                        // LK
                        try {
                            mDatasetLK.mDescription = versions.getString("lk");
                        } catch (Exception e) {
                            e.printStackTrace();
                            mDatasetLK.mDescription = getString(R.string.error_occurred);
                        }

                        // MULTIBOOT
                        try {
                            mDatasetMultiboot.mDescription = versions.getString("multiboot");
                        } catch (Exception e) {
                            e.printStackTrace();
                            mDatasetMultiboot.mDescription = getString(R.string.error_occurred);
                        }
                    }

                } catch (Exception e) {
                    // install: error
                    e.printStackTrace();
                    mDatasetInstall.mTitle = getString(R.string.install);
                    mDatasetInstall.mDescription = getString(R.string.error_occurred);
                } finally {
                    mAdapterSysinfo.notifyDataSetChanged();
                }
            }
        }, true);
    }

    private void doDownload(final JSONObject updateBuild) {
        // get filename
        try {
            mUpdateHandler.mFilename = updateBuild.getString("filename");
        } catch (JSONException e) {
            mUpdateHandler.obtainMessage(UpdateHandler.MESSAGE_EXCEPTION, null).sendToTarget();
            return;
        }

        // create dialog
        mUpdateHandler.mDialog = ProgressDialog.show(UpdateActivity.this, null, null, true, true);
        mUpdateHandler.mDialog.setMessage("Downloading " + mUpdateHandler.mFilename);
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

                    if (!sha1.equals(updateBuild.getString("checksum_sha1"))) {
                        throw new Exception("Invalid checksum");
                    }
                } catch (Exception e) {
                    mUpdateHandler.obtainMessage(UpdateHandler.MESSAGE_EXCEPTION, null).sendToTarget();
                    return;
                }

                // extract
                doExtract(response);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                mUpdateHandler.obtainMessage(UpdateHandler.MESSAGE_EXCEPTION, null).sendToTarget();
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
            RootUtils.installPackage(dir + "/install.sh", mBootPath, dir.getAbsolutePath(), mUpdateHandler.mUpdateBuild.getString("checksum_sha1"), UpdaterClient.mDeviceInfo.getString("lk_installation_partition"), new RootUtils.CommandFinished() {
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
        public JSONObject mUpdateBuild = null;
        public String mFilename = null;

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

                    // update UI
                    updateUI();
                    break;
            }
        }
    }
}
