package com.shaungc.utilities;

public enum RedisPubSubChannelName {
    SCRAPER_JOB_CHANNEL("scraperJobChannel");

    private final String string;

    private RedisPubSubChannelName(String string) {
        this.string = string;
    }

    public String getString() {
        return this.string;
    }
}