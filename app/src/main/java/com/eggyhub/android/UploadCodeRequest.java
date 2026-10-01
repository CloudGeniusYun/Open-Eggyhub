package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

public class UploadCodeRequest {
    @SerializedName("code")
    private String code;
    @SerializedName("id")
    private String id;

    public UploadCodeRequest(String code, String id) {
        this.code = code;
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public String getId() {
        return id;
    }
}
