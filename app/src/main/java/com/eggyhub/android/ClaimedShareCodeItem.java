package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

public class ClaimedShareCodeItem {
    @SerializedName("code")
    private String code;
    @SerializedName("id")
    private int id;
    @SerializedName("name")
    private String name;

    public ClaimedShareCodeItem(String code, int id, String name) {
        this.code = code;
        this.id = id;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
