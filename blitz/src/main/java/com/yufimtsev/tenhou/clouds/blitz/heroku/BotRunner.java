package com.yufimtsev.tenhou.clouds.blitz.heroku;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.yufimtsev.tenhou.clouds.blitz.MainProvider;
import com.yufimtsev.tenhou.clouds.blitz.network.BlitzApi;
import com.yufimtsev.tenhou.clouds.logger.Log;
import okhttp3.*;

public class BotRunner {

    private static final String[] pingBots = new String[]{
            "https://enigmatic-gorge-52008.herokuapp.com/info?id=0",
            "https://evening-brushlands-66717.herokuapp.com/info?id=0",
            "https://immense-wave-10390.herokuapp.com/info?id=0",
            "https://pacific-inlet-75025.herokuapp.com/info?id=0",
            "http://frozen-oasis-89748.herokuapp.com/info?id=0",
            "http://infinite-headland-96718.herokuapp.com/info?id=0",
            "http://pacific-springs-87245.herokuapp.com/info?id=0",
            "http://peaceful-beach-57299.herokuapp.com/info?id=0",
            "http://pure-refuge-17376.herokuapp.com/info?id=0",
            "http://mahjongbot.herokuapp.com/info?id=0",
    };

    private static String[] bots = generateUrls();

    private static OkHttpClient redirectClient = new OkHttpClient.Builder().followRedirects(true).build();

    private static String[] generateUrls() {
        return new String[]{
                "https://enigmatic-gorge-52008.herokuapp.com/startBot?lobby=" + MainProvider.LOBBY.substring(0, 9),
                "https://evening-brushlands-66717.herokuapp.com/startBot?lobby=" + MainProvider.LOBBY.substring(0, 9),
                "https://immense-wave-10390.herokuapp.com/startBot?lobby=" + MainProvider.LOBBY.substring(0, 9),
                "https://pacific-inlet-75025.herokuapp.com/startBot?lobby=" + MainProvider.LOBBY.substring(0, 9),
                "http://frozen-oasis-89748.herokuapp.com/startBot?lobby=" + MainProvider.LOBBY.substring(0, 9),
                "http://infinite-headland-96718.herokuapp.com/startBot?lobby=" + MainProvider.LOBBY.substring(0, 9),
                "http://pacific-springs-87245.herokuapp.com/startBot?lobby=" + MainProvider.LOBBY.substring(0, 9),
                "http://peaceful-beach-57299.herokuapp.com/startBot?lobby=" + MainProvider.LOBBY.substring(0, 9),
                "http://pure-refuge-17376.herokuapp.com/startBot?lobby=" + MainProvider.LOBBY.substring(0, 9),
                "http://mahjongbot.herokuapp.com/startBot?lobby=" + MainProvider.LOBBY.substring(0, 9),
        };
    }

    private static long lastTime = 0;

    public static void pingBots() {
        Log.d("BotRunner", "pingBots()");
        new Thread() {
            @Override
            public void run() {
                for (String bot : pingBots) {
                    redirectClient.newCall(new Request.Builder().url(bot).build()).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {

                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            response.close();
                        }
                    });
                }
            }
        }.start();
    }

    public static void runBots() {
        Log.d("BotRunner", "runBots()");
        long now = System.currentTimeMillis();
        if (now - lastTime < TimeUnit.MINUTES.toMillis(1)) {
            return;
        }
        lastTime = now;
        new Thread() {
            @Override
            public void run() {
                for (String bot : bots) {
                    redirectClient.newCall(new Request.Builder().url(bot).build()).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {

                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            response.close();
                        }
                    });
                }
            }
        }.start();
    }
}
