package com.shaungc.dataTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * ReviewData
 */
public class GlassdoorCompanyParsedData {
    public Float overallRating = Float.valueOf(-1);
    public Integer localReviewCount = 0;
    public Integer reviewCount = 0;
    public List<EmployeeReviewData> employeeReviewDataList = new ArrayList<EmployeeReviewData>();

    public void debug() {
        this.debug(0);
    }
    public void debug (Integer numberedMessageOffset) {
        System.out.println("overallRating: " + this.overallRating);
        System.out.println("localReviewCount: " + this.localReviewCount);
        System.out.println("reviewCount: " + this.reviewCount);

        Integer messageCounter = 1;
        for (final EmployeeReviewData employeeReviewData : this.employeeReviewDataList) {
            employeeReviewData.debug(messageCounter + numberedMessageOffset);
            messageCounter++;
        }
    }
}