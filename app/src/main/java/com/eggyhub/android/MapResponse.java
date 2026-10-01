package com.eggyhub.android;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MapResponse {
    @SerializedName("data")
    private Data data;

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {
        @SerializedName("gameMapInfoList")
        private List<MapItem> gameMapInfoList;

        public List<MapItem> getGameMapInfoList() {
            return gameMapInfoList;
        }

        public void setGameMapInfoList(List<MapItem> gameMapInfoList) {
            this.gameMapInfoList = gameMapInfoList;
        }
    }
}
