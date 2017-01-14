package com.yufimtsev.tenhou.clouds.blitz.lobby;

import com.yufimtsev.tenhou.clouds.blitz.model.GameEntity;

import java.util.ArrayList;

public interface IGamesNotStartedCallback {

    void gamesStarted(ArrayList<GameEntity> games, ArrayList<String> notFound, ArrayList<ArrayList<Long>> pendingBoards);
}
