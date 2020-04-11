package com.shaungc.dataTypes;

import com.shaungc.utilities.RedisPubSubChannelPrefix;

/**
 * ScraperJobData
 */
public class ScraperJobData {
    private final String pubsubChannelName;
    private final String orgId;
    private final String orgName;
    private final ScraperProgressData lastProgress;
    private final String nextReviewPageUrl;
    private final String scrapeMode;

    public ScraperJobData(
        final String orgId,
        final String orgName,
        final ScraperProgressData scraperProgressData,
        final String nextReviewPageUrl,
        final String scrapeMode
    ) {
        this.pubsubChannelName =
            String.format(
                "%s:%s:%s:startAtPage%s",
                RedisPubSubChannelPrefix.SCRAPER_JOB_CHANNEL.getString(),
                orgName,
                scraperProgressData.processedSession,
                scraperProgressData.page
            );
        this.orgId = orgId;
        this.orgName = orgName;
        this.lastProgress = scraperProgressData;
        this.nextReviewPageUrl = nextReviewPageUrl;
        this.scrapeMode = scrapeMode;
    }
}
