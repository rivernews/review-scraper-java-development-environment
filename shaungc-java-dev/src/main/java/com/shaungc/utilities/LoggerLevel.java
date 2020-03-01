package com.shaungc.utilities;

public enum LoggerLevel {
    ERROR(1, "ERROR", "🛑 "),
    WARN(2, "WARN", "🟠 "),
    INFO(3, "INFO", "ℹ️ "),
    DEBUG(4, "DEBUG", "");

    private final Integer verbosenessLevelValue;
    private final String aliasText;
    private final String visualCueEmoji;

    private LoggerLevel(Integer verbosenessLevelValue, String aliasText, String visualCueEmoji) {
        this.verbosenessLevelValue = verbosenessLevelValue;
        this.aliasText = aliasText;
        this.visualCueEmoji = visualCueEmoji;
    }

    public Integer getVerbosenessLevelValue() {
        return this.verbosenessLevelValue;
    }

    public String getAliasText() {
        return this.aliasText;
    }

    public String getVisualCueEmoji() {
        return this.visualCueEmoji;
    }
}
