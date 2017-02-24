package com.yufimtsev.tenhou.clouds.blitz.model;

import java.util.concurrent.TimeUnit;

public class Constants {
    public static final int PLAYER_PER_TABLE = 4;
    public static final int TOTAL_ROUNDS = 16;

    //public static final long REGISTRATION_DELAY = TimeUnit.MINUTES.toMillis(3);
    public static final long WAIT_DELAY = TimeUnit.MINUTES.toMillis(30);
    public static final long PLAY_PART_DELAY = TimeUnit.MINUTES.toMillis(3);
    public static final long[] PAUSE_DELAYS = new long[] {
            TimeUnit.MINUTES.toMillis(1), // after 1st round
            TimeUnit.MINUTES.toMillis(1),
            TimeUnit.MINUTES.toMillis(1),
            TimeUnit.MINUTES.toMillis(1),
            TimeUnit.MINUTES.toMillis(1),
            TimeUnit.MINUTES.toMillis(1),
            TimeUnit.MINUTES.toMillis(1),
            TimeUnit.MINUTES.toMillis(15),
            TimeUnit.MINUTES.toMillis(1),
            TimeUnit.MINUTES.toMillis(1),
            TimeUnit.MINUTES.toMillis(1),
            TimeUnit.MINUTES.toMillis(1),
            TimeUnit.MINUTES.toMillis(1),
            TimeUnit.MINUTES.toMillis(1),
            TimeUnit.MINUTES.toMillis(1),
            TimeUnit.MINUTES.toMillis(1),
    };
}
