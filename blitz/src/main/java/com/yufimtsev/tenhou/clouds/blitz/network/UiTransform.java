package com.yufimtsev.tenhou.clouds.blitz.network;

import com.yufimtsev.tenhou.clouds.blitz.network.response.BaseResponse;
import rx.Observable;
import rx.schedulers.Schedulers;

public class UiTransform<T extends BaseResponse> implements Observable.Transformer<T, T> {

    public static <T extends BaseResponse> UiTransform<T> getInstance() {
        return new UiTransform<>();
    }

    @Override
    public Observable<T> call(Observable<T> tObservable) {
        return tObservable
                // TOOD: .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.newThread())
                .retryWhen(RetryPolicy.newInstance())
                .doOnNext(response -> {
                    if (!"ok".equals(response.status)) {
                        throw new Error();
                    }
                })
                .onErrorReturn(throwable -> null);
    }
}
