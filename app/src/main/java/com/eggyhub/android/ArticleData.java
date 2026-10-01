package com.eggyhub.android;

import androidx.annotation.Keep;
import com.google.gson.annotations.SerializedName;

import java.util.List;

@Keep
public class ArticleData {
    @SerializedName("category")
    private String category;

    @SerializedName("id")
    private int id;

    @SerializedName("items")
    private List<ArticleItemData> items;

    @Keep
    public static class ArticleItemData {
        @SerializedName("title")
        private String title;

        @SerializedName("author")
        private String author;

        @SerializedName("id")
        private int id;

        @SerializedName("date")
        private String date;

        @SerializedName("content")
        private String content;

        public String getTitle() {
            return title;
        }

        public String getAuthor() {
            return author;
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
    }

    public String getCategory() {
        return category;
    }

    public int getId() {
        return id;
    }

    public List<ArticleItemData> getItems() {
        return items;
    }
}
