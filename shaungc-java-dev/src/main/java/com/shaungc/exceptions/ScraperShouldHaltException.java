package com.shaungc.exceptions;

import com.shaungc.utilities.SlackService;

/**
 * ScraperShouldHaltException
 */
public class ScraperShouldHaltException extends RuntimeException {
    public ScraperShouldHaltException(String errorMessage) {
        super(errorMessage);
        SlackService.asyncSendMessage(errorMessage);
    }
    
}