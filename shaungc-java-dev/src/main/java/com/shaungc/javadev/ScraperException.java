package com.shaungc.javadev;


/**
 * ScraperException
 * We extends from `Exception` instead of `RuntimeException`, 
 * since scraper exception is kind of a "flow-control" of this java program,
 * and should not be considered as a "mistake" of the program
 */
public class ScraperException extends Exception {

    public ScraperException(String errorMessage) {
        super(errorMessage);
    }
    public ScraperException(String errorMessage, Throwable originalError) {
        super(errorMessage, originalError);
    }
}