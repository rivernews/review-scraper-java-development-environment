package com.shaungc.javadev;

import com.shaungc.utilities.Logger;
import com.shaungc.utilities.RequestAddressValidator;
import com.shaungc.utilities.ReviewCollisionStrategy;
import com.shaungc.utilities.ScraperMode;
import java.net.URI;

/**
 * Configuration
 */
public final class Configuration {
    // flow control

    // DEBUG and LOGGER_LEVEL needs to be declared first in order to use Logger.debug()
    public static Boolean DEBUG = Boolean.parseBoolean(Configuration.getenvOrDefault("DEBUG", "true"));
    public static Integer LOGGER_LEVEL = System.getenv("LOGGER_LEVEL") != null ? Integer.parseInt(System.getenv("LOGGER_LEVEL")) : 4;

    public static Boolean RUNNING_FROM_CONTAINER = System.getenv("RUNNING_FROM_CONTAINER") != null
        ? Boolean.parseBoolean(System.getenv("RUNNING_FROM_CONTAINER"))
        : true;
    public static Integer REVIEW_COLLISION_STRATEGY = Integer.valueOf(
        Configuration.getenvOrDefault("REVIEW_COLLISION_STRATEGY", ReviewCollisionStrategy.ABORT.toString())
    );

    static {
        Logger.debug("REVIEW_COLLISION_STRATEGY: " + Configuration.REVIEW_COLLISION_STRATEGY);
    }

    // credentials

    public static String GLASSDOOR_USERNAME = System.getenv("GLASSDOOR_USERNAME") != null
        ? System.getenv("GLASSDOOR_USERNAME")
        : System.getProperty("GLASSDOOR_USERNAME");
    public static String GLASSDOOR_PASSWORD = System.getenv("GLASSDOOR_PASSWORD") != null
        ? System.getenv("GLASSDOOR_PASSWORD")
        : System.getProperty("GLASSDOOR_PASSWORD");

    public static URI SLACK_WEBHOOK_URL = RequestAddressValidator.toURI(System.getenv("SLACK_WEBHOOK_URL"));

    // constants

    public static Integer AVOID_GLITCH_WAIT_SECOND = 2;
    public static Integer EXPECTED_CONDITION_WAIT_SECOND = 25;
    public static Integer EXPECTED_CONDITION_WAIT_SECOND_LONGER = 60;

    public static String AWS_S3_ARCHIVE_BUCKET_NAME = System.getenv("AWS_S3_ARCHIVE_BUCKET_NAME") != null
        ? System.getenv("AWS_S3_ARCHIVE_BUCKET_NAME")
        : "iriversland-qualitative-org-review--debug03";

    static {
        Logger.debug("S3 bucket name is " + Configuration.AWS_S3_ARCHIVE_BUCKET_NAME);
    }

    // inputs

    public static String TEST_COMPANY_INFORMATION_STRING = System.getenv("TEST_COMPANY_INFORMATION_STRING") != null
        ? System.getenv("TEST_COMPANY_INFORMATION_STRING")
        : "";
    public static String TEST_COMPANY_ID = Configuration.getenvOrDefault("TEST_COMPANY_ID", "");
    public static String TEST_COMPANY_NAME = Configuration.getenvOrDefault("TEST_COMPANY_NAME", "");

    public static Integer TEST_COMPANY_LAST_PROGRESS_PROCESSED = Integer.valueOf(
        Configuration.getenvOrDefault("TEST_COMPANY_LAST_PROGRESS_PROCESSED", "0")
    );
    public static Integer TEST_COMPANY_LAST_PROGRESS_WENTTHROUGH = Integer.valueOf(
        Configuration.getenvOrDefault("TEST_COMPANY_LAST_PROGRESS_WENTTHROUGH", "0")
    );
    public static Integer TEST_COMPANY_LAST_PROGRESS_TOTAL = Integer.valueOf(
        Configuration.getenvOrDefault("TEST_COMPANY_LAST_PROGRESS_TOTAL", "0")
    );
    public static String TEST_COMPANY_LAST_PROGRESS_DURATION = Configuration.getenvOrDefault("TEST_COMPANY_LAST_PROGRESS_DURATION", "0");
    public static Integer TEST_COMPANY_LAST_PROGRESS_PAGE = Integer.valueOf(
        Configuration.getenvOrDefault("TEST_COMPANY_LAST_PROGRESS_PAGE", "0")
    );
    public static Integer TEST_COMPANY_LAST_PROGRESS_SESSION = Integer.valueOf(
        Configuration.getenvOrDefault("TEST_COMPANY_LAST_PROGRESS_SESSION", "0")
    );

    public static String TEST_COMPANY_LAST_REVIEW_PAGE_URL = Configuration.getenvOrDefault("TEST_COMPANY_LAST_REVIEW_PAGE_URL", "");
    public static String SCRAPER_MODE = Configuration.getenvOrDefault("SCRAPER_MODE", ScraperMode.REGULAR.getString());

    // misc helper

    public static String getenvOrDefault(String envKey, String defaultString) {
        String envValue = System.getenv(envKey);
        return envValue != null ? envValue : defaultString;
    }
}
