package com.shaungc.events;

import com.shaungc.dataStorage.ArchiveManager;
import com.shaungc.dataTypes.BasicParsedData;
import com.shaungc.dataTypes.EmployeeReviewData;
import com.shaungc.dataTypes.EmployeeReviewTextData;
import com.shaungc.dataTypes.GlassdoorCompanyReviewParsedData;
import com.shaungc.dataTypes.GlassdoorReviewMetadata;
import com.shaungc.exceptions.ScraperException;
import com.shaungc.exceptions.ScraperShouldHaltException;
import com.shaungc.javadev.Configuration;
import com.shaungc.utilities.Logger;
import com.shaungc.utilities.LoggerLevel;
import com.shaungc.utilities.PubSubSubscription;
import com.shaungc.utilities.ScraperJobMessageTo;
import com.shaungc.utilities.ScraperJobMessageType;
import com.shaungc.utilities.ScraperMode;
import com.shaungc.utilities.Timer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * ScrapeReviewFromCompanyReviewPage
 */
public class ScrapeReviewFromCompanyReviewPage extends AScraperEvent<GlassdoorCompanyReviewParsedData, GlassdoorCompanyReviewParsedData> {
    /** previous session data */

    final BasicParsedData orgMetadata;
    private final String orgNameSlackMessagePrefix;

    /** session utilities */

    private final Timer scraperSessionTimer;
    final PubSubSubscription pubSubSubscription;

    /** element locating resources */

    private final String reviewPanelElementCssSelector = "article[id*=MainCol] main";
    private final String employeeReviewElementsLocalCssSelector = "div#ReviewsFeed ol > li";

    /** scraper session metadata */

    public Integer processedReviewsCount = Configuration.TEST_COMPANY_LAST_PROGRESS_PROCESSED;
    public Integer wentThroughReviewsCount = Configuration.TEST_COMPANY_LAST_PROGRESS_WENTTHROUGH;
    // if scraper mode == regular, will obtain when scraping review meta
    // else if mode == renewal, will obtain from env var (Configuration)
    public Integer localReviewCount = Configuration.TEST_COMPANY_LAST_PROGRESS_TOTAL;
    public Integer processedReviewPages = Configuration.TEST_COMPANY_LAST_PROGRESS_PAGE;

    /** expose other data for external use */

    public Boolean doesCollidedReviewExist = false;
    public Boolean isFinalSession = false;

    /**
     * For regular scraper mode
     */
    public ScrapeReviewFromCompanyReviewPage(
        final WebDriver driver,
        final PubSubSubscription pubSubSubscription,
        final ArchiveManager archiveManager,
        final Timer scraperSessionTimer,
        final BasicParsedData orgMetadata,
        final String orgNameSlackMessagePrefix
    ) {
        super(driver, archiveManager);
        this.pubSubSubscription = pubSubSubscription;
        this.scraperSessionTimer = scraperSessionTimer;
        this.orgMetadata = orgMetadata;

        this.orgNameSlackMessagePrefix = orgNameSlackMessagePrefix;
    }

    /**
     * For renewal scraper mode
     */
    public ScrapeReviewFromCompanyReviewPage(
        final WebDriver driver,
        final PubSubSubscription pubSubSubscription,
        final ArchiveManager archiveManager,
        final Timer scraperSessionTimer,
        final String orgNameSlackMessagePrefix
    ) {
        super(driver, archiveManager);
        this.pubSubSubscription = pubSubSubscription;
        this.scraperSessionTimer = scraperSessionTimer;
        this.orgMetadata = null;

        this.orgNameSlackMessagePrefix = orgNameSlackMessagePrefix;
    }

