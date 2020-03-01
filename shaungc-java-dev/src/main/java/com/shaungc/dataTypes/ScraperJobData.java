package com.shaungc.dataTypes;

/**
 * ScraperJobData
 */
public class ScraperJobData {
    private final String orgId;
    private final String orgName;
    private final ScraperProgressData lastProgress;
    private final String lastReviewPage;
    private final String scrapeMode;

    public ScraperJobData(
        final String orgId,
        final String orgName,
        final ScraperProgressData scraperProgressData,
        final String lastReviewPage,
        final String scrapeMode
    ) {
        this.orgId = orgId;
        this.orgName = orgName;
        this.lastProgress = scraperProgressData;
        this.lastReviewPage = lastReviewPage;
        this.scrapeMode = scrapeMode;
    }
}
