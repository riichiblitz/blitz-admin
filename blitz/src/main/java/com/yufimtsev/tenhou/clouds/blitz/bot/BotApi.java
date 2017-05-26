package com.yufimtsev.tenhou.clouds.blitz.bot;

import com.yufimtsev.tenhou.clouds.blitz.MainProvider;
import com.yufimtsev.tenhou.clouds.blitz.network.UiTransform;
import com.yufimtsev.tenhou.clouds.logger.Log;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;

public class BotApi {

    private static OkHttpClient sClient;

    private final String baseUrl;
    private final IBotApi api;
    private String lastId = null;
    private String lastReplay = null;

    public BotApi(String baseUrl) {
        this.baseUrl = baseUrl;
        api = newInstance(baseUrl);
    }

    private static IBotApi newInstance(String baseUrl) {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
        if (sClient == null) {
            sClient = new OkHttpClient.Builder().addInterceptor(interceptor).followRedirects(false).build();
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(sClient)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(IBotApi.class);
    }

    public void ping() {
        if (lastId == null) {
            api.hello().compose(UiTransform.getInstance()).subscribe();
        } else {
            api.getInfo(lastId).compose(UiTransform.getInstance())
                    .subscribe(info -> {
                        if (info == null || "error".equals(info.status) || "DISCONNECTED".equals(info.status)) {
                            Log.d(getBaseUrl(), "Error receiving bot info for id " + lastId);
                            Log.d(getBaseUrl(), "detail: " + (info == null ? "null" : info.toString()));
                            lastId = null;
                            return;
                        }
                        Log.d(getBaseUrl(), "Bot info received: " + info.toString() + ", lastReplay was: " + (lastReplay == null ? "null" : lastReplay));
                        lastReplay = info.log;
                    });
        }
    }

    public String getLastReplay() {
        return lastReplay;
    }

    public void checkOrStart() {
        Log.d(getBaseUrl(), "checkOrStart()");
        if (lastId == null) {
            startBot();
        } else {
            api.getInfo(lastId).compose(UiTransform.getInstance())
                    .subscribe(info -> {
                        if (info == null || "error".equals(info.status) || "DISCONNECTED".equals(info.status)) {
                            Log.d(getBaseUrl(), "error receiving bot info for " + lastId);
                            Log.d(getBaseUrl(), "detail: " + (info == null ? "null" : info.toString()));
                            lastId = null;
                            startBot();
                        }
                    });
        }
    }

    private void startBot() {
        Log.d(getBaseUrl(), "startBot()");
        api.startBot(null, null, MainProvider.LOBBY.substring(0, 9)).compose(UiTransform.getInstance())
                .subscribe(startResponse -> {
                    if (startResponse == null) {
                        return;
                    }
                    Log.d(getBaseUrl(), "bot started with id  " + startResponse.id);
                    lastId = startResponse.id;
                    lastReplay = null;
                });
    }

    private void stopBot(String id) {
        api.stopBot(id).compose(UiTransform.getInstance())
                .subscribe(startResponse -> {
                    if (startResponse == null) {
                        return;
                    }
                    lastId = null;
                    lastReplay = null;
                });
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getCheckUrl() {
        if (lastId == null) {
            return null;
        }
        return baseUrl + "info?id=" + lastId;
    }

    public StatusResponse getStatus() {
        try {
            return api.getStatus().execute().body();
        } catch (Exception e) {
            return null;
        }
    }
}
