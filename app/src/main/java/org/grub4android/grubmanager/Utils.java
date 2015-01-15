package org.grub4android.grubmanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.text.format.DateFormat;

import com.afollestad.materialdialogs.MaterialDialog;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Locale;

public class Utils {
    public static File copyAssets(Context context, String filename) {
        String appFileDirectory = context.getFilesDir().getPath();
        AssetManager assetManager = context.getAssets();

        try {
            // open input file
            InputStream in = assetManager.open(filename);

            // copy file
            File outFile = new File(appFileDirectory, filename);
            FileUtils.copyInputStreamToFile(in, outFile);

            // close input file
            in.close();

            return outFile;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getDate(long time) {
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(time * 1000);
        String date = DateFormat.format("yyyyMMdd", cal).toString();
        return date;
    }

    public static void runOnUiThread(final Activity a, final long delay, final Runnable r) {
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                }

                a.runOnUiThread(r);
            }
        }.start();
    }

    public static void alert(Context context, int title, String message) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(context);
        builder.accentColor(context.getResources().getColor(R.color.material_green));
        builder.title(title);
        builder.content(message);
        builder.positiveText(android.R.string.ok);
        builder.show();
    }

    public static void alertError(final Context context, final int title, final Exception e) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(context);
        builder.accentColor(context.getResources().getColor(R.color.material_green));
        builder.title(title);
        builder.content(e.getLocalizedMessage());
        builder.positiveText(android.R.string.ok);
        builder.negativeText(R.string.report);
        builder.callback(new MaterialDialog.ButtonCallback() {
            @Override
            public void onNegative(MaterialDialog dialog) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri data = Uri.parse("mailto:bugreport@grub4android.org?subject=" + context.getString(title) + "&body=" + ExceptionUtils.getFullStackTrace(e));
                intent.setData(data);
                context.startActivity(intent);
            }
        });
        builder.show();
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

}