    @Override
    protected List<WebElement> locate() {
        final List<WebElement> locatedElements = new ArrayList<>();

        // navigate to reviews page
        if (Configuration.SCRAPER_MODE.equals(ScraperMode.RENEWAL.getString())) {
            this.driver.navigate().to(Configuration.TEST_COMPANY_LAST_REVIEW_PAGE_URL);
        } else {
            this.driver.findElement(By.cssSelector("article[id*=WideCol] a.eiCell.reviews")).click();
        }

        // confirm that we are on review page while locating filter button
        final WebElement filterButtonElement = wait.until(
            ExpectedConditions.elementToBeClickable(By.cssSelector("article[id*=MainCol] main div.search > div > button"))
        );

        // TODO: filter by engineering category

        // TODO: remove sort if not needed - especially when we will scrape all reviews anyway, and the ordering may not matter. This is also to scrape "featured flag", which is only displayed only in popular ordering
        // use wait which is based on this.driver to avoid click() interrupted by
        // element structure changed, or "element not attach to page document" error
        // sort by most recent
        // final String sortDropdownElementCssSelector = "body div#PageContent article[id=MainCol] .filterSorts select[name=filterSorts]";

        // // locate sort dropdown list
        // this.wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(sortDropdownElementCssSelector))).click();

        // this.wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(sortDropdownElementCssSelector + " option[value=DATE]")))
        //     .click();

        // // wait for loading sort
        // this.waitForReviewPanelLoading();

        // locate review panel
        final WebElement reviewPanelElement =
            this.wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(this.reviewPanelElementCssSelector)));
        locatedElements.add(reviewPanelElement);

        return locatedElements;
    }

    private void waitForReviewPanelLoading() {
        this.wait.until(
                ExpectedConditions.invisibilityOfElementLocated(
                    By.cssSelector("article[id*=MainCol] main > div > div[class*=LoadingIndicator]")
                )
            );
    }

    @Override
    protected GlassdoorCompanyReviewParsedData parser(final List<WebElement> locatedElements) throws ScraperException {
        final WebElement reviewPanelElement = locatedElements.get(0);

        // initialize data pack to store review data
        final GlassdoorCompanyReviewParsedData glassdoorCompanyParsedData = new GlassdoorCompanyReviewParsedData();

        if (Configuration.SCRAPER_MODE.equals(ScraperMode.REGULAR.getString())) {
            // Scrape Review Metadata

            // scrape for review metadata
            this.scrapeReviewMetadata(reviewPanelElement, glassdoorCompanyParsedData.reviewMetadata);

            // write out review metadata
            this.archiveManager.writeGlassdoorOrganizationReviewsMetadataAsJson(glassdoorCompanyParsedData.reviewMetadata);

            this.localReviewCount = glassdoorCompanyParsedData.reviewMetadata.localReviewCount;
        }

        // foreach review
        final Integer reviewReportTime = 5;
        final Integer reportingRate = (Integer) (this.localReviewCount / reviewReportTime);
        final Timer progressReportingTimer = new Timer(Duration.ofSeconds(5));
        while (true) {
            // pull out review elements
            final List<WebElement> employeeReviewElements = reviewPanelElement.findElements(
                By.cssSelector(this.employeeReviewElementsLocalCssSelector)
            );

            for (final WebElement employeeReviewElement : employeeReviewElements) {
                final EmployeeReviewData employeeReviewData = new EmployeeReviewData();

                this.scrapeEmployeeReview(employeeReviewElement, employeeReviewData);

                this.wentThroughReviewsCount++;

                if (this.archiveManager.writeGlassdoorOrganizationReviewDataAsJson(employeeReviewData)) {
                    this.processedReviewsCount++;
                }

                if (progressReportingTimer.doesReachCountdownDuration()) {
                    this.pubSubSubscription.publish(
                            String.format(
                                "%s:%s:{\"processed\":%d,\"wentThrough\":%d,\"total\":%d,\"elapsedTimeString\":\"%s\"}",
                                ScraperJobMessageType.PROGRESS.getString(),
                                ScraperJobMessageTo.SLACK_MD_SVC.getString(),
                                this.processedReviewsCount,
                                this.wentThroughReviewsCount,
                                this.localReviewCount,
                                this.scraperSessionTimer.captureOverallElapseDurationString()
                            )
                        );
                    progressReportingTimer.restart();
                }

                // write out review data
                // final String md5ExistenceTest =
                // this.archiveManager.doesGlassdoorOrganizationReviewExist(employeeReviewData.stableReviewData.reviewId);
                // if (null == md5ExistenceTest) {
                // Logger.info("Review hasn't existed yet, let's write to bucket.");
                // this.archiveManager.writeGlassdoorOrganizationReviewDataAsJson(employeeReviewData);
                // this.processedReviewsCount++;
                // } else {
                // if (md5ExistenceTest.isEmpty()) {
                // // no md5 information; just follow collision strategy

                // this.doesCollidedReviewExist = true;

                // if (Configuration.REVIEW_COLLISION_STRATEGY ==
                // ReviewCollisionStrategy.ALWAYS_WRITE.getValue()) {
                // this.archiveManager.writeCollidedGlassdoorOrganizationReviewDataAsJson(employeeReviewData);
                // Logger.warn(
                // "Review already existed in our archive; w/o md5 cannot safely skip: " +
                // employeeReviewData.stableReviewData.reviewId +
                // "\nAt url: " +
                // this.driver.getCurrentUrl() +
                // "\nYou configured to store collision in S3 anyway and move on, but please
                // check if it's a duplicated one in s3 by prefix `collision.`."
                // );
                // this.processedReviewsCount++;
                // } else if (Configuration.REVIEW_COLLISION_STRATEGY ==
                // ReviewCollisionStrategy.SKIP.getValue()) {
                // // do nothing
                // Logger.warn(
                // "Skipping review. You configured to skip any review data collision. Review id
                // is " +
                // employeeReviewData.stableReviewData.reviewId +
                // " at url " +
                // this.driver.getCurrentUrl()
                // );
                // } else if (Configuration.REVIEW_COLLISION_STRATEGY ==
                // ReviewCollisionStrategy.ABORT.getValue()) {
                // // end review scraper task right
                // Logger.warnAlsoSlack(
                // this.orgNameSlackMessagePrefix +
                // "Aborting scraper. You configured to abort upon any review collision.
                // Collided review id is " +
                // employeeReviewData.stableReviewData.reviewId +
                // " at url " +
                // this.driver.getCurrentUrl()
                // );
                // return glassdoorCompanyParsedData;
                // } else if (Configuration.REVIEW_COLLISION_STRATEGY ==
                // ReviewCollisionStrategy.OVERWRITE.getValue()) {
                // Logger.warn(
                // "Overwriting. You configured to overwrite regardless of collision. Bare in
                // mind of possible data lost due to lack of md5 metadata."
                // );
                // this.archiveManager.writeGlassdoorOrganizationReviewDataAsJson(employeeReviewData);
                // this.processedReviewsCount++;
                // } else {
                // throw new ScraperException(
                // this.orgNameSlackMessagePrefix +
                // "REVIEW_COLLISION_STRATEGY is misconfigured: " +
                // Configuration.REVIEW_COLLISION_STRATEGY
                // );
                // }
                // } else {
                // // compare md5 info; if identical then skip; otherwise store and report
                // collision
                // if (S3Service.toMD5Base64String(employeeReviewData).equals(md5ExistenceTest))
                // {
                // // identical, so just safely skip & do nothing
                // Logger.info(
                // "Identical review data found (md5 verified), safely skipping review " +
                // employeeReviewData.stableReviewData.reviewId
                // );
                // } else {
                // // review id is same but data is different
                // // need to raise attention to this case
                // // and definately store the collided review
                // // will not follow the global collision strategy in this case
                // final String fullPath =
                // this.archiveManager.writeCollidedGlassdoorOrganizationReviewDataAsJson(employeeReviewData);

                // Logger.errorAlsoSlack(
                // this.orgNameSlackMessagePrefix +
                // "Collided with existing review id but their contents are different. You
                // should investigate into this. Will now abort scraper. Collided review stored
                // in s3 at `" +
                // fullPath +
                // "`" +
                // "\nReview on S3 MD5: `" +
                // md5ExistenceTest +
                // "`" +
                // "\nReview on scraper MD5: `" +
                // S3Service.toMD5Base64String(employeeReviewData) +
                // "`"
                // );

                // this.doesCollidedReviewExist = true;

                // this.processedReviewsCount++;

                // throw new ScraperException(
                // "There's a data integrity concern and we need to abort, please refer to error
                // log or slack message."
                // );
                // }
                // }
                // }

                // TODO: remove this if not needed, since we write each review to s3 right after
                // we parsed it, so collecting all reviews here seems unecessary
                glassdoorCompanyParsedData.employeeReviewDataList.add(employeeReviewData);

                // send message per 50 reviews (5 page, each around 10 reviews)
                if (this.wentThroughReviewsCount % (reportingRate) == 0) {
                    final String elapsedTimeString = this.scraperSessionTimer != null
                        ? this.scraperSessionTimer.captureOverallElapseDurationString()
                        : "";
                    Logger.infoAlsoSlack(
                        String.format(
                            "%s%s <%s|Page %d> presents %d elements, processed/wentThrough/total %d/%d/%d reviews, keep processing for the next %d reviews ...\n%s\n",
                            this.orgNameSlackMessagePrefix,
                            elapsedTimeString != "" ? "(" + elapsedTimeString + ") " : "",
                            this.driver.getCurrentUrl(),
                            // + 1 to get current page number
                            this.processedReviewPages + 1,
                            employeeReviewElements.size(),
                            this.processedReviewsCount,
                            this.wentThroughReviewsCount,
                            this.localReviewCount,
                            reportingRate,
                            this.doesCollidedReviewExist ? "🟠 Some reviews collided previously" : ""
                        )
                    );
                }

                if (Configuration.LOGGER_LEVEL >= LoggerLevel.DEBUG.getVerbosenessLevelValue()) {
                    employeeReviewData.debug(this.wentThroughReviewsCount);
                }
            }

            this.processedReviewPages++;

            // click next page
            Boolean noNextPageLink = false;
            try {
                this.driver.findElement(By.cssSelector("ul[class^=pagination] li[class$=next] a:not([class$=disabled])")).click();
            } catch (final NoSuchElementException e) {
                noNextPageLink = true;
            }

            if (noNextPageLink) {
                Logger.info("No next page link available, ready to wrap up scraper session.");
                this.isFinalSession = true;
                break;
            }

            Logger.info("Found next page link, going to continue...");

            this.waitForReviewPanelLoading();

            // check if approaching travis build limit
            // if so, stop session and try to schedule a cross-session job instead
            if (this.scraperSessionTimer.doesReachCountdownDuration()) {
                // stop current scraper session
                return glassdoorCompanyParsedData;
            }
        }

        return glassdoorCompanyParsedData;
    }

    private void scrapeReviewMetadata(final WebElement reviewPanelElement, final GlassdoorReviewMetadata glassdoorReviewMetadataStore) {
        // scrape overall rating value
        final WebElement overallRatingElement = reviewPanelElement.findElement(By.cssSelector("div[class*=ratingNum]"));
        try {
            glassdoorReviewMetadataStore.overallRating = Float.parseFloat(overallRatingElement.getText().strip());
        } catch (final NumberFormatException e) {}

        // scrape ovwerall review counts
        this.scrapeReviewCount(reviewPanelElement, glassdoorReviewMetadataStore);
    }

    private void scrapeReviewCount(final WebElement reviewPanelElement, final GlassdoorReviewMetadata glassdoorReviewMetadataStore) {
        try {
            final List<WebElement> countElements = reviewPanelElement.findElements(
                By.cssSelector("div[class*=ReviewsPageContainer] div.mt:last-child span strong")
            );

            if (countElements.size() == 2) {
                glassdoorReviewMetadataStore.localReviewCount =
                    Integer.parseInt(countElements.get(0).getText().strip().replaceAll("\\D+", ""));

                glassdoorReviewMetadataStore.globalReviewCount =
                    Integer.parseInt(countElements.get(1).getText().strip().replaceAll("\\D+", ""));
            } else {
                final String reviewPanelElementRawContent = reviewPanelElement.getText();
                final String htmlDumpPath =
                    this.archiveManager.writeHtml("reviewMeta:NoLocalGlobalReviewCountWarning", this.driver.getPageSource());
                throw new ScraperShouldHaltException(
                    "Unable to scrape local & global review count from reviewPanelElement: " +
                    reviewPanelElementRawContent.substring(0, Math.min(reviewPanelElementRawContent.length(), 500)) +
                    "...\n" +
                    "Please check the review page html, see why scraper cannot find the review counts. Html saved on s3 at key `" +
                    htmlDumpPath +
                    "`"
                );
            }
        } catch (final NoSuchElementException e) {}
    }

    private String parseReviewId(final WebElement employeeReviewLiElement) {
        final String idAttributeString = employeeReviewLiElement.getAttribute("id");

        final String employeeReviewId = idAttributeString.split("_")[1];

        return employeeReviewId;
    }

    private void scrapeEmployeeReview(final WebElement employeeReviewLiElement, final EmployeeReviewData reviewDataStore) {
        reviewDataStore.stableReviewData.reviewId = this.parseReviewId(employeeReviewLiElement);

        // scrape time
        try {
            reviewDataStore.stableReviewData.reviewDate =
                employeeReviewLiElement.findElement(By.cssSelector("time.date")).getAttribute("datetime").strip();
        } catch (final NoSuchElementException e) {}

        // scrape comment title
        reviewDataStore.stableReviewData.reviewHeaderTitle =
            employeeReviewLiElement.findElement(By.cssSelector("h2.summary a")).getText().strip();

        // scrape position
        reviewDataStore.stableReviewData.reviewEmployeePositionText =
            employeeReviewLiElement.findElement(By.cssSelector("div.author span.authorInfo span.authorJobTitle")).getText().strip();

        // scrape location
        try {
            reviewDataStore.stableReviewData.reviewEmployeeLocation =
                employeeReviewLiElement.findElement(By.cssSelector("div.author span.authorInfo span.authorLocation")).getText().strip();
        } catch (final NoSuchElementException e) {}

        // TODO: scrape text
        final EmployeeReviewTextData reviewTextData = null;
        this.parseReviewTextData(employeeReviewLiElement, reviewDataStore);

        // scrape helpful
        // TODO: handle possible comma in text
        try {
            reviewDataStore.varyingReviewData.helpfulCount =
                Integer.parseInt(
                    employeeReviewLiElement
                        .findElement(By.cssSelector("div.helpfulReviews.helpfulCount"))
                        .getText()
                        .strip()
                        .replaceAll("\\D+", "")
                );
        } catch (final NoSuchElementException e) {}

        // scrape rating
        reviewDataStore.stableReviewData.reviewRatingMetrics.overallRating =
            Float.parseFloat(
                employeeReviewLiElement.findElement(By.cssSelector("span.gdRatings span.rating span.value-title")).getAttribute("title")
            );

        // scrape rating metrics
        try {
            // if has dropdown icon, means that it has rating metrics data
            employeeReviewLiElement.findElement(By.cssSelector("span.gdRatings i.subRatingsDrop"));
            this.parseReviewRatingMetrics(employeeReviewLiElement, reviewDataStore);
        } catch (final NoSuchElementException e) {}

        // scrape featured
        try {
            employeeReviewLiElement.findElement(By.cssSelector("div.hreview div.featuredFlag"));
            // employeeReviewLiElement.findElement(By.cssSelector("div.hreview > div.d-flex.justify-content-between > div > div.featuredFlag"));
            reviewDataStore.varyingReviewData.featured = true;
        } catch (final NoSuchElementException e) {}

        return;
    }

    private void parseReviewRatingMetrics(final WebElement employeeReviewLiElement, final EmployeeReviewData reviewDataStore) {
        // pre-processing - show rating metric element (the hover-dropdown tooltips that
        // displays subratings in each review are hidden at first)
        final WebElement ratingMetricsElement = this.showRatingMetricsElement(reviewDataStore.stableReviewData.reviewId);

        // scrape work life balance rating
        try {
            reviewDataStore.stableReviewData.reviewRatingMetrics.workLifeBalanceRating =
                Float.parseFloat(
                    ratingMetricsElement
                        .findElement(By.cssSelector("ul li:nth-child(1) span.gdBars.gdRatings"))
                        .getAttribute("title")
                        .strip()
                );
        } catch (final NoSuchElementException e) {
            if (Configuration.DEBUG) {
                Logger.info("WARN: cannot scrape rating metrics - work & life balance");
            }
        }

        // culture and values rating
        try {
            reviewDataStore.stableReviewData.reviewRatingMetrics.cultureAndValuesRating =
                Float.parseFloat(
                    ratingMetricsElement
                        .findElement(By.cssSelector("ul li:nth-child(2) span.gdBars.gdRatings"))
                        .getAttribute("title")
                        .strip()
                );
        } catch (final NoSuchElementException e) {
            if (Configuration.DEBUG) {
                Logger.info("WARN: cannot scrape rating metrics - culture & values");
            }
        }

        // career opportunities rating
        try {
            reviewDataStore.stableReviewData.reviewRatingMetrics.careerOpportunitiesRating =
                Float.parseFloat(
                    ratingMetricsElement
                        .findElement(By.cssSelector("ul li:nth-child(3) span.gdBars.gdRatings"))
                        .getAttribute("title")
                        .strip()
                );
        } catch (final NoSuchElementException e) {
            if (Configuration.DEBUG) {
                Logger.info("WARN: cannot scrape rating metrics - career opportunities");
            }
        }

        // compensation and benefits rating
        try {
            reviewDataStore.stableReviewData.reviewRatingMetrics.compensationAndBenefitsRating =
                Float.parseFloat(
                    ratingMetricsElement
                        .findElement(By.cssSelector("ul li:nth-child(4) span.gdBars.gdRatings"))
                        .getAttribute("title")
                        .strip()
                );
        } catch (final NoSuchElementException e) {
            if (Configuration.DEBUG) {
                Logger.info("WARN: cannot scrape rating metrics - compensation & benefits");
            }
        }

        // senior management rating
        try {
            reviewDataStore.stableReviewData.reviewRatingMetrics.seniorManagementRating =
                Float.parseFloat(
                    ratingMetricsElement
                        .findElement(By.cssSelector("ul li:nth-child(5) span.gdBars.gdRatings"))
                        .getAttribute("title")
                        .strip()
                );
        } catch (final NoSuchElementException e) {
            if (Configuration.DEBUG) {
                Logger.info("WARN: cannot scrape rating metrics - senior management");
            }
        }

        // hide so that it won't overlap other elements, which can cause click() on
        // other elements not working
        this.hideRatingMetricsElement(reviewDataStore.stableReviewData.reviewId);
    }

    private String getRatingMetricsElementCssSelector(final String reviewId) {
        // example resulting css selector:
        // "article[id*=MainCol]
        // main div#ReviewsFeed ol > li[id$='reviewId']
        // div.mt span.gdRatings div.subRatings"
        return (
            this.reviewPanelElementCssSelector +
            String.format(" %s[id$='%s']", this.employeeReviewElementsLocalCssSelector, reviewId) +
            " div.mt span.gdRatings div.subRatings"
        );
    }

    private String getRatingMetricsElementDisplayJavascriptCommand(final String reviewId, final String styleDisplayString) {
        return String.format(
            "const metricElements = document.querySelectorAll(\"%1$s\"); %2$s;",
            this.getRatingMetricsElementCssSelector(reviewId),
            String.format("for (let metricElement of metricElements) { metricElement.style.display = \"%s\"; }", styleDisplayString)
        );
    }

    private WebElement changeDisplayRatingMetricsElement(final String reviewId, final String styleDisplayString) {
        WebElement ratingMetricsElement = null;

        final JavascriptExecutor javascriptExecutor = (JavascriptExecutor) this.driver;

        final String javascriptCommand = this.getRatingMetricsElementDisplayJavascriptCommand(reviewId, styleDisplayString);

        if (Configuration.DEBUG) {
            Logger.info("About to execute javascript: rating metric elements: change display style to " + styleDisplayString);

            Logger.info(String.format("Javascript command:\n%s", javascriptCommand));
        }
        javascriptExecutor.executeScript(javascriptCommand);
        if (Configuration.DEBUG) {
            Logger.info("Finished executing javascript.");
        }

        // verify changes applied in UI
        if (styleDisplayString != "none") {
            ratingMetricsElement =
                this.wait.until(
                        ExpectedConditions.visibilityOfElementLocated(By.cssSelector(this.getRatingMetricsElementCssSelector(reviewId)))
                    );
        } else {
            this.wait.until(
                    ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(this.getRatingMetricsElementCssSelector(reviewId)))
                );
        }

        if (Configuration.DEBUG) {
            Logger.info("Confirmed UI applied.");
        }

        return ratingMetricsElement;
    }

    private WebElement showRatingMetricsElement(final String reviewId) {
        return this.changeDisplayRatingMetricsElement(reviewId, "block");
    }

    private WebElement hideRatingMetricsElement(final String reviewId) {
        return this.changeDisplayRatingMetricsElement(reviewId, "none");
    }

    private void parseReviewTextData(final WebElement employeeReviewLiElement, final EmployeeReviewData reviewDataStore) {
        final List<WebElement> showMoreLinkElements = employeeReviewLiElement.findElements(By.cssSelector("div.hreview div.mt span.link"));

        for (final WebElement showMoreLinkElement : showMoreLinkElements) {
            showMoreLinkElement.click();
        }

        // main text
        try {
            reviewDataStore.stableReviewData.reviewTextData.mainText =
                employeeReviewLiElement.findElement(By.cssSelector("p.mainText")).getText().strip();
        } catch (final Exception e) {}

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

        final List<WebElement> paragraphElements = employeeReviewLiElement.findElements(By.cssSelector("div.hreview div.mt p"));

        for (final WebElement paragraphElement : paragraphElements) {
            reviewDataStore.stableReviewData.reviewTextData.rawParagraphs.add(paragraphElement.getText().strip());
        }
    }

    @Override
    protected void postAction(final GlassdoorCompanyReviewParsedData parsedData) {
        this.sideEffect = parsedData;
        Logger.info("Total reviews processed: " + parsedData.employeeReviewDataList.size());
    }
}
