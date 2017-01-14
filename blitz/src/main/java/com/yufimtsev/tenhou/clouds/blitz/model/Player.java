package com.yufimtsev.tenhou.clouds.blitz.model;

public class Player {

    public long id;
    public String name;
    public Integer placeSum;

    public Player(String name) {
        id = 0;
        this.name = name;
    }

    @Override
    public String toString() {
        return "Player{" +name + '\'' +
                '}';
    }
}
