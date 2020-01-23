package com.shaungc.javadev;

/**
 * Configuration
 */
public final class Configuration {
    public static Integer AVOID_GLITCH_WAIT_SECOND = 2;
    public static Integer EXPECTED_CONDITION_WAIT_SECOND = 25;
    public static Boolean DEBUG = System.getenv("DEBUG") != null ? Boolean.parseBoolean(System.getenv("DEBUG")) : false;
    
    public static String GLASSDOOR_USERNAME = System.getenv("GLASSDOOR_USERNAME") != null ? System.getenv("GLASSDOOR_USERNAME") : System.getProperty("GLASSDOOR_USERNAME");
    public static String GLASSDOOR_PASSWORD = System.getenv("GLASSDOOR_PASSWORD") != null ? System.getenv("GLASSDOOR_PASSWORD") : System.getProperty("GLASSDOOR_PASSWORD");

    public static Boolean RUNNING_FROM_CONTAINER = System.getenv("RUNNING_FROM_CONTAINER") != null ? Boolean.parseBoolean(System.getenv("RUNNING_FROM_CONTAINER")) : true;

    static {
        if (DEBUG) {
            System.out.println("\nConfiguration:GLASSDOOR_USERNAME = " + GLASSDOOR_USERNAME);
            System.out.println("\nConfiguration:GLASSDOOR_PASSWORD = " + GLASSDOOR_PASSWORD);
        }
    }
}