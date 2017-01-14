package com.yufimtsev.tenhou.clouds.blitz.model;


import com.yufimtsev.tenhou.clouds.blitz.network.request.BasePostBody;

public class Wish extends BasePostBody {

    public Long id;
    public long who;
    public long withWhom;
    public Integer done;

    public Wish(long who, long withWhom) {
        this.who = who;
        this.withWhom = withWhom;
    }
}
