package com.eggyhub.android;

public class MailItem {
    private String title;
    private String content;
    private String from;
    private String to;
    private String createdAt;
    private String imageRes;

    public MailItem(String title, String content, String from, String to, String createdAt, String imageRes) {
        this.title = title;
        this.content = content;
        this.from = from;
        this.to = to;
        this.createdAt = createdAt;
        this.imageRes = imageRes;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getImageRes() {
        return imageRes;
    }
}