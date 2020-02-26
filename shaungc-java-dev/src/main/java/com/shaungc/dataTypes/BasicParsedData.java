package com.shaungc.dataTypes;

import com.shaungc.utilities.Logger;
import java.time.Instant;

public class BasicParsedData {
    public String companyId;
    public String companyLogoUrl;
    public String reviewNumberText;
    public String companyName;

    public String companySizeText;
    public String companyFoundYearText;
    public String companyLocationText;
    public String companyWebsiteUrl;

    public BasicParsedData(
        String pCompanyId,
        String pCompanyLogoUrl,
        String pReviewNumberText,
        String pCompanySizeText,
        String pCompanyFoundYearText,
        String pCompanyLocationText,
        String pCompanyWebsiteUrl,
        String pCompanyName
    ) {
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
