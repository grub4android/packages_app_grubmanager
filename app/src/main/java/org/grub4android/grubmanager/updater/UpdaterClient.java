package org.grub4android.grubmanager.updater;

import android.content.Context;
import android.os.Build;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestHandle;

import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.grub4android.grubmanager.models.JSONDeviceInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UpdaterClient {
    // URL's
    private static final String URL_BASE = "http://192.168.10.173:8000/";
    private static final String URL_DEVICES = URL_BASE + "devices.json";
    private static final String URL_DEVICE_INFO = URL_BASE + "devices/" + Build.DEVICE + "/manifest.json";
    private static final String URL_BUILDS = URL_BASE + "devices/" + Build.DEVICE + "/builds";
    // members
    public static ArrayList<String> mDeviceList = null;
    private static JSONDeviceInfo mDeviceInfo = null;
    // client
    private static AsyncHttpClient client = new AsyncHttpClient();

    public static void clearCache() {
        mDeviceList = null;
        mDeviceInfo = null;
    }

    public static void getDeviceList(final Context context, final DeviceListReceivedCallback cb) {
        // we have the data already
        if (mDeviceList != null) {
            if (cb != null) cb.onDeviceListReceived(mDeviceList, null);
            return;
        }

        // fetch from server
        client.get(URL_DEVICES, new JsonHttpResponseHandler() {

            private File getCacheFile() {
                return new File(context.getCacheDir(), "devices.json");
            }

            private void loadJSONArray(JSONArray json) {
                mDeviceList = new ArrayList<>();
                for (int i = 0; i < json.length(); i++) {
                    try {
                        mDeviceList.add(json.getString(i));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                // cache in memory
                loadJSONArray(response);

                // cache on disk
                try {
                    FileUtils.writeStringToFile(getCacheFile(), response.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (cb != null) cb.onDeviceListReceived(mDeviceList, null);
            }

            private void onAnyError() {
                // read from disk
                try {
                    String data = FileUtils.readFileToString(getCacheFile());
                    loadJSONArray(new JSONArray(data));

                    if (cb != null) cb.onDeviceListReceived(mDeviceList, null);
                } catch (Exception e) {
                    e.printStackTrace();

                    // an error occurred
                    if (cb != null) cb.onDeviceListReceived(null, e);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                onAnyError();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                onAnyError();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                onAnyError();
            }
        });
    }

    public static void getDeviceInfo(final Context context, final DeviceInfoReceivedCallback cb) {
        // we have the data already
        if (mDeviceInfo != null) {
            if (cb != null) cb.onDeviceInfoReceived(mDeviceInfo, null);
            return;
        }

        // fetch from server
        client.get(URL_DEVICE_INFO, new JsonHttpResponseHandler() {

            private File getCacheFile() {
                return new File(context.getCacheDir(), "device_info.json");
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                // cache in memory
                mDeviceInfo = new JSONDeviceInfo(response);

                // cache on disk
                try {
                    FileUtils.writeStringToFile(getCacheFile(), response.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (cb != null) cb.onDeviceInfoReceived(mDeviceInfo, null);
            }

            private void onAnyError() {
                // read from disk
                try {
                    String data = FileUtils.readFileToString(getCacheFile());
                    mDeviceInfo = new JSONDeviceInfo(new JSONObject(data));

                    if (cb != null) cb.onDeviceInfoReceived(mDeviceInfo, null);
                } catch (Exception e) {
                    e.printStackTrace();

                    // an error occurred
                    if (cb != null) cb.onDeviceInfoReceived(null, e);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                onAnyError();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                onAnyError();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                onAnyError();
            }
        });
    }

    public static RequestHandle downloadPackage(String name, FileAsyncHttpResponseHandler handler) {
        AsyncHttpClient client = new AsyncHttpClient();
        return client.get(URL_BUILDS + "/" + name, handler);
    }

    public static interface DeviceInfoReceivedCallback {
        public abstract void onDeviceInfoReceived(JSONDeviceInfo deviceInfo, Exception e);
    }

    public static interface DeviceListReceivedCallback {
        public abstract void onDeviceListReceived(List<String> devices, Exception e);
    }
}
