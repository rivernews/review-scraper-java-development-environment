package com.shaungc.utilities;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import com.shaungc.exceptions.ScraperShouldHaltException;

/**
 * URLValidator
 */
public class RequestAddressValidator {

    static public URL toURL(String urlString) {
        URL validatedUrl = null;
        try {
            validatedUrl = new URL(urlString);
        } catch (MalformedURLException e) {
            throw new ScraperShouldHaltException("You attempted to initialize an url but it is misformated. Your url string is " + urlString);
        }

        return validatedUrl;
    }

    static public URI toURI(String uriString) {
        URI validatedUri = null;
        try {
            validatedUri = new URI(uriString);
        } catch (URISyntaxException e) {
            throw new ScraperShouldHaltException("You attempted to initialize an uri but it is misformated. Your uri string is " + uriString);
        }

        return validatedUri;
    }
}