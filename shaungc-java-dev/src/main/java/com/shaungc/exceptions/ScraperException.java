package com.shaungc.exceptions;

import com.shaungc.utilities.Logger;

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
}
