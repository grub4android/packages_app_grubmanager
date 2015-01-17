package org.grub4android.grubmanager.updater;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;

import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.RequestHandle;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.grub4android.grubmanager.Pointer;
import org.grub4android.grubmanager.R;
import org.grub4android.grubmanager.RootUtils;
import org.grub4android.grubmanager.Utils;
import org.grub4android.grubmanager.models.JSONBuild;
import org.grub4android.grubmanager.models.JSONDeviceInfo;

import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.locks.ReentrantLock;


public class UpdaterTools {
    private static final ReentrantLock mutex = new ReentrantLock();

    public static void doUninstall(final Context context, final JSONDeviceInfo deviceInfo, final UpdateListener cb) {
        if (mutex.isLocked())
            return;

        new AsyncTask<Object, String, Exception>() {
            private ProgressDialog mDialog;

            @Override
            protected void onPreExecute() {
                mDialog = ProgressDialog.show(context, null, null, true, false);
                mDialog.setMessage("Uninstalling");
            }

            @Override
            protected Exception doInBackground(Object... params) {
                mutex.lock();
                Exception ret = null;
                try {
                    // restore backups
                    RootUtils.restoreBackup("lk.img", deviceInfo.mDeviceInfo.getString("lk_installation_partition"));

                    // delete manifest
                    RootTools.deleteFileOrDirectory(deviceInfo.getAbsoluteBootPath(true), false);
                } catch (Exception e) {
                    ret = e;
                } finally {
                    mutex.unlock();
                }
                return ret;
            }

            @Override
            protected void onPostExecute(Exception e) {
                mDialog.dismiss();
                if (e != null) {
                    Utils.alertError(context, R.string.error_occurred, e);
                }
                cb.onFinished();
            }
        }.execute();
    }

    public static void doDownload(final Activity activity, final JSONDeviceInfo deviceInfo, final JSONBuild updateBuild, final UpdateListener cb) {
        if (mutex.isLocked())
            return;

        mutex.lock();

        // create dialog
        final ProgressDialog dialog = ProgressDialog.show(activity, null, null, true, true);
        dialog.setMessage("Downloading " + updateBuild.getFilename());

        // start download
        final RequestHandle request = UpdaterClient.downloadPackage(updateBuild.getFilename(), new FileAsyncHttpResponseHandler(activity) {

            @Override
            public void onSuccess(int statusCode, Header[] headers, File response) {
                try {
                    // compute checksum
                    FileInputStream fis = new FileInputStream(response);
                    byte data[] = org.apache.commons.codec.digest.DigestUtils.sha1(fis);
                    fis.close();
                    char sha1Chars[] = Hex.encodeHex(data);
                    String sha1 = String.valueOf(sha1Chars);

                    // compare checksum
                    if (!sha1.equals(updateBuild.getSHA1())) {
                        throw new Exception("Invalid checksum");
                    }

                    // extract
                    doExtract(activity, deviceInfo, updateBuild, cb, dialog, response);
                }

                // checksum error
                catch (final Exception e) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            Utils.alertError(activity, R.string.error_occurred, e);
                            mutex.unlock();
                            cb.onFinished();
                        }
                    });
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, final Throwable throwable, File file) {
                // request error
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        Utils.alertError(activity, R.string.error_occurred, new Exception(throwable));
                        mutex.unlock();
                        cb.onFinished();
                    }
                });
            }
        });

        // cancel listener
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface d) {
                dialog.setOnCancelListener(null);
                request.cancel(true);
                mutex.unlock();
                cb.onFinished();
            }
        });

    }

    private static void doExtract(final Activity activity, final JSONDeviceInfo deviceInfo, final JSONBuild updateBuild, final UpdateListener cb, final ProgressDialog dialog, final File file) {
        // set title
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.setMessage("Extracting " + updateBuild.getFilename());
            }
        });

        try {
            // delete directory if it exists
            final File dir = new File(activity.getCacheDir().getPath() + "/installation_package");
            if (dir.exists())
                FileUtils.deleteDirectory(dir);

            // create directory
            if (!dir.mkdirs())
                throw new Exception("Couldn't create extraction directory");

            final Pointer<Boolean> userAbort = new Pointer<>(false);
            final Command cmd = RootUtils.unzip(file.getAbsolutePath(), dir.getAbsolutePath(), new RootUtils.CommandFinished() {
                @Override
                public void commandFinished(int exitcode) {
                    try {
                        if (exitcode != 0)
                            throw new Exception("exitCode=" + exitcode);

                        doInstall(activity, deviceInfo, updateBuild, cb, dialog, dir);
                    } catch (final Exception e) {
                        if (!userAbort.val) {
                            // unzip error or terminated
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dialog.dismiss();
                                    Utils.alertError(activity, R.string.error_occurred, e);
                                    mutex.unlock();
                                    cb.onFinished();
                                }
                            });
                        }
                    }
                }
            }, false);

            // cancel listener
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface d) {
                    dialog.setOnCancelListener(null);
                    userAbort.val = true;
                    cmd.terminate();
                    mutex.unlock();
                    cb.onFinished();
                }
            });
        }

        // preparation error
        catch (final Exception e) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dialog.dismiss();
                    Utils.alertError(activity, R.string.error_occurred, e);
                    mutex.unlock();
                    cb.onFinished();
                }
            });
        }
    }

    private static void doInstall(final Activity activity, final JSONDeviceInfo deviceInfo, final JSONBuild updateBuild, final UpdateListener cb, final ProgressDialog dialog, final File dir) {
        // configure dialog
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.setMessage("Installing " + updateBuild.getFilename());
                dialog.setCancelable(false);
                dialog.setOnCancelListener(null);
            }
        });

        try {
            String lkPart = deviceInfo.getLKInstallationPartition();

            // backup important files
            if (!RootUtils.backupExists("lk.img") || !deviceInfo.isInstalled(true))
                RootUtils.doBackup(lkPart, "lk.img");

            // install package
            RootUtils.installPackage(activity, dir + "/install.sh", deviceInfo.getAbsoluteBootPath(true), dir.getAbsolutePath(), updateBuild.getSHA1(), lkPart, new RootUtils.CommandFinished() {
                @Override
                public void commandFinished(int exitcode) {
                    try {
                        if (exitcode != 0)
                            throw new Exception("exitCode=" + exitcode);

                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.dismiss();
                                mutex.unlock();
                                cb.onFinished();
                            }
                        });
                    }

                    // exit code is non zero
                    catch (final Exception e) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.dismiss();
                                Utils.alertError(activity, R.string.error_occurred, e);
                                mutex.unlock();
                                cb.onFinished();
                            }
                        });
                    }
                }
            });
        }

        // preparation error
        catch (final Exception e) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dialog.dismiss();
                    Utils.alertError(activity, R.string.error_occurred, e);
                    mutex.unlock();
                    cb.onFinished();
                }
            });
        }
    }

    public static interface UpdateListener {
        public abstract void onFinished();
    }
}
