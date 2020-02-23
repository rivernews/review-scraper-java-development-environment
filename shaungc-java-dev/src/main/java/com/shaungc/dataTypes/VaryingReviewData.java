package com.shaungc.dataTypes;

import com.shaungc.utilities.Logger;

/**
 * VaryingReviewData
 */
public class VaryingReviewData {
    public Integer helpfulCount = 0;
    public Boolean featured = false;

    public void debug() {
        Logger.debug("helpfulCount: " + this.helpfulCount);
        Logger.debug("featured: " + this.featured);
    }
}
