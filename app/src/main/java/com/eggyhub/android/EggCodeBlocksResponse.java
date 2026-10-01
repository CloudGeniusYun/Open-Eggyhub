package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

public class EggCodeBlocksResponse {
    @SerializedName("message")
    private int count;

    public int getCount() {
        return count;
    }
}
