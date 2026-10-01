package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

public class DisplayShareCodeItem {
    @SerializedName("id")
    private int id;
    @SerializedName("code")
    private String code;

    public DisplayShareCodeItem() {}

    public DisplayShareCodeItem(int id, String code) {
        this.id = id;
        this.code = code;
    }

    public int getId() {
        return id;
    }

    public String getCode() {
        return code;
    }
    
    public void setId(int id) { this.id = id; }
    public void setCode(String code) { this.code = code; }
}
