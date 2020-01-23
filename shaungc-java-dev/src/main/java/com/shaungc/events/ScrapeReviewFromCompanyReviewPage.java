package com.shaungc.events;

import java.util.ArrayList;
import java.util.List;

import com.shaungc.dataTypes.EmployeeReviewData;
import com.shaungc.dataTypes.GlassdoorCompanyParsedData;
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
public class ScrapeReviewFromCompanyReviewPage
        extends AScraperEvent<GlassdoorCompanyParsedData, GlassdoorCompanyParsedData> {

    private final WebDriverWait wait;

    public ScrapeReviewFromCompanyReviewPage(final WebDriver driver) {
        super(driver);

        this.wait = new WebDriverWait(this.driver, Configuration.EXPECTED_CONDITION_WAIT_SECOND);
    }

    @Override
    protected List<WebElement> locate() {
        final List<WebElement> locatedElements = new ArrayList<>();

        // confirm is on review page while locating filter button

        // TODO: filter by engineering category
        // WebElement filterButtonElement = wait.until(
        // ExpectedConditions.elementToBeClickable(By.cssSelector("article[id*=MainCol]
        // main div.search > div > button"))
        // );

        // locate sort dropdown list
        final WebElement sortDropdownElement = this.driver.findElement(
                By.cssSelector("body div#PageContent article[id=MainCol] .filterSorts select[name=filterSorts]"));

        // sort by most recent
        sortDropdownElement.click();
        sortDropdownElement.findElement(By.cssSelector("option[value=DATE]")).click();

        // wait for loading sort
        this.waitForReviewPanelLoading();

        // locate review panel
        final WebElement reviewPanelElement = this.wait
                .until(ExpectedConditions.elementToBeClickable(By.cssSelector("article[id*=MainCol] main")));
        locatedElements.add(reviewPanelElement);

        return locatedElements;
    }

    private void waitForReviewPanelLoading() {
        this.wait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.cssSelector("article[id*=MainCol] main > div > div[class*=LoadingIndicator]")));
    }

    @Override
    protected GlassdoorCompanyParsedData parser(final List<WebElement> locatedElements) {
        final WebElement reviewPanelElement = locatedElements.get(0);

        // initialize data pack to store review data
        final GlassdoorCompanyParsedData glassdoorCompanyParsedData = new GlassdoorCompanyParsedData();

        // scrape overall rating value
        final WebElement overallRatingElement = reviewPanelElement.findElement(By.cssSelector("div[class*=ratingNum]"));
        try {
            glassdoorCompanyParsedData.overallRating = Float.parseFloat(overallRatingElement.getText().strip());
        } catch (final NumberFormatException e) {
        }

        // scrape ovwerall review counts
        this.scrapeReviewCount(reviewPanelElement, glassdoorCompanyParsedData);

        // foreach review
        try {
            Integer messageNumberOffset = 0;

            while (true) {
                // pull out review elements
                final List<WebElement> employeeReviewElements = reviewPanelElement
                        .findElements(By.cssSelector("div#ReviewsFeed ol > li"));

                for (final WebElement employeeReviewElement : employeeReviewElements) {
                    final EmployeeReviewData employeeReviewData = new EmployeeReviewData();
                    this.scrapeEmployeeReview(employeeReviewElement, employeeReviewData);

                    glassdoorCompanyParsedData.employeeReviewDataList.add(employeeReviewData);

                    System.out.println("\n\n");
                    employeeReviewData.debug(messageNumberOffset);

                    messageNumberOffset++;
                }

                // click next page
                this.driver
                        .findElement(By.cssSelector("ul[class^=pagination] li[class$=next] a:not([class$=disabled])"))
                        .click();

                this.waitForReviewPanelLoading();
            }
        } catch (final NoSuchElementException e) {
            // next page link grayed out so we are done
        }

        return glassdoorCompanyParsedData;
    }

    private void scrapeReviewCount(WebElement reviewPanelElement,
            GlassdoorCompanyParsedData glassdoorCompanyDataStore) {
        try {
            List<WebElement> countElements = reviewPanelElement
                    .findElements(By.cssSelector("div[class*=ReviewsPageContainer] div.mt:last-child span strong"));

            if (countElements.size() == 2) {
                glassdoorCompanyDataStore.localReviewCount = Integer
                        .parseInt(countElements.get(0).getText().strip().replaceAll("\\D+", ""));

                glassdoorCompanyDataStore.reviewCount = Integer
                        .parseInt(countElements.get(1).getText().strip().replaceAll("\\D+", ""));
            }
        } catch (NoSuchElementException e) {
        }
    }

    private String parseReviewId(final WebElement employeeReviewLiElement) {
        final String idAttributeString = employeeReviewLiElement.getAttribute("id");

        final String employeeReviewId = idAttributeString.split("_")[1];

        if (Configuration.DEBUG) {
            System.out.println("INFO: review id is " + employeeReviewId);
        }

        return employeeReviewId;
    }

    private void scrapeEmployeeReview(final WebElement employeeReviewLiElement,
            final EmployeeReviewData reviewDataStore) {
        reviewDataStore.reviewId = this.parseReviewId(employeeReviewLiElement);
        // TODO: check review id

        // scrape time
        try {
            reviewDataStore.reviewDate = employeeReviewLiElement.findElement(By.cssSelector("time.date"))
                    .getAttribute("datetime").strip();
        } catch (NoSuchElementException e) {
        }

        // scrape comment title
        reviewDataStore.reviewHeaderTitle = employeeReviewLiElement.findElement(By.cssSelector("h2.summary a"))
                .getText().strip();

        // scrape rating
        reviewDataStore.reviewRating = Float.parseFloat(employeeReviewLiElement
                .findElement(By.cssSelector("span.gdRatings  span.rating span.value-title")).getAttribute("title"));

        // scrape position
        reviewDataStore.reviewEmployeePositionText = employeeReviewLiElement
                .findElement(By.cssSelector("div.author span.authorInfo span.authorJobTitle")).getText().strip();

        // TODO: scrape location
        final String reviewEmployeeLocation = "";

        // TODO: scrape text
        // TODO: click on "Show More" if presents
        final ReviewTextData reviewTextData = null;
        this.parseReviewTextData(employeeReviewLiElement, reviewDataStore);

        // scrape helpful
        final Integer helpfulCount = 0;
        // TODO: handle possible comma in text
        try {
            reviewDataStore.helpfulCount = Integer
                    .parseInt(employeeReviewLiElement.findElement(By.cssSelector("div.helpfulReviews.helpfulCount"))
                            .getText().strip().replaceAll("\\D+", ""));
        } catch (final NoSuchElementException e) {
        }

        return;
    }

    private void parseReviewTextData(final WebElement employeeReviewLiElement,
            final EmployeeReviewData reviewDataStore) {
        final List<WebElement> showMoreLinkElements = employeeReviewLiElement
                .findElements(By.cssSelector("div.hreview div.mt span.link"));

        for (final WebElement showMoreLinkElement : showMoreLinkElements) {
            showMoreLinkElement.click();
        }

        // main text
        try {
            reviewDataStore.reviewTextData.mainText = employeeReviewLiElement.findElement(By.cssSelector("p.mainText"))
                    .getText().strip();
        } catch (final Exception e) {
        }

        // pro text
        // TODO: cannot find element
        // reviewDataStore.reviewTextData.proText =
        // employeeReviewLiElement.findElement(By.cssSelector("div.hreview div.mt
        // div[class*=ReviewText]:nth-child(1) p:nth-child(2)")).getText().strip();

        // con text
        // TODO: cannot find element
        // reviewDataStore.reviewTextData.conText =
        // employeeReviewLiElement.findElement(By.cssSelector("div.hreview div.mt
        // div[class*=ReviewText]:nth-child(2) p:nth-child(2)")).getText().strip();

        final List<WebElement> paragraphElements = employeeReviewLiElement
                .findElements(By.cssSelector("div.hreview div.mt p"));

        for (final WebElement paragraphElement : paragraphElements) {
            reviewDataStore.reviewTextData.rawParagraphs.add(paragraphElement.getText().strip());
        }
    }

    @Override
    protected void postAction(final GlassdoorCompanyParsedData parsedData) {
        // TODO Auto-generated method stub
        // parsedData.debug();
        System.out.println("\n\nINFO: Total reviews processed: " + parsedData.employeeReviewDataList.size());
    }
}