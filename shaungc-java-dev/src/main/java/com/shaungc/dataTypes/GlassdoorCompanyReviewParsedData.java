package com.shaungc.dataTypes;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.shaungc.javadev.Logger;


/**
 * ReviewData
 */
public class GlassdoorCompanyReviewParsedData {
    public Float overallRating = Float.valueOf(-1);
    public Integer localReviewCount = 0;
    public Integer reviewCount = 0;
    public List<EmployeeReviewData> employeeReviewDataList = new ArrayList<EmployeeReviewData>();

    public Date scrapedTimestamp;

    public void debug() {
        this.debug(0);
    }
    public void debug (Integer numberedMessageOffset) {
        Logger.info("overallRating: " + this.overallRating);
        Logger.info("localReviewCount: " + this.localReviewCount);
        Logger.info("reviewCount: " + this.reviewCount);
        Logger.info("scrapedTimestamp: " + this.scrapedTimestamp);

        Integer messageCounter = 1;
        for (final EmployeeReviewData employeeReviewData : this.employeeReviewDataList) {
            employeeReviewData.debug(messageCounter + numberedMessageOffset);
            messageCounter++;
        }
    }
}