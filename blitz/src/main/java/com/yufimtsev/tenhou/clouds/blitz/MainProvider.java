package com.yufimtsev.tenhou.clouds.blitz;

import com.google.gson.Gson;
import com.yufimtsev.tenhou.clouds.blitz.heroku.BotRunner;
import com.yufimtsev.tenhou.clouds.blitz.lobby.IGamesNotStartedCallback;
import com.yufimtsev.tenhou.clouds.blitz.lobby.IStarterCallback;
import com.yufimtsev.tenhou.clouds.blitz.lobby.Starter;
import com.yufimtsev.tenhou.clouds.blitz.model.*;
import com.yufimtsev.tenhou.clouds.blitz.network.BlitzApi;
import com.yufimtsev.tenhou.clouds.blitz.network.UiTransform;
import com.yufimtsev.tenhou.clouds.blitz.network.request.*;
import com.yufimtsev.tenhou.clouds.blitz.network.response.BaseResponse;
import com.yufimtsev.tenhou.clouds.blitz.network.response.Status;
import com.yufimtsev.tenhou.clouds.lobbybot.service.*;
import com.yufimtsev.tenhou.clouds.logger.Log;
import com.yufimtsev.tenhou.discord.DiscordBot;
import rx.functions.Action1;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.yufimtsev.tenhou.clouds.blitz.model.TournamentState.*;

public class MainProvider implements IOnPlayersCheckedCallback, IGamesNotStartedCallback, IOnGameEndedCallback, IOnWishAdded {

    private static final Gson gson = new Gson();
    public static String LOBBY = "";
    public static String CALLBACK_URL = "";

    private ArrayList<String> messagesBuffer = new ArrayList<>();


    private ArrayList<String> playersToCheck = new ArrayList<>();
    private ArrayList<String> checkedPlayers = new ArrayList<>();
    private boolean isCheckingPlayers = false;
    private boolean isCheckingLastPlayers = false;

    private TimerTask newTimerTask() {
        return new TimerTask() {
            @Override
            public void run() {
                cancelPing();
                BlitzApi.getInstance().getStatus().compose(UiTransform.getInstance())
                        .subscribe(new Action1<BaseResponse<Status>>() {
                            @Override
                            public void call(BaseResponse<Status> response) {
                                if (response == null) {
                                    BlitzApi.getInstance().getStatus().compose(UiTransform.getInstance())
                                            .subscribe(this);
                                } else {
                                    onStatusUpdated(response.data);
                                }
                            }
                        });
            }
        };
    }

    private ArrayList<ArrayList<Long>> pendingBoards;
    private Timer updateTimer;

    public void start(String lobby, String callbackUrl) {
        LobbyService instance = LobbyService.getInstance();
        if (instance != null) {
            instance.setOnGameEndedCallback(this);
        }
        new Thread() {
            @Override
            public void run() {
                DiscordBot.getInstance().start();
            }
        }.start();
        Log.d("MainProvider", "start()");
        LOBBY = lobby;
        CALLBACK_URL = callbackUrl;
        LobbyService lobbyService = LobbyService.getInstance();
        lobbyService.setLobby(LOBBY.substring(0, 9));
        lobbyService.startClient(this);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR, 11);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Log.d("MTEST", "Prefered start time: " + calendar.getTimeInMillis());
        //Fabric.with(this, new Crashlytics());
        //setContentView(R.layout.activity_main);
        //ButterKnife.bind(this);

