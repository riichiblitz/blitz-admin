package com.yufimtsev.tenhou.clouds.lobbybot.service;

public class ResultBody implements Comparable<ResultBody> {

    public String name;
    public int score;
    public int place;

    public ResultBody() {
    }

    public ResultBody(String name) {
        this.name = name;
    }

    public ResultBody(String name, int score) {
        this.name = name;
        this.score = score;
    }

    public ResultBody(String name, int score, int place) {
        this.name = name;
        this.score = score;
        this.place = place;
    }

    public int compareTo(ResultBody resultBody) {
        return this.score - resultBody.score;
    }
}
