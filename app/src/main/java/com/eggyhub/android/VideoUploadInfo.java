package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

public class VideoUploadInfo {
    @SerializedName("name")
    private String name;
    @SerializedName("cover")
    private String cover;
    @SerializedName("description")
    private String description;
    @SerializedName("stock")
    private int stock;
    @SerializedName("link")
    private String link;

    public VideoUploadInfo(String name, String cover, String description, int stock, String link) {
        this.name = name;
        this.cover = cover;
        this.description = description;
        this.stock = stock;
        this.link = link;
    }
}
