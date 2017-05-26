package com.yufimtsev.tenhou.clouds.blitz.network;

import com.yufimtsev.tenhou.clouds.blitz.network.response.BaseResponse;
import rx.Observable;
import rx.schedulers.Schedulers;

public class UiTransform<T extends BaseResponse> implements Observable.Transformer<T, T> {

    public static <T extends BaseResponse> UiTransform<T> getInstance() {
        return new UiTransform<>(true);
    }

    public static <T extends BaseResponse> UiTransform<T> getInstance(boolean failIfNotOk) {
        return new UiTransform<>(failIfNotOk);
    }

    private final boolean failIfNotOk;

    public UiTransform(boolean failIfNotOk) {
        this.failIfNotOk = failIfNotOk;
    }

    @Override
    public Observable<T> call(Observable<T> tObservable) {
        return tObservable
                // TOOD: .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.newThread())
                .retryWhen(RetryPolicy.newInstance())
                .doOnNext(response -> {
                    if (failIfNotOk && !"ok".equals(response.status)) {
                        throw new Error();
                    }
                })
                .onErrorReturn(throwable -> null);
    }
}
