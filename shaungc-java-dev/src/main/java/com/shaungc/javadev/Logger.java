package com.shaungc.javadev;


public class Logger {

    public static void info(String message) {
        if (Configuration.DEBUG || Configuration.LOGGER_LEVEL > 1) {
            System.out.println("\nINFO: " + message);
        }
    }
}