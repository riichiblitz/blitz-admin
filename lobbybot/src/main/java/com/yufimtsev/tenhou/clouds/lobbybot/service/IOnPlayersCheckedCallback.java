package com.yufimtsev.tenhou.clouds.lobbybot.service;

import java.util.ArrayList;

public interface IOnPlayersCheckedCallback {

    void onPlayersChecked(ArrayList<String> idlePlayers, ArrayList<String> playingPlayers);
}
