package com.shaungc.dataTypes;


import com.shaungc.utilities.Logger;


/**
 * EmployeeReviewRatingMetrics
 */
public class EmployeeReviewRatingMetrics {

    public Float overallRating = Float.valueOf(-1);
    public Float workLifeBalanceRating = Float.valueOf(-1);
    public Float cultureAndValuesRating = Float.valueOf(-1);
    public Float careerOpportunitiesRating = Float.valueOf(-1);
    public Float compensationAndBenefitsRating = Float.valueOf(-1);
    public Float seniorManagementRating = Float.valueOf(-1);

    public EmployeeReviewRatingMetrics(
        Float overallRating,
        Float workLifeBalanceRating,
        Float cultureAndValuesRating,
        Float careerOpportunitiesRating,
        Float compensationAndBenefitsRating,
        Float seniorManagementRating
    ) {
        this.overallRating = overallRating;
        this.workLifeBalanceRating = workLifeBalanceRating;
        this.cultureAndValuesRating = cultureAndValuesRating;
        this.careerOpportunitiesRating = careerOpportunitiesRating;
        this.compensationAndBenefitsRating = compensationAndBenefitsRating;
        this.seniorManagementRating = seniorManagementRating;
    }

    public EmployeeReviewRatingMetrics() {}

    public void debug() {
        Logger.info("overallRating: " + overallRating);
        Logger.info("workLifeBalanceRating: " + workLifeBalanceRating);
        Logger.info("cultureAndValuesRating: " + cultureAndValuesRating);
        Logger.info("careerOpportunitiesRating: " + careerOpportunitiesRating);
        Logger.info("compensationAndBenefitsRating: " + compensationAndBenefitsRating);
        Logger.info("seniorManagementRating: " + seniorManagementRating);
    }
}