package com.yufimtsev.tenhou.clouds.blitz.model;

import com.google.gson.annotations.SerializedName;

public class GameEntity {

    public Long id;
    public int round;
    public Integer board;
    @SerializedName("player_id") public long playerId;
    @SerializedName("start_points") public Integer startPoints;
    @SerializedName("end_points") public Integer endPoints;
    public Integer place;

    public GameEntity(int round, Integer board, long playerId, Integer startPoints) {
        this.round = round;
        this.board = board;
        this.playerId = playerId;
        this.startPoints = startPoints;
    }


    public GameEntity(int round, long playerId, int endPoints, int place) {
        this.round = round;
        this.playerId = playerId;
        this.endPoints = endPoints;
        this.place = place;
    }
}
