package com.shaungc.events;

import java.util.ArrayList;
import java.util.List;

import com.shaungc.dataTypes.ReviewParsedData;
import com.shaungc.javadev.Configuration;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;


/**
 * ScrapeReviewFromCompanyReviewPage
 */
public class ScrapeReviewFromCompanyReviewPage extends AScraperEvent<ReviewParsedData, ReviewParsedData> {
    public ScrapeReviewFromCompanyReviewPage(WebDriver driver) {
        super(driver);
    }

    @Override
    protected List<WebElement> locate() {
        List<WebElement> locatedElements = new ArrayList<>();

        // confirm is on review page while locating filter button
        
        WebDriverWait wait = new WebDriverWait(this.driver, Configuration.EXPECTED_CONDITION_WAIT_SECOND);

        WebElement filterButtonElement = wait.until(
            ExpectedConditions.elementToBeClickable(By.cssSelector("article[id*=MainCol] main div.search > div > button"))
        );
        
        // locate sort dropdown list
        WebElement sortDropdownElement = this.driver.findElement(By.cssSelector("body div#PageContent article[id=MainCol] .filterSorts select[name=filterSorts]"));

        // sort by most recent
        sortDropdownElement.click();
        sortDropdownElement.findElement(By.cssSelector("option[value=DATE]")).click();

        // wait for loading sort
        wait.until(
            ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("article[id*=MainCol] main > div > div[class*=LoadingIndicator]"))
        );

        // locate review panel
        WebElement reviewPanelElement = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("article[id*=MainCol] main")));
        locatedElements.add(reviewPanelElement);

        return locatedElements;
    }

    @Override
    protected ReviewParsedData parser(List<WebElement> locatedElements) {
        WebElement reviewPanelElement = locatedElements.get(0);

        // scrape overall rating value
        WebElement overallRatingElement = reviewPanelElement.findElement(By.cssSelector("div[class*=ratingNum]"));
        Float overallRating = Float.valueOf(-1);
        try {
            overallRating = Float.parseFloat(overallRatingElement.getText().trim());
        } catch (NumberFormatException e) {
        }

        // TODO: retrieve reviews

        // foreach review

        // TODO: check review id

        // TODO: scrape comment title
        // TODO: scrape rating
        // TODO: scrape position
        // TODO: scrape location
        // TODO: scrape text
        // TODO: scrape helpful
        // TODO: scrape time

        // TODO: click next page

        // TODO: until next link grayed out

        return new ReviewParsedData(
            overallRating
        );
    }

    @Override
    protected void postAction(ReviewParsedData parsedData) {
        // TODO Auto-generated method stub
        parsedData.debug();
    }
}