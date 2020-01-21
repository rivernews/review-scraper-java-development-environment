package com.shaungc.events;

import java.util.List;

import com.shaungc.events.AScraperEvent;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * JudgeQueryCompanyPageEvent
 * 
 * Design flow:
 * 0. (URL queried page)
 * 1. Distinguish "no result", "exactly one" or "multiple match"
 *  
 *  exact one: exists of class "v2__EIReviewsRatingsStylesV2__ratingNum v2__EIReviewsRatingsStylesV2__large"  --> `ratingNum` exactly one in html doc. (if multiple, then don't have `ratingNum` at all)
 *  multiple: `eiHdrModule module snug` (a company) > `bigRating`. However if `bigRating` does not exist in a company, means no reviews yet (like https://www.glassdoor.com/Reviews/cmd-reviews-SRCH_KE0,3_IP2.htm > Vitras)
 *  no result: no `eiHdrModule` and no `ratingNum`
 *  
 */


public class JudgeQueryCompanyPageEvent extends AScraperEvent<List<WebElement>, Boolean> {
    public JudgeQueryCompanyPageEvent(RemoteWebDriver passedInRemoteWebDriver) {
        super(passedInRemoteWebDriver);
    }

    @Override
    protected List<WebElement> parser(List<WebElement> locatedElements) {
        return locatedElements;
    }

    @Override
    protected void postAction(List<WebElement> parsedData) {
        String resultPageUrl = this.remoteWebDriver.getCurrentUrl();

        if (resultPageUrl.contains("/Overview/")) {
            this.sideEffect = true;
        } else {
            this.sideEffect = false;
        }
    }
}
