package com.yufimtsev.tenhou.clouds.blitz.network;

import java.util.ArrayList;

import com.yufimtsev.tenhou.clouds.blitz.model.InitialState;
import com.yufimtsev.tenhou.clouds.blitz.model.Player;
import com.yufimtsev.tenhou.clouds.blitz.model.Replay;
import com.yufimtsev.tenhou.clouds.blitz.model.Wish;
import com.yufimtsev.tenhou.clouds.blitz.network.request.*;
import com.yufimtsev.tenhou.clouds.blitz.network.response.BaseResponse;
import com.yufimtsev.tenhou.clouds.blitz.network.response.Status;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import rx.Observable;

public interface IBlitzApi {

    @GET("status")
    Observable<BaseResponse<Status>> getStatus();

    @POST("status")
    Observable<BaseResponse<Void>> setStatus(@Body Status body);

    @POST("players")
    Observable<BaseResponse<ArrayList<Player>>> getPlayers(@Body BasePostBody body);

    @GET("unconfirmed")
    Observable<BaseResponse<ArrayList<String>>> getUnconfirmedPlayers();

    @GET("confirmed")
    Observable<BaseResponse<ArrayList<String>>> getConfirmedPlayers();

    @POST("autodisqual")
    Observable<BaseResponse<Void>> autoDisqual(@Body BasePostBody body);

    @POST("autodisqualpending")
    Observable<BaseResponse<Void>> autoDisqualPending(@Body RoundPostBody body);

    @POST("autounconfirm")
    Observable<BaseResponse<Void>> autoUnconfirm(@Body BasePostBody body);

    @POST("initial_state")
    Observable<BaseResponse<InitialState>> getInitialState(@Body BasePostBody body);

    @POST("wish")
    Observable<BaseResponse<Void>> addWish(@Body Wish body);

    @POST("remove_wishes")
    Observable<BaseResponse<Void>> removeWishes(@Body BasePostBody body);

    @POST("start")
    Observable<BaseResponse<Void>> start(@Body StartPostBody body);

    @POST("start_last")
    Observable<BaseResponse<Void>> startLast(@Body StartPostBody body);

    @POST("confirm")
    Observable<BaseResponse<Void>> confirm(@Body ConfirmationsPostBody body);

    @POST("unconfirm")
    Observable<BaseResponse<Void>> unconfirm(@Body ConfirmationsPostBody body);

    @POST("result")
    Observable<BaseResponse<Void>> gameEnded(@Body ResultsPostBody body);


    @POST("new_replays")
    Observable<BaseResponse<ArrayList<Replay>>> getNewReplays(@Body BasePostBody body);

    @POST("replay")
    Observable<BaseResponse<Void>> sendReplay(@Body Replay body);
}
