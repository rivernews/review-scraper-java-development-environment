package com.shaungc.dataTypes;

import com.shaungc.utilities.Logger;

public class BasicParsedData {
    public String companyOverviewPageUrl;
    public String companyId;
    public String companyLogoUrl;
    public String reviewNumberText;
    public String companyName;

    public String companySizeText;
    public String companyFoundYearText;
    public String companyLocationText;
    public String companyWebsiteUrl;

    public BasicParsedData(
        final String pcompanyOverviewPageUrl,
        final String pCompanyId,
        final String pCompanyLogoUrl,
        final String pReviewNumberText,
        final String pCompanySizeText,
        final String pCompanyFoundYearText,
        final String pCompanyLocationText,
        final String pCompanyWebsiteUrl,
        final String pCompanyName
    ) {
        this.companyOverviewPageUrl = pcompanyOverviewPageUrl;
        this.companyId = pCompanyId;
        this.companyLogoUrl = pCompanyLogoUrl;
        this.reviewNumberText = pReviewNumberText;
        this.companyName = pCompanyName;

        this.companySizeText = pCompanySizeText;
        this.companyFoundYearText = pCompanyFoundYearText;
        this.companyLocationText = pCompanyLocationText;
        this.companyWebsiteUrl = pCompanyWebsiteUrl;
    }

    public void debugPrintAllFields() {
        Logger.debug("companyOverviewPageUrl " + this.companyOverviewPageUrl);
        Logger.debug("companyId " + this.companyId);
        Logger.debug("companyName " + this.companyName);
        Logger.debug("companyLogoUrl " + this.companyLogoUrl);
        Logger.debug("reviewNumber " + this.reviewNumberText);

        Logger.debug("companySizeText " + this.companySizeText);
        Logger.debug("companyFoundYearText " + this.companyFoundYearText);
        Logger.debug("companyLocationText " + this.companyLocationText);
        Logger.debug("companyWebsiteUrl " + this.companyWebsiteUrl);
    }
}
