package com.shaungc.dataTypes;

import com.shaungc.utilities.Logger;

/**
 * GlassdoorReviewMetadata
 */
public class GlassdoorReviewMetadata {
    public String reviewPageUrl = "";
    public Float overallRating = Float.valueOf(-1);
    public Integer localReviewCount = 0;
    public Integer globalReviewCount = 0;

    public void debug() {
        Logger.debug("reviewPageUrl: " + this.reviewPageUrl);
        Logger.debug("overallRating: " + this.overallRating);
        Logger.debug("localReviewCount: " + this.localReviewCount);
        Logger.debug("globalReviewCount: " + this.globalReviewCount);
    }
}
