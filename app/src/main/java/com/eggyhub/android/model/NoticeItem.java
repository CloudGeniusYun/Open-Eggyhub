package com.eggyhub.android.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 公告项数据模型
 */
public class NoticeItem {
    private String title; // 分类标题
    private String titleBgPath; // 分类标题背景图片路径
    private List<String> content; // 内容列表

    public NoticeItem() {
        content = new ArrayList<>();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitleBgPath() {
        return titleBgPath;
    }

    public void setTitleBgPath(String titleBgPath) {
        this.titleBgPath = titleBgPath;
    }

    public List<String> getContent() {
        return content;
    }

    public void setContent(List<String> content) {
        this.content = content;
    }

    /**
     * 从JSON对象解析公告项
     */
    public static NoticeItem fromJson(JSONObject json) {
        NoticeItem item = new NoticeItem();
        try {
            item.setTitle(json.optString("title"));
            item.setTitleBgPath(json.optString("title_bg_path"));

            JSONArray contentArray = json.optJSONArray("content");
            if (contentArray != null) {
                for (int i = 0; i < contentArray.length(); i++) {
                    item.getContent().add(contentArray.optString(i));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return item;
    }

    /**
     * 从JSON数组解析公告列表
     */
    public static List<NoticeItem> fromJsonArray(String jsonString) {
        List<NoticeItem> items = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.getJSONObject(i);
                items.add(fromJson(json));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return items;
    }
}