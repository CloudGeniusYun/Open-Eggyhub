package com.eggyhub.android;

import androidx.annotation.Keep;
import com.google.gson.annotations.SerializedName;

@Keep
public class ArticleItem {
    @SerializedName("title")
    private String title;
    @SerializedName("author")
    private String author;
    @SerializedName("category")
    private String category;
    @SerializedName("id")
    private int id;
    @SerializedName("date")
    private String date;
    @SerializedName("content")
    private String content;

    public ArticleItem(String title, String author, String category, int id, String date) {
        this.title = title;
        this.author = author;
        this.category = category;
        this.id = id;
        this.date = date;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getCategory() {
        return category;
    }

    public int getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}