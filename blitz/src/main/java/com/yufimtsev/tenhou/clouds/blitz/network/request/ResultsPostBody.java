package com.yufimtsev.tenhou.clouds.blitz.network.request;

import com.yufimtsev.tenhou.clouds.blitz.model.GameEntity;

import java.util.ArrayList;

public class ResultsPostBody extends BasePostBody<ArrayList<GameEntity>> {

    public ResultsPostBody(ArrayList<GameEntity> data) {
        super(data);
    }
}
