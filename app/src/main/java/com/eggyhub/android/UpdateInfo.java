package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

public class UpdateInfo {
    @SerializedName("isused")
    private boolean isUsed = true;

    @SerializedName("usedVersion")
    private int usedVersion = -1;

    @SerializedName("notice")
    private String notice = "软件当前不可用";

    @SerializedName("latestVersionCode")
    private int latestVersionCode;

    @SerializedName("updateMessage")
    private String updateMessage;

    @SerializedName("forceUpdate")
    private boolean forceUpdate = false;

    @SerializedName("download_url")
    private String downloadUrl;

    public boolean isUsed() {
        return isUsed;
    }

    public void setUsed(boolean used) {
        isUsed = used;
    }

    public int getUsedVersion() {
        return usedVersion;
    }

    public void setUsedVersion(int usedVersion) {
        this.usedVersion = usedVersion;
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }

    public int getLatestVersionCode() {
        return latestVersionCode;
    }

    public void setLatestVersionCode(int latestVersionCode) {
        this.latestVersionCode = latestVersionCode;
    }

    public String getUpdateMessage() {
        return updateMessage;
    }

    public void setUpdateMessage(String updateMessage) {
        this.updateMessage = updateMessage;
    }

    public boolean isForceUpdate() {
        return forceUpdate;
    }

    public void setForceUpdate(boolean forceUpdate) {
        this.forceUpdate = forceUpdate;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
}
