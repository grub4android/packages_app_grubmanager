package org.grub4android.grubmanager.models;

import org.grub4android.grubmanager.RootUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class JSONDeviceInfo {
    public final JSONObject mDeviceInfo;
    private final String mLKInstallationPartition;
    private final String mBootPath;
    private String mBootloaderMPCache = null;

    public JSONDeviceInfo(JSONObject jsonObject) throws JSONException {
        mDeviceInfo = jsonObject;
        mLKInstallationPartition = mDeviceInfo.getString("lk_installation_partition");
        mBootPath = mDeviceInfo.getString("grub_boot_path_prefix");
    }

    public String getLKInstallationPartition() {
        return mLKInstallationPartition;
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

    public String getBootPath() {
        return mBootPath;
    }

    public String getAbsoluteBootPath(boolean allowCache) throws Exception {
        return getBootloaderMountpoint(allowCache) + "/" + getBootPath();
    }

    public String getInstalledManifestPath(boolean allowCache) throws Exception {
        return getAbsoluteBootPath(allowCache) + "/manifest.json";
    }

    public boolean isInstalled(boolean allowCache) throws Exception {
        return RootUtils.exists(getInstalledManifestPath(allowCache));
    }

    public JSONBuild getLatestBuild() throws JSONException {
        // set update info
        JSONArray builds = (JSONArray) mDeviceInfo.get("builds");

        JSONObject build = null;
        for (int i = 0; i < builds.length(); i++) {
            JSONObject tmp = (JSONObject) builds.get(i);

            if (build == null || tmp.getLong("timestamp") > build.getLong("timestamp"))
                build = tmp;
        }
        return new JSONBuild(build);
    }
}
