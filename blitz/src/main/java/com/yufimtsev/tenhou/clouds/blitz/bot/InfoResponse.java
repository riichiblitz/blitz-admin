package com.yufimtsev.tenhou.clouds.blitz.bot;

import com.yufimtsev.tenhou.clouds.blitz.network.response.BaseResponse;

public class InfoResponse extends BaseResponse {

    public String message;
    public String log;

    @Override
    public String toString() {
        return "InfoResponse{" +
                "status='" + status + '\'' +
                "message='" + message + '\'' +
                ", log='" + log + '\'' +
                '}';
    }
}
