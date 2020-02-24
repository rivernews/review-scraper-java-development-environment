package com.shaungc.utilities;

public enum ScraperMode {
    REGULAR("regular"),
    RENEWAL("renewal");

    private final String string;

    private ScraperMode(String string) {
        this.string = string;
    }

    public String getString() {
        return this.string;
    }
}
