package com.shaungc.dataTypes;

import com.shaungc.utilities.Logger;
import java.time.Instant;
import java.util.Date;

/**
 * GlassdoorReviewMetadata
 */
public class GlassdoorReviewMetadata {
    public Float overallRating = Float.valueOf(-1);
    public Integer localReviewCount = 0;
    public Integer reviewCount = 0;

    public void debug() {
        Logger.debug("overallRating: " + this.overallRating);
        Logger.debug("localReviewCount: " + this.localReviewCount);
        Logger.debug("reviewCount: " + this.reviewCount);
    }
}
