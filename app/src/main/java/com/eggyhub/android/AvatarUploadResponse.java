package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

public class AvatarUploadResponse {
    @SerializedName("cover")
    private String cover;

    public String getCover() { return cover; }
}
