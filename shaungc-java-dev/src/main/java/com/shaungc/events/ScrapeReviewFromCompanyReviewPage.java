package com.shaungc.events;

import java.util.ArrayList;
import java.util.List;

import com.shaungc.dataTypes.EmployeeReviewData;
import com.shaungc.dataTypes.EmplopyeeReviewParsedData;
import com.shaungc.dataTypes.ReviewTextData;
import com.shaungc.javadev.Configuration;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;


/**
 * ScrapeReviewFromCompanyReviewPage
 */
public class ScrapeReviewFromCompanyReviewPage extends AScraperEvent<EmplopyeeReviewParsedData, EmplopyeeReviewParsedData> {
    public ScrapeReviewFromCompanyReviewPage(WebDriver driver) {
        super(driver);
    }

    @Override
    protected List<WebElement> locate() {
        List<WebElement> locatedElements = new ArrayList<>();

        // confirm is on review page while locating filter button
        
        WebDriverWait wait = new WebDriverWait(this.driver, Configuration.EXPECTED_CONDITION_WAIT_SECOND);

        // TODO: filter by engineering category
        // WebElement filterButtonElement = wait.until(
        //     ExpectedConditions.elementToBeClickable(By.cssSelector("article[id*=MainCol] main div.search > div > button"))
        // );
        
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
    protected EmplopyeeReviewParsedData parser(List<WebElement> locatedElements) {
        WebElement reviewPanelElement = locatedElements.get(0);

        // initialize data pack to store review data
        EmplopyeeReviewParsedData emplopyeeReviewParsedData = new EmplopyeeReviewParsedData();

        // scrape overall rating value
        WebElement overallRatingElement = reviewPanelElement.findElement(By.cssSelector("div[class*=ratingNum]"));
        try {
            emplopyeeReviewParsedData.overallRating = Float.parseFloat(overallRatingElement.getText().strip());
        } catch (NumberFormatException e) {
        }

        // pull out review element
        List<WebElement> employeeReviewElements = reviewPanelElement.findElements(By.cssSelector("div#ReviewsFeed ol > li"));

        // foreach review
        for (WebElement employeeReviewElement: employeeReviewElements) {
            EmployeeReviewData employeeReviewData = new EmployeeReviewData();
            this.scrapeEmployeeReview(employeeReviewElement, employeeReviewData);

            emplopyeeReviewParsedData.employeeReviewDataList.add(employeeReviewData);

            System.out.println("\n\n");
            employeeReviewData.debug();
        }

        return emplopyeeReviewParsedData;
    }

    private String parseReviewId(WebElement employeeReviewLiElement) {
        final String idAttributeString = employeeReviewLiElement.getAttribute("id");

        final String employeeReviewId = idAttributeString.split("_")[1];

        if (Configuration.DEBUG) {
            System.out.println("INFO: review id is " + employeeReviewId);
        }

        return employeeReviewId;
    }

    private void scrapeEmployeeReview(WebElement employeeReviewLiElement, EmployeeReviewData reviewDataStore) {
        reviewDataStore.reviewId = this.parseReviewId(employeeReviewLiElement);
        // TODO: check review id

        // scrape time
        reviewDataStore.reviewDate = employeeReviewLiElement.findElement(By.cssSelector("time.date")).getAttribute("datetime");

        // scrape comment title
        reviewDataStore.reviewEmployeePositionText = employeeReviewLiElement.findElement(By.cssSelector("h2.summary a")).getText().strip();

        // scrape rating
        reviewDataStore.reviewRating = Float.parseFloat(
            employeeReviewLiElement.findElement(By.cssSelector("span.gdRatings  span.rating span.value-title")).getAttribute("title")
        );

        // scrape position
        reviewDataStore.reviewEmployeePositionText = employeeReviewLiElement.findElement(By.cssSelector("div.author span.authorInfo span.authorJobTitle")).getText().strip();

        // TODO: scrape location
        String reviewEmployeeLocation = "";

        // TODO: scrape text
        // TODO: click on "Show More" if presents
        ReviewTextData reviewTextData = null;
        this.parseReviewTextData(employeeReviewLiElement, reviewDataStore);

        // scrape helpful
        Integer helpfulCount = 0;
        // TODO: handle possible comma in text
        try {
            reviewDataStore.helpfulCount = Integer.parseInt(
                employeeReviewLiElement.findElement(By.cssSelector("div.helpfulReviews.helpfulCount")).getText().strip().replaceAll("\\D+", "")
            );
        } catch (NoSuchElementException e) {
        }
        
        // TODO: click next page

        // TODO: until next link grayed out

        return;
    }

    private void parseReviewTextData(WebElement employeeReviewLiElement, EmployeeReviewData reviewDataStore) {
        final List<WebElement> showMoreLinkElements = employeeReviewLiElement.findElements(By.cssSelector("div.hreview div.mt span.link"));

        for (WebElement showMoreLinkElement: showMoreLinkElements) {
            showMoreLinkElement.click();
        }


        // main text
        try {
            reviewDataStore.reviewTextData.mainText = employeeReviewLiElement.findElement(By.cssSelector("p.mainText")).getText().strip();
        } catch (Exception e) {
        }

        // pro text
        // TODO: cannot find element
        // reviewDataStore.reviewTextData.proText = employeeReviewLiElement.findElement(By.cssSelector("div.hreview div.mt div[class*=ReviewText]:nth-child(1) p:nth-child(2)")).getText().strip();

        // con text
        // TODO: cannot find element
        // reviewDataStore.reviewTextData.conText = employeeReviewLiElement.findElement(By.cssSelector("div.hreview div.mt div[class*=ReviewText]:nth-child(2) p:nth-child(2)")).getText().strip();
        
        List<WebElement> paragraphElements = employeeReviewLiElement.findElements(By.cssSelector("div.hreview div.mt p"));

        for (WebElement paragraphElement: paragraphElements) {
            reviewDataStore.reviewTextData.rawParagraphs.add(
                paragraphElement.getText().strip()
            );
        }
    }

    @Override
    protected void postAction(EmplopyeeReviewParsedData parsedData) {
        // TODO Auto-generated method stub
        // parsedData.debug();
    }
}