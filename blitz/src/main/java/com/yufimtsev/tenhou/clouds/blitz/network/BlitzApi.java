package com.yufimtsev.tenhou.clouds.blitz.network;

import com.yufimtsev.tenhou.clouds.blitz.MainProvider;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class BlitzApi {

    private static IBlitzApi instance;
    private static OkHttpClient sClient;
    private static OkHttpClient sRedirectClient;

    public static IBlitzApi getInstance() {
        if (instance == null) {
            instance = newInstance();
        }
        return instance;
    }

    private static IBlitzApi newInstance() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
        sClient = new OkHttpClient.Builder().addInterceptor(interceptor).followRedirects(false).build();
        sRedirectClient = new OkHttpClient.Builder().addInterceptor(interceptor).followRedirects(true).build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(MainProvider.CALLBACK_URL)
                //.baseUrl("http://192.168.1.217/api/")
                //.baseUrl("http://testblitz-riichi.rhcloud.com/api/")
                //.baseUrl("http://blitz-riichi.rhcloud.com/api/")
                //.baseUrl("http://10.0.2.2/api/")
                .client(sClient)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(IBlitzApi.class);
    }

    public static OkHttpClient getClient() {
        return sClient;
    }

    public static OkHttpClient getRedirectClient() {
        return sRedirectClient;
    }
}
