package com.eggyhub.android;

import static android.content.ContentValues.TAG;

import com.eggyhub.android.log.AppLogger;
import com.google.gson.annotations.SerializedName;

public class Myvditem {
    @SerializedName("cover")
    String cover;
    @SerializedName("description")
    String description;
    @SerializedName("gr")
    int gr;
    @SerializedName("id")
    int id;
    @SerializedName("name")
    String name;
    @SerializedName("link")
    String link;

    public Myvditem(String cover, String description, int gr, int id, String name, String link) {
        this.cover = cover;
        this.description = description;
        this.gr = gr;
        this.id = id;
        this.name = name;
        this.link = link;
    }
    public Myvditem(){}

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCover() {
        AppLogger.e(TAG, "getCover:"+ cover);
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public int getGr() {
        return gr;
    }

    public void setGr(int gr) {
        this.gr = gr;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public  String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
