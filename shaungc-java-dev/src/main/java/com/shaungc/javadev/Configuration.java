package com.shaungc.javadev;

/**
 * Configuration
 */
public final class Configuration {
    public static Integer AVOID_GLITCH_WAIT_SECOND = 2;
    public static Integer EXPECTED_CONDITION_WAIT_SECOND = 10;
    public static Boolean DEBUG = true;
    
    public static String GLASSDOOR_USERNAME = System.getenv("GLASSDOOR_USERNAME") != null ? System.getenv("GLASSDOOR_USERNAME") : System.getProperty("GLASSDOOR_USERNAME");
    public static String GLASSDOOR_PASSWORD = System.getenv("GLASSDOOR_PASSWORD") != null ? System.getenv("GLASSDOOR_PASSWORD") : System.getProperty("GLASSDOOR_PASSWORD");

    static {
        if (DEBUG) {
            System.out.println("\nConfiguration:GLASSDOOR_USERNAME = " + GLASSDOOR_USERNAME);
            System.out.println("\nConfiguration:GLASSDOOR_PASSWORD = " + GLASSDOOR_PASSWORD);
        }
    }
}