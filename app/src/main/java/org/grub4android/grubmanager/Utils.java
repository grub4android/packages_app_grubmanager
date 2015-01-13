package org.grub4android.grubmanager;

import android.content.Context;
import android.content.res.AssetManager;
import android.text.format.DateFormat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Locale;

public class Utils {
    public static void copyFile(InputStream is, OutputStream os) throws IOException {
        try {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void copyFile(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            os.close();
        }
    }

    public static File copyAssets(Context context, String filename) {
        String appFileDirectory = context.getFilesDir().getPath();
        AssetManager assetManager = context.getAssets();

        try {
            // open input file
            InputStream in = assetManager.open(filename);

            // open output file
            File outFile = new File(appFileDirectory, filename);
            OutputStream out = new FileOutputStream(outFile);

            // copy file
            copyFile(in, out);

            // close input file
            in.close();

            // close output file
            out.flush();
            out.close();

            return outFile;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public static String getStringFromFile(File fl) throws Exception {
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        fin.close();
        return ret;
    }

    public static String getDate(long time) {
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(time * 1000);
        String date = DateFormat.format("yyyyMMdd", cal).toString();
        return date;
    }
}
