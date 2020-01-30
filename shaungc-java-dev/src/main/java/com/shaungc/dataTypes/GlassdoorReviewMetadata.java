package com.shaungc.dataTypes;

import java.util.Date;

import com.shaungc.javadev.Logger;

/**
 * GlassdoorReviewMetadata
 */
public class GlassdoorReviewMetadata {

    public Float overallRating = Float.valueOf(-1);
    public Integer localReviewCount = 0;
    public Integer reviewCount = 0;
    public Date scrapedTimestamp;

    public void debug () {
        Logger.info("overallRating: " + this.overallRating);
        Logger.info("localReviewCount: " + this.localReviewCount);
        Logger.info("reviewCount: " + this.reviewCount);
        Logger.info("scrapedTimestamp: " + this.scrapedTimestamp);
    }
}