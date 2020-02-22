package com.shaungc.utilities;

public enum ScraperJobMessageType {
    PREFLIGHT("preflight"),
    PROGRESS("progress"),
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
