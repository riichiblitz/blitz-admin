package com.yufimtsev.tenhou.clouds.blitz.lobby;

import java.util.ArrayList;

public interface IStarterCallback {

    void onGameStarted(ArrayList<String> players);

    void onMembersNotFound(ArrayList<String> players);

}
