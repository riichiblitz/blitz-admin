package com.yufimtsev.tenhou.clouds.blitzserver;

import com.google.gson.Gson;
import com.yufimtsev.tenhou.clouds.blitz.MainProvider;
import com.yufimtsev.tenhou.clouds.blitz.heroku.BotRunner;
import com.yufimtsev.tenhou.clouds.blitz.lobby.IStarterCallback;
import com.yufimtsev.tenhou.clouds.blitz.lobby.Starter;
import com.yufimtsev.tenhou.clouds.blitz.network.BlitzApi;
import com.yufimtsev.tenhou.clouds.lobbybot.service.LobbyService;
import com.yufimtsev.tenhou.clouds.logger.Log;

import java.util.ArrayList;

import static spark.Spark.*;

public class BlitzServer {


    private static MainProvider mainProvider;
    private static Gson gson = new Gson();

    public static void main(String[] args) {
        Log.d("BlitzServer", "ENTERED INTO MAIN, WHOOHOO!");
        port(getAssignedPort());
        staticFileLocation("/public");

        get("/start", (req, res) -> {
            Log.resetLogFile();
            Log.d("BlitzServer", "GET START, WHOOHOO!");
            res.type("application/json");
            String secret = req.queryParams("lobbySecret");
            String apiUrl = req.queryParams("apiUrl");
            if (secret == null) {
                return "{\"status\":\"error\",\"error\":\"lobbySecret query param is not provided\"}";
            }

            if (apiUrl == null) {
                return "{\"status\":\"error\",\"error\":\"apiUrl query param is not provided\"}";
            }

            if (mainProvider == null) {
                mainProvider = new MainProvider();
                mainProvider.start(secret, apiUrl);
                return "{\"status\":\"ok\"}";
            } else {
                return "{\"status\":\"none\",\"error\":\"already was started\"}";
            }
        });

        get("/stop", (req, res) -> {
            Log.resetLogFile();
            Log.d("BlitzServer", "GET STOP, WHOOHOO!");
            res.type("application/json");
            if (mainProvider != null) {
                mainProvider.stop();
                mainProvider = null;
                return "{\"status\":\"stopped\"}";
            } else {
                return "{\"status\":\"stopped\",\"error\":\"already was stopped\"}";
            }
        });

        get("/checkUrls", (req, res) -> {
            res.type("application/json");
            return gson.toJson(BotRunner.getCheckUrls());
        });

        get("/checkBots", (req, res) -> {
            res.type("application/json");
            return gson.toJson(BotRunner.getStatuses());
        });

        get("/pingBots", (req, res) -> {
            res.type("application/json");
            BotRunner.pingBots();
            return gson.toJson(BotRunner.getStatuses());
        });

        get("/startBots", (req, res) -> {
            res.type("application/json");
            MainProvider.LOBBY = "C1816703204372567";
            BotRunner.runBots();
            return gson.toJson(BotRunner.getStatuses());
        });

        get("/runFakeGame", (req, res) -> {
            res.type("application/json");
            MainProvider.LOBBY = "C1816703204372567";
            MainProvider.CALLBACK_URL = "http://127.0.0.1:4567/";
            ArrayList<String> names = new ArrayList<>();
            names.add("NoName");
            names.add("NoName");
            names.add("NoName");
            names.add("NoName");
            BlitzApi.getInstance();
            Starter.startGame(MainProvider.LOBBY, null, names, null, new IStarterCallback() {
                @Override
                public void onGameStarted(ArrayList<String> players) { }

                @Override
                public void onMembersNotFound(ArrayList<String> players) { }
            });
            return gson.toJson(BotRunner.getStatuses());
        });

        get("/status", (req, res) -> {
            res.type("application/json");
            return gson.toJson(Log.collect());
        });
    }

    private static int getAssignedPort() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (processBuilder.environment().get("PORT") != null) {
            return Integer.parseInt(processBuilder.environment().get("PORT"));
        }
        return 4567; //return default port if heroku-port isn't set (i.e. on localhost)
    }
}
