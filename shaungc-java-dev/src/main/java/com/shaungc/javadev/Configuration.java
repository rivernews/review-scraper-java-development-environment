package com.shaungc.javadev;

import com.shaungc.utilities.Logger;
import com.shaungc.utilities.RequestAddressValidator;
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

    public static String WEBDRIVER_MODE = Configuration.getenvOrDefault("WEBDRIVER_MODE", "localInstalled");
    public static String SELENIUM_SERVER_CUSTOM_HOST = Configuration.getenvOrDefault("SELENIUM_SERVER_CUSTOM_HOST", "");

    public static String REDIS_MODE = Configuration.getenvOrDefault("REDIS_MODE", "serverFromMacosDockerContainer");
    public static String REDIS_CUSTOM_HOST = Configuration.getenvOrDefault("REDIS_CUSTOM_HOST", "");

    // credentials

    public static String GLASSDOOR_USERNAME = System.getenv("GLASSDOOR_USERNAME") != null
        ? System.getenv("GLASSDOOR_USERNAME")
        : System.getProperty("GLASSDOOR_USERNAME");
    public static String GLASSDOOR_PASSWORD = System.getenv("GLASSDOOR_PASSWORD") != null
        ? System.getenv("GLASSDOOR_PASSWORD")
        : System.getProperty("GLASSDOOR_PASSWORD");

    public static URI SLACK_WEBHOOK_URL = RequestAddressValidator.toURI(System.getenv("SLACK_WEBHOOK_URL"));

    // external resources

    public static String AWS_S3_ARCHIVE_BUCKET_NAME = System.getenv("AWS_S3_ARCHIVE_BUCKET_NAME") != null
        ? System.getenv("AWS_S3_ARCHIVE_BUCKET_NAME")
        : "iriversland-qualitative-org-review--debug03";

    static {
        Logger.debug("S3 bucket name is " + Configuration.AWS_S3_ARCHIVE_BUCKET_NAME);
    }

    public static String SUPERVISOR_PUBSUB_REDIS_DB = Configuration.getenvOrDefault("SUPERVISOR_PUBSUB_REDIS_DB", "");

    static {
        Logger.debug("SUPERVISOR_PUBSUB_REDIS_DB is " + Configuration.SUPERVISOR_PUBSUB_REDIS_DB);
    }

    // constants

    public static Integer AVOID_GLITCH_WAIT_SECOND = 2;
    public static Integer EXPECTED_CONDITION_WAIT_SECOND = 25;
    public static Integer EXPECTED_CONDITION_WAIT_SECOND_LONGER = 60;

    public static Long CROSS_SESSION_TIME_LIMIT_MINUTES = Float
        .valueOf(Configuration.getenvOrDefault("CROSS_SESSION_TIME_LIMIT_MINUTES", "1"))
        .longValue();

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
