package com.shaungc.exceptions;

import com.shaungc.utilities.SlackService;

/**
 * ScraperException
 * We extends from `Exception` instead of `RuntimeException`, 
 * since scraper exception is kind of a "flow-control" of this java program,
 * and should not be considered as a "mistake" of the program
 */
public class ScraperException extends Exception {

    public ScraperException(String errorMessage) {
        super(errorMessage);
        SlackService.asyncSendMessage(errorMessage);
    }
    public ScraperException(String errorMessage, Throwable originalError) {
        super(errorMessage, originalError);
        SlackService.asyncSendMessage(errorMessage + "\nOriginal Error:\n" + originalError.toString());
    }
}