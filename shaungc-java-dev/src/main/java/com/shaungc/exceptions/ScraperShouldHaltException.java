package com.shaungc.exceptions;

import com.shaungc.utilities.SlackService;

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
        SlackService.asyncSendMessage(errorMessage);
    }
}
