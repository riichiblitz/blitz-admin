package com.yufimtsev.tenhou.clouds.blitz.network.request;

import com.yufimtsev.tenhou.clouds.blitz.model.GameEntity;

import java.util.ArrayList;

public class StartPostBody extends BasePostBody<ArrayList<GameEntity>> {

    public StartPostBody(ArrayList<GameEntity> data) {
        super(data);
    }
}
