package com.eggyhub.android.model.bili;

import java.io.Serializable;
import java.util.List;

public class BiliPlayUrlResponse implements Serializable {
    private int code;
    private String message;
    private Data data;

    public int getCode() { return code; }
    public String getMessage() { return message; }
    public Data getData() { return data; }

    public static class Data implements Serializable {
        private List<DUrl> durl;
        private Dash dash;

        public List<DUrl> getDurl() { return durl; }
        public Dash getDash() { return dash; }
    }

    public static class DUrl implements Serializable {
        private String url;
        public String getUrl() { return url; }
    }

    public static class Dash implements Serializable {
        private List<Media> video;
        private List<Media> audio;
        public List<Media> getVideo() { return video; }
        public List<Media> getAudio() { return audio; }
    }

    public static class Media implements Serializable {
        private int id;
        private String baseUrl;
        private int codecid;

        public int getId() { return id; }
        public String getBaseUrl() { return baseUrl; }
        public int getCodecid() { return codecid; }
    }
}
