package com.shaungc.dataTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * ReviewData
 */
public class GlassdoorCompanyReviewParsedData {
    public GlassdoorReviewMetadata reviewMetadata = new GlassdoorReviewMetadata();
    public List<EmployeeReviewData> employeeReviewDataList = new ArrayList<EmployeeReviewData>();

    public void debug() {
        this.debug(0);
    }

    public void debug(Integer numberedMessageOffset) {
        this.reviewMetadata.debug();

        Integer messageCounter = 1;
        for (final EmployeeReviewData employeeReviewData : this.employeeReviewDataList) {
            employeeReviewData.debug(messageCounter + numberedMessageOffset);
            messageCounter++;
        }
    }
}
