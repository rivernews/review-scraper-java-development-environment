package com.shaungc.events;

import com.shaungc.events.AScraperEvent;
import com.shaungc.javadev.Configuration;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * JudgeQueryCompanyPageEvent
 *
 * Design flow: 0. (URL queried page) 1. Distinguish "no result", "exactly one"
 * or "multiple match"
 *
 * exact one: exists of class "v2__EIReviewsRatingsStylesV2__ratingNum
 * v2__EIReviewsRatingsStylesV2__large" --> `ratingNum` exactly one in html doc.
 * (if multiple, then don't have `ratingNum` at all) multiple: `eiHdrModule
 * module snug` (a company) > `bigRating`. However if `bigRating` does not exist
 * in a company, means no reviews yet (like
 * https://www.glassdoor.com/Reviews/cmd-reviews-SRCH_KE0,3_IP2.htm > Vitras) no
 * result: no `eiHdrModule` and no `ratingNum`
 *
 */

public class JudgeQueryCompanyPageEvent extends AScraperEvent<Boolean, Boolean> {

    public JudgeQueryCompanyPageEvent(WebDriver driver) {
        super(driver);
    }

    @Override
    protected Boolean parser(List<WebElement> locatedElements) {
        try {
            String pageType = this.driver.findElement(By.cssSelector("#EI-Srch")).getAttribute("data-page-type").strip();
            if (pageType.contentEquals("OVERVIEW")) {
                return true;
            }
            return false;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    @Override
    protected void postAction(Boolean parsedData) {
        this.sideEffect = parsedData;
    }
}
