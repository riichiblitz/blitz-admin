package com.yufimtsev.tenhou.clouds.logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {

    private static final int MAX_LOG_PER_FILE = 5000;
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("YYYY-MM-DD-HH-mm-ss");

    private static FileWriter writer;
    private static long logCounter;

    static {
        resetLogFile();
    }


    public static void d(String tag, String message) {
        if (++logCounter > MAX_LOG_PER_FILE) {
            resetLogFile();
        }
        String logMessage = DATE_TIME_FORMAT.format(new Date()) + " " + tag + ": " + message;
        try {
            writer.append(logMessage);
        } catch (IOException e) {
            System.out.println("COULD NOT APPEND TO LOGFILE");
            resetLogFile();
        }
        System.out.println(logMessage);
    }

    public static void d(String tag, Exception exception) {
        if (++logCounter > MAX_LOG_PER_FILE) {
            resetLogFile();
        }
        String logMessage = DATE_TIME_FORMAT.format(new Date()) + " " + tag + ": EXCEPTION";
        exception.printStackTrace();
        try {
            writer.append(logMessage);
            writer.append(exception.getMessage());
        } catch (IOException e) {
            System.out.println("COULD NOT APPEND TO LOGFILE");
            resetLogFile();
        }
        System.out.println(logMessage);
    }

    public static void resetLogFile() {
        if (writer != null) {
            try {
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String logName = "log-" + DATE_TIME_FORMAT.format(new Date()) + ".txt";

        File file = new File(logName);
        System.out.print("New log file: " + file.getAbsolutePath() +"... ");
        try {
            writer = new FileWriter(file);
            System.out.println("OK");
        } catch (IOException e) {
            System.out.println("FAILED");
            e.printStackTrace();
        }
    }

}
