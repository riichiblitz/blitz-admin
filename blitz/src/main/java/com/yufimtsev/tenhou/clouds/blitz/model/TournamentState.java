package com.yufimtsev.tenhou.clouds.blitz.model;

import com.google.gson.Gson;
import com.yufimtsev.tenhou.clouds.blitz.network.response.Status;
import com.yufimtsev.tenhou.clouds.logger.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class TournamentState {


    public static final String STATUS_ANNOUNCE = "announce";
    public static final String STATUS_REGISTRATION = "registration";
    public static final String STATUS_WAIT = "wait";
    //private static final String STATUS_ALMOST = "almost";
    public static final String STATUS_PLAY_PART = "playpart";
    public static final String STATUS_PLAY_ALL = "playall";
    public static final String STATUS_PAUSE = "pause";
    public static final String STATUS_END = "end";

    private static final String NOT_POSSIBLE_MESSAGE = "Not possible while playing, please wait for pause";
    private static final String NO_POSSIBILITIES_MESSAGE = "Not enough possibilities";
    private static final String IMPOSSIBLE_PLAYERS_MESSAGE = "Not possible to make such table";

    private static final TournamentState instance = new TournamentState();
    private ArrayList<GameEntity> games;

    public static TournamentState getInstance() {
        return instance;
    }


    private HashMap<String, Integer> possibilityMap = new HashMap<>();
    private HashMap<Long, ArrayList<Long>> wishes = new HashMap<>();

    private int currentRound;
    private Status currentStatus;
    private int gamesStarted = 0;


    private HashMap<Long, Integer> placeSums;
    private HashMap<Long, String> names;
    private HashMap<String, Long> ids;
    private ArrayList<ArrayList<Long>> oldSeatings;

    private ArrayList<ArrayList<Long>> startedTables = new ArrayList<>();

    public synchronized void setInitialState(InitialState state) {
        setStatus(state.status);
        placeSums = new HashMap<>();
        for (Player player : state.players) {
            placeSums.put(player.id, player.placeSum == null ?
                    25 * Constants.TOTAL_ROUNDS :
                    player.placeSum * 10 + 25 * (Constants.TOTAL_ROUNDS - TournamentState.getInstance().getStatus().round + 1));
        }

        oldSeatings = new ArrayList<>();
        games = state.games;
        for (int i = 0; i < games.size(); i += Constants.PLAYER_PER_TABLE) {
            ArrayList<Long> seating = new ArrayList<>();
            for (int j = 0; j < Constants.PLAYER_PER_TABLE; j++) {
                seating.add(games.get(i + j).playerId);
            }
            oldSeatings.add(seating);
        }

        wishes.clear();
        possibilityMap.clear();
        state.wish.forEach(this::proceedWish);

        if (state.force != null) {
            ArrayList<Wish> voiceWishes = new ArrayList<>();
            for (String namesString : state.force) {
                String[] names = namesString.split(" ");
                long who = getPlayerIdByName(names[0]);
                for (int i = 1, size = names.length; i < size; i++) {
                    long withWhom = getPlayerIdByName(names[i]);
                    Wish wish = new Wish(who, withWhom);
                    wish.done = 0;
                    voiceWishes.add(wish);
                }
            }

            voiceWishes.forEach(this::proceedWish);
        }

    }

    private void proceedWish(Wish wish) {
        if (wish.done != null && wish.done == 0) {
            long who = Math.min(wish.who, wish.withWhom);
            long withWhom = Math.max(wish.who, wish.withWhom);
            ArrayList<Long> wished = null;
            if (wishes.containsKey(who)) {
                wished = wishes.get(who);
            } else {
                wished = new ArrayList<>();
                wishes.put(who, wished);
            }
            wished.add(withWhom);
        }

        String whoName = getPlayerNameById(wish.who);
        Integer possibilityCount;
        if (possibilityMap.containsKey(whoName)) {
            possibilityCount = possibilityMap.remove(whoName);
        } else {
            possibilityCount = 3;
        }
        possibilityCount--;
        possibilityMap.put(whoName, possibilityCount);
    }

    public synchronized void setPlayers(ArrayList<Player> players) {
        if (names != null) names.clear();
        if (ids != null) ids.clear();
        names = new HashMap<>();
        ids = new HashMap<>();
        for (Player player : players) {
            names.put(player.id, player.name);
            ids.put(player.name, player.id);
        }
    }

    public ArrayList<ArrayList<Long>> getOldSeatings() {
        return oldSeatings;
    }

    public ArrayList<GameEntity> getGames() {
        return games;
    }

    public void setStatus(Status status) {
        currentStatus = status;
    }

    public Status getStatus() {
        return currentStatus;
    }

    public long getPlayerIdByName(String name) {
        return ids.containsKey(name) ? ids.get(name) : 0L;
    }

    public String getPlayerNameById(long id) {
        return names.containsKey(id) ? names.get(id) : "NoName";
    }

    public int getPlaceSum(long id) {
        return placeSums.containsKey(id) ? placeSums.get(id) : 25 * Constants.TOTAL_ROUNDS;
    }

    public HashMap<Long, ArrayList<Long>> getWishes() {
        return wishes;
    }

    public synchronized void gameStarted(ArrayList<Long> table) {
        startedTables.add(table);
    }

    public synchronized boolean gameEnded(ArrayList<Long> table) {
        Log.d("TournamentState", "gameEnded(" + table.toString() + ")");
        Log.d("TournamentState", "startedTables:(" + startedTables.toString() + ")");
        for (int i = 0; i < startedTables.size(); i++) {
            ArrayList<Long> startedTable = startedTables.get(i);
            boolean samePlayers = true;
            for (Long player : startedTable) {
                if (!table.contains(player)) {
                    samePlayers = false;
                    break;
                }
            }
            if (samePlayers) {
                startedTables.remove(i);
                break;
            }
        }
        Log.d("TournamentState", "startedTables in result:(" + startedTables.toString() + ")");
        return startedTables.size() == 0;
    }

    public ArrayList<ArrayList<Long>> getWishedTables() {
        Log.d("TournamentState", "getWishedTables()");
        ArrayList<ArrayList<Long>> result = new ArrayList<>();
        for (Long wishing : wishes.keySet()) {
            ArrayList<Long> found = null;
            ArrayList<Long> wished = wishes.get(wishing);
            inner: for (ArrayList<Long> board : result) {
                for (Long player : board) {
                    if (player.equals(wishing) || wished.contains(player)) {
                        if (board.size() + wished.size() - getIntersectionCount(board, wished) <= Constants.PLAYER_PER_TABLE) {
                            found = board;
                            break inner;
                        }
                    }
                }
            }

            if (found == null) {
                found = new ArrayList<>();
                result.add(found);
            }
            if (!found.contains(wishing)) {
                found.add(wishing);
            }
            for (Long player : wished) {
                if (!found.contains(player)) {
                    found.add(player);
                }
            }
        }
        for (int i = 0; i < result.size() - 1; i++) {
            ArrayList<Long> table1 = result.get(i);
            if (table1.size() == 2) {
                for (int j = i + 1; j < result.size(); j++) {
                    ArrayList<Long> table2 = result.get(j);
                    if (table2.size() == 2) {
                        table1.addAll(table2);
                        result.remove(j);
                        break;
                    }
                }
            }
        }
        Log.d("TournamentState", "getWishedTables(): " + result.toString());
        return result;
    }

    public void insertSeating(ArrayList<GameEntity> newGames) {
        if (games != null) {
            games.addAll(newGames);
        }
    }

    public String tryToAddWish(ArrayList<String> wish) {
        ArrayList<Long> preferedTable = new ArrayList<>();
        for (String player : wish) {
            if (ids.containsKey(player)) {
                preferedTable.add(ids.get(player));
            }
        }
        if (!STATUS_WAIT.equals(currentStatus.status) && !STATUS_PAUSE.equals(currentStatus.status)) {
            return NOT_POSSIBLE_MESSAGE;
        }
        if (preferedTable.size() != wish.size()) {
            StringBuilder names = new StringBuilder();
            names.append("Could not find players: ");
            for (int i = 0; i < wish.size(); i++) {
                String player = wish.get(i);
                if (!ids.containsKey(player)) {
                    names.append(player).append(" ");
                }
            }
            return names.toString();
        }

        int possibleCount = possibilityMap.containsKey(wish.get(0)) ?
                possibilityMap.remove(wish.get(0)) :
                3;
        int nextPossibleCount = possibleCount - preferedTable.size() + 1;
        if (nextPossibleCount < 0) {
            return wish.get(0) + ": " + NO_POSSIBILITIES_MESSAGE;
        }
        possibilityMap.put(wish.get(0), nextPossibleCount);

        ArrayList<ArrayList<Long>> wishedTables = getWishedTables();

        //boolean impossible = false;
        String impossible = null;
        outer:
        for (ArrayList<Long> table : wishedTables) {
            if (table.size() > Constants.PLAYER_PER_TABLE) {
                //impossible = true;
                impossible = "Some table exceeds the limit";
                break;
            }
            for (int i = 0; i < preferedTable.size() - 1; i++) {
                if (table.contains(table.get(i))) {
                    if (table.size() + preferedTable.size() - getIntersectionCount(table, preferedTable) > Constants.PLAYER_PER_TABLE) {
                        //impossible = true;
                        impossible = "Table " + new Gson().toJson(table) + " is planned, your wish will make this table exceed the limits";
                        break outer;
                    }
                    for (int j = i + 1; j < preferedTable.size(); j++) {
                        if (table.contains(preferedTable.get(j))) {
                            //impossible = true;
                            impossible = getPlayerNameById(table.get(i)) + " is already confirmed with " + getPlayerNameById(table.get(j));
                            break outer;
                        }
                    }
                }
            }
        }

        if (oldSeatings != null && oldSeatings.size() > 0) {
            outer: for (ArrayList<Long> oldSeating : oldSeatings) {
                for (int i = 0; i < preferedTable.size() - 1; i++) {
                    if (oldSeating.contains(preferedTable.get(i))) {
                        for (int j = i + 1; j < preferedTable.size(); j++) {
                            if (oldSeating.contains(preferedTable.get(j))) {
                                //impossible = true;
                                impossible = getPlayerNameById(preferedTable.get(i)) + " has already played with " + getPlayerNameById(preferedTable.get(j));
                                break outer;
                            }
                        }
                    }
                }
            }
        }

        if (impossible != null) {
            possibilityMap.remove(wish.get(0));
            possibilityMap.put(wish.get(0), possibleCount);
            return impossible;
        }

        ArrayList<Long> wished = null;

        Collections.sort(preferedTable);

        Long key = null;
        for (Long preferredPlayer : preferedTable) {
            if (wishes.containsKey(preferredPlayer)) {
                wished = wishes.get(preferredPlayer);
                key = preferredPlayer;
                break;
            }
        }
        if (wished == null) {
            wished = new ArrayList<>();
            key = preferedTable.get(0);
            wishes.put(key, wished);
        }
        for (int i = 0; i < preferedTable.size(); i++) {
            Long preferredPlayer = preferedTable.get(i);
            if (!preferredPlayer.equals(key) && !wished.contains(preferredPlayer)) {
                wished.add(preferredPlayer);
            }
        }

        return null;
    }

    private int getIntersectionCount(ArrayList<Long> lhr, ArrayList<Long> rhr) {
        if (lhr == null || lhr.size() == 0 || rhr == null || rhr.size() == 0) {
            return 0;
        }
        int result = 0;
        for (Long lval : lhr) {
            for (Long rval : rhr) {
                if (lval.equals(rval)) {
                    result++;
                }
            }
        }
        return result;
    }


}
