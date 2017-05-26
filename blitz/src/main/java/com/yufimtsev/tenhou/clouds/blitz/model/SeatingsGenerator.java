package com.yufimtsev.tenhou.clouds.blitz.model;

import java.util.ArrayList;
import java.util.Collections;

public class SeatingsGenerator {

    public static ArrayList<ArrayList<Long>> generateSeatings(ArrayList<Long> players, ArrayList<ArrayList<Long>> wishedTables, ArrayList<ArrayList<Long>> oldSeatings) {
        ArrayList<ArrayList<Long>> result = new ArrayList<>();

        int originalPlayersCount = players.size();
        int botCount = (Constants.PLAYER_PER_TABLE - (originalPlayersCount % Constants.PLAYER_PER_TABLE)) % Constants.PLAYER_PER_TABLE;
        int totalTables = (originalPlayersCount + botCount) / Constants.PLAYER_PER_TABLE;

        // it is possible for 12 players make 4 tables with 3 players, which will ruin everything
        // last table should be discarded
        while (wishedTables.size() > totalTables) {
            wishedTables.remove(totalTables);
        }

        for (ArrayList<Long> wishedPlayers : wishedTables) {
            for (int i = 0; i < wishedPlayers.size(); i++) {
                Long wishedPlayer = wishedPlayers.get(i);
                if (!players.contains(wishedPlayer)) {
                    wishedPlayers.remove(i);
                    i--;
                } else {
                    for (int j = 0; j < players.size(); j++) {
                        if (players.get(j).equals(wishedPlayer)) {
                            players.remove(j);
                            j--;
                        }
                    }
                }
            }
        }

        ArrayList<Long> playersBackup = new ArrayList<>(players);

        for (int shuffler = 0; shuffler < 100; shuffler++) {
            boolean lastShuffle = shuffler == 99;
            result.clear();

            botCount = (Constants.PLAYER_PER_TABLE - (originalPlayersCount % Constants.PLAYER_PER_TABLE)) % Constants.PLAYER_PER_TABLE;
            for (int i = 0; i < totalTables; i++) {
                ArrayList<Long> table = new ArrayList<>();
                if (i < wishedTables.size()) {
                    table.addAll(wishedTables.get(i));
                }
                if (table.size() < Constants.PLAYER_PER_TABLE && botCount-- > 0) {
                    table.add(0L);
                }
                result.add(table);
            }

            while (botCount > 0) {
                for (int i = 0; i < totalTables; i++) {
                    ArrayList<Long> table = result.get(i);
                    if (table.size() < 4 && botCount-- > 0) {
                        table.add(0L);
                    }
                }
            }

            players = new ArrayList<>(playersBackup);
            Collections.shuffle(players);

            int currentTable = 0;
            int fullCounter = 0;
            for (; players.size() > 0;) {
                if (currentTable == result.size()) { // maybe we can insert last players to first tables
                    if (fullCounter++ < 5) {
                        currentTable = 0;
                    } else {
                        break;
                    }
                }
                ArrayList<Long> table = result.get(currentTable);
                if (table.size() == Constants.PLAYER_PER_TABLE) {
                    currentTable++;
                    continue;
                }

                int next = 0;
                while (table.size() < Constants.PLAYER_PER_TABLE && next < players.size()) {
                    long nextPlayer = players.get(next);
                    if (notPlayedWithAnyone(oldSeatings, table, nextPlayer) || lastShuffle) {
                        table.add(nextPlayer);
                        players.remove(next);
                    } else {
                        next++;
                    }
                }
                if (table.size() == Constants.PLAYER_PER_TABLE) {
                    currentTable++;
                } else {
                    break;
                }
            }
            if (players.size() == 0) {
                break;
            }
        }

        for (ArrayList<Long> seating : result) {
            Collections.shuffle(seating);
        }

        return result;
    }

    private static boolean notPlayedWithAnyone(ArrayList<ArrayList<Long>> oldSeatings, ArrayList<Long> table, long nextPlayer) {
        boolean allOk = true;
        for (ArrayList<Long> oldTable : oldSeatings) {
            if (oldTable.contains(nextPlayer)) {
                for (Long oldPlayer : oldTable) {
                    if (table.contains(oldPlayer)) {
                        allOk = false;
                    }
                }
            }
        }
        return allOk;
    }
}
