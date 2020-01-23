package com.shaungc.dataTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * ReviewData
 */
public class EmplopyeeReviewParsedData {
    public Float overallRating = Float.valueOf(-1);
    public List<EmployeeReviewData> employeeReviewDataList = new ArrayList<EmployeeReviewData>();

    public void debug() {
        System.out.println("overallRating: " + this.overallRating);
        for (EmployeeReviewData employeeReviewData: this.employeeReviewDataList) {
            employeeReviewData.debug();
        }
    }
}