package com.yufimtsev.tenhou.clouds.blitz.model;

import java.util.concurrent.TimeUnit;

public class Constants {
    public static final int PLAYER_PER_TABLE = 4;
    public static final int TOTAL_ROUNDS = 4;

    //public static final long REGISTRATION_DELAY = TimeUnit.MINUTES.toMillis(3);
    public static final long WAIT_DELAY = TimeUnit.MINUTES.toMillis(60);
    public static final long PLAY_PART_DELAY = TimeUnit.MINUTES.toMillis(5);
    public static final long[] PAUSE_DELAYS = new long[] {
            TimeUnit.MINUTES.toMillis(10), // after 1st round
            TimeUnit.MINUTES.toMillis(30), // after 2nd round
            TimeUnit.MINUTES.toMillis(30), // after 3rd round
            TimeUnit.MINUTES.toMillis(10), // after 4th round
            TimeUnit.MINUTES.toMillis(5), // after 5th round
    };
}
