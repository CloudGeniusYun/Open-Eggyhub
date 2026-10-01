package com.eggyhub.android.model.bili;

import java.io.Serializable;
import java.util.List;

public class BiliViewResponse implements Serializable {
    private int code;
    private String message;
    private Data data;

    public int getCode() { return code; }
    public String getMessage() { return message; }
    public Data getData() { return data; }

    public static class Data implements Serializable {
        private String title;
        private String bvid;
        private String pic;
        private List<Page> pages;

        public String getTitle() { return title; }
        public String getBvid() { return bvid; }
        public String getPic() { return pic; }
        public List<Page> getPages() { return pages; }
    }

    public static class Page implements Serializable {
        private long cid;
        private String part;

        public long getCid() { return cid; }
        public String getPart() { return part; }
    }
}
