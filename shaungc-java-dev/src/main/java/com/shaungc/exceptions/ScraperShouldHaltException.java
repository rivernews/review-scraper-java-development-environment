package com.shaungc.exceptions;

import com.shaungc.dataStorage.ArchiveManager;
import com.shaungc.utilities.Logger;
import com.shaungc.utilities.SlackService;
import org.openqa.selenium.WebDriver;

/**
 * ScraperShouldHaltException
 *
 * Any expected exceptions that you want to collect here,
 * also where using `ScraperException` is not possible
 *
 */
public class ScraperShouldHaltException extends RuntimeException {

    public ScraperShouldHaltException(String errorMessage) {
        super(errorMessage);
        Logger.errorAlsoSlack(errorMessage);
    }

    public ScraperShouldHaltException(
        final String logFilePrefix,
        final String errorMessage,
        final ArchiveManager archiveManager,
        final WebDriver driver
    ) {
        super(errorMessage);
        final String htmlDumpPath = archiveManager.writeHtml(logFilePrefix, driver.getPageSource());

        Logger.errorAlsoSlack(
            String.format(
                "%s\n<%s|Download dumped html on s3>, scraper was facing `%s`.",
                errorMessage,
                archiveManager.getFullUrlOnS3FromFilePathBasedOnOrgDirectory(htmlDumpPath),
                driver.getCurrentUrl()
            )
        );
    }
}
