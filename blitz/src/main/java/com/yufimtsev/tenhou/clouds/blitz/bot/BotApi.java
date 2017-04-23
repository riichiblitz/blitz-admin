package com.yufimtsev.tenhou.clouds.blitz.bot;

import com.yufimtsev.tenhou.clouds.blitz.MainProvider;
import com.yufimtsev.tenhou.clouds.blitz.network.UiTransform;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class BotApi {

    private static OkHttpClient sClient;

    private final IBotApi api;
    private String lastId = null;
    private String lastReplay = null;

    public BotApi(String baseUrl) {
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
                            lastId = null;
                            lastReplay = null;
                            return;
                        }
                        lastReplay = info.log;
                    });
        }
    }

    public String getLastReplay() {
        return lastReplay;
    }

    public void checkOrStart() {
        if (lastId == null) {
            startBot();
        } else {
            api.getInfo(lastId).compose(UiTransform.getInstance())
                    .subscribe(info -> {
                        if (info == null || "error".equals(info.status) || "DISCONNECTED".equals(info.status)) {
                            lastId = null;
                            lastReplay = null;
                            startBot();
                        }
                    });
        }
    }

    private void startBot() {
        api.startBot(null, null, MainProvider.LOBBY.substring(0, 9)).compose(UiTransform.getInstance())
                .subscribe(startResponse -> {
                    if (startResponse == null) {
                        return;
                    }
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
}
