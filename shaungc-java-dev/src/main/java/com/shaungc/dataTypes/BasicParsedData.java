package com.shaungc.dataTypes;

import java.util.Date;

import com.shaungc.javadev.Logger;

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
        Logger.info("companyName " + this.companyName);
        Logger.info("companyLogoUrl " + this.companyLogoUrl);
        Logger.info("reviewNumber " + this.reviewNumberText);

        Logger.info("companySizeText " + this.companySizeText);
        Logger.info("companyFoundYearText " + this.companyFoundYearText);
        Logger.info("companyLocationText " + this.companyLocationText);
        Logger.info("companyWebsiteUrl " + this.companyWebsiteUrl);
        Logger.info("scrapedTimestamp " + this.scrapedTimestamp);
    }
}
