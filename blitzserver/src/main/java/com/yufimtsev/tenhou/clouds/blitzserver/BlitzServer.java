package com.yufimtsev.tenhou.clouds.blitzserver;

import com.google.gson.Gson;
import com.yufimtsev.tenhou.clouds.blitz.MainProvider;
import com.yufimtsev.tenhou.clouds.blitz.heroku.BotRunner;
import com.yufimtsev.tenhou.clouds.logger.Log;

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
            Log.resetLogFile();
            res.type("application/json");
            return gson.toJson(BotRunner.getCheckUrls());
        });

        get("/checkBots", (req, res) -> {
            Log.resetLogFile();
            res.type("application/json");
            return gson.toJson(BotRunner.getStatuses());
        });

        get("/status", (req, res) -> {
            Log.resetLogFile();
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
