package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

public class PublishArticleRequest {
    @SerializedName("id")
    private String id;
    @SerializedName("title")
    private String title;
    @SerializedName("author")
    private int author;
    @SerializedName("content")
    private String content;
    @SerializedName("group")
    private int group;

    public PublishArticleRequest(String id, String title, int author, String content, int group) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.content = content;
        this.group = group;
    }
}
