package com.shaungc.dataTypes;

import java.util.Date;

public class BasicParsedData {
    public String companyLogoUrl;
    public String reviewNumberText;
    public String companyName;

    public String companySizeText;
    public String companyFoundYearText;
    public String companyLocationText;
    public String companyWebsiteUrl;

    public Date scrapedTimestamp;

    public BasicParsedData(String pCompanyLogoUrl, String pReviewNumberText, String pCompanySizeText,
            String pCompanyFoundYearText, String pCompanyLocationText, String pCompanyWebsiteUrl, String pCompanyName) {
        this.companyLogoUrl = pCompanyLogoUrl;
        this.reviewNumberText = pReviewNumberText;
        this.companyName = pCompanyName;

        this.companySizeText = pCompanySizeText;
        this.companyFoundYearText = pCompanyFoundYearText;
        this.companyLocationText = pCompanyLocationText;
        this.companyWebsiteUrl = pCompanyWebsiteUrl;
    }

    public void debugPrintAllFields() {
        System.out.println("companyName " + this.companyName);
        System.out.println("companyLogoUrl " + this.companyLogoUrl);
        System.out.println("reviewNumber " + this.reviewNumberText);

        System.out.println("companySizeText " + this.companySizeText);
        System.out.println("companyFoundYearText " + this.companyFoundYearText);
        System.out.println("companyLocationText " + this.companyLocationText);
        System.out.println("companyWebsiteUrl " + this.companyWebsiteUrl);
    }
}
