package com.shaungc.exceptions;

import com.shaungc.dataStorage.ArchiveManager;
import com.shaungc.utilities.Logger;
import org.openqa.selenium.WebDriver;

/**
 * ScraperException
 * We extends from `Exception` instead of `RuntimeException`,
 * since scraper exception is kind of a "flow-control" of this java program,
 * and should not be considered as a "mistake" of the program.
 *
 * You will need to specify "throws ScraperException"
 */
public class ScraperException extends Exception {

    public ScraperException(String errorMessage) {
        super(errorMessage);
        Logger.errorAlsoSlack(errorMessage);
    }

    public ScraperException(String errorMessage, Throwable originalError) {
        super(errorMessage, originalError);
        Logger.errorAlsoSlack(errorMessage + "\nOriginal Error:\n" + originalError.toString());
    }

    public ScraperException(
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
