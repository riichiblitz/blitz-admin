package com.yufimtsev.tenhou.clouds.blitz.network;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Func1;

public class RetryPolicy implements Func1<Observable<? extends Throwable>, Observable<?>> {

    private static final int[] DELAYS_IN_SECOND = new int[] { 2, 3, 4 };
    private int mRetries = 0;

    public static RetryPolicy newInstance() {
        return new RetryPolicy();
    }

    private RetryPolicy() {
    }

    @Override
    public Observable<?> call(Observable<? extends Throwable> observable) {
        return observable.flatMap(new Func1<Throwable, Observable<?>>() {
            @Override
            public Observable<?> call(Throwable throwable) {
                return mRetries < DELAYS_IN_SECOND.length ?
                        Observable.timer(DELAYS_IN_SECOND[mRetries++], TimeUnit.SECONDS) :
                        Observable.error(throwable);
            }
        });
    }
}
