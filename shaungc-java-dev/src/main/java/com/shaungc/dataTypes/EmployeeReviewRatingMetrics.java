package com.shaungc.dataTypes;

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
        System.out.println("overallRating: " + overallRating);
        System.out.println("workLifeBalanceRating: " + workLifeBalanceRating);
        System.out.println("cultureAndValuesRating: " + cultureAndValuesRating);
        System.out.println("careerOpportunitiesRating: " + careerOpportunitiesRating);
        System.out.println("compensationAndBenefitsRating: " + compensationAndBenefitsRating);
        System.out.println("seniorManagementRating: " + seniorManagementRating);
    }
}