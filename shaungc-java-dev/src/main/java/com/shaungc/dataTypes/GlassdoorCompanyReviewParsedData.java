package com.shaungc.dataTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * ReviewData
 */
public class GlassdoorCompanyReviewParsedData {
    public GlassdoorReviewMetadata reviewMetadata = new GlassdoorReviewMetadata();

    // TODO: remove this if not needed
    // currently commenting out for reducing memory usage
    // public List<EmployeeReviewData> employeeReviewDataList = new ArrayList<EmployeeReviewData>();

    public void debug() {
        this.debug(0);
    }

    public void debug(Integer numberedMessageOffset) {
        this.reviewMetadata.debug();
        // TODO: remove this if reviews[] data not needed
        // Integer messageCounter = 1;
        // for (final EmployeeReviewData employeeReviewData : this.employeeReviewDataList) {
        //     employeeReviewData.debug(messageCounter + numberedMessageOffset);
        //     messageCounter++;
        // }
    }
}
