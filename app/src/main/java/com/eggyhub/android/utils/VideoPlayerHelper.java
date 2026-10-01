package com.eggyhub.android.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.eggyhub.android.BiliPlayerActivity;
import com.eggyhub.android.api.BiliRetrofitClient;
import com.eggyhub.android.log.AppLogger;
import com.eggyhub.android.model.bili.BiliPlayUrlResponse;
import com.eggyhub.android.model.bili.BiliViewResponse;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import com.eggyhub.android.utils.OkHttpClientFactory;

public class VideoPlayerHelper {

    private static final String TAG = "VideoPlayerHelper";
    private final Context context;
    private final OkHttpClient okHttpClient;

    public VideoPlayerHelper(Context context) {
        this.context = context;
        this.okHttpClient = OkHttpClientFactory.getSharedClient();
    }

    public void fetchBiliPlayUrl(String inputId) {
        if (inputId == null || inputId.isEmpty()) {
            showError("无效的视频 ID");
            return;
        }

        if (inputId.contains("b23.tv")) {
            resolveShortLink(inputId);
        } else {
            processBiliId(inputId);
        }
    }

    private android.app.Dialog loadingDialog;
    private android.widget.TextView tvLoadingMessage;

    private void showLoading(String message) {
        if (loadingDialog == null) {
            loadingDialog = new android.app.Dialog(context);
            loadingDialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
            loadingDialog.setContentView(com.eggyhub.android.R.layout.dialog_video_loading);
            loadingDialog.setCancelable(false);
            if (loadingDialog.getWindow() != null) {
                loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
            tvLoadingMessage = loadingDialog.findViewById(com.eggyhub.android.R.id.tv_loading_message);
        }

        if (tvLoadingMessage != null) {
            tvLoadingMessage.setText(message);
        }

        if (!loadingDialog.isShowing()) {
            loadingDialog.show();
        }
    }

    private void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void resolveShortLink(String shortUrl) {
        showLoading("正在还原短链接...");

        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36";
        
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(shortUrl)
                .header("User-Agent", userAgent)
                .build();

        okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) {
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    String resolvedUrl = response.request().url().toString();
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        // 不关闭弹窗，直接进行下一步解析，实现无缝衔接
                        processBiliId(resolvedUrl);
                    });
                }
            }

            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                ((android.app.Activity) context).runOnUiThread(() -> {
                    showError("短链接解析失败: " + e.getMessage());
                });
            }
        });
    }

    private void processBiliId(String url) {
        String bvid = null;
        String aid = null;

        Pattern bvidPattern = Pattern.compile("BV[0-9A-Za-z]+");
        Matcher bvidMatcher = bvidPattern.matcher(url);
        if (bvidMatcher.find()) {
            bvid = bvidMatcher.group(0);
        }

        Pattern aidPattern = Pattern.compile("av(\\d+)");
        Matcher aidMatcher = aidPattern.matcher(url);
        if (aidMatcher.find()) {
            aid = aidMatcher.group(1);
        } else if (url.matches("\\d+")) {
            aid = url;
        }

        if (bvid != null) {
            fetchVideoInfo(bvid, true);
        } else if (aid != null) {
            fetchVideoInfo(aid, false);
        } else {
            showError("无法从链接中提取视频 ID");
        }
    }

    private void fetchVideoInfo(String id, boolean isBvid) {
        showLoading("正在获取视频信息...");

        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36";
        String referer = "https://www.bilibili.com/";

        retrofit2.Call<BiliViewResponse> infoCall = isBvid ? 
            BiliRetrofitClient.getService().getVideoInfo(id, userAgent, referer) :
            BiliRetrofitClient.getService().getVideoInfoByAid(id, userAgent, referer);

        infoCall.enqueue(new retrofit2.Callback<BiliViewResponse>() {
            @Override
            public void onResponse(retrofit2.Call<BiliViewResponse> call, retrofit2.Response<BiliViewResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BiliViewResponse info = response.body();
                    if (info.getCode() == 0) {
                        String actualBvid = info.getData().getBvid();
                        long cid = info.getData().getPages().get(0).getCid();
                        
                        // 请求 mp4 格式，设置 fnval=1
                        BiliRetrofitClient.getService().getPlayUrl(actualBvid, cid, 64, "json", 1, userAgent, referer)
                            .enqueue(new retrofit2.Callback<BiliPlayUrlResponse>() {
                                @Override
                                public void onResponse(retrofit2.Call<BiliPlayUrlResponse> call, retrofit2.Response<BiliPlayUrlResponse> playResponse) {
                                    hideLoading();
                                    if (playResponse.isSuccessful() && playResponse.body() != null && playResponse.body().getCode() == 0) {
                                        BiliPlayUrlResponse.Data data = playResponse.body().getData();
                                        String videoUrl = null;
                                        String audioUrl = null;

                                        // 优先使用 durl (MP4/FLV)
                                        if (data.getDurl() != null && !data.getDurl().isEmpty()) {
                                            videoUrl = data.getDurl().get(0).getUrl();
                                        } else if (data.getDash() != null) {
                                            if (data.getDash().getVideo() != null && !data.getDash().getVideo().isEmpty()) {
                                                videoUrl = data.getDash().getVideo().get(0).getBaseUrl();
                                            }
                                            if (data.getDash().getAudio() != null && !data.getDash().getAudio().isEmpty()) {
                                                audioUrl = data.getDash().getAudio().get(0).getBaseUrl();
                                            }
                                        }

                                        if (videoUrl != null) {
                                            Intent intent = new Intent(context, BiliPlayerActivity.class);
                                            intent.putExtra("video_url", videoUrl);
                                            intent.putExtra("audio_url", audioUrl);
                                            intent.putExtra("bvid", actualBvid);
                                            context.startActivity(intent);
                                        } else {
                                            showError("解析成功但无有效播放地址");
                                        }
                                    } else {
                                        showError("获取播放地址失败: " + (playResponse.body() != null ? playResponse.body().getMessage() : "HTTP " + playResponse.code()));
                                    }
                                }

                                @Override
                                public void onFailure(retrofit2.Call<BiliPlayUrlResponse> call, Throwable t) {
                                    hideLoading();
                                    showError("请求播放地址失败: " + t.getMessage());
                                }
                            });
                    } else {
                        hideLoading();
                        showError("视频信息解析失败: " + info.getMessage());
                    }
                } else {
                    hideLoading();
                    showError("获取视频信息失败: HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<BiliViewResponse> call, Throwable t) {
                hideLoading();
                showError("网络请求失败: " + t.getMessage());
            }
        });
    }

    private void showError(String message) {
        hideLoading();
        ((android.app.Activity) context).runOnUiThread(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }
}
