package com.shaungc.dataTypes;

public class BasicParsedData {
    public String companyLogoUrl;
    public String reviewNumberText;

    public String companySizeText;
    public String companyFoundYearText;
    public String companyLocationText;
    public String companyWebsiteUrl;

    public BasicParsedData(String pCompanyLogoUrl, String pReviewNumberText, String pCompanySizeText,
            String pCompanyFoundYearText, String pCompanyLocationText, String pCompanyWebsiteUrl) {
        this.companyLogoUrl = pCompanyLogoUrl;
        this.reviewNumberText = pReviewNumberText;

        this.companySizeText = pCompanySizeText;
        this.companyFoundYearText = pCompanyFoundYearText;
        this.companyLocationText = pCompanyLocationText;
        this.companyWebsiteUrl = pCompanyWebsiteUrl;
    }

    public void debugPrintAllFields() {
        System.out.println("companyLogoUrl " + this.companyLogoUrl);
        System.out.println("reviewNumber " + this.reviewNumberText);

        System.out.println("companySizeText " + this.companySizeText);
        System.out.println("companyFoundYearText " + this.companyFoundYearText);
        System.out.println("companyLocationText " + this.companyLocationText);
        System.out.println("companyWebsiteUrl " + this.companyWebsiteUrl);
    }
}
