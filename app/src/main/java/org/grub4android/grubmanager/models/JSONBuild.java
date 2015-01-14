package org.grub4android.grubmanager.models;

import org.json.JSONException;
import org.json.JSONObject;

public class JSONBuild {
    public final JSONObject mJSON;

    public JSONBuild(JSONObject jsonObject) {
        mJSON = jsonObject;
    }

    public String getSHA1() throws JSONException {
        return mJSON.getString("checksum_sha1");
    }

    public String getFilename() throws JSONException {
        return mJSON.getString("filename");
    }
}
