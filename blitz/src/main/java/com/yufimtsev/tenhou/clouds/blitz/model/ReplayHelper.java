package com.yufimtsev.tenhou.clouds.blitz.model;

import com.yufimtsev.tenhou.clouds.blitz.network.BlitzApi;
import com.yufimtsev.tenhou.clouds.blitz.network.request.BasePostBody;
import com.yufimtsev.tenhou.clouds.blitz.network.response.BaseResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Request;
import rx.functions.Action1;

public class ReplayHelper {

    private static boolean lock = false;

    public static void proceedReplays() {
        if (lock) return;
        lock = true;
        new Thread() {
            @Override
            public void run() {
                BlitzApi.getInstance().getNewReplays(new BasePostBody()).onErrorReturn(t -> null)
                        .subscribe(new Action1<BaseResponse<ArrayList<Replay>>>() {
                            @Override
                            public void call(BaseResponse<ArrayList<Replay>> response) {
                                if (response == null || !"ok".equals(response.status)) {
                                    BlitzApi.getInstance().getNewReplays(new BasePostBody()).onErrorReturn(t -> null)
                                            .subscribe(this);
                                } else {
                                    ArrayList<Replay> data = response.data;
                                    for (int i = 0; i < data.size(); i++) {
                                        Replay replay = data.get(i);
                                        String hash = getHash(replay.url);
                                        if (hash == null) {
                                            response.data.remove(i--);
                                            continue;
                                        }
                                        ArrayList<String> players = null;
                                        try {
                                            players = getPlayers(hash);
                                        } catch (IOException e) {
                                            continue;
                                        }

                                        ArrayList<Long> playerIds = new ArrayList<>();
                                        for (String player : players) {
                                            playerIds.add(TournamentState.getInstance().getPlayerIdByName(player));
                                        }
                                        ArrayList<GameEntity> games = TournamentState.getInstance().getGames();
                                        Integer state = null;
                                        for (int j = 0; j < games.size(); j += Constants.PLAYER_PER_TABLE) {
                                            boolean found = true;
                                            int noNames = 0;
                                            for (int k = 0; k < Constants.PLAYER_PER_TABLE; k++) {
                                                long playerInGame = games.get(j + k).playerId;
                                                if (playerInGame == 0L) {
                                                    noNames++;
                                                } else if (!playerIds.contains(playerInGame)) {
                                                    found = false;
                                                    break;
                                                }
                                            }
                                            if (noNames < Constants.PLAYER_PER_TABLE && found) {
                                                if (state != null) {
                                                    state = null;
                                                    break;
                                                } else {
                                                    state = j;
                                                }
                                            }
                                        }
                                        if (state != null) {
                                            GameEntity game = games.get(state);
                                            Replay processed = new Replay();
                                            processed.url = hash;
                                            processed.round = game.round;
                                            processed.board = game.board;
                                            BlitzApi.getInstance().sendReplay(processed).onErrorReturn(t -> null)
                                                    .subscribe(new Action1<BaseResponse<Void>>() {
                                                        @Override
                                                        public void call(BaseResponse<Void> baseResponse) {
                                                            if (baseResponse == null) {
                                                                BlitzApi.getInstance().sendReplay(processed).onErrorReturn(t -> null)
                                                                        .subscribe(this);
                                                            }
                                                        }
                                                    });
                                        }
                                    }
                                    lock = false;
                                }
                            }
                        });
            }
        }.start();
    }

    public static String getHash(String url) {
        if (!url.contains("-0009-19070-")) {
            return null;
        }
        Pattern pattern = Pattern.compile("\\?log=([^&\\n ]+)");
        Matcher matcher = pattern.matcher(url);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }

    public static ArrayList<String> getPlayers(String hash) throws IOException {
        ArrayList<String> result = new ArrayList<>();
        String string;
        String downloadLink = "http://e.mjv.jp/0/log/?" + hash;
        //String downloadLink = "http://e.mjv.jp/0/log/plainfiles.cgi?" + hash;
        okhttp3.Response replayResponse = BlitzApi.getRedirectClient().newCall(new Request.Builder().url(downloadLink).build()).execute();
        if (!replayResponse.isSuccessful()) {
            return result;
        }
        string = replayResponse.body().string();
        replayResponse.body().close();
        Document document = Jsoup.parse(string);
        Element un = document.getElementsByTag("un").first();
        result.add(URLDecoder.decode(un.attr("n0"), "UTF-8"));
        result.add(URLDecoder.decode(un.attr("n1"), "UTF-8"));
        result.add(URLDecoder.decode(un.attr("n2"), "UTF-8"));
        result.add(URLDecoder.decode(un.attr("n3"), "UTF-8"));
        return result;
    }
}
