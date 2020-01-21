package com.shaungc.dataTypes;

/**
 * ReviewParsedData
 */
public class ReviewParsedData {

    Float overallRating;
    
    public ReviewParsedData(
        Float overallRating
    ) {
        this.overallRating = overallRating;
    }

    public void debug() {
        System.out.println("overallRating: " + this.overallRating);
    }
}