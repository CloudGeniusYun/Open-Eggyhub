package com.eggyhub.android;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.content.Intent;
import android.widget.TextView;
import android.widget.Toast;
import com.eggyhub.android.utils.VideoPlayerHelper;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.RequestBody;
import okhttp3.MediaType;
import com.eggyhub.android.utils.OkHttpClientFactory;
import android.content.SharedPreferences;

public class SearchActivity extends BaseActivity {

    private Spinner searchTypeSpinner;
    private EditText searchEditText;
    private ImageView searchButton;
    private RecyclerView searchResultsRecyclerView;
    private TextView noResultsTextView;

    private int currentSearchType;
    private String currentKeyword;

    private OkHttpClient client = OkHttpClientFactory.getSharedClient();
    private Gson gson = new Gson();

    private ArticleAdapter articleAdapter;
    private VideoListAdapter videoListAdapter;
    private VideoPlayerHelper videoPlayerHelper;
    private ShareCodeAdapter shareCodeAdapter;
    private List<ShareCodeItem> shareCodeList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        searchTypeSpinner = findViewById(R.id.search_type_spinner_search_page);
        searchEditText = findViewById(R.id.search_edit_text_search_page);
        searchButton = findViewById(R.id.search_button_search_page);
        searchResultsRecyclerView = findViewById(R.id.search_results_recycler_view);
        noResultsTextView = findViewById(R.id.no_results_text_view);

        videoPlayerHelper = new VideoPlayerHelper(this);

        // 初始化下拉选框
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.search_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        searchTypeSpinner.setAdapter(adapter);

        // 获取从MainActivity传递过来的搜索参数
        String initialKeyword = getIntent().getStringExtra("keyword");
        int initialSearchType = getIntent().getIntExtra("searchType", 0);

        if (initialKeyword != null && !initialKeyword.isEmpty()) {
            searchEditText.setText(initialKeyword);
            searchTypeSpinner.setSelection(initialSearchType);
            performSearch(initialSearchType, initialKeyword, true);
        }

        // 设置搜索按钮点击事件
        searchButton.setOnClickListener(v -> {
            String keyword = searchEditText.getText().toString().trim();
            int searchType = searchTypeSpinner.getSelectedItemPosition();
            if (!keyword.isEmpty()) {
                performSearch(searchType, keyword, true);
            } else {
                Toast.makeText(SearchActivity.this, "请输入搜索内容", Toast.LENGTH_SHORT).show();
            }
        });

        // 设置回车键搜索
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                String keyword = searchEditText.getText().toString().trim();
                int searchType = searchTypeSpinner.getSelectedItemPosition();
                if (!keyword.isEmpty()) {
                    performSearch(searchType, keyword, true);
                } else {
                    Toast.makeText(SearchActivity.this, "请输入搜索内容", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });
    }

    private void performSearch(int searchType, String keyword, boolean showToast) {
        this.currentSearchType = searchType;
        this.currentKeyword = keyword;
        if (showToast) {
            Toast.makeText(this, "正在搜索...", Toast.LENGTH_SHORT).show();
        }
        String baseUrl = "https://eggyhub.top/api/";
        String url = "";
        try {
            String encodedKeyword = URLEncoder.encode(keyword, "UTF-8");
            switch (searchType) {
                case 0: // 文章
                    url = baseUrl + "article_all/search?keyword=" + encodedKeyword;
                    break;
                case 1: // 视频
                    url = baseUrl + "videos/search?keyword=" + encodedKeyword;
                    break;
                case 2: // 分享码
                    url = baseUrl + "gifts/search?q=" + encodedKeyword;
                    break;
                default:
                    Toast.makeText(this, "无效的搜索类型", Toast.LENGTH_SHORT).show();
                    return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "搜索失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(SearchActivity.this, "网络请求失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful() && responseBody != null) {
                        String responseData = responseBody.string();
                        runOnUiThread(() -> {
                            try {
                                handleSearchResults(searchType, responseData);
                            } catch (Exception e) {
                                Toast.makeText(SearchActivity.this, "数据解析失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                            }
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(SearchActivity.this, "搜索失败: " + response.message(), Toast.LENGTH_SHORT).show());
                    }
                }
            }
        });
    }

    private void handleSearchResults(int searchType, String responseData) {
        switch (searchType) {
            case 0: // 文章
                List<ArticleItem> articleList = gson.fromJson(responseData, new TypeToken<List<ArticleItem>>() {}.getType());
                if (articleList == null) articleList = new ArrayList<>();
                
                if (articleList.isEmpty()) {
                    noResultsTextView.setVisibility(View.VISIBLE);
                    searchResultsRecyclerView.setVisibility(View.GONE);
                } else {
                    noResultsTextView.setVisibility(View.GONE);
                    searchResultsRecyclerView.setVisibility(View.VISIBLE);
                    articleAdapter = new ArticleAdapter(articleList); // 传递List<ArticleItem>
                    searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                    searchResultsRecyclerView.setAdapter(articleAdapter);
                }
                break;
            case 1: // 视频
                List<VideoItem> videoList = gson.fromJson(responseData, new TypeToken<List<VideoItem>>() {}.getType());
                videoListAdapter = new VideoListAdapter(videoList);
              videoListAdapter.setOnItemClickListener(videoItem -> {
                  if (videoItem != null && videoItem.getLink() != null) {
                      videoPlayerHelper.fetchBiliPlayUrl(videoItem.getLink());
                  }
              });
                if (videoList.isEmpty()) {
                    noResultsTextView.setVisibility(View.VISIBLE);
                    searchResultsRecyclerView.setVisibility(View.GONE);
                } else {
                    noResultsTextView.setVisibility(View.GONE);
                    searchResultsRecyclerView.setVisibility(View.VISIBLE);
                    searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                    searchResultsRecyclerView.setAdapter(videoListAdapter);
                }
                break;
            case 2: // 分享码
                shareCodeList = gson.fromJson(responseData, new TypeToken<List<ShareCodeItem>>() {}.getType());
                if (shareCodeList == null) shareCodeList = new ArrayList<>();
                shareCodeAdapter = new ShareCodeAdapter(shareCodeList, new ShareCodeAdapter.OnLikeClickListener() {
                    @Override
                    public void onLikeClick(int shareCodeId) {
                        // Handle like click (e.g., send like request to API)
                        sendLikeRequest(shareCodeId);
                    }
                }, new ShareCodeAdapter.OnClaimClickListener() {
                    @Override
                    public void onClaimClick(ShareCodeItem item) {
                        sendClaimRequest(item.getId());
                    }
                });
                if (shareCodeList.isEmpty()) {
                    noResultsTextView.setVisibility(View.VISIBLE);
                    searchResultsRecyclerView.setVisibility(View.GONE);
                } else {
                    noResultsTextView.setVisibility(View.GONE);
                    searchResultsRecyclerView.setVisibility(View.VISIBLE);
                    searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                    searchResultsRecyclerView.setAdapter(shareCodeAdapter);
                }
                break;
        }
    }

    /**
     * 发送点赞请求
     * @param shareCodeId 分享码 ID
     */
    private void sendLikeRequest(int shareCodeId) {
        String token = SecureStorageManager.getAccessToken();

        String url = "https://eggyhub.top/api/gifts/like?id=" + shareCodeId;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

                runOnUiThread(() -> Toast.makeText(SearchActivity.this, "点赞请求失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        // 假设成功点赞响应不返回特定消息
                        runOnUiThread(() -> {
                            Toast.makeText(SearchActivity.this, "点赞成功", Toast.LENGTH_SHORT).show();
                            // 不刷新整个列表，而是更新本地数据并刷新对应项
                            if (shareCodeList != null) {
                                for (int i = 0; i < shareCodeList.size(); i++) {
                                    ShareCodeItem item = shareCodeList.get(i);
                                    if (item.getId() == shareCodeId) {
                                        item.setLikes(item.getLikes() + 1);
                                        if (shareCodeAdapter != null) {
                                            shareCodeAdapter.notifyItemChanged(i);
                                        }
                                        break;
                                    }
                                }
                            }
                        });
                    } else if (response.code() == 403) {
                        // 处理403错误（已点赞）
                        runOnUiThread(() -> Toast.makeText(SearchActivity.this, "您已经点过赞了", Toast.LENGTH_SHORT).show());
                    } else {
                        final String responseData = responseBody != null ? responseBody.string() : "";
                        LikeResponse likeResponse = gson.fromJson(responseData, LikeResponse.class);
                        runOnUiThread(() -> {
                            if (likeResponse != null && "have liked".equals(likeResponse.getMessage())) {
                                Toast.makeText(SearchActivity.this, "您已经点过赞了", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(SearchActivity.this, "点赞请求失败: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        });
    }

    /**
     * 发送领取请求
     * @param giftId 礼品 ID
     */
    private void sendClaimRequest(int giftId) {
        String token = SecureStorageManager.getAccessToken();

        String url = "https://eggyhub.top/api/claim";
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), "{\"giftId\":" + giftId + "}");

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    // 不显示Toast消息
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            Toast.makeText(SearchActivity.this, "领取成功", Toast.LENGTH_SHORT).show();
                            performSearch(currentSearchType, currentKeyword, false);
                        });
                    } else {
                        final String responseData = responseBody != null ? responseBody.string() : "";
                        ClaimResponse claimResponse = gson.fromJson(responseData, ClaimResponse.class);
                        runOnUiThread(() -> {
                            // 不显示Toast消息
                        });
                    }
                }
            }
        });
    }

    /**
     * 点赞响应类
     */
    private static class LikeResponse {
        @SerializedName("message")
        private String message;

        public String getMessage() {
            return message;
        }
    }

    /**
     * 领取响应类
     */
    private static class ClaimResponse {
        @SerializedName("code")
        private String code;

        public String getCode() {
            return code;
        }
    }
}
