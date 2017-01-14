package com.yufimtsev.tenhou.clouds.blitz.network.response;

import java.util.ArrayList;

public class PlayerResult extends ArrayList<String> {

    public String getName() {
        return get(0);
    }

    public String getState() {
        return get(1);
    }

    public Integer getPlace() {
        String score = get(2);
        return score == null ? null : Integer.parseInt(score);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
