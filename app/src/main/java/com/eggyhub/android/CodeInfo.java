package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

public class CodeInfo {
    @SerializedName("name")
    private String name;
    @SerializedName("cover")
    private String cover = "";
    @SerializedName("description")
    private String description;
    @SerializedName("stock")
    private int stock = 1;
    @SerializedName("first")
    private String first;
    @SerializedName("val")
    private String val;

    public CodeInfo(String name, String description, String first, String val) {
        this.name = name;
        this.description = description;
        this.first = first;
        this.val = val;
    }
}
