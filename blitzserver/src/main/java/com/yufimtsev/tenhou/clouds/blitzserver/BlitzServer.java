package com.yufimtsev.tenhou.clouds.blitzserver;

import com.yufimtsev.tenhou.clouds.blitz.MainProvider;

import static spark.Spark.*;

public class BlitzServer {


    private static MainProvider mainProvider;

    public static void main(String[] args) {
        System.out.println("ENTERED INTO MAIN, WHOOHOO!");
        port(getAssignedPort());
        staticFileLocation("/public");

        get("/start", (req, res) -> {
            System.out.println("GET START, WHOOHOO!");
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
            System.out.println("GET STOP, WHOOHOO!");
            res.type("application/json");
            if (mainProvider != null) {
                mainProvider.stop();
                mainProvider = null;
                return "{\"status\":\"stopped\"}";
            } else {
                return "{\"status\":\"stopped\",\"error\":\"already was stopped\"}";
            }
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
