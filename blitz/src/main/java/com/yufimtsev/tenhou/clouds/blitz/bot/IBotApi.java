package com.yufimtsev.tenhou.clouds.blitz.bot;

import com.yufimtsev.tenhou.clouds.blitz.network.response.BaseResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

public interface IBotApi {

    @GET("/hello")
    Observable<BaseResponse> hello();

    @GET("/startBot")
    Observable<StartResponse> startBot(@Query("name") String name, @Query("type") String type, @Query("lobby") String lobby);

    @GET("/stopBot")
    Observable<StopResponse> stopBot(@Query("id") String id);

    @GET("/info")
    Observable<InfoResponse> getInfo(@Query("id") String id);

    @GET("/status")
    Call<StatusResponse> getStatus();

}
