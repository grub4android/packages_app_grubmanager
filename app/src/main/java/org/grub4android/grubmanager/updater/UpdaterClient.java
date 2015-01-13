package org.grub4android.grubmanager.updater;

import android.os.Build;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestHandle;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class UpdaterClient {
    // URL's
    private static final String URL_BASE = "http://192.168.10.173:8000/";
    private static final String URL_DEVICES = URL_BASE + "devices.json";
    private static final String URL_DEVICE_INFO = URL_BASE + "devices/" + Build.DEVICE + "/manifest.json";
    private static final String URL_BUILDS = URL_BASE + "devices/" + Build.DEVICE + "/builds";
    // members
    public static ArrayList<String> mDevices = new ArrayList<>();
    public static JSONObject mDeviceInfo = null;
    // client
    private static AsyncHttpClient client = new AsyncHttpClient();

    public static void fetchData(final RequestDoneCallback cb, final boolean update) {
        // devices
        if (update) mDevices.clear();
        getDevices(new RequestDoneCallback() {
            @Override
            public void onRequestDone(boolean success) {
                // error or device not found
                if (!success || !mDevices.contains(Build.DEVICE)) {
                    if (cb != null) cb.onRequestDone(false);
                    return;
                }

                // devices
                if (update) mDeviceInfo = null;
                getDeviceInfo(new RequestDoneCallback() {
                    @Override
                    public void onRequestDone(boolean success) {
                        // error or device not found
                        if (!success || mDeviceInfo == null) {
                            if (cb != null) cb.onRequestDone(false);
                            return;
                        }

                        // we're done, run the callback
                        if (cb != null) cb.onRequestDone(true);
                    }
                });
            }
        });
    }


    public static void getDevices(final RequestDoneCallback cb) {
        // fetch data
        if (mDevices.size() == 0) {
            client.get(URL_DEVICES, new JsonHttpResponseHandler() {

                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                    synchronized (mDevices) {
                        mDevices.clear();
                        for (int i = 0; i < response.length(); i++) {
                            try {
                                mDevices.add(response.getString(i));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    if (cb != null) cb.onRequestDone(true);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    super.onFailure(statusCode, headers, responseString, throwable);

                    // an error occurred
                    if (cb != null) cb.onRequestDone(false);
                }
            });
        }

        // we have all data already
        else if (cb != null) cb.onRequestDone(true);
    }

    public static void getDeviceInfo(final RequestDoneCallback cb) {
        // fetch data
        if (mDeviceInfo == null) {
            client.get(URL_DEVICE_INFO, new JsonHttpResponseHandler() {

                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    mDeviceInfo = response;

                    if (cb != null) cb.onRequestDone(true);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    super.onFailure(statusCode, headers, responseString, throwable);

                    // an error occurred
                    if (cb != null) cb.onRequestDone(false);
                }
            });
        }

        // we have all data already
        else if (cb != null) cb.onRequestDone(true);
    }

    public static RequestHandle downloadPackage(String name, FileAsyncHttpResponseHandler handler) {
        AsyncHttpClient client = new AsyncHttpClient();
        return client.get(URL_BUILDS + "/" + name, handler);
    }

    public static interface RequestDoneCallback {
        public abstract void onRequestDone(boolean success);
    }
}
