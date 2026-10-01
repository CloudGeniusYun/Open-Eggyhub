package com.eggyhub.android.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.eggyhub.android.utils.OkHttpClientFactory;

public class BiliRetrofitClient {
    private static final String BASE_URL = "https://api.bilibili.com/";
    private static Retrofit retrofit = null;

    public static BiliApiService getService() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            
            // 使用不带代理拦截器的 OkHttpClient（Bilibili API 不需要代理）
            OkHttpClient client = OkHttpClientFactory.createClientWithoutProxy()
                    .newBuilder()
                    .addInterceptor(logging)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(BiliApiService.class);
    }
}
