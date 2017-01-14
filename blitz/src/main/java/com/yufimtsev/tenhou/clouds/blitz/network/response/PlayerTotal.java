package com.yufimtsev.tenhou.clouds.blitz.network.response;

import java.util.ArrayList;

public class PlayerTotal extends ArrayList<String> {

    public String getName() {
        return get(0);
    }

    public Integer getScore() {
        String score = get(1);
        return score == null ? null : Integer.parseInt(score);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
