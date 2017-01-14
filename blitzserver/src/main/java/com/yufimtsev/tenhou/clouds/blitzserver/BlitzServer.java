package com.yufimtsev.tenhou.clouds.blitzserver;

import com.yufimtsev.tenhou.clouds.blitz.MainProvider;
import static spark.Spark.*;

public class BlitzServer {


    private static MainProvider mainProvider;

    public static void main(String[] args) {
        //port();
        staticFileLocation("/public");

        get("/start", (req, res) -> {
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
}
