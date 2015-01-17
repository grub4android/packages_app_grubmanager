package org.grub4android.grubmanager.models;

import org.json.JSONException;
import org.json.JSONObject;

public class JSONBuild {
    public final JSONObject mJSON;
    private final String mFilename;
    private final String mSHA1;

    public JSONBuild(JSONObject jsonObject) throws JSONException {
        mJSON = jsonObject;
        mFilename = mJSON.getString("filename");
        mSHA1 = mJSON.getString("checksum_sha1");
    }

    public String getSHA1() {
        return mSHA1;
    }

    public String getFilename() {
        return mFilename;
    }
}
