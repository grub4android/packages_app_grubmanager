package org.grub4android.grubmanager.models;

import org.grub4android.grubmanager.RootUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class JSONDeviceInfo {
    public final JSONObject mDeviceInfo;
    private String mBootloaderMPCache = null;

    public JSONDeviceInfo(JSONObject jsonObject) {
        mDeviceInfo = jsonObject;
    }

    public String getBootloaderMountpoint(boolean allowCache) throws Exception {
        if (allowCache && mBootloaderMPCache != null)
            return mBootloaderMPCache;

        // get boot partition mountpoint
        String mountPoint = null;
        int[] majmin = RootUtils.getBootloaderPartMajorMinor(mDeviceInfo.getString("grub_boot_partition_name"));
        ArrayList<MountInfo> mountInfo = RootUtils.getMountInfo();
        for (MountInfo i : mountInfo) {
            if (i.getMajor() == majmin[0] && i.getMinor() == majmin[1] && i.getRoot().equals("/")) {
                mountPoint = i.getMountPoint();
                break;
            }
        }
        if (mountPoint == null) {
            throw new Exception("Bootloader partition is not mounted!");
        }

        mBootloaderMPCache = mountPoint;
        return mountPoint;
    }

    public String getBootPath() throws JSONException {
        return mDeviceInfo.getString("grub_boot_path_prefix");
    }

    public String getAbsoluteBootPath(boolean allowCache) throws Exception {
        return getBootloaderMountpoint(allowCache) + "/" + getBootPath();
    }

    public JSONBuild getLatestBuild() throws JSONException {
        // set update info
        JSONArray builds = (JSONArray) mDeviceInfo.get("builds");
        return new JSONBuild((JSONObject) builds.get(0));
    }
}
