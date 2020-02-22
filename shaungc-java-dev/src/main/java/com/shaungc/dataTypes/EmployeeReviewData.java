package com.shaungc.dataTypes;

import com.shaungc.utilities.Logger;
import java.util.Date;

/**
 * ReviewParsedData
 */
public class EmployeeReviewData {
    public String reviewId = "";
    public String reviewHeaderTitle = "";
    public EmployeeReviewRatingMetrics reviewRatingMetrics = new EmployeeReviewRatingMetrics();
    public String reviewEmployeePositionText = "";
    public String reviewEmployeeLocation = "";
    public EmployeeReviewTextData reviewTextData = new EmployeeReviewTextData();
    public Integer helpfulCount = 0;
    public String reviewDate = "";

    public EmployeeReviewData() {}

    public void debug(Integer messageNumber) {
        final Integer baseOneIndexMessageNumber = messageNumber + 1;
        Logger.debug("=========================== " + baseOneIndexMessageNumber);
        Logger.debug("reviewId: " + this.reviewId);
        Logger.debug("reviewHeaderTitle: " + this.reviewHeaderTitle);
        this.reviewRatingMetrics.debug();
        Logger.debug("reviewEmployeePositionText: " + this.reviewEmployeePositionText);
        Logger.debug("reviewEmployeeLocation: " + this.reviewEmployeeLocation);
        this.reviewTextData.debug();
        Logger.debug("helpfulCount: " + this.helpfulCount);
        Logger.debug("reviewDate: " + this.reviewDate);
    }
}
