package com.yufimtsev.tenhou.clouds.blitz.network.request;

public class BasePostBody<T> {

    public String payload = "matte_is_the_greatest";
    public T data;

    public BasePostBody() {
    }

    public BasePostBody(T data) {
        this.data = data;
    }
}
