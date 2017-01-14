package com.yufimtsev.tenhou.clouds.lobbybot.service;

import java.util.ArrayList;

public interface IOnGameEndedCallback {

    void onGameEnded(ArrayList<ResultBody> results);
}
