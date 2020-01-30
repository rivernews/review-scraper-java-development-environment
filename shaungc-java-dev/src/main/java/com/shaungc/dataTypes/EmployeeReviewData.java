package com.shaungc.dataTypes;

import java.util.Date;

import com.shaungc.javadev.Logger;


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

    public Date scrapedTimestamp;
    
    public EmployeeReviewData() {
        
    }

    public void debug(Integer messageNumber) {
        final Integer baseOneIndexMessageNumber = messageNumber + 1;
        Logger.info("=========================== " + baseOneIndexMessageNumber);
        Logger.info("reviewId: " + this.reviewId);
        Logger.info("reviewHeaderTitle: " + this.reviewHeaderTitle);
        this.reviewRatingMetrics.debug();
        Logger.info("reviewEmployeePositionText: " + this.reviewEmployeePositionText);
        Logger.info("reviewEmployeeLocation: " + this.reviewEmployeeLocation);
        this.reviewTextData.debug();
        Logger.info("helpfulCount: " + this.helpfulCount);
        Logger.info("reviewDate: " + this.reviewDate);
        Logger.info("scrapedTimestamp: " + this.scrapedTimestamp);
    }
}