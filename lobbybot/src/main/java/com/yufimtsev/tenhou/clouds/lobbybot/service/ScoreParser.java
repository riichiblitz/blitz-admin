package com.yufimtsev.tenhou.clouds.lobbybot.service;

import com.yufimtsev.tenhou.clouds.lobbybot.service.ResultBody;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScoreParser {

    public static ArrayList<ResultBody> parseResult(String endLine) {
        String[] players = endLine.split(" ");
        ArrayList<ResultBody> result = new ArrayList<>(players.length - 1);
        Pattern pattern = Pattern.compile("([^()]*)[(]([^()]*)[)]");
        for (int i = 1; i < players.length; i++) {
            Matcher matcher = pattern.matcher(players[i]);
            matcher.find();
            float score = Float.parseFloat(matcher.group(2).split(",")[0]);
            result.add(new ResultBody(matcher.group(1), Math.round(score * 1000 + 25000)));
        }
        Collections.sort(result);
        Collections.reverse(result);
        for (int i = 0; i < result.size(); i++) {
            result.get(i).place = i + 1;
        }
        /*ResultBody thirdResult = result.get(Constants.PLAYER_PER_TABLE - 2);
        for (ResultBody resultBody : result) {
            resultBody.points = resultBody.score - thirdResult.score;
        }*/
        return result;
    }
}
