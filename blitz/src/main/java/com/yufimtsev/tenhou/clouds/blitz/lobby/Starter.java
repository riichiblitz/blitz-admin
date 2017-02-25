package com.yufimtsev.tenhou.clouds.blitz.lobby;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import com.yufimtsev.tenhou.clouds.blitz.MainProvider;
import com.yufimtsev.tenhou.clouds.blitz.model.Constants;
import com.yufimtsev.tenhou.clouds.blitz.model.GameEntity;
import com.yufimtsev.tenhou.clouds.blitz.model.TournamentState;
import com.yufimtsev.tenhou.clouds.blitz.network.BlitzApi;
import com.yufimtsev.tenhou.clouds.logger.Log;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Starter {

    private static boolean starting = false;
    public static int gamesCounter = 0;
    private static ArrayList<String> notFound = new ArrayList<>();

    public static synchronized int incrementGamesCounter() {
        int result = ++Starter.gamesCounter;
        Log.d("MTEST", "GAMES COUNTER INCREMENTED: " + result);
        return result;
    }

    public static synchronized int decrementGamesCounter() {
        int result = --Starter.gamesCounter;
        Log.d("MTEST", "GAMES COUNTER DECREMENTED: " + result);
        return result;
    }

    public static synchronized void addNotFound(ArrayList<String> newNotFound) {
        notFound.addAll(newNotFound);
    }

    public static void startGames(ArrayList<ArrayList<Long>> seatings, final IGamesNotStartedCallback callback) {
        if (gamesCounter == 0) starting = false;
        if (starting) return;
        starting = true;
        notFound.clear();
        ArrayList<GameEntity> games = new ArrayList<>();
        int board = 0;
        for (ArrayList<Long> seating : seatings) {
            board++;
            incrementGamesCounter();
            final ArrayList<String> namesOnBoard = new ArrayList<>(4);
            final ArrayList<Integer> startPoints = new ArrayList<>(4);
            for (Long playerId : seating) {
                games.add(new GameEntity(TournamentState.getInstance().getStatus().round, board, playerId, null));
                namesOnBoard.add(TournamentState.getInstance().getPlayerNameById(playerId));
                startPoints.add(25000);
            }

            startGame(MainProvider.LOBBY, null, namesOnBoard, null, new IStarterCallback() {
                @Override
                public void onGameStarted(ArrayList<String> players) {
                    ArrayList<Long> playerIds = new ArrayList<>();
                    outer: for (String player : players) {
                        long playerId = TournamentState.getInstance().getPlayerIdByName(player);
                        playerIds.add(playerId);
                        if (playerId != 0) {
                            for (GameEntity entity : games) {
                                if (entity.playerId == playerId) {
                                    entity.startPoints = startPoints.get(namesOnBoard.indexOf(player));
                                    continue outer;
                                }
                            }
                        }
                    }

                    TournamentState.getInstance().gameEnded(playerIds);

                    if (decrementGamesCounter() == 0) {
                        gamesProcessed(games, callback);
                    }
                }

                @Override
                public void onMembersNotFound(ArrayList<String> players) {
                    addNotFound(players);
                    if (decrementGamesCounter() == 0) {
                        gamesProcessed(games, callback);
                    }
                }
            });

        }

    }

    private static void gamesProcessed(ArrayList<GameEntity> entities, IGamesNotStartedCallback callback) {
        ArrayList<ArrayList<Long>> pendingBoards = new ArrayList<>();
        for (int i = 0; i < entities.size(); i += Constants.PLAYER_PER_TABLE) {
            for (int j = 0; j < Constants.PLAYER_PER_TABLE; j++) {
                GameEntity entity = entities.get(i + j);
                if (entity.playerId != 0L && entity.startPoints == null) {
                    ArrayList<Long> board = new ArrayList<>();
                    for (int k = 0; k < Constants.PLAYER_PER_TABLE; k++) {
                        board.add(entities.get(i + k).playerId);
                    }
                    pendingBoards.add(board);
                    break;
                }
            }
        }
        starting = false;
        callback.gamesStarted(entities, notFound, pendingBoards);
    }

    public static void startGame(final String lobby, String password, final ArrayList<String> players, final ArrayList<Integer> points, final IStarterCallback callback) {
        StringBuilder body = new StringBuilder();
        // R2=074F - hanchan with some type of ratings (#END 名まで(+64.8,+2枚) Csan(-4.3,-1枚) Dsan(-24.3,+0枚) Bsan(-36.2,-1枚) )
        // R2=0001 - tonpuusen without ratings (#END 名まで(+15.9) Csan(-4.3) Bsan(-4.3) Dsan(-7.3))
        // R2=0009 - hanchan with aka and without ratings
        // R2=0003 - tonpusen without aka and without ratings
        body.append("L=").append(lobby).append("&R2=0011&RND=default&WG=1&M=");
        try {
            for (int i = 0, size = players.size(); i < size; i++) {
                body.append(URLEncoder.encode(players.get(i), "UTF-8"));
                if (points != null) {
                    body.append('+').append(points.get(i));
                }
                if (i != size - 1) {
                    body.append("%0D%0A");
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        body.append("&PW=");
        if (password != null) {
            body.append(password);
        }
        final Request request = new Request.Builder()
                .url("http://tenhou.net/cs/edit/start.cgi")
                .post(RequestBody.create(MediaType.parse("text/plain"), body.toString()))
                .build();
        Callback futureCallback = new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                BlitzApi.getClient().newCall(request).enqueue(this);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 302) {
                    String message = response.header("Location").split(lobby + "&")[1];
                    if ("OK".equals(message)) {
                        callback.onGameStarted(players);
                    } else if (message.contains("MEMBER%20NOT%20FOUND")) {
                        ArrayList<String> notFound = new ArrayList<>(4);
                        String[] members = URLDecoder.decode(message, "UTF-8").split("\\r\\n");
                        notFound.addAll(Arrays.asList(members).subList(1, members.length));
                        callback.onMembersNotFound(notFound);
                    } else if (message.contains("FAILED")) {
                        callback.onMembersNotFound(players);
                    }
                } else {
                    callback.onMembersNotFound(players);
                }
            }
        };
        BlitzApi.getClient().newCall(request).enqueue(futureCallback);
    }

}
