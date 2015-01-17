package org.grub4android.grubmanager.updater;

import android.app.Activity;
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
    // files
    private static final String FILE_DEVICES = "devices.json";
    private static final String FILE_DEVICE_INFO = "device_info.json";
    // members
    public static ArrayList<String> mDeviceList = null;
    private static JSONDeviceInfo mDeviceInfo = null;
    // client
    private static AsyncHttpClient client = new AsyncHttpClient();

    private static ArrayList<String> getDeviceListFromJSON(JSONArray json) {
        ArrayList<String> tmp = new ArrayList<>();
        for (int i = 0; i < json.length(); i++) {
            try {
                tmp.add(json.getString(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return tmp;
    }

    private static File getCacheFile(Context context, String filename) {
        return new File(context.getCacheDir(), filename);
    }

    public static void getDeviceListCached(final Activity activity, final DeviceListListener cb) {
        new Thread() {
            @Override
            public void run() {
                try {
                    if (mDeviceList == null) {
                        File f = getCacheFile(activity, FILE_DEVICES);
                        String data = FileUtils.readFileToString(f);
                        mDeviceList = getDeviceListFromJSON(new JSONArray(data));
                    }

                    // run callback
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            cb.onDeviceListReceived(mDeviceList, true);
                        }
                    });
                } catch (final Exception e) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            cb.onError(e);
                        }
                    });
                }
            }
        }.start();
    }

    public static RequestHandle getDeviceList(final Activity activity, boolean tryCache, final DeviceListListener cb) {
        // try cache first
        if (tryCache) {
            getDeviceListCached(activity, new DeviceListListener() {
                @Override
                public void onDeviceListReceived(List<String> devices, boolean fromCache) {
                    cb.onDeviceListReceived(devices, true);
                }

                @Override
                public void onError(Exception e) {
                    getDeviceList(activity, false, cb);
                }
            });
            return null;
        }

        // fetch from server
        return client.get(URL_DEVICES, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                // load to memory
                mDeviceList = getDeviceListFromJSON(response);

                // write to disk
                try {
                    FileUtils.writeStringToFile(getCacheFile(activity, FILE_DEVICES), response.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // run callback
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cb.onDeviceListReceived(mDeviceList, false);
                    }
                });
            }

            private void onAnyError(final Throwable throwable) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cb.onError(new Exception(throwable));
                    }
                });
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                onAnyError(throwable);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                onAnyError(throwable);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                onAnyError(throwable);
            }
        });
    }

    public static void getDeviceInfoCached(final Activity activity, final DeviceInfoListener cb) {
        new Thread() {
            @Override
            public void run() {
                try {
                    if (mDeviceInfo == null) {
                        File f = getCacheFile(activity, FILE_DEVICE_INFO);
                        String data = FileUtils.readFileToString(f);
                        mDeviceInfo = new JSONDeviceInfo(new JSONObject(data));
                    }

                    // run callback
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            cb.onDeviceInfoReceived(mDeviceInfo, true);
                        }
                    });
                } catch (final Exception e) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            cb.onError(e);
                        }
                    });
                }
            }
        }.start();
    }

    public static RequestHandle getDeviceInfo(final Activity activity, final boolean tryCache, final DeviceInfoListener cb) {
        // try cache first
        if (tryCache) {
            getDeviceInfoCached(activity, new DeviceInfoListener() {
                @Override
                public void onDeviceInfoReceived(JSONDeviceInfo deviceInfo, boolean fromCache) {
                    cb.onDeviceInfoReceived(deviceInfo, true);
                }

                @Override
                public void onError(Exception e) {
                    getDeviceInfo(activity, false, cb);
                }
            });
            return null;
        }

        // fetch from server
        return client.get(URL_DEVICE_INFO, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                // load to memory
                try {
                    mDeviceInfo = new JSONDeviceInfo(response);
                } catch (JSONException e) {
                    onAnyError(new Throwable(e));
                    return;
                }

                // write to disk
                try {
                    FileUtils.writeStringToFile(getCacheFile(activity, FILE_DEVICE_INFO), response.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // run callback
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cb.onDeviceInfoReceived(mDeviceInfo, false);
                    }
                });
            }

            private void onAnyError(final Throwable throwable) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cb.onError(new Exception(throwable));
                    }
                });
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                onAnyError(throwable);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                onAnyError(throwable);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                onAnyError(throwable);
            }
        });
    }

    public static RequestHandle downloadPackage(String name, FileAsyncHttpResponseHandler handler) {
        AsyncHttpClient client = new AsyncHttpClient();
        return client.get(URL_BUILDS + "/" + name, handler);
    }

    public static interface DeviceInfoListener {
        public abstract void onDeviceInfoReceived(JSONDeviceInfo deviceInfo, boolean fromCache);

        public abstract void onError(Exception e);
    }

    public static interface DeviceListListener {
        public abstract void onDeviceListReceived(List<String> devices, boolean fromCache);

        public abstract void onError(Exception e);
    }
}
