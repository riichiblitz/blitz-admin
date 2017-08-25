package com.yufimtsev.tenhou.clouds.blitz.network.request;

public class BasePostBody<T> {

    public static String staticPayload = "matte_is_the_greatest";
    public String payload = staticPayload;
    public T data;

    public BasePostBody() {
    }

    public BasePostBody(T data) {
        this.data = data;
    }
}
