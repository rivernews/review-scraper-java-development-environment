package com.shaungc.dataTypes;

/**
 * ReviewParsedData
 */
public class EmployeeReviewData {

    public String reviewId = "";
    public String reviewTitle = "";
    public Float reviewRating = Float.valueOf(-1);
    public String reviewEmployeePositionText = "";
    public String reviewEmployeeLocation = "";
    public ReviewTextData reviewTextData = new ReviewTextData();
    public Integer helpfulCount = 0;
    public String reviewDate = "";
    
    public EmployeeReviewData() {
        
    }

    public void debug(Integer messageNumber) {
        System.out.println("INFO: =========================== " + messageNumber);
        System.out.println("reviewId: " + this.reviewId);
        System.out.println("reviewTitle: " + this.reviewTitle);
        System.out.println("reviewRating: " + this.reviewRating);
        System.out.println("reviewEmployeePositionText: " + this.reviewEmployeePositionText);
        System.out.println("reviewEmployeeLocation: " + this.reviewEmployeeLocation);
        this.reviewTextData.debug();
        System.out.println("helpfulCount: " + this.helpfulCount);
        System.out.println("reviewDate: " + this.reviewDate);
    }
}