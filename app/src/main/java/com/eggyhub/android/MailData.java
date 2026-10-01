package com.eggyhub.android;

import androidx.annotation.Keep;
import com.google.gson.annotations.SerializedName;

@Keep
public class MailData {
    @SerializedName("title")
    private String title;
    @SerializedName("content")
    private String content;
    @SerializedName("created_at")
    private String createdAt;
    @SerializedName("is_sent")
    private boolean isSent;
    @SerializedName("senderid")
    private int senderId;
    @SerializedName("targetid")
    private int targetId;
    @SerializedName("status")
    private int status;

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public boolean isSent() {
        return isSent;
    }

    public int getSenderId() {
        return senderId;
    }

    public int getTargetId() {
        return targetId;
    }

    public int getStatus() {
        return status;
    }
}
