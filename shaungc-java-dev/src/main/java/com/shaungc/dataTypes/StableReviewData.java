package com.shaungc.dataTypes;

import com.shaungc.utilities.Logger;

/**
 * StableReviewData
 */
public class StableReviewData {
    public String reviewId = "";
    public String reviewHeaderTitle = "";
    public EmployeeReviewRatingMetrics reviewRatingMetrics = new EmployeeReviewRatingMetrics();
    public String reviewEmployeePositionText = "";
    public String reviewEmployeeLocation = "";
    public EmployeeReviewTextData reviewTextData = new EmployeeReviewTextData();

    public String reviewDate = "";

    public void debug() {
        Logger.debug("reviewId: " + this.reviewId);
        Logger.debug("reviewHeaderTitle: " + this.reviewHeaderTitle);
        this.reviewRatingMetrics.debug();
        Logger.debug("reviewEmployeePositionText: " + this.reviewEmployeePositionText);
        Logger.debug("reviewEmployeeLocation: " + this.reviewEmployeeLocation);
        this.reviewTextData.debug();
        Logger.debug("reviewDate: " + this.reviewDate);
    }
}
