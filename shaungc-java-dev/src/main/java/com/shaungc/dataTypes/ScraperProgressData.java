package com.shaungc.dataTypes;

/**
 * ScraperProgressData
 */
public class ScraperProgressData {
    private final Integer processed;
    private final Integer wentThrough;
    private final Integer total;
    private final String durationInMilli;
    private final Integer page;
    private final Integer processedSession;

    public ScraperProgressData(
        final Integer processed,
        final Integer wentThrough,
        final Integer total,
        final String durationInMilli,
        final Integer page,
        final Integer processedSession
    ) {
        this.processed = processed;
        this.wentThrough = wentThrough;
        this.total = total;
        this.durationInMilli = durationInMilli;
        this.page = page;
        this.processedSession = processedSession;
    }
}
