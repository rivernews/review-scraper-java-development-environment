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

    private final Integer stopPage;
    private final Integer shardIndex;

    public ScraperJobData(
        final String orgId,
        final String orgName,
        final ScraperProgressData scraperProgressData,
        final String nextReviewPageUrl,
        final String scrapeMode,
        final Integer stopPage,
        final Integer shardIndex
    ) {
        this.pubsubChannelName =
            String.format(
                "%s:%s:%s:startAtPage%s",
                RedisPubSubChannelPrefix.SCRAPER_JOB_CHANNEL.getString(),
                orgName.replaceAll("[^0-9a-zA-Z]", "-"),
                scraperProgressData.processedSession,
                scraperProgressData.page
            );
        this.orgId = orgId;
        this.orgName = orgName;
        this.lastProgress = scraperProgressData;
        this.nextReviewPageUrl = nextReviewPageUrl;
        this.scrapeMode = scrapeMode;

        this.stopPage = stopPage;
        this.shardIndex = shardIndex;
    }
}
