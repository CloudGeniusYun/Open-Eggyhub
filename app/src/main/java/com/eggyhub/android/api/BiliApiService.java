package com.eggyhub.android.api;

import com.eggyhub.android.model.bili.BiliPlayUrlResponse;
import com.eggyhub.android.model.bili.BiliViewResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface BiliApiService {
    
    @GET("x/web-interface/view")
    Call<BiliViewResponse> getVideoInfo(
        @Query("bvid") String bvid,
        @Header("User-Agent") String userAgent,
        @Header("Referer") String referer
    );

    @GET("x/web-interface/view")
    Call<BiliViewResponse> getVideoInfoByAid(
        @Query("aid") String aid,
        @Header("User-Agent") String userAgent,
        @Header("Referer") String referer
    );

    @GET("x/player/playurl")
    Call<BiliPlayUrlResponse> getPlayUrl(
        @Query("bvid") String bvid,
        @Query("cid") long cid,
        @Query("qn") int qn,
        @Query("otype") String otype,
        @Query("fnval") int fnval,
        @Header("User-Agent") String userAgent,
        @Header("Referer") String referer
    );
}
