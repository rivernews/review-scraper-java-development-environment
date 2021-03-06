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

    public static Boolean RUNNING_IN_TRAVIS = Boolean.parseBoolean(Configuration.getenvOrDefault("TRAVIS", "false"));

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

    public static Integer GLASSDOOR_REVIEW_COUNT_PER_PAGE = 10;

    public static URI SLACK_WEBHOOK_URL = RequestAddressValidator.toURI(System.getenv("SLACK_WEBHOOK_URL"));

    public static String REDIS_PORT = Configuration.getenvOrDefault("REDIS_PORT", "6379");
    public static String REDIS_PASSWORD = Configuration.getenvOrDefault("REDIS_PASSWORD", "");

    // external resources

    public static String AWS_S3_ARCHIVE_BUCKET_NAME = System.getenv("AWS_S3_ARCHIVE_BUCKET_NAME") != null
        ? System.getenv("AWS_S3_ARCHIVE_BUCKET_NAME")
        : "iriversland-qualitative-org-review--debug03";

    static {
        Logger.debug("S3 bucket name is " + Configuration.AWS_S3_ARCHIVE_BUCKET_NAME);
    }

    public static String SUPERVISOR_PUBSUB_REDIS_DB = Configuration.getenvOrDefault("SUPERVISOR_PUBSUB_REDIS_DB", "");
    public static String SUPERVISOR_PUBSUB_CHANNEL_NAME = Configuration.getenvOrDefault("SUPERVISOR_PUBSUB_CHANNEL_NAME", "");

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
    public static String TEST_COMPANY_NAME;

    static {
        // global orgName should not include double quotes
        // one exception is pubsub channel name - but pubsub will explicitly add double quote there

        // remove double quote, if included
        final StringBuilder noQuoteOrgName = new StringBuilder(Configuration.getenvOrDefault("TEST_COMPANY_NAME", "").strip());

        if (noQuoteOrgName.length() != 0) {
            if (
                noQuoteOrgName.charAt(0) == '"' && noQuoteOrgName.charAt(noQuoteOrgName.length() - 1) == '"' && noQuoteOrgName.length() >= 2
            ) {
                noQuoteOrgName.deleteCharAt(0);
                noQuoteOrgName.deleteCharAt(noQuoteOrgName.length() - 1);
            }

            Configuration.TEST_COMPANY_NAME = noQuoteOrgName.toString();
        } else {
            Configuration.TEST_COMPANY_NAME = "";
        }
    }

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

    public static String TEST_COMPANY_NEXT_REVIEW_PAGE_URL = Configuration.getenvOrDefault("TEST_COMPANY_NEXT_REVIEW_PAGE_URL", "");

    public static String SCRAPER_MODE = Configuration.getenvOrDefault("SCRAPER_MODE", ScraperMode.REGULAR.getString());

    public static Integer TEST_COMPANY_STOP_AT_PAGE = Integer.valueOf(Configuration.getenvOrDefault("TEST_COMPANY_STOP_AT_PAGE", "0"));

    public static Integer TEST_COMPANY_SHARD_INDEX = Integer.valueOf(Configuration.getenvOrDefault("TEST_COMPANY_SHARD_INDEX", "0"));

    // misc helper

    public static String getenvOrDefault(final String envKey, final String defaultString) {
        final String envValue = System.getenv(envKey);
        return envValue != null && !envValue.strip().isEmpty() ? envValue : defaultString;
    }
}
