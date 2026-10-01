package com.eggyhub.android.utils;

import android.os.Handler;
import android.os.Looper;

import com.eggyhub.android.model.NoticeItem;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * 公告数据管理类 - 单例模式
 * 用于预加载和缓存公告数据
 */
public class NoticeDataManager {
    private static NoticeDataManager instance;
    private List<NoticeItem> cachedNoticeItems;
    private boolean isLoading = false;
    private boolean isLoaded = false;
    private CountDownLatch loadLatch = new CountDownLatch(1);

    private NoticeDataManager() {
        cachedNoticeItems = new ArrayList<>();
    }

    public static synchronized NoticeDataManager getInstance() {
        if (instance == null) {
            instance = new NoticeDataManager();
        }
        return instance;
    }

    /**
     * 预加载公告数据
     */
    public void preloadNoticeData() {
        if (isLoading || isLoaded) {
            return;
        }

        isLoading = true;

        new Thread(() -> {
            try {
                URL url = new URL("https://u5.update.netease.com/game_notice/android.txt");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // 解析JSON
                    cachedNoticeItems = NoticeItem.fromJsonArray(response.toString());
                    isLoaded = true;
                }
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                // 如果加载失败，使用本地默认数据
                cachedNoticeItems = new ArrayList<>();
            } finally {
                isLoading = false;
                loadLatch.countDown();
            }
        }).start();
    }

    /**
     * 等待数据加载完成
     */
    public void waitForLoad() {
        if (isLoaded) {
            return;
        }

        try {
            loadLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取缓存的公告数据
     */
    public List<NoticeItem> getCachedNoticeItems() {
        return cachedNoticeItems;
    }

    /**
     * 是否已加载完成
     */
    public boolean isLoaded() {
        return isLoaded;
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        cachedNoticeItems.clear();
        isLoaded = false;
        loadLatch = new CountDownLatch(1);
    }
}