package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;

public class CoinsResponse {
    @SerializedName("coins")
    private int coins;

    public int getCoins() {
        return coins;
    }
}
