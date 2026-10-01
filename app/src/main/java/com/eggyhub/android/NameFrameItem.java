package com.eggyhub.android;

import androidx.annotation.Keep;

@Keep
public class NameFrameItem {
    private String frameName;
    private String thumbnailName;

    public NameFrameItem(String frameName, String thumbnailName) {
        this.frameName = frameName;
        this.thumbnailName = thumbnailName;
    }

    public String getFrameName() {
        return frameName;
    }

    public String getThumbnailName() {
        return thumbnailName;
    }
}