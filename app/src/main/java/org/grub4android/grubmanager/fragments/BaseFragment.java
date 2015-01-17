package org.grub4android.grubmanager.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.support.v4.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.loopj.android.http.RequestHandle;

import org.grub4android.grubmanager.Pointer;
import org.grub4android.grubmanager.R;
import org.grub4android.grubmanager.Utils;
import org.grub4android.grubmanager.models.JSONDeviceInfo;
import org.grub4android.grubmanager.updater.UpdaterClient;

import java.util.List;

public class BaseFragment extends Fragment {

    /**
     * data fetching stages
     */
    private static final int STAGE_DEVICE_LIST = 0;
    private static final int STAGE_DEVICE_INFO = 1;
    private static final int STAGE_DONE = 2;

    /**
     * Loaded data for all Fragments
     */
    protected static JSONDeviceInfo DEVICE_INFO;
    protected static List<String> DEVICE_LIST;

    /**
     * synchronized flags
     */
    private final Pointer<Boolean> mDataReceived = new Pointer<>(false);

    private Boolean mRequiresData = true;
    private int mTitle = 0;
    private Activity mActivity;
    private boolean mUseCache = true;
    private boolean mCancelable = false;

    public void init(final Activity activity) {
        mActivity = activity;

        // init data if requested and not already done
        if (mRequiresData && (DEVICE_LIST == null || !DEVICE_LIST.contains(Build.DEVICE) || DEVICE_INFO == null)) {
            ProgressDialog dialog = ProgressDialog.show(mActivity, null, null, true, false);
            mUseCache = true;
            mCancelable = false;
            fetchData(dialog, STAGE_DEVICE_LIST);
        } else {
            mDataReceived.val = true;
            runDataCallback();
        }
    }

    public void fetchFromServer() {
        ProgressDialog dialog = ProgressDialog.show(mActivity, null, null, true, true);
        mUseCache = false;
        mCancelable = true;
        fetchData(dialog, STAGE_DEVICE_LIST);
    }

    private void fetchData(ProgressDialog dialog, int stage) {
        switch (stage) {
            case STAGE_DEVICE_LIST:
                dialog.setMessage("Loading Device List");
                fetchDeviceList(mUseCache, dialog);
                break;

            case STAGE_DEVICE_INFO:
                dialog.setMessage("Loading Device Info");
                fetchDeviceInfo(mUseCache, dialog);
                break;

            case STAGE_DONE:
                dialog.dismiss();
                synchronized (mDataReceived) {
                    mDataReceived.val = true;
                    runDataCallback();
                }
                break;
        }
    }

    private void fetchDeviceList(final boolean useCache, final ProgressDialog dialog) {
        final RequestHandle handle = UpdaterClient.getDeviceList(mActivity, useCache, new UpdaterClient.DeviceListListener() {
            @Override
            public void onDeviceListReceived(List<String> devices, boolean fromCache) {

                // not supported
                if (!devices.contains(Build.DEVICE)) {
                    // try without cache
                    if (fromCache) {
                        fetchDeviceList(false, dialog);
                    }

                    // not supported, retry
                    else {
                        Utils.alertRetry(mActivity, R.string.error_device_not_supported, mActivity.getString(R.string.error_device_not_supported_msg), mCancelable, new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog d) {
                                fetchDeviceList(useCache, dialog);
                            }

                            @Override
                            public void onNegative(MaterialDialog d) {
                                dialog.dismiss();
                            }
                        });
                    }
                }

                // supported
                else {
                    DEVICE_LIST = devices;
                    fetchData(dialog, STAGE_DEVICE_INFO);
                }
            }

            @Override
            public void onError(Exception e) {
                // error, retry
                Utils.alertRetry(mActivity, R.string.error_fetch_device_info, e.getMessage(), mCancelable, new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog d) {
                        dialog.show();
                        fetchDeviceList(useCache, dialog);
                    }

                    @Override
                    public void onNegative(MaterialDialog d) {
                        dialog.dismiss();
                    }
                });
            }
        });

        if (handle != null && mCancelable) {
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    handle.cancel(true);
                }
            });
        }
    }

    private void fetchDeviceInfo(final boolean useCache, final ProgressDialog dialog) {
        final RequestHandle handle = UpdaterClient.getDeviceInfo(mActivity, useCache, new UpdaterClient.DeviceInfoListener() {
            @Override
            public void onDeviceInfoReceived(JSONDeviceInfo deviceInfo, boolean fromCache) {
                DEVICE_INFO = deviceInfo;
                fetchData(dialog, STAGE_DONE);
            }

            @Override
            public void onError(Exception e) {
                // error, retry
                Utils.alertRetry(mActivity, R.string.error_fetch_device_list, e.getMessage(), mCancelable, new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog d) {
                        dialog.show();
                        fetchDeviceInfo(useCache, dialog);
                    }

                    @Override
                    public void onNegative(MaterialDialog d) {
                        dialog.dismiss();
                    }
                });
            }
        });


        if (handle != null && mCancelable) {
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    handle.cancel(true);
                }
            });
        }
    }

    private void runDataCallback() {
        if (isResumed()) {
            onDataReceived();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // check if we can init already
        synchronized (mDataReceived) {
            if (mDataReceived.val) runDataCallback();
        }
    }

    /**
     * Initialize the Fragment
     * <p/>
     * Called when all required data is available
     */
    public void onDataReceived() {
    }

    /**
     * Enable or disable fetching data
     * <p/>
     * Needs to be called from the constructor
     *
     * @param b true if enabled
     */
    public void setRequiresData(boolean b) {
        mRequiresData = b;
    }

    public int getTitle() {
        return mTitle;
    }

    public void setTitle(int title) {
        mTitle = title;
    }
}
