package com.shaungc.utilities;

import com.shaungc.javadev.Configuration;

enum LoggerLevel {
    ERROR(1, "ERROR"),
    WARN(2, "WARN"),
    INFO(3, "INFO");

    private final Integer verbosenessLevelValue;
    private final String aliasText;

    private LoggerLevel(Integer verbosenessLevelValue, String aliasText) {
        this.verbosenessLevelValue = verbosenessLevelValue;
        this.aliasText = aliasText;
    }

    public Integer getVerbosenessLevelValue() {
        return this.verbosenessLevelValue;
    }

    public String getAliasText() {
        return this.aliasText;
    }
}


public class Logger {

    public static void info(String message) {
        Logger.getMessage(LoggerLevel.INFO, message, 1, 0, false);
    }
    public static void infoAlsoSlack(String message) {
        Logger.getMessage(LoggerLevel.INFO, message, 1, 0, true);
    }

    public static void info(String message, Integer leadingNewlineCount) {
        Logger.getMessage(LoggerLevel.INFO, message, leadingNewlineCount, 0, false);
    }

    public static void warn(String message) {
        Logger.getMessage(LoggerLevel.WARN, message, 1, 0, false);
    }

    public static void error(String message) {
        Logger.getMessage(LoggerLevel.ERROR, message, 1, 0, false);
    }
    public static void errorAlsoSlack(String message) {
        Logger.getMessage(LoggerLevel.ERROR, message, 1, 0, true);
    }

    // helper functions

    private static void getMessage(LoggerLevel logLevel, String message, Integer leadingNewlineCount, Integer trailingNewlineCount, Boolean alsoSendSlackMessage) {
        String leadingNewlines = "\n".repeat(leadingNewlineCount);
        String trailingNewlines = "\n".repeat(trailingNewlineCount);
        String finalMessage = leadingNewlines + logLevel.getAliasText() + ": " + message + trailingNewlines;

        if (Configuration.DEBUG || Configuration.LOGGER_LEVEL >= logLevel.getVerbosenessLevelValue()) {
            System.out.println(finalMessage);
        }

        if (alsoSendSlackMessage) {
            SlackService.sendMessage(finalMessage);
            // SlackService.asyncSendMessage(finalMessage);
        }
    }
}