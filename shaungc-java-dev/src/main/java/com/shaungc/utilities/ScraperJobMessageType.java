package com.shaungc.utilities;

public enum ScraperJobMessageType {
    PREFLIGHT("preflight"),
    PROGRESS("progress"),

    // scraper wants to do a cross-session job
    CROSS("cross"),

    FINISH("finish"),
    ERROR("error");

    private final String string;

    private ScraperJobMessageType(String string) {
        this.string = string;
    }

    public String getString() {
        return this.string;
    }
}
