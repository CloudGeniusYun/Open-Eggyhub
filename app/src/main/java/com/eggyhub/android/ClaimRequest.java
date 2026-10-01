package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

public class ClaimRequest {
    @SerializedName("giftId")
    private int giftId;

    public ClaimRequest(int giftId) {
        this.giftId = giftId;
    }

    public int getGiftId() {
        return giftId;
    }

    public void setGiftId(int giftId) {
        this.giftId = giftId;
    }
}
