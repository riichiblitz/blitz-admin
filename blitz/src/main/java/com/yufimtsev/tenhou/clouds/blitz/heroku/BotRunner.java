package com.yufimtsev.tenhou.clouds.blitz.heroku;

import com.yufimtsev.tenhou.clouds.blitz.bot.BotApi;
import com.yufimtsev.tenhou.clouds.blitz.bot.StatusResponse;
import com.yufimtsev.tenhou.clouds.blitz.model.Replay;
import com.yufimtsev.tenhou.clouds.blitz.network.BlitzApi;
import com.yufimtsev.tenhou.clouds.blitz.network.UiTransform;
import com.yufimtsev.tenhou.clouds.logger.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BotRunner {

    private static final ArrayList<BotApi> bots = new ArrayList<>();

    static {
        // mateplaysmahjong
        bots.add(new BotApi("http://vast-shelf-53916.herokuapp.com/"));
        bots.add(new BotApi("http://damp-dawn-64264.herokuapp.com/"));
        bots.add(new BotApi("http://blooming-tor-57149.herokuapp.com/"));
        bots.add(new BotApi("http://morning-brook-51068.herokuapp.com/"));
        bots.add(new BotApi("http://mysterious-hamlet-82283.herokuapp.com/"));
    }

    private static long lastTime = 0;

    public static void pingBots() {
        Log.d("BotRunner", "pingBots()");
        bots.forEach(BotApi::ping);
    }

    public static void runBots() {
        Log.d("BotRunner", "runBots()");
        long now = System.currentTimeMillis();
        if (now - lastTime < TimeUnit.MINUTES.toMillis(1)) {
            return;
        }
        lastTime = now;
        bots.forEach(BotApi::checkOrStart);
    }

    public static void sendReplays() {
        bots.forEach(bot -> {
            String logHash = bot.getLastReplay();
            if (logHash != null) {
                Replay replay = new Replay();
                replay.payload = null;
                replay.url = "http://tenhou.net/0/?log=" + logHash + "&tw=0";
                replay.cheat = 0;
                BlitzApi.getInstance().sendReplay(replay).compose(UiTransform.getInstance())
                        .subscribe();
            }
        });
    }


    public static List<String> getCheckUrls() {
        final ArrayList<String> result = new ArrayList<>();
        bots.forEach(bot -> result.add(bot.getCheckUrl()));
        return result;
    }

    public static HashMap<String, StatusResponse> getStatuses() {
        HashMap<String, StatusResponse> result = new HashMap<>();
        bots.forEach(bot -> result.put(bot.getBaseUrl(), bot.getStatus()));
        return result;
    }

}
