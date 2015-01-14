package org.grub4android.grubmanager.models;

import org.json.JSONException;
import org.json.JSONObject;

public class JSONManifest {
    public final JSONObject mJSON;

    public JSONManifest(JSONObject jsonObject) {
        mJSON = jsonObject;
    }

    public JSONObject getVersions() throws JSONException {
        return (JSONObject) mJSON.get("versions");
    }

    public String getGRUBVersion() throws JSONException {
        return getVersions().getString("grub");
    }

    public String getLKVersion() throws JSONException {
        return getVersions().getString("lk");
    }

    public String getMultibootVersion() throws JSONException {
        return getVersions().getString("multiboot");
    }

    public long getTimeStamp() throws JSONException {
        return mJSON.getLong("timestamp");
    }

    public long getRevisionHash() throws JSONException {
        return mJSON.getLong("revision_hash");
    }
}
