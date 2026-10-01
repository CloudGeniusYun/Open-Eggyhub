package com.eggyhub.android;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.lang.reflect.Type;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.eggyhub.android.log.AppLogger;
import com.eggyhub.android.utils.OkHttpClientFactory;

/**
 * 文章列表Activity
 * 负责显示文章组列表和文章内容
 */
public class ArticleActivity extends BaseActivity {

    private static final String TAG = "ArticleActivity";
    private static final String CACHE_KEY = "article_groups_v2";
    private final OkHttpClient client = OkHttpClientFactory.getSharedClient();
    private RecyclerView recyclerViewArticles;
    private ArticleAdapter articleAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article);

        ImageButton backButton = findViewById(R.id.button_back);
        backButton.setOnClickListener(v -> onBackPressed());

        recyclerViewArticles = findViewById(R.id.recyclerViewArticles);
        recyclerViewArticles.setLayoutManager(new LinearLayoutManager(this));

        fetchArticleGroups();
    }

    private void fetchArticleGroups() {
        String url = "https://eggyhub.top/api/article_groups";
        String cachedData = getSharedPreferences("article_cache", MODE_PRIVATE).getString(CACHE_KEY, null);
        if (cachedData != null) {
            AppLogger.d(TAG, "Loading from cache");
            parseAndDisplayArticles(cachedData);
        }

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                AppLogger.e(TAG, "Failed to fetch articles: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(ArticleActivity.this, "获取文章失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful() && responseBody != null) {
                        final String responseData = responseBody.string();
                        AppLogger.d(TAG, "API response: " + responseData);
                        getSharedPreferences("article_cache", MODE_PRIVATE).edit().putString(CACHE_KEY, responseData).apply();
                        parseAndDisplayArticles(responseData);
                    } else {
                        runOnUiThread(() -> Toast.makeText(ArticleActivity.this, "获取文章失败: " + response.code(), Toast.LENGTH_SHORT).show());
                    }
                }
            }
        });
    }

    private void parseAndDisplayArticles(String jsonData) {
        try {
            Gson gson = new Gson();
            java.lang.reflect.Type listType = new TypeToken<List<ArticleData>>() {}.getType();
            List<ArticleData> groups = gson.fromJson(jsonData, listType);
            
            AppLogger.d(TAG, "Parsed groups count: " + (groups != null ? groups.size() : 0));
            
            List<ArticleItem> articleList = new ArrayList<>();
            if (groups != null) {
                for (ArticleData group : groups) {
                    String category = group.getCategory();
                    AppLogger.d(TAG, "Group category: " + category);
                    
                    List<ArticleData.ArticleItemData> items = group.getItems();
                    AppLogger.d(TAG, "Items count: " + (items != null ? items.size() : 0));
                    
                    if (items != null) {
                        for (ArticleData.ArticleItemData itemData : items) {
                            ArticleItem item = new ArticleItem(
                                itemData.getTitle() != null ? itemData.getTitle() : "",
                                itemData.getAuthor() != null ? itemData.getAuthor() : "",
                                category,
                                itemData.getId(),
                                itemData.getDate() != null ? itemData.getDate() : ""
                            );
                            if (itemData.getContent() != null) {
                                item.setContent(itemData.getContent());
                            }
                            AppLogger.d(TAG, "Item: title=" + item.getTitle() + ", author=" + item.getAuthor());
                            articleList.add(item);
                        }
                    }
                }
            }

            AppLogger.d(TAG, "Total articles: " + articleList.size());
            
            final List<ArticleItem> finalList = articleList;
            runOnUiThread(() -> {
                if (!finalList.isEmpty()) {
                    articleAdapter = new ArticleAdapter(finalList);
                    recyclerViewArticles.setAdapter(articleAdapter);
                } else {
                    Toast.makeText(ArticleActivity.this, "暂无文章数据", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            AppLogger.e(TAG, "Parse error: " + e.getMessage());
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(ArticleActivity.this, "解析文章数据失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }
}