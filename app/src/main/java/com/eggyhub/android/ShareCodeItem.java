package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

public class ShareCodeItem {
    @SerializedName("cover")
    private String cover;
    @SerializedName("description")
    private String description;
    @SerializedName("id")
    private int id;
    @SerializedName("likes")
    private int likes;
    @SerializedName("name")
    private String name;
    @SerializedName("provider")
    private String provider;
    @SerializedName("providerid")
    private int providerid;
    @SerializedName("status")
    private int status;
    @SerializedName("stock")
    private int stock;
    @SerializedName("gr")
    private int gr;
    @SerializedName("value")
    private int value;

    public ShareCodeItem(String cover, String description, int id, int likes, String name, String provider, int providerid, int status, int stock, int gr, int value) {
        this.cover = cover;
        this.description = description;
        this.id = id;
        this.likes = likes;
        this.name = name;
        this.provider = provider;
        this.providerid = providerid;
        this.status = status;
        this.stock = stock;
        this.gr = gr;
        this.value = value;
    }

    public String getCover() {
        return cover;
    }

    public String getDescription() {
        return description;
    }

    public int getId() {
        return id;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public String getName() {
        return name;
    }

    public String getProvider() {
        return provider;
    }

    public int getProviderid() {
        return providerid;
    }

    public int getStatus() {
        return status;
    }

    public int getStock() {
        return stock;
    }

    public int getGr() {
        return gr;
    }

    public void setGr(int gr) {
        this.gr = gr;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