        BlitzApi.getInstance().getInitialState(new BasePostBody()).compose(UiTransform.getInstance())
                .subscribe(new Action1<BaseResponse<InitialState>>() {
                    @Override
                    public void call(BaseResponse<InitialState> initialState) {
                        if (initialState == null) {
                            BlitzApi.getInstance().getInitialState(new BasePostBody()).compose(UiTransform.getInstance())
                                    .subscribe(this);
                        } else {
                            syncDiscordState(initialState.data);
                            BlitzApi.getInstance().getPlayers(new BasePostBody()).compose(UiTransform.getInstance())
                                    .subscribe(new Action1<BaseResponse<ArrayList<Player>>>() {
                                        @Override
                                        public void call(BaseResponse<ArrayList<Player>> response) {
                                            if (response == null) {
                                                BlitzApi.getInstance().getPlayers(new BasePostBody()).compose(UiTransform.getInstance())
                                                        .subscribe(this);
                                            } else {
                                                TournamentState.getInstance().setPlayers(response.data);
                                                TournamentState.getInstance().setInitialState(initialState.data);
                                                updatePendingBoards(initialState);
                                                sendPing(5000);
                                            }
                                        }
                                    });
                        }
                    }
                });

    }

    public void stop() {
        Log.d("MainProvider", "stop()");
        cancelPing();
        LobbyService.getInstance().stopClient();
        DiscordBot.getInstance().stop();
    }

    private void updatePendingBoards(BaseResponse<InitialState> initialState) {
        if (STATUS_PLAY_PART.equals(initialState.data.status.status)) {
            for (int i = 0; i < initialState.data.games.size(); i += Constants.PLAYER_PER_TABLE) {
                GameEntity entity = initialState.data.games.get(i);
                if (entity.round != initialState.data.status.round) {
                    continue;
                }
                if (entity.startPoints == null) {
                    ArrayList<Long> table = new ArrayList<>();
                    int noNames = 0;
                    for (int j = 0; j < Constants.PLAYER_PER_TABLE; j++) {
                        long playerId = initialState.data.games.get(i + j).playerId;
                        if (playerId == 0) {
                            noNames++;
                        }
                        table.add(playerId);
                    }
                    if (noNames < Constants.PLAYER_PER_TABLE) {
                        if (pendingBoards == null) {
                            pendingBoards = new ArrayList<>();
                        }
                        pendingBoards.add(table);
                    }
                }
            }
        }
    }


    public boolean timeHasPassed(Status status) {
        Date date = new Date();
        Runnable extraAction;
        if (date.getTime() > status.time) {
            Log.d("MainProvider", "Time has passed for status " + status.status);
            switch (status.status) {
                case STATUS_ANNOUNCE:
                    status.status = STATUS_REGISTRATION;
                    status.time = date.getTime() + TimeUnit.MINUTES.toMillis(1);
                    status.delay = 60000;
                    extraAction = () -> sendPing(10000);
                    break;
                case STATUS_REGISTRATION:
                    status.status = STATUS_WAIT;
                    // TODO: set tournament start time
                    status.time = date.getTime() + Constants.WAIT_DELAY;
                    status.round = 0;
                    status.delay = 20000;
                    status.lobby = LOBBY.substring(0, 9);//"C81991854";
                    extraAction = () -> sendPing(10000);
                    break;
                case STATUS_WAIT:
                case STATUS_PAUSE:
                    status.status = STATUS_PLAY_PART;
                    status.time = date.getTime() + Constants.PLAY_PART_DELAY;
                    status.delay = 10000;
                    status.round++;
                    if (status.round > Constants.TOTAL_ROUNDS) {
                        status.status = STATUS_END;
                        status.delay = 600000;
                        extraAction = () -> sendPing(10000);
                    } else {
                        extraAction = this::disqualAndStartGames;
                    }
                    break;
                case STATUS_PLAY_PART:
                    // timeout - disqual every unconfirmed fellow, change him to NoName in seatings
                    // and try to start all the pending tables
                    extraAction = new Runnable() {
                        @Override
                        public void run() {
                            BlitzApi.getInstance().getUnconfirmedPlayers().compose(UiTransform.getInstance())
                                    .subscribe(new Action1<BaseResponse<ArrayList<String>>>() {
                                        @Override
                                        public void call(BaseResponse<ArrayList<String>> response) {
                                            if (response == null) {
                                                BlitzApi.getInstance().getUnconfirmedPlayers().compose(UiTransform.getInstance())
                                                        .subscribe(this);
                                            } else {
                                                disqualAndRestart(response.data);

                                            }
                                        }
                                    });
                        }
                    };
                    break;
                case STATUS_PLAY_ALL:
                    // IMPOSSIBLE!!
                    extraAction = () -> sendPing(10000);
                    status.status = STATUS_PAUSE;
                    status.time = new Date().getTime() + Constants.PAUSE_DELAYS[status.round - 1];
                    break;
                default:
                    extraAction = null;
                    break;
            }
            TournamentState.getInstance().setStatus(status);
            BlitzApi.getInstance().setStatus(status).compose(UiTransform.getInstance())
                    .subscribe(new Action1<BaseResponse<Void>>() {
                        @Override
                        public void call(BaseResponse<Void> response) {
                            Log.d("MTEST", "Status changed to " + status.toString());
                            if (response == null) {
                                BlitzApi.getInstance().setStatus(status).compose(UiTransform.getInstance())
                                        .subscribe(this);
                            } else {
                                if (extraAction != null) {
                                    extraAction.run();
                                }
                            }
                        }
                    });
            return true;
        }
        return false;
    }

    private void disqualAndStartGames() {
        Log.d("MainProvider", "disqualAndStartGames()");
        BotRunner.runBots();
        LobbyService instance = LobbyService.getInstance();
        if (instance != null) {
            instance.setOnGameEndedCallback(this);
        }
        BlitzApi.getInstance().autoDisqual(new BasePostBody()).compose(UiTransform.getInstance())
                .subscribe(new Action1<BaseResponse<Void>>() {
                    @Override
                    public void call(BaseResponse<Void> response) {
                        if (response == null) {
                            BlitzApi.getInstance().autoDisqual(new BasePostBody()).compose(UiTransform.getInstance())
                                    .subscribe(this);
                        } else {
                            BlitzApi.getInstance().getInitialState(new BasePostBody()).compose(UiTransform.getInstance())
                                    .subscribe(new Action1<BaseResponse<InitialState>>() {
                                        @Override
                                        public void call(BaseResponse<InitialState> response) {
                                            if (response == null) {
                                                BlitzApi.getInstance().getInitialState(new BasePostBody()).compose(UiTransform.getInstance())
                                                        .subscribe(this);
                                            } else {
                                                syncDiscordState(response.data);
                                                startGames(response.data);
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    private void disqualAndRestart(ArrayList<String> notFoundPlayers) {
        Log.d("MainProvider", "disqualAndRestart()");
        BotRunner.runBots();
        // called only on timeout for all unconfirmed players
        BlitzApi.getInstance().autoDisqualPending(new RoundPostBody(TournamentState.getInstance().getStatus().round)).compose(UiTransform.getInstance())
                .subscribe(new Action1<BaseResponse<Void>>() {
                    @Override
                    public void call(BaseResponse<Void> response) {
                        if (response == null) {
                            BlitzApi.getInstance().autoDisqualPending(new RoundPostBody(TournamentState.getInstance().getStatus().round)).compose(UiTransform.getInstance())
                                    .subscribe(this);
                        } else {
                            for (int i = 0; i < pendingBoards.size(); i++) {
                                ArrayList<Long> pendingBoard = pendingBoards.get(i);
                                int noNames = 0;
                                for (int j = 0; j < pendingBoard.size(); j++) {
                                    Long pendingPlayer = pendingBoard.get(j);
                                    for (String player : notFoundPlayers) {
                                        if (pendingPlayer.equals(TournamentState.getInstance().getPlayerIdByName(player))) {
                                            pendingBoard.remove(j);
                                            pendingBoard.add(j, 0L);
                                            break;
                                        }
                                    }
                                    if (pendingBoard.get(j) == 0L) {
                                        noNames++;
                                    }
                                }
                                if (noNames == Constants.PLAYER_PER_TABLE) {
                                    pendingBoards.remove(i--);
                                }
                            }
                            if (pendingBoards.size() > 0) {
                                Starter.startGames(pendingBoards, MainProvider.this);
                            } else {
                                sendPlayAll();
                            }
                        }
                    }
                });
    }

    private void autoDisqual() {
        Log.d("MainProvider", "autoDisqual()");
        // call only when PLAYP->PLAYA
        BlitzApi.getInstance().autoDisqual(new BasePostBody()).compose(UiTransform.getInstance())
                .subscribe(new Action1<BaseResponse<Void>>() {
                    @Override
                    public void call(BaseResponse<Void> response) {
                        if (response == null) {
                            BlitzApi.getInstance().autoDisqual(new BasePostBody()).compose(UiTransform.getInstance())
                                    .subscribe(this);
                        } else {
                            sendPing(10000);
                        }
                    }
                });
    }

    public void onStatusUpdated(Status status) {
        Log.d("STATUS UPDATED", status.status + " " + status.lobby);

        TournamentState.getInstance().setStatus(status);

        if (!STATUS_END.equals(status.status)) {
            BotRunner.pingBots();
        }
        ReplayHelper.proceedReplays();

        LobbyService currentInstance = LobbyService.getInstance();
        if (currentInstance != null) {
            currentInstance.setOnWishAddedCallback(this);
            if (messagesBuffer.size() > 0) {
                if (currentInstance.sendMessages(messagesBuffer)) {
                    messagesBuffer.clear();
                }
            }
        }


        if (timeHasPassed(status)) {
            //setNextStatus(status);
            //sendPing(30000);
            return;
        }

        switch (status.status) {
            case STATUS_ANNOUNCE:
                //dunno, not used
                break;
            case STATUS_REGISTRATION:
                //dunno, this is initial status
                break;
            case STATUS_WAIT:
            case STATUS_PAUSE:
                BlitzApi.getInstance().getUnconfirmedPlayers().compose(new UiTransform<>())
                        .subscribe(new Action1<BaseResponse<ArrayList<String>>>() {
                            @Override
                            public void call(BaseResponse<ArrayList<String>> arrayListBaseResponse) {
                                if (arrayListBaseResponse == null) {
                                    BlitzApi.getInstance().getUnconfirmedPlayers().compose(new UiTransform<>())
                                            .subscribe(this);
                                } else {
                                    checkPlayers(arrayListBaseResponse.data);
                                }
                            }
                        });
                sendPing(60000); // just in case
                return;
            case STATUS_PLAY_PART:
                if (pendingBoards != null) { // else we are not started all the games
                    BotRunner.runBots();
                    Starter.startGames(pendingBoards, this);
                }
                return;
            case STATUS_PLAY_ALL:
                //check if all the games are ended
                // then set PAUSE and next time (depends on current round)
                break;
            case STATUS_END:
                //dunno
                break;
            default:
                //TODO: finish();
                return;
        }

        sendPing(10000);
    }

    public void startGames(InitialState state) {
        Log.d("MainProvider", "startGames()");

        TournamentState.getInstance().setInitialState(state);

        ArrayList<Long> players = new ArrayList<>();
        for (Player player : state.players) {
            players.add(player.id);
        }

        ArrayList<ArrayList<Long>> wishedTables = TournamentState.getInstance().getWishedTables();

        ArrayList<ArrayList<Long>> seatings = SeatingsGenerator.generateSeatings(players, wishedTables, TournamentState.getInstance().getOldSeatings());

        BlitzApi.getInstance().removeWishes(new BasePostBody()).compose(UiTransform.getInstance())
                .subscribe(new Action1<BaseResponse<Void>>() {
                    @Override
                    public void call(BaseResponse<Void> response) {
                        if (response == null) {
                            BlitzApi.getInstance().removeWishes(new BasePostBody()).compose(UiTransform.getInstance())
                                    .subscribe(this);
                        } else {
                            TournamentState.getInstance().getWishes().clear();

                            Starter.startGames(seatings, MainProvider.this);
                        }
                    }
                });
    }

    public void checkPlayers(ArrayList<String> playersToCheck) {
        Log.d("MainProvider", "checkPlayers()");
        if (isCheckingPlayers) {
            Log.d("MainProvider", "isCheckingPlayers == true, stop checking");
            return;
        }
        isCheckingPlayers = true;
        this.playersToCheck = playersToCheck;
        LobbyService.getInstance().updatePlayers();
    }

    @Override
    public void onPlayersChecked(ArrayList<String> idlePlayers, ArrayList<String> playingPlayers) {
        Log.d("MainProvider", "onPlayersChecked()");
        // calling from WAIT or PAUSE
        if (isCheckingPlayers && !isCheckingLastPlayers) {
            Log.d("MainProvider", "isCheckingPlayers == true, isCheckingLastPlayers == false, allgood");
            checkedPlayers.clear();
            for (String idlePlayer : idlePlayers) {
                if (playersToCheck.remove(idlePlayer)) {
                    checkedPlayers.add(idlePlayer);
                }
            }
            if (playersToCheck.size() == 0 || (idlePlayers.size() + playingPlayers.size()) < 56) {
                Log.d("MainProvider", "onPlayersChecked: players checked, send them back");
                sendPlayers(checkedPlayers);
                isCheckingPlayers = false;
            } else {
                Log.d("MainProvider", "onPlayersChecked: not all players are checked");
                isCheckingLastPlayers = true;
                checkLastPlayers();
            }
        }

    }

    private void sendPlayers(final ArrayList<String> idlePlayers) {
        Log.d("MainProvider", "sendPlayers()");
        if (idlePlayers.size() > 0) {
            BlitzApi.getInstance().confirm(new ConfirmationsPostBody(idlePlayers)).compose(UiTransform.getInstance())
                    .subscribe(new Action1<BaseResponse<Void>>() {
                        @Override
                        public void call(BaseResponse<Void> response) {
                            if (response == null) {
                                BlitzApi.getInstance().confirm(new ConfirmationsPostBody(idlePlayers)).compose(UiTransform.getInstance())
                                        .subscribe(this);
                            } else {
                                sendPing(10000);
                            }
                        }
                    });
        } else {
            sendPing(10000);
        }
    }


    private void checkLastPlayers() {
        Log.d("MainProvider", "checkLastPlayers()");
        if (playersToCheck.size() == 0) {
            Log.d("MainProvider", "checkLastPlayers: players checked, send them back");
            sendPlayers(checkedPlayers);
            isCheckingPlayers = false;
            isCheckingLastPlayers = false;
            return;
        }
        ArrayList<String> table = new ArrayList<>();
        for (int i = 0; i < 3 && i < playersToCheck.size(); i++) {
            String player = playersToCheck.remove(0);
            table.add(player);
            checkedPlayers.add(player);
        }
        for (int i = 4; i > table.size(); i--) {
            table.add("BBCBBBD" + i);
        }
        Starter.startGame(LOBBY, null, table, null, new IStarterCallback() {
            @Override
            public void onGameStarted(ArrayList<String> players) {
                // impossible
            }

            @Override
            public void onMembersNotFound(ArrayList<String> players) {
                for (String player : players) {
                    checkedPlayers.remove(player);
                }
                checkLastPlayers();
            }
        });
    }

    @Override
    public void gamesStarted(ArrayList<GameEntity> games, ArrayList<String> notFound, ArrayList<ArrayList<Long>> notStarted) {
        Log.d("MainProvider", "gameStarted()");
        Log.d("MainProvider", "games: " + gson.toJson(games));
        Log.d("MainProvider", "notFound: " + gson.toJson(notFound));
        // calling after games start attempt (PAUSE->PLAYP, PLAYP->PLAYP, PLAYP->PLAYA)
        Action1<BaseResponse<Void>> callback = new Action1<BaseResponse<Void>>() {
            @Override
            public void call(BaseResponse<Void> baseResponse) {
                if (baseResponse == null) {
                    if (pendingBoards == null) {
                        BlitzApi.getInstance().start(new StartPostBody(games)).compose(UiTransform.getInstance())
                                .subscribe(this);
                    } else {
                        BlitzApi.getInstance().startLast(new StartPostBody(games)).compose(UiTransform.getInstance())
                                .subscribe(this);
                    }
                } else {
                    if (notStarted.size() == 0) {
                        pendingBoards = null;
                        sendPlayAll();
                    } else {
                        pendingBoards = notStarted;
                        if (notFound.size() > 0) {
                            sendNotFound(notFound);
                        } else {
                            sendPing(30000);
                        }
                    }
                }
            }


        };

        for (int i = 0; i < games.size(); i += Constants.PLAYER_PER_TABLE) {
            for (int j = 0; j < Constants.PLAYER_PER_TABLE; j++) {
                GameEntity entity = games.get(i + j);
                if (entity.playerId != 0L && entity.startPoints != null) {
                    ArrayList<GameEntity> addedSeating = new ArrayList<>();
                    ArrayList<Long> startedTable = new ArrayList<>();
                    ArrayList<String> players = new ArrayList<>();
                    for (int k = 0; k < Constants.PLAYER_PER_TABLE; k++) {
                        GameEntity e = games.get(i + k);
                        addedSeating.add(e);
                        startedTable.add(e.playerId);
                        players.add(TournamentState.getInstance().getPlayerNameById(e.playerId));
                    }
                    TournamentState.getInstance().insertSeating(addedSeating);
                    TournamentState.getInstance().gameStarted(startedTable);
                    DiscordBot.getInstance().gameStarted(addedSeating.get(0).board, players);
                    break;
                }
            }
        }

        if (this.pendingBoards == null) {
            BlitzApi.getInstance().start(new StartPostBody(games)).compose(UiTransform.getInstance())
                    .subscribe(callback);
        } else {
            for (String notFoundName : notFound) {
                long notFoundId = TournamentState.getInstance().getPlayerIdByName(notFoundName);
                for (int i = 0; i < games.size(); i += Constants.PLAYER_PER_TABLE) {
                    for (int j = 0; j < Constants.PLAYER_PER_TABLE; j++) {
                        if (games.get(i + j).playerId == notFoundId) {
                            for (int k = 0; k < Constants.PLAYER_PER_TABLE; k++) {
                                games.remove(i);
                            }
                            i -= Constants.PLAYER_PER_TABLE;
                            break;
                        }
                    }
                }
            }

            if (games.size() > 0) {
                BlitzApi.getInstance().startLast(new StartPostBody(games)).compose(UiTransform.getInstance())
                        .subscribe(callback);
            } else {
                sendNotFound(notFound);
                //sendPing(10000);
            }
        }

    }

    private void sendNotFound(ArrayList<String> notFound) {
        BlitzApi.getInstance().unconfirm(new ConfirmationsPostBody(notFound)).compose(UiTransform.getInstance())
                .subscribe(new Action1<BaseResponse<Void>>() {
                    @Override
                    public void call(BaseResponse<Void> response) {
                        if (response == null) {
                            BlitzApi.getInstance().unconfirm(new ConfirmationsPostBody(notFound)).compose(UiTransform.getInstance())
                                    .subscribe(this);
                        } else {
                            Log.d("Starter", "Unconfirm query: " + response.query);
                            sendPing(30000);
                        }
                    }
                });
    }

    private void sendPlayAll() {
        Log.d("MainProvider", "sendPlayAll()");
        // call when PLAYP-PLAYA if all the tables with at least 1 player are started
        Status status = TournamentState.getInstance().getStatus();
        status.status = STATUS_PLAY_ALL;
        status.time = new Date().getTime() + TimeUnit.HOURS.toMillis(3);
        status.delay = 60000;
        BlitzApi.getInstance().setStatus(status).compose(UiTransform.getInstance())
                .subscribe(new Action1<BaseResponse<Void>>() {
                    @Override
                    public void call(BaseResponse<Void> response) {
                        if (response == null) {
                            BlitzApi.getInstance().setStatus(status).compose(UiTransform.getInstance())
                                    .subscribe(this);
                        } else {
                            autoDisqual();
                        }
                    }
                });
    }

    @Override
    public void onGameEnded(ArrayList<ResultBody> results) {
        Log.d("MainProvider", "onGameEnded()");
        // if game was ended we should post result to backend
        // and check if all the games are ended already
        ArrayList<GameEntity> result = new ArrayList<>();
        ArrayList<Long> table = new ArrayList<>();
        int currentRound = TournamentState.getInstance().getStatus().round;
        long anyRealPlayerId = 0;
        for (ResultBody resultBody : results) {
            long playerId = TournamentState.getInstance().getPlayerIdByName(resultBody.name);
            if (playerId != 0) {
                anyRealPlayerId = playerId;
                result.add(new GameEntity(currentRound, playerId, resultBody.score, resultBody.place));
            }
            table.add(playerId);
        }
        for (GameEntity game : TournamentState.getInstance().getGames()) {
            if (game.round == currentRound && game.playerId == anyRealPlayerId) {
                DiscordBot.getInstance().gameEnded(game.board);
                break;
            }
        }

        BlitzApi.getInstance().gameEnded(new ResultsPostBody(result)).compose(UiTransform.getInstance())
                .subscribe(new Action1<BaseResponse<Void>>() {
                    @Override
                    public void call(BaseResponse<Void> response) {
                        if (response == null) {
                            BlitzApi.getInstance().gameEnded(new ResultsPostBody(result)).compose(UiTransform.getInstance())
                                    .subscribe(this);
                        } else {
                            checkAllTheGamesAreEnded(table);
                        }
                    }
                });
    }

    private void checkAllTheGamesAreEnded(ArrayList<Long> endedGame) {
        Log.d("MainProvider", "checkAllTheGamesAreEnded()");
        // if all of the games are ended - unconfirm all the players
        // then set status to PAUSE with next time, and ping backend later
        if (TournamentState.getInstance().gameEnded(endedGame)) {
            BotRunner.sendReplays();
            final Status status = TournamentState.getInstance().getStatus();
            status.status = STATUS_PAUSE;
            status.time = new Date().getTime() + Constants.PAUSE_DELAYS[status.round - 1];
            status.delay = 10000;
            BlitzApi.getInstance().autoUnconfirm(new BasePostBody()).compose(UiTransform.getInstance())
                    .subscribe(new Action1<BaseResponse<Void>>() {
                        @Override
                        public void call(BaseResponse<Void> autoUnconfirmResponse) {
                            if (autoUnconfirmResponse == null) {
                                BlitzApi.getInstance().autoUnconfirm(new BasePostBody()).compose(UiTransform.getInstance())
                                        .subscribe(this);
                            } else {
                                BlitzApi.getInstance().setStatus(status).compose(UiTransform.getInstance())
                                        .subscribe(new Action1<BaseResponse<Void>>() {
                                            @Override
                                            public void call(BaseResponse<Void> statusResponse) {
                                                if (statusResponse == null) {
                                                    BlitzApi.getInstance().setStatus(status).compose(UiTransform.getInstance())
                                                            .subscribe(this);
                                                } else {
                                                    sendPing(TimeUnit.MINUTES.toMillis(2));
                                                }
                                            }
                                        });
                            }
                        }
                    });

        }
    }

    @Override
    public void onWishAdded(ArrayList<String> wish) {
        Log.d("MainProvider", "onWishAdded()");
        BlitzApi.getInstance().getConfirmedPlayers().compose(UiTransform.getInstance())
                .subscribe(new Action1<BaseResponse<ArrayList<String>>>() {
                    @Override
                    public void call(BaseResponse<ArrayList<String>> response) {
                        if (response == null) {
                            BlitzApi.getInstance().getConfirmedPlayers().compose(UiTransform.getInstance())
                                    .subscribe(this);
                        } else {
                            StringBuilder unconfirmed = new StringBuilder();
                            for (String player : wish) {
                                if (!response.data.contains(player)) {
                                    if (unconfirmed.length() > 0) {
                                        unconfirmed.append(", ");
                                    }
                                    unconfirmed.append(player);
                                }
                            }
                            if (unconfirmed.length() > 0) {
                                messagesBuffer.add(wish.get(0) + ": someone still not confirmed - " + unconfirmed.toString());
                                return;
                            }

                            String error = TournamentState.getInstance().tryToAddWish(wish);

                            if (error != null) {
                                messagesBuffer.add(error);
                                return;
                            }

                            StringBuilder names = new StringBuilder();
                            names.append("Confirmed ").append(wish.get(0)).append(" with ");
                            for (int i = 1; i < wish.size(); i++) {
                                names.append(wish.get(i));
                                if (i < wish.size() - 2) {
                                    names.append(", ");
                                } else if (i < wish.size() - 1) {
                                    names.append(" and ");
                                }
                            }

                            long who = TournamentState.getInstance().getPlayerIdByName(wish.get(0));
                            ArrayList<Long> withWhom = new ArrayList<>();
                            for (int i = 1; i < wish.size(); i++) {
                                withWhom.add(TournamentState.getInstance().getPlayerIdByName(wish.get(i)));
                            }

                            BlitzApi.getInstance().addWish(new Wish(who, withWhom.get(0))).compose(UiTransform.getInstance())
                                    .subscribe(new Action1<BaseResponse<Void>>() {
                                        @Override
                                        public void call(BaseResponse<Void> response) {
                                            if (response == null) {
                                                BlitzApi.getInstance().addWish(new Wish(who, withWhom.get(0))).compose(UiTransform.getInstance())
                                                        .subscribe(this);
                                            } else {
                                                withWhom.remove(0);
                                                if (withWhom.size() > 0) {
                                                    BlitzApi.getInstance().addWish(new Wish(who, withWhom.get(0))).compose(UiTransform.getInstance())
                                                            .subscribe(this);
                                                } else {
                                                    messagesBuffer.add(names.toString());
                                                    sendPing(5000);
                                                }
                                            }
                                        }
                                    });
                        }
                    }
                });

    }

    public void cancelPing() {
        if (updateTimer != null) {
            updateTimer.cancel();
            updateTimer = null;
        }
    }

    public void sendPing(long delay) {
        Log.d("MainProvider", "sendPing(" + delay + ")");
        cancelPing();
        updateTimer = new Timer();
        updateTimer.schedule(newTimerTask(), delay);
    }

    private void syncDiscordState(InitialState state) {
        for (Player player : state.players) {
            if (player.discordName != null && player.discriminator != null) {
                DiscordBot.getInstance().setAlias(player.name, player.discordName.trim(), player.discriminator.trim());
            }
        }
        int a = 0;
    }


}
