package com.shaungc.utilities;

import com.shaungc.javadev.Configuration;
import java.time.Instant;

public class Logger {

    public static void debug(String message) {
        // dense for debug message, no leading & trailing newline
        Logger.getMessage(LoggerLevel.DEBUG, message, 0, 0, false, false);
    }

    public static void info(String message) {
        Logger.getMessage(LoggerLevel.INFO, message, 1, 0, false, false);
    }

    public static void infoAlsoSlack(String message) {
        Logger.getMessage(LoggerLevel.INFO, message, 1, 0, true, false);
    }

    public static void infoAlsoSlack(String message, final Boolean forceLog) {
        Logger.getMessage(LoggerLevel.INFO, message, 1, 0, true, forceLog);
    }

    public static void info(String message, Integer leadingNewlineCount) {
        Logger.getMessage(LoggerLevel.INFO, message, leadingNewlineCount, 0, false, false);
    }

    public static void warn(String message) {
        Logger.getMessage(LoggerLevel.WARN, message, 1, 0, false, false);
    }

    public static void warnAlsoSlack(String message) {
        Logger.getMessage(LoggerLevel.WARN, message, 1, 0, true, false);
    }

    public static void error(String message) {
        Logger.getMessage(LoggerLevel.ERROR, message, 1, 0, false, false);
    }

    public static void errorAlsoSlack(String message) {
        Logger.getMessage(LoggerLevel.ERROR, message, 1, 0, true, false);
    }

    // helper functions

    private static void getMessage(
        LoggerLevel logLevel,
        String message,
        Integer leadingNewlineCount,
        Integer trailingNewlineCount,
        Boolean alsoSendSlackMessage,
        Boolean forceLog
    ) {
        String leadingNewlines = "\n".repeat(leadingNewlineCount);
        String trailingNewlines = "\n".repeat(trailingNewlineCount);
        StringBuilder finalMessage =
            (new StringBuilder()).append(logLevel.getVisualCueEmoji())
                .append(logLevel.getAliasText())
                .append(": ")
                .append(message)
                .append(trailingNewlines);

        if (forceLog || Configuration.DEBUG || Configuration.LOGGER_LEVEL.compareTo(logLevel.getVerbosenessLevelValue()) >= 0) {
            // print system log
            StringBuilder prefix = new StringBuilder(leadingNewlines).append(Instant.now().toString()).append(" ");
            System.out.println(prefix.append(finalMessage.toString()));

            // send slack if needed
            if (alsoSendSlackMessage) {
                SlackService.sendMessage(finalMessage.toString());
                // additionally post to #error channel
                if (
                    logLevel.getVerbosenessLevelValue() == LoggerLevel.ERROR.getVerbosenessLevelValue() ||
                    logLevel.getVerbosenessLevelValue() == LoggerLevel.WARN.getVerbosenessLevelValue()
                ) {
                    SlackService.sendMessageToErrorChannel(finalMessage.toString());
                }
            }
        }
    }
}
