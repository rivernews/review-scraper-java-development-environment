package com.shaungc.dataTypes;

import java.util.Date;

import com.shaungc.utilities.Logger;

/**
 * GlassdoorReviewMetadata
 */
public class GlassdoorReviewMetadata {

    public Float overallRating = Float.valueOf(-1);
    public Integer localReviewCount = 0;
    public Integer reviewCount = 0;
    public Date scrapedTimestamp;

    public void debug () {
        Logger.debug("overallRating: " + this.overallRating);
        Logger.debug("localReviewCount: " + this.localReviewCount);
        Logger.debug("reviewCount: " + this.reviewCount);
        Logger.debug("scrapedTimestamp: " + this.scrapedTimestamp);
    }
}