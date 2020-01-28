package com.shaungc.javadev;

/**
 * Configuration
 */
public final class Configuration {
    public static Integer AVOID_GLITCH_WAIT_SECOND = 2;
    public static Integer EXPECTED_CONDITION_WAIT_SECOND = 25;
    public static Integer EXPECTED_CONDITION_WAIT_SECOND_LONGER = 60;
    public static Boolean DEBUG = System.getenv("DEBUG") != null ? Boolean.parseBoolean(System.getenv("DEBUG")) : false;
    
    public static String GLASSDOOR_USERNAME = System.getenv("GLASSDOOR_USERNAME") != null ? System.getenv("GLASSDOOR_USERNAME") : System.getProperty("GLASSDOOR_USERNAME");
    public static String GLASSDOOR_PASSWORD = System.getenv("GLASSDOOR_PASSWORD") != null ? System.getenv("GLASSDOOR_PASSWORD") : System.getProperty("GLASSDOOR_PASSWORD");

    public static Boolean RUNNING_FROM_CONTAINER = System.getenv("RUNNING_FROM_CONTAINER") != null ? Boolean.parseBoolean(System.getenv("RUNNING_FROM_CONTAINER")) : true;

    public static String TEST_COMPANY_INFORMATION_STRING = System.getenv("TEST_COMPANY_INFORMATION_STRING") != null ? System.getenv("TEST_COMPANY_INFORMATION_STRING") : null;

    public static String AWS_S3_ARCHIVE_BUCKET_NAME = System.getenv("AWS_S3_ARCHIVE_BUCKET_NAME") != null ? System.getenv("AWS_S3_ARCHIVE_BUCKET_NAME") : "shaungc-qualitative-org-review-data";

    public static Integer LOGGER_LEVEL = System.getenv("LOGGER_LEVEL") != null ? Integer.parseInt(System.getenv("LOGGER_LEVEL")) : 2;

    static {
        Logger.info("S3 bucket name is " + Configuration.AWS_S3_ARCHIVE_BUCKET_NAME);
    }
}