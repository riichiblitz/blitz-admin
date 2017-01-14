package com.yufimtsev.tenhou.clouds.lobbybot.service;

import com.yufimtsev.tenhou.clouds.client.Client;
import com.yufimtsev.tenhou.clouds.client.UserState;
import com.yufimtsev.tenhou.clouds.client.callback.IOnChatMessageReceived;
import com.yufimtsev.tenhou.clouds.client.callback.IOnStateChangedCallback;
import com.yufimtsev.tenhou.clouds.logger.Log;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class LobbyService implements IOnStateChangedCallback, IOnChatMessageReceived {

    private static LobbyService instance;

    private IOnPlayersCheckedCallback playersCheckedCallback;
    private IOnGameEndedCallback gameEndedCallback;
    private IOnWishAdded wishAddedCallback;

    private Timer swapTimer;

    boolean isActive = false;
    boolean isTsumi = false;

    private Client client;

    private String lobby;

    private TimerTask newSwapTask() {
        return new TimerTask() {
            @Override
            public void run() {
                Log.d("LobbyService", "Swapping bot");
                final Client nextClient = new Client();
                nextClient.setCallback(new IOnStateChangedCallback() {
                    private boolean inLobby = false;

                    @Override
                    public void onStateChanged(UserState state) {
                        switch (state) {
                            case CONNECTED:
                                Log.d("LobbyService", "Loggin with TSUMI? " + isTsumi);
                                nextClient.authenticate(isTsumi ? "ID56D2786B-2RhbfBFW" : "ID03261FDC-WXGADJBB", null);
                                isTsumi = !isTsumi;
                                break;
                            case IDLE:
                                if (inLobby) {
                                    nextClient.setCallback(LobbyService.this);
                                    nextClient.setChatCallback(LobbyService.this);
                                    if (client != null) {
                                        client.setCallback(null);
                                        client.setChatCallback(null);
                                        client.disconnect(null);
                                    }
                                    client = nextClient;
                                } else {
                                    nextClient.changeChampLobby(lobby, null);
                                }
                                break;
                            case CHANGING_LOBBY:
                                inLobby = true;
                                break;
                        }
                    }
                });
                nextClient.connect();
            }
        };
    }


    public LobbyService() {
    }

    public static LobbyService getInstance() {
        if (instance == null) {
            instance = new LobbyService();
        }
        return instance;
    }

    public void setLobby(String lobby) {
        this.lobby = lobby;
    }

    public void startClient(IOnPlayersCheckedCallback callback) {
        playersCheckedCallback = callback;
        isActive = true;
        runSwapper();
    }

    public void stopClient() {
        isActive = false;
        stopSwapper();
        if (client != null) {
            client.disconnect(null);
        }
    }

    public boolean sendMessages(ArrayList<String> messages) {
        if (client != null) {
            StringBuilder builder = new StringBuilder();
            for (String message : messages) {
                builder.append(message).append("   ");
            }
            client.sendMessage(builder.toString());
            return true;
        }
        return false;
    }

    public void setOnGameEndedCallback(IOnGameEndedCallback callback) {
        gameEndedCallback = callback;
    }

    public void setOnWishAddedCallback(IOnWishAdded callback) {
        wishAddedCallback = callback;
    }



    @Override
    public void onChatMessageReceived(String message) {
        if (!isActive) {
            return;
        }
        Log.d("MTEST", "Chat: " + message);
        ArrayList<String> idlePlayers = new ArrayList<>();
        ArrayList<String> playingPlayers = new ArrayList<>();
        for (String line : message.split("[\n\r]")) {
            if (line.startsWith("[IDLE]")) {
                String[] playersInLine = line.split(" ");
                idlePlayers.addAll(Arrays.asList(playersInLine).subList(1, playersInLine.length));
            } else if (line.startsWith("[PLAY]")) {
                String[] playersInLine = line.split(" ");
                playingPlayers.addAll(Arrays.asList(playersInLine).subList(1, playersInLine.length));
            } else if (line.contains(": # ")) {
                String[] playersInLine = line.split(" ");
                String wishingPlayer = playersInLine[0].substring(0, playersInLine[0].length()-1);
                List<String> wishedPlayers = Arrays.asList(playersInLine).subList(2, playersInLine.length);
                if (wishAddedCallback != null) {
                    ArrayList<String> fullList = new ArrayList<>();
                    fullList.add(wishingPlayer);
                    fullList.addAll(wishedPlayers);
                    wishAddedCallback.onWishAdded(fullList);
                }
            } else if (line.startsWith("#END")) {
                ArrayList<ResultBody> results = ScoreParser.parseResult(line);
                if (gameEndedCallback != null) {
                    gameEndedCallback.onGameEnded(results);
                }
            }
        }

        if (idlePlayers.size() > 0 || playingPlayers.size() > 0) {
            playersCheckedCallback.onPlayersChecked(idlePlayers, playingPlayers);
        }



    }

    public void runSwapper() {
        stopSwapper();
        if (isActive) {
            swapTimer = new Timer();
            long delay = 1000;
            long period = TimeUnit.MINUTES.toMillis(5);
            swapTimer.scheduleAtFixedRate(newSwapTask(), delay, period);
        }
    }

    public void stopSwapper() {
        if (swapTimer != null) {
            swapTimer.cancel();
            swapTimer = null;
        }
    }

    public void updatePlayers() {
        if (client != null) {
            client.who();
        }
    }


    @Override
    public void onStateChanged(UserState state) {
        if (state == UserState.DISCONNECTING || state == UserState.DISCONNECTED) {
            Log.d("MainProvider", "Bot disconnected");
            runSwapper();
        }
    }
}
