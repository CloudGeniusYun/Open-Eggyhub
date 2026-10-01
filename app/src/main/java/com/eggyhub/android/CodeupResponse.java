package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

public class CodeupResponse {
    @SerializedName("cover")
    private String cover;
    @SerializedName("id")
    private int id;

    public String getCover() {
        return cover;
    }

    public int getId() {
        return id;
    }
}
