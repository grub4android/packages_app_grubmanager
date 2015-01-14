package org.grub4android.grubmanager;

import android.content.Context;
import android.content.res.AssetManager;
import android.text.format.DateFormat;

import org.apache.commons.io.FileUtils;

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
}
