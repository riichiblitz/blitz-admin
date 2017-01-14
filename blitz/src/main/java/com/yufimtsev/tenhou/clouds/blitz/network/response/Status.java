package com.yufimtsev.tenhou.clouds.blitz.network.response;

import com.yufimtsev.tenhou.clouds.blitz.network.request.BasePostBody;

public class Status extends BasePostBody {

    public String status;
    public int round;
    public long time;
    public String lobby;
    public int delay;

    @Override
    public String toString() {
        return "Status{" +
                "status='" + status + '\'' +
                ", round=" + round +
                ", time=" + time +
                ", lobby='" + lobby + '\'' +
                ", delay=" + delay +
                '}';
    }
}
