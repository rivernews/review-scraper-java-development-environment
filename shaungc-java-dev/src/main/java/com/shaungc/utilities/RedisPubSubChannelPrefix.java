package com.shaungc.utilities;

public enum RedisPubSubChannelPrefix {
    SCRAPER_JOB_CHANNEL("scraperJobChannel"),
    ADMIN("scraperAdmin");

    private final String string;

    private RedisPubSubChannelPrefix(String string) {
        this.string = string;
    }

    public String getString() {
        return this.string;
    }
}
