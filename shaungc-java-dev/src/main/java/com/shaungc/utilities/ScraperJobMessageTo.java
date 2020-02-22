package com.shaungc.utilities;

public enum ScraperJobMessageTo {
    SLACK_MD_SVC("slackMiddlewareService"),
    SCRAPER("scraper");

    private final String string;

    private ScraperJobMessageTo(String string) {
        this.string = string;
    }

    public String getString() {
        return this.string;
    }
}
