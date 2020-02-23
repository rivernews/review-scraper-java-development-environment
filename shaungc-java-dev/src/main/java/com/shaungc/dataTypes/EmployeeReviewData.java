package com.shaungc.dataTypes;

import com.shaungc.utilities.Logger;

/**
 * ReviewParsedData
 */
public class EmployeeReviewData {
    public StableReviewData stableReviewData;
    public VaryingReviewData varyingReviewData;

    public void debug(Integer messageNumber) {
        final Integer baseOneIndexMessageNumber = messageNumber + 1;
        Logger.debug("=========================== " + baseOneIndexMessageNumber);
        this.stableReviewData.debug();
        this.varyingReviewData.debug();
    }
}
