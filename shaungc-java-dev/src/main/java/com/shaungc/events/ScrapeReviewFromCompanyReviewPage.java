package com.shaungc.events;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.shaungc.dataStorage.ArchiveManager;
import com.shaungc.dataTypes.EmployeeReviewData;
import com.shaungc.dataTypes.GlassdoorCompanyReviewParsedData;
import com.shaungc.dataTypes.GlassdoorReviewMetadata;
import com.shaungc.dataTypes.EmployeeReviewTextData;
import com.shaungc.javadev.Configuration;
import com.shaungc.utilities.Logger;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * ScrapeReviewFromCompanyReviewPage
 */
public class ScrapeReviewFromCompanyReviewPage
        extends AScraperEvent<GlassdoorCompanyReviewParsedData, GlassdoorCompanyReviewParsedData> {

    /** element locating resources */
    private final String reviewPanelElementCssSelector = "article[id*=MainCol] main";
    private final String employeeReviewElementsLocalCssSelector = "div#ReviewsFeed ol > li";
    private final String employeeReviewElementsCssSelector = reviewPanelElementCssSelector
            + employeeReviewElementsLocalCssSelector;
    
    public Integer processedReviewsCount = 0;

    public ScrapeReviewFromCompanyReviewPage(final WebDriver driver) {
        super(driver);
    }
    public ScrapeReviewFromCompanyReviewPage(final WebDriver driver, ArchiveManager archiveManager) {
        super(driver, archiveManager);
    }

    @Override
    protected List<WebElement> locate() {
        final List<WebElement> locatedElements = new ArrayList<>();

        // navigate to reviews page
        this.driver.findElement(By.cssSelector("article[id*=WideCol] a.eiCell.reviews")).click();

        // confirm that we are on review page while locating filter button
        WebElement filterButtonElement = wait.until(ExpectedConditions
                .elementToBeClickable(By.cssSelector("article[id*=MainCol] main div.search > div > button")));

        // TODO: filter by engineering category

        // use wait which is based on this.driver to avoid click() interrupted by element structure changed, or "element not attach to page document" error
        // sort by most recent
        final String sortDropdownElementCssSelector = "body div#PageContent article[id=MainCol] .filterSorts select[name=filterSorts]";
        
        // locate sort dropdown list
        this.wait.until(
            ExpectedConditions.elementToBeClickable(
                By.cssSelector(sortDropdownElementCssSelector)
            )
        ).click();
        
        this.wait.until(
            ExpectedConditions.elementToBeClickable(
                By.cssSelector(
                    sortDropdownElementCssSelector + " option[value=DATE]"
                )
            )
        ).click();

        // wait for loading sort
        this.waitForReviewPanelLoading();

        // locate review panel
        final WebElement reviewPanelElement = this.wait
                .until(ExpectedConditions.elementToBeClickable(By.cssSelector(this.reviewPanelElementCssSelector)));
        locatedElements.add(reviewPanelElement);

        return locatedElements;
    }

    private void waitForReviewPanelLoading() {
        this.wait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.cssSelector("article[id*=MainCol] main > div > div[class*=LoadingIndicator]")));
    }

    @Override
    protected GlassdoorCompanyReviewParsedData parser(final List<WebElement> locatedElements) {
        final WebElement reviewPanelElement = locatedElements.get(0);

        // initialize data pack to store review data
        final GlassdoorCompanyReviewParsedData glassdoorCompanyParsedData = new GlassdoorCompanyReviewParsedData();

        // scrape for review metadata
        this.scrapeReviewMetadata(reviewPanelElement, glassdoorCompanyParsedData.reviewMetadata);

        // write out review metadata
        this.archiveManager.writeGlassdoorOrganizationReviewsMetadataAsJson(glassdoorCompanyParsedData.reviewMetadata);

        // foreach review
        Integer processedReviewPages = 0;
        final Integer reviewReportTime = 5;
        final Integer reportingRate = (Integer)(glassdoorCompanyParsedData.reviewMetadata.localReviewCount / reviewReportTime);
        while (true) {
            // pull out review elements
            final List<WebElement> employeeReviewElements = reviewPanelElement
                    .findElements(By.cssSelector(this.employeeReviewElementsLocalCssSelector));

            for (final WebElement employeeReviewElement : employeeReviewElements) {
                final EmployeeReviewData employeeReviewData = new EmployeeReviewData();

                this.scrapeEmployeeReview(employeeReviewElement, employeeReviewData);

                // write out review data
                if (!this.archiveManager.doesGlassdoorOrganizationReviewExist(employeeReviewData.reviewId)) {
                    this.archiveManager.writeGlassdoorOrganizationReviewDataAsJson(employeeReviewData);
                    processedReviewsCount++;
                } else {
                    Logger.infoAlsoSlack("🛑 Review already existed in our archive: " + employeeReviewData.reviewId +
                        "\nAt url: " + this.driver.getCurrentUrl() + 
                        "\nWe will store it in S3 anyway, but please check if it's a duplicated one."
                    );
                    this.archiveManager.writeCollidedGlassdoorOrganizationReviewDataAsJson(employeeReviewData);
                }
                
                // TODO: remove this if not needed, since we write each review to s3 right after we parsed it, so collecting all reviews here seems unecessary
                glassdoorCompanyParsedData.employeeReviewDataList.add(employeeReviewData);

                // send message per 50 reviews (5 page, each around 10 reviews)
                if (this.processedReviewsCount % (reportingRate) == 0) {
                    Logger.infoAlsoSlack(
                        String.format(
                            "%s\nOn this page presents %d elements\nSo far processed %d/%d reviews, keep processing for the next %d reviews ... (processed page count %d)\n",
                            this.driver.getCurrentUrl(),
                            employeeReviewElements.size(),

                            this.processedReviewsCount,
                            glassdoorCompanyParsedData.reviewMetadata.localReviewCount,

                            reportingRate,
                            processedReviewPages
                        )
                    );
                }

                // Logger.info("\n\n");
                // employeeReviewData.debug(processedReviewsCount);
            }

            processedReviewPages++;

            // click next page
            Boolean noNextPageLink = false;
            try {
                this.driver
                        .findElement(By.cssSelector("ul[class^=pagination] li[class$=next] a:not([class$=disabled])"))
                        .click();
            } catch (NoSuchElementException e) {
                noNextPageLink = true;
            }

            if (noNextPageLink) {
                break;
            }

            this.waitForReviewPanelLoading();
        }

        return glassdoorCompanyParsedData;
    }

    private void scrapeReviewMetadata(WebElement reviewPanelElement,
            GlassdoorReviewMetadata glassdoorReviewMetadataStore) {
        // scrape overall rating value
        final WebElement overallRatingElement = reviewPanelElement.findElement(By.cssSelector("div[class*=ratingNum]"));
        try {
            glassdoorReviewMetadataStore.overallRating = Float
                    .parseFloat(overallRatingElement.getText().strip());
        } catch (final NumberFormatException e) {
        }

        // scrape ovwerall review counts
        this.scrapeReviewCount(reviewPanelElement, glassdoorReviewMetadataStore);

        // write out data
        glassdoorReviewMetadataStore.scrapedTimestamp = new Date();
    }

    private void scrapeReviewCount(WebElement reviewPanelElement,
    GlassdoorReviewMetadata glassdoorReviewMetadataStore) {
        try {
            List<WebElement> countElements = reviewPanelElement
                    .findElements(By.cssSelector("div[class*=ReviewsPageContainer] div.mt:last-child span strong"));

            if (countElements.size() == 2) {
                glassdoorReviewMetadataStore.localReviewCount = Integer
                        .parseInt(countElements.get(0).getText().strip().replaceAll("\\D+", ""));

                glassdoorReviewMetadataStore.reviewCount = Integer
                        .parseInt(countElements.get(1).getText().strip().replaceAll("\\D+", ""));
            }
        } catch (NoSuchElementException e) {
        }
    }

    private String parseReviewId(final WebElement employeeReviewLiElement) {
        final String idAttributeString = employeeReviewLiElement.getAttribute("id");

        final String employeeReviewId = idAttributeString.split("_")[1];

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

        // scrape position
        reviewDataStore.reviewEmployeePositionText = employeeReviewLiElement
                .findElement(By.cssSelector("div.author span.authorInfo span.authorJobTitle")).getText().strip();

        // scrape location
        try {
            reviewDataStore.reviewEmployeeLocation = employeeReviewLiElement
                    .findElement(By.cssSelector("div.author span.authorInfo span.authorLocation")).getText().strip();
        } catch (NoSuchElementException e) {
        }

        // TODO: scrape text
        final EmployeeReviewTextData reviewTextData = null;
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

        // scrape rating
        reviewDataStore.reviewRatingMetrics.overallRating = Float.parseFloat(employeeReviewLiElement
                .findElement(By.cssSelector("span.gdRatings span.rating span.value-title")).getAttribute("title"));

        // scrape rating metrics
        try {
            // if has dropdown icon, means that it has rating metrics data
            employeeReviewLiElement.findElement(By.cssSelector("span.gdRatings i.subRatingsDrop"));
            this.parseReviewRatingMetrics(employeeReviewLiElement, reviewDataStore);
        } catch (NoSuchElementException e) {
        }

        reviewDataStore.scrapedTimestamp = new Date();

        return;
    }

    private void parseReviewRatingMetrics(final WebElement employeeReviewLiElement,
            final EmployeeReviewData reviewDataStore) {

        // pre-processing - show rating metric element (the hover-dropdown tooltips that
        // displays subratings in each review are hidden at first)
        WebElement ratingMetricsElement = this.showRatingMetricsElement(reviewDataStore.reviewId);

        // scrape work life balance rating
        try {
            reviewDataStore.reviewRatingMetrics.workLifeBalanceRating = Float.parseFloat(
                    ratingMetricsElement.findElement(By.cssSelector("ul li:nth-child(1) span.gdBars.gdRatings"))
                            .getAttribute("title").strip());
        } catch (NoSuchElementException e) {
            if (Configuration.DEBUG) {
                Logger.info("WARN: cannot scrape rating metrics - work & life balance");
            }
        }

        // culture and values rating
        try {
            reviewDataStore.reviewRatingMetrics.cultureAndValuesRating = Float.parseFloat(
                    ratingMetricsElement.findElement(By.cssSelector("ul li:nth-child(2) span.gdBars.gdRatings"))
                            .getAttribute("title").strip());
        } catch (NoSuchElementException e) {
            if (Configuration.DEBUG) {
                Logger.info("WARN: cannot scrape rating metrics - culture & values");
            }
        }

        // career opportunities rating
        try {
            reviewDataStore.reviewRatingMetrics.careerOpportunitiesRating = Float.parseFloat(
                    ratingMetricsElement.findElement(By.cssSelector("ul li:nth-child(3) span.gdBars.gdRatings"))
                            .getAttribute("title").strip());
        } catch (NoSuchElementException e) {
            if (Configuration.DEBUG) {
                Logger.info("WARN: cannot scrape rating metrics - career opportunities");
            }
        }

        // compensation and benefits rating
        try {
            reviewDataStore.reviewRatingMetrics.compensationAndBenefitsRating = Float.parseFloat(
                    ratingMetricsElement.findElement(By.cssSelector("ul li:nth-child(4) span.gdBars.gdRatings"))
                            .getAttribute("title").strip());
        } catch (NoSuchElementException e) {
            if (Configuration.DEBUG) {
                Logger.info("WARN: cannot scrape rating metrics - compensation & benefits");
            }
        }

        // senior management rating
        try {
            reviewDataStore.reviewRatingMetrics.seniorManagementRating = Float.parseFloat(
                    ratingMetricsElement.findElement(By.cssSelector("ul li:nth-child(5) span.gdBars.gdRatings"))
                            .getAttribute("title").strip());
        } catch (NoSuchElementException e) {
            if (Configuration.DEBUG) {
                Logger.info("WARN: cannot scrape rating metrics - senior management");
            }
        }

        // hide so that it won't overlap other elements, which can cause click() on
        // other elements not working
        this.hideRatingMetricsElement(reviewDataStore.reviewId);
    }

    private String getRatingMetricsElementCssSelector(String reviewId) {
        // example resulting css selector:
        // "article[id*=MainCol]
        // main div#ReviewsFeed ol > li[id$='reviewId']
        // div.mt span.gdRatings div.subRatings"
        return this.reviewPanelElementCssSelector
                + String.format(" %s[id$='%s']", this.employeeReviewElementsLocalCssSelector, reviewId)
                + " div.mt span.gdRatings div.subRatings";
    }

    private String getRatingMetricsElementDisplayJavascriptCommand(String reviewId, String styleDisplayString) {
        return String.format("const metricElements = document.querySelectorAll(\"%1$s\"); %2$s;",

                this.getRatingMetricsElementCssSelector(reviewId),

                String.format("for (let metricElement of metricElements) { metricElement.style.display = \"%s\"; }",
                        styleDisplayString));
    }

    private WebElement changeDisplayRatingMetricsElement(String reviewId, String styleDisplayString) {
        WebElement ratingMetricsElement = null;

        JavascriptExecutor javascriptExecutor = (JavascriptExecutor) this.driver;

        final String javascriptCommand = this.getRatingMetricsElementDisplayJavascriptCommand(reviewId,
                styleDisplayString);

        if (Configuration.DEBUG) {
            Logger.info("About to execute javascript: rating metric elements: change display style to "
                    + styleDisplayString);

            Logger.info(String.format("Javascript command:\n%s", javascriptCommand));
        }
        javascriptExecutor.executeScript(javascriptCommand);
        if (Configuration.DEBUG) {
            Logger.info("Finished executing javascript.");
        }

        // verify changes applied in UI
        if (styleDisplayString != "none") {
            ratingMetricsElement = this.wait.until(ExpectedConditions
                    .visibilityOfElementLocated(By.cssSelector(this.getRatingMetricsElementCssSelector(reviewId))));
        } else {
            this.wait.until(ExpectedConditions
                    .invisibilityOfElementLocated(By.cssSelector(this.getRatingMetricsElementCssSelector(reviewId))));
        }

        if (Configuration.DEBUG) {
            Logger.info("Confirmed UI applied.");
        }

        return ratingMetricsElement;
    }

    private WebElement showRatingMetricsElement(String reviewId) {
        return this.changeDisplayRatingMetricsElement(reviewId, "block");
    }

    private WebElement hideRatingMetricsElement(String reviewId) {
        return this.changeDisplayRatingMetricsElement(reviewId, "none");
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
    protected void postAction(final GlassdoorCompanyReviewParsedData parsedData) {
        this.sideEffect = parsedData;
        Logger.info("Total reviews processed: " + parsedData.employeeReviewDataList.size());
    }
}