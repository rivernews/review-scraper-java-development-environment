package com.shaungc.javadev;


public class Logger {

    public static void info(String message) {
        if (Configuration.DEBUG) {
            System.out.println("\nINFO: " + message);
        }
    }
}