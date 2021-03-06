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
import com.shaungc.utilities.ScraperMode;
import com.shaungc.utilities.Timer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
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

    private String reviewPanelElementCssSelector = "article#MainCol main";
    private final String employeeReviewElementsLocalCssSelector = "div#ReviewsFeed ol > li";

    /** scraper session metadata */

    public Integer processedReviewsCount = Configuration.TEST_COMPANY_LAST_PROGRESS_PROCESSED;
    public Integer wentThroughReviewsCount = Configuration.TEST_COMPANY_LAST_PROGRESS_WENTTHROUGH;
    // if scraper mode == regular, will obtain when scraping review meta
    // else if mode == renewal, will obtain from env var (Configuration)
    public Integer localReviewCount = Configuration.TEST_COMPANY_LAST_PROGRESS_TOTAL;
    public Integer wentThroughReviewPages = Configuration.TEST_COMPANY_LAST_PROGRESS_PAGE;

    /** expose other data for external use */
    public Boolean isFinalSession = true;

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

    protected List<WebElement> locate(
        final String reviewPageUrl,
        final String currentPageLinkForFindingNextPageLink,
        final Boolean onlyLocateReviewCards,
        final Boolean throwException
    ) {
        final List<WebElement> locatedElements = new ArrayList<>();

        final String appraochType = reviewPageUrl != null
            ? "direct review page url"
            : currentPageLinkForFindingNextPageLink != null ? "click next page link" : "initial review tab click";

        // locate review panel
        // critical mission so set retry to higher number
        final Integer FIND_REVIEW_PANEL_RETRY = 2;
        Integer findReviewPanelRetryCounter = 0;
        while (findReviewPanelRetryCounter <= FIND_REVIEW_PANEL_RETRY) {
            // check if there's a outstanding termination request first
            if (pubSubSubscription.receivedTerminationRequest) {
                throw new ScraperShouldHaltException(
                    (new StringBuilder()).append(this.orgNameSlackMessagePrefix)
                        .append(" ")
                        .append(this.scraperSessionTimer.captureCurrentSessionElapseDurationString())
                        .append(" Termination request received")
                        .toString()
                );
            }

            findReviewPanelRetryCounter++;

            try {
                // navigate to / click for reviews page

                if (reviewPageUrl != null && !reviewPageUrl.strip().isEmpty()) {
                    Logger.infoAlsoSlack("Now accessing review page url `" + reviewPageUrl + "`");

                    this.driver.get(reviewPageUrl);
                } else if (currentPageLinkForFindingNextPageLink != null) {
                    this.driver.get(currentPageLinkForFindingNextPageLink);

                    final WebElement nextPageLinkElement = this.getNextPageLinkElement();

                    // short circuit to next retry, b/c
                    // if can't even get the next page link element, then no need to locate review panel
                    if (nextPageLinkElement == null || !nextPageLinkElement.isEnabled() || !nextPageLinkElement.isDisplayed()) {
                        if (findReviewPanelRetryCounter > FIND_REVIEW_PANEL_RETRY) {
                            if (throwException) {
                                this.raiseLocateRetryExhaustedError(findReviewPanelRetryCounter);
                            } else {
                                break;
                            }
                        } else {
                            this.applyLocateRetryBuffer(findReviewPanelRetryCounter);
                            continue;
                        }
                    }

                    final String nextPageLinkElementText = nextPageLinkElement.getText();

                    // click and wait for review panel loading
                    nextPageLinkElement.click();
                    try {
                        TimeUnit.SECONDS.sleep(2);
                    } catch (InterruptedException e) {}
                    this.waitForReviewPanelLoading();

                    Logger.infoAlsoSlack(
                        "Now clicked element `" + nextPageLinkElementText + "`" + ", url is `" + this.driver.getCurrentUrl() + "`."
                    );
                } else {
                    this.driver.findElement(By.cssSelector("article#WideCol a.eiCell.reviews")).click();
                }

                // capture review panel element

                // 1st appraoch
                WebElement reviewPanelElement = null;
                try {
                    reviewPanelElement =
                        this.wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(this.reviewPanelElementCssSelector)));
                    locatedElements.add(reviewPanelElement);
                    break;
                } catch (TimeoutException e) {
                    Logger.warnAlsoSlack(
                        (new StringBuilder()).append(this.orgNameSlackMessagePrefix)
                            .append(" ")
                            .append("1st approach failed locating review panel")
                            .toString()
                    );
                }

                // 2nd approach
                final String reviewPanelElementCssSelector2ndApproach = "div[data-test=EIReviewsPage]";
                reviewPanelElement =
                    this.wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(reviewPanelElementCssSelector2ndApproach)));

                if (reviewPanelElement != null) {
                    locatedElements.add(reviewPanelElement);
                    break;
                } else {
                    Logger.infoAlsoSlack(
                        String.format(
                            "*(%s)* (current session %s) 2nd (last) approach failed locating review panel. Tried %sth time(s), approach type `%s`.",
                            this.archiveManager.orgName,
                            this.scraperSessionTimer.captureCurrentSessionElapseDurationString(),
                            findReviewPanelRetryCounter,
                            appraochType
                        )
                    );
                }
            } catch (TimeoutException e) {
                Logger.warnAlsoSlack(
                    String.format(
                        "*(%s)* (current session %s) Timeout while locating review panel, trying %sth time(s), approach type %s.",
                        this.archiveManager.orgName,
                        this.scraperSessionTimer.captureCurrentSessionElapseDurationString(),
                        findReviewPanelRetryCounter,
                        appraochType
                    )
                );
            }

            if (findReviewPanelRetryCounter > FIND_REVIEW_PANEL_RETRY) {
                if (throwException) {
                    this.raiseLocateRetryExhaustedError(findReviewPanelRetryCounter);
                } else {
                    break;
                }
            }

            this.applyLocateRetryBuffer(findReviewPanelRetryCounter);
        }
        // TODO: filter by engineering category
        // confirm that we are on review page while locating filter button
        // final WebElement filterButtonElement = wait.until(ExpectedConditions
        // .elementToBeClickable(By.cssSelector("article[id*=MainCol] main div.search >
        // div > button")));

        // TODO: remove sort if not needed - especially when we will scrape all reviews
        // anyway, and the ordering may not matter. This is also to scrape "featured
        // flag", which is only displayed only in popular ordering
        // use wait which is based on this.driver to avoid click() interrupted by
        // element structure changed, or "element not attach to page document" error
        // sort by most recent
        // final String sortDropdownElementCssSelector = "body div#PageContent
        // article[id=MainCol] .filterSorts select[name=filterSorts]";

        // // locate sort dropdown list
        // this.wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(sortDropdownElementCssSelector))).click();

        // this.wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(sortDropdownElementCssSelector
        // + " option[value=DATE]")))
        // .click();

        if (locatedElements.size() == 0) {
            return null;
        }
        if (onlyLocateReviewCards) {
            return locatedElements.get(0).findElements(By.cssSelector(this.employeeReviewElementsLocalCssSelector));
        }

        return locatedElements;
    }

    private void raiseLocateRetryExhaustedError(final Integer retryCounter) {
        throw new ScraperShouldHaltException(
            "review:cannotLocateReviewPanel",
            String.format("Cannot locate review panel after `%s` retries.", retryCounter),
            this.archiveManager,
            this.driver
        );
    }

    private void applyLocateRetryBuffer(final Integer findReviewPanelRetryCounter) {
        // try to mitigate redis disconnection & avoid SLK timeout by keeping some
        // publish commands out there
        Logger.warn("Publish progress before cooling down");
        this.publishProgress();

        // add some sleep between retry, if it's network congestion this may mitigate it
        Logger.warn("Cooling down before next retry for seconds: " + (10 * findReviewPanelRetryCounter));
        try {
            TimeUnit.SECONDS.sleep(10 * findReviewPanelRetryCounter);
        } catch (InterruptedException interruptedException) {
            throw new ScraperShouldHaltException("Sleep interrupted: while sleeping for review panel capture retry");
        }
    }

    @Override
    protected List<WebElement> locate() {
        if (Configuration.SCRAPER_MODE.equals(ScraperMode.RENEWAL.getString())) {
            return this.locate(Configuration.TEST_COMPANY_NEXT_REVIEW_PAGE_URL, null, false, true);
        } else {
            return this.locate(null, null, false, true);
        }
    }

    // TODO: remove this if not needed
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

        // Scrape Review Metadata

        if (Configuration.SCRAPER_MODE.equals(ScraperMode.REGULAR.getString())) {
            // scrape for review metadata
            this.scrapeReviewMetadata(reviewPanelElement, glassdoorCompanyParsedData.reviewMetadata);

            // write out review metadata
            this.archiveManager.writeGlassdoorOrganizationReviewsMetadataAsJson(glassdoorCompanyParsedData.reviewMetadata);

            this.localReviewCount = glassdoorCompanyParsedData.reviewMetadata.localReviewCount;
        }

        // Scrape Reviews

        // narrow down locating to review cards
        List<WebElement> employeeReviewElements = reviewPanelElement.findElements(
            By.cssSelector(this.employeeReviewElementsLocalCssSelector)
        );
        // meta info
        final Integer reviewReportTimes = 5;
        final Integer reportingRate = (this.localReviewCount > reviewReportTimes)
            ? (Integer) (this.localReviewCount / reviewReportTimes)
            : 1;
        final Timer progressReportingTimer = new Timer(Duration.ofSeconds(10));
        final Timer browserGarbageCollectionTimer = !Configuration.RUNNING_IN_TRAVIS ? new Timer(Duration.ofMinutes(5)) : null;

        while (this.wentThroughReviewsCount < this.localReviewCount) {
            // Travis requires us to output something every minute
            if (Configuration.RUNNING_IN_TRAVIS) {
                System.out.println("Scraping reviews on page " + (this.wentThroughReviewPages + 1));
            }

            for (final WebElement employeeReviewElement : employeeReviewElements) {
                if (this.pubSubSubscription.receivedTerminationRequest) {
                    throw new ScraperException("Terminating per request");
                }

                // check if element is staled (state changed) before proceeding
                try {
                    employeeReviewElement.isEnabled();
                } catch (StaleElementReferenceException e) {
                    throw new ScraperException(
                        "staleElementError:employeeReviewElement",
                        "employeeReviewElement is staled. Please check if something changed the element's state.",
                        this.archiveManager,
                        this.driver
                    );
                }

                final EmployeeReviewData employeeReviewData = new EmployeeReviewData();

                if (this.scrapeEmployeeReview(employeeReviewElement, employeeReviewData)) {
                    if (this.archiveManager.writeGlassdoorOrganizationReviewDataAsJson(employeeReviewData)) {
                        this.processedReviewsCount++;
                    }
                }

                this.wentThroughReviewsCount++;

                if (progressReportingTimer.doesReachCountdownDuration()) {
                    this.publishProgress();
                    progressReportingTimer.restart();
                }

                // send message per 50 reviews (5 page, each around 10 reviews)
                if (this.wentThroughReviewsCount % (reportingRate) == 0) {
                    final String elapsedTimeString = this.scraperSessionTimer != null
                        ? this.scraperSessionTimer.captureOverallElapseDurationString()
                        : "";
                    Logger.infoAlsoSlack(
                        String.format(
                            "%s%s <%s|Page %d> presents %d elements, processed/wentThrough/total %d/%d/%d reviews, keep processing for the next %d reviews...",
                            this.orgNameSlackMessagePrefix,
                            elapsedTimeString != "" ? "(" + elapsedTimeString + ") " : "",
                            this.driver.getCurrentUrl(),
                            // + 1 to get current page number
                            this.wentThroughReviewPages + 1,
                            employeeReviewElements.size(),
                            this.processedReviewsCount,
                            this.wentThroughReviewsCount,
                            this.localReviewCount,
                            reportingRate
                        )
                    );
                }

                if (Configuration.LOGGER_LEVEL >= LoggerLevel.DEBUG.getVerbosenessLevelValue()) {
                    employeeReviewData.debug(this.wentThroughReviewsCount);
                }
            }

            this.wentThroughReviewPages++;

            // instructed to stop & wrap up if reached stop page (for splitted job, specifically)
            if (
                !Configuration.TEST_COMPANY_STOP_AT_PAGE.equals(0) &&
                Configuration.TEST_COMPANY_STOP_AT_PAGE.compareTo(this.wentThroughReviewPages) <= 0
            ) {
                Logger.debug("Will now wrap up scraper session because we reached stop page " + Configuration.TEST_COMPANY_STOP_AT_PAGE);
                break;
            }

            // Proceed to next page
            //
            //

            employeeReviewElements = null;

            // === approach type: try direct url first ===

            final String currentPageLink = this.driver.getCurrentUrl();

            // 4th approach for getting url
            String nextPageLink = this.judgeNoNextPageLinkOrGetLinkForthApproach();

            // 5th approach for getting url
            // this approach will always give us a link, so make sure we also check for an actual review panel
            if (nextPageLink == null) {
                nextPageLink = this.judgeNoNextPageLinkOrGetLinkFifthApproach();
            }

            employeeReviewElements = this.locate(nextPageLink, null, true, false);

            // === approach type: try finding a next link and click on it ===

            if (employeeReviewElements == null || employeeReviewElements.size() == 0) {
                employeeReviewElements = this.locate(null, currentPageLink, true, false);
            }

            // if still no any review card, probably we reached the bottom page (review panel exists but empty)
            // (we cannot completely rely on went through count vs total count to detect bottom or not, because the last splitted job's total is the org's total,
            // not the total of the very last splitted job itself)
            // (thus will have to identify bottom page by failing review card element check above)
            if (employeeReviewElements == null || employeeReviewElements.size() == 0) {
                break;
            }

            // check if approaching time limit e.g. for travis build
            // if so, stop session and try to schedule a cross-session job instead
            if (this.scraperSessionTimer.doesReachCountdownDuration()) {
                // stop current scraper session
                this.isFinalSession = false;
                return glassdoorCompanyParsedData;
            }

            // order garbage collect on both scraper and browser driver
            // in case of danger memory utilization, schedule for cross session
            if (browserGarbageCollectionTimer != null && browserGarbageCollectionTimer.doesReachCountdownDuration()) {
                final Double memoryUtilizationMi = this.orderGarbageCollectionAgainstBrowser();
                if (memoryUtilizationMi > 240) {
                    Logger.warnAlsoSlack("Danger memory water meter, use cross session and abort current scraper");
                    this.isFinalSession = false;
                    return glassdoorCompanyParsedData;
                }
                browserGarbageCollectionTimer.restart();
            }
        }

        // default to finalize (not interrupted by timer)
        //
        // if want to finalize the session, then use `break` in while loop above
        //
        // if want to continue session in next job, then use:
        // this.isFinalSession = false;
        // return glassdoorCompanyParsedData;
        //
        this.isFinalSession = true;
        return glassdoorCompanyParsedData;
    }

    private void publishProgress() {
        this.pubSubSubscription.publishProgress(
                this.processedReviewsCount,
                this.wentThroughReviewsCount,
                this.localReviewCount,
                this.scraperSessionTimer.captureOverallElapseDurationString()
            );
    }

    // TODO: remove this if not used
    private WebElement getNextPageLinkElement() {
        WebElement nextPageLinkElement;
        // 1st approach
        if ((nextPageLinkElement = this.getNextPageLinkElementFirstApproach()) != null) {
            return nextPageLinkElement; // has link then short-circuit
        }

        // 2nd approach
        if ((nextPageLinkElement = this.getNextPageLinkElementSecondApproach()) != null) {
            return nextPageLinkElement;
        }

        // 3rd approach
        if ((nextPageLinkElement = this.getNextPageLinkElementThirdApproach()) != null) {
            return nextPageLinkElement;
        }

        return null;
    }

    /**
     * This method try to guess next page url based on the observed pattern in url
     * across pages sample url:
     * https://www.glassdoor.com/Reviews/Target-Reviews-E194_P177.htm
     *
     * @return url of next page link if presents, otherwise null
     */
    private String judgeNoNextPageLinkOrGetLinkFifthApproach() {
        StringBuilder logMessageStringBuilder = new StringBuilder();
        logMessageStringBuilder
            .append("*(")
            .append(this.archiveManager.orgName)
            .append(")* ")
            .append("Trying 5th approach to capture next page link");
        Logger.info(logMessageStringBuilder.toString());

        return this.driver.getCurrentUrl().replaceFirst("_P\\d+\\.htm$", String.format("_P%s.htm", this.wentThroughReviewPages + 1));
    }

    /**
     * This method tries to find a next page link, then get the link. Particularly,
     * this 4th approach looks into the html head block and try to find something
     * like below:
     *
     * <link rel="next" href=
     * "https://www.glassdoor.com/Reviews/SAP-Reviews-E10471_P822.htm">
     *
     * @return url of next page link if presents, otherwise null
     */
    private String judgeNoNextPageLinkOrGetLinkForthApproach() {
        Logger.info("Trying 4th approach to capture next page link");

        try {
            final String nextPageUrl = this.driver.findElement(By.cssSelector("head > link[rel=next]")).getAttribute("href").strip();
            if (!nextPageUrl.isEmpty()) {
                return nextPageUrl;
            }
        } catch (NoSuchElementException e) {}

        return null;
    }

    // TODO: remove this if not used
    private WebElement getNextPageLinkElementThirdApproach() {
        // example webpage in mind:
        // https://s3.console.aws.amazon.com/s3/object/iriversland-qualitative-org-review-v3/Amazon-6036/logs/reviewDataLostWarning.2020-03-04T23%253A44%253A01.848981Z.html?region=us-west-2&tab=overview

        // TODO: remove this or change to debug after things get stable
        Logger.info("Trying 3rd approach to capture next page link");

        List<WebElement> anchorElements =
            this.driver.findElements(
                    By.cssSelector("div#NodeReplace > main.gdGrid > div:first-child > div[class*=eiReviews] > div[class$=pagination] a")
                );

        // TODO: remove this or change to debug after things get stable
        Logger.info("found anchorElements " + anchorElements.size());

        if (anchorElements.size() == 0) {
            return null;
        }

        if (anchorElements.size() == 1) {
            // not sure what this case is, but we want to avoid clicking a link that goes to
            // a previous page or current page, which will cause a inifinite loop where
            // scraper job renews forever
            // so we'd rather just play safe and stop here, but we'll log this incident
            Logger.warn("Using third approach to capture next page link, but got an unknown case where only one `<a>` found");
            return null;
        }

        // just look at last anchor
        // but still, it should contain some content related to "next",
        // whether it is in class, some attribute, or as text displayed in UI
        // otherwise we do not recognize this anchor as next page link
        WebElement lastAnchorElement = anchorElements.get(anchorElements.size() - 1);

        // TODO: remove this or change to debug after things get stable
        Logger.info("lastAnchorElement: " + lastAnchorElement.getText());

        if (!lastAnchorElement.getText().toLowerCase().contains("next")) {
            // TODO: remove this or change to debug after things get stable
            Logger.info("no `next` text exist in lastAnchorElement");
            return null;
        }

        return lastAnchorElement;
    }

    // TODO: remove this if not used
    private WebElement getNextPageLinkElementSecondApproach() {
        // TODO: remove this or change to debug after things get stable
        Logger.info("Trying 2nd approach to capture next page link");

        List<WebElement> anchorElements =
            this.driver.findElements(
                    By.cssSelector(
                        "div#NodeReplace > main.gdGrid > div:first-child > div[class*=eiReviews] > div[class$=pagination] > ul a"
                    )
                );

        // try to capture no next page cases
        if (anchorElements.size() == 0) {
            return null;
        } else {
            WebElement lastAnchorElement = anchorElements.get(anchorElements.size() - 1);
            if (lastAnchorElement.getAttribute("class") != null) {
                if (lastAnchorElement.getAttribute("class").strip().equals("disabled")) {
                    return null;
                }
            }

            // verify anchor is for next link, not other random link
            try {
                lastAnchorElement.findElement(By.cssSelector("span[alt=Next]"));
            } catch (Exception e2CheckAnchorIsNotNext) {
                return null;
            }

            return lastAnchorElement;
        }
    }

    // TODO: remove this if not used
    private WebElement getNextPageLinkElementFirstApproach() {
        try {
            return this.driver.findElement(By.cssSelector("ul[class^=pagination] li[class$=next] a:not([class$=disabled])"));
        } catch (final NoSuchElementException e) {
            return null;
        }
    }

    private void scrapeReviewMetadata(final WebElement reviewPanelElement, final GlassdoorReviewMetadata glassdoorReviewMetadataStore) {
        // scrape overall rating value
        final WebElement overallRatingElement = reviewPanelElement.findElement(By.cssSelector("div[class*=ratingNum]"));
        try {
            glassdoorReviewMetadataStore.overallRating = Float.parseFloat(overallRatingElement.getText().strip());
        } catch (final NumberFormatException e) {}

        // scrape ovwerall review counts
        this.scrapeReviewCount(reviewPanelElement, glassdoorReviewMetadataStore);

        glassdoorReviewMetadataStore.reviewPageUrl = this.driver.getCurrentUrl();
    }

    /**
     * Scrapes two things - local review count and global review count. Usually we
     * are finding information from text of this form `5,426 English reviews out of
     * 6,053`.
     *
     * Note that not finding such information does not necessarily means a scraper
     * exception, org could have no reviews yet.
     *
     * @param reviewPanelElement           - uses selector `article#MainCol main`
     * @param glassdoorReviewMetadataStore
     */
    private void scrapeReviewCount(final WebElement reviewPanelElement, final GlassdoorReviewMetadata glassdoorReviewMetadataStore) {
        // 1st approach
        try {
            final List<WebElement> countElements = reviewPanelElement.findElements(
                By.cssSelector("div[class*=ReviewsPageContainer] div.mt:last-child span strong")
            );

            if (countElements.size() == 2) {
                glassdoorReviewMetadataStore.localReviewCount =
                    Integer.parseInt(countElements.get(0).getText().strip().replaceAll("\\D+", ""));

                glassdoorReviewMetadataStore.globalReviewCount =
                    Integer.parseInt(countElements.get(1).getText().strip().replaceAll("\\D+", ""));

                if (!glassdoorReviewMetadataStore.localReviewCount.equals(0)) {
                    return;
                }
            }

            // abnormal countElements; print out warnings
            final StringBuilder countElementsDump = new StringBuilder();
            for (final WebElement countElement : countElements) {
                countElementsDump.append("\n\n").append(countElement.getText()).append("\n\n");
            }
            Logger.warn("1st approach for scraping Local review count: abnormal countElementsDump: " + countElementsDump);
        } catch (final NoSuchElementException e) {
            Logger.warn("1st approach for local review count: No such element.");
        }
        if (glassdoorReviewMetadataStore.localReviewCount.equals(0)) {
            Logger.warnAlsoSlack("NoSuchElementException - 1st approach for scraping Local review count failed");
        }

        // 2nd approach regex trying to extract stuff - if doesn't even match the regex,
        // then it's likely no reviews yet
        String reviewCountElementTextContent = " ";
        try {
            reviewCountElementTextContent =
                reviewPanelElement
                    .findElement(By.cssSelector("div[class*=EIReviewsPageContainer] > div[class*=sortsHeader] > h2 > span"))
                    .getText();

            if (!this.parseReviewCountsFromText(reviewCountElementTextContent, glassdoorReviewMetadataStore)) {
                Logger.warn("2nd approach for scraping local review count: cannot parse review count");
            }
        } catch (NoSuchElementException e) {
            Logger.warn("2nd approach for local review count: No such element.");
        }
        if (glassdoorReviewMetadataStore.localReviewCount.equals(0)) {
            Logger.warnAlsoSlack(
                String.format(
                    "2nd approach for scraping Local review count failed, `reviewCountElementTextContent`:\n```%s```",
                    reviewCountElementTextContent
                )
            );
        }

        // 3rd approach
        // last check in case previous approaches cannot capture the right element,
        // perhaps the website structure changed
        final String reviewPanelElementRawContent = reviewPanelElement.getText();
        if (this.parseReviewCountsFromText(reviewPanelElementRawContent, glassdoorReviewMetadataStore)) {
            Logger.info("3rd approach for local review count succeed");
        } else {
            Logger.warn("3rd approach for local review count failed");
        }

        // Verify
        if (glassdoorReviewMetadataStore.localReviewCount.equals(0)) {
            // Report abnormal case - we should be scraping an org because it has reviews;
            // otherwise it's not of our concern
            throw new ScraperShouldHaltException(
                "reviewMeta:NoLocalGlobalReviewCountWarning",
                "Unable to scrape local & global review count or scraped `localReviewCount=0` from reviewPanelElement:\n```" +
                reviewPanelElementRawContent.substring(0, Math.min(reviewPanelElementRawContent.length(), 500)) +
                "...```\n" +
                "Please check the review page html, see why scraper cannot find the review counts.",
                this.archiveManager,
                this.driver
            );
        }
    }

    private Boolean parseReviewCountsFromText(final String text, final GlassdoorReviewMetadata glassdoorReviewMetadataStore) {
        final String sanitizedText = text.strip().toLowerCase().replaceAll("[^\\d\\w\\s]", "");

        if (sanitizedText.isBlank()) {
            return false;
        }

        final Pattern reviewCountPattern = Pattern.compile("(\\d+)\\s+\\w+\\s+reviews\\s+out\\s+of\\s+(\\d+)");
        final Matcher reviewCountMatcher = reviewCountPattern.matcher(sanitizedText);
        if (reviewCountMatcher.find()) {
            final String localReviewCountString = reviewCountMatcher.group(1);
            final String globalReviewCountString = reviewCountMatcher.group(2);
            Logger.infoAlsoSlack(
                String.format("scraper found localCount/globalCount = %s/%s", localReviewCountString, globalReviewCountString)
            );

            glassdoorReviewMetadataStore.localReviewCount = Integer.valueOf(localReviewCountString);
            glassdoorReviewMetadataStore.globalReviewCount = Integer.valueOf(globalReviewCountString);

            if (!glassdoorReviewMetadataStore.localReviewCount.equals(0)) {
                return true;
            }
        } else {
            Logger.debug(
                (new StringBuilder()).append("Cannot parse review counts from text. Sanitized text: ").append(sanitizedText).toString()
            );
        }

        return false;
    }

    private String parseReviewId(final WebElement employeeReviewLiElement) {
        final String idAttributeString = employeeReviewLiElement.getAttribute("id");

        final String employeeReviewId = idAttributeString.split("_")[1];

        return employeeReviewId;
    }

    private static final Set<String> contentBlockReviewIdWhiteListSet = new HashSet<String>();

    static {
        // SAP
        // https://www.glassdoor.com/Reviews/SAP-Reviews-E10471_P329.htm
        // s3://iriversland-qualitative-org-review-v3/SAP-10471/logs/review:commentTitleNotCaptured.2020-03-05T19:58:36.533556Z.html
        ScrapeReviewFromCompanyReviewPage.contentBlockReviewIdWhiteListSet.add("31306489");
        ScrapeReviewFromCompanyReviewPage.contentBlockReviewIdWhiteListSet.add("31546268");
        // review date 2020-07-15, page 321
        // s3 link -> https://s3.console.aws.amazon. dcom/s3/object/iriversland-qualitative-org-review-v3/SAP-10471/logs/review:commentTitleNotCaptured2020-07-20T23:40:27.832681Z.html?region=us-west-2&tab=overview
        ScrapeReviewFromCompanyReviewPage.contentBlockReviewIdWhiteListSet.add("34247323");

        // Salesforce
        // Salesforce-11159/logs/review:commentTitleNotCaptured.2020-03-15T09:16:28.501665Z.html
        // https://www.glassdoor.com/Reviews/Salesforce-Reviews-E11159_P358.htm
        ScrapeReviewFromCompanyReviewPage.contentBlockReviewIdWhiteListSet.add("32284203");

        // SAP Concur (2020-06-10)
        // s3 link -> https://s3.console.aws.amazon.com/s3/object/iriversland-qualitative-org-review-v3/SAP%2520Concur-8763/logs/review%253AcommentTitleNotCaptured2020-06-16T06%253A55%253A47.510226Z.html?region=us-west-2&tab=overview
        ScrapeReviewFromCompanyReviewPage.contentBlockReviewIdWhiteListSet.add("33629280");

        // Lawrence Livermore National Laboratory (review date 2020-07-01, page 4 & page 1)
        // s3 link -> https://s3.console.aws.amazon.com/s3/object/iriversland-qualitative-org-review-v3/Lawrence%2520Livermore%2520National%2520Laboratory-35235/logs/review%253AcommentTitleNotCaptured2020-07-20T22%253A23%253A18.255086Z.html?region=us-west-2&tab=overview
        ScrapeReviewFromCompanyReviewPage.contentBlockReviewIdWhiteListSet.add("34003408");
    }

    private Boolean scrapeEmployeeReview(final WebElement employeeReviewLiElement, final EmployeeReviewData reviewDataStore)
        throws ScraperException {
        reviewDataStore.stableReviewData.reviewId = this.parseReviewId(employeeReviewLiElement);

        // scrape time
        try {
            reviewDataStore.stableReviewData.reviewDate =
                employeeReviewLiElement.findElement(By.cssSelector("time.date")).getAttribute("datetime").strip();
        } catch (final NoSuchElementException e) {}

        // scrape comment title
        WebElement commentTitleH2Element = null;
        try {
            commentTitleH2Element = employeeReviewLiElement.findElement(By.cssSelector("h2.summary"));

            reviewDataStore.stableReviewData.reviewHeaderTitle = commentTitleH2Element.findElement(By.cssSelector("a")).getText().strip();
        } catch (final NoSuchElementException e) {
            // Exception case 'Content Blocked':
            if (commentTitleH2Element != null && commentTitleH2Element.getText().toLowerCase().contains("content blocked")) {
                // whitelist (known "Content Blocked" case)
                if (
                    !ScrapeReviewFromCompanyReviewPage.contentBlockReviewIdWhiteListSet.contains(reviewDataStore.stableReviewData.reviewId)
                ) {
                    throw new ScraperException(
                        "review:commentTitleNotCaptured",
                        String.format(
                            "Found a 'Content Blocked' review at <%s|current page>, this is a new blocked review\n```%s```\n" +
                            "Please check if review `%s` is stored before and if so, figure out a way to store this change. " +
                            "Then, add this review to whitelist (hard coded) in function `scrapeEmployeeReview()`.",
                            this.driver.getCurrentUrl(),
                            commentTitleH2Element.getText(),
                            reviewDataStore.stableReviewData.reviewId
                        ),
                        this.archiveManager,
                        this.driver
                    );
                }

                Logger.warnAlsoSlack(
                    String.format(
                        "Found a known 'Content Blocked' review at <%s|current page>, skipping this review:\n```%s```",
                        this.driver.getCurrentUrl(),
                        commentTitleH2Element.getText()
                    )
                );
                return false;
            }

            throw new ScraperShouldHaltException(
                "review:commentTitleNotCaptured",
                "Comment title canot be captured.",
                this.archiveManager,
                this.driver
            );
        }

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
                employeeReviewLiElement.findElement(By.cssSelector("span.gdRatings span.rating span[title]")).getAttribute("title")
            );

        // scrape rating metrics
        WebElement dropdownIcon = null;
        // if has dropdown icon, means that it has rating metrics data
        try {
            // example see
            // https://s3.console.aws.amazon.com/s3/object/iriversland-qualitative-org-review-v3/AMD-15/logs/review%253AcannotLocateWorkLifeBalanceMetricValue2020-06-15T23%253A18%253A14.765055Z.html?region=us-west-2&tab=overview
            dropdownIcon = employeeReviewLiElement.findElement(By.cssSelector("span.gdRatings span.SVGInline"));
        } catch (final NoSuchElementException e) {}
        // for second attempt let's try previously used selector
        if (dropdownIcon == null) {
            try {
                dropdownIcon = employeeReviewLiElement.findElement(By.cssSelector("span.gdRatings i.subRatingsDrop"));
            } catch (final NoSuchElementException e) {}
        }
        if (dropdownIcon != null) {
            this.parseReviewRatingMetrics(employeeReviewLiElement, reviewDataStore);
        } else {
            // since it could be the webpage indeed the review has no detail metrics
            // so only raise attention in DEBUG mode
            if (Configuration.DEBUG) {
                final String htmlDumpPath =
                    this.archiveManager.writeHtml("review:cannotLocateDetailMetricsDropdown", this.driver.getPageSource());
                final String dumpedFileDownloadUrl = this.archiveManager.getFullUrlOnS3FromFilePathBasedOnOrgDirectory(htmlDumpPath);
                Logger.warnAlsoSlack(
                    String.format(
                        "cannot locate dropdown icon for detail metrics for review id `%s` at <%s|webpage>. You may check the <%s|dumped S3 file> and see if the review indeed has no breakdown ratings, then you're good.",
                        reviewDataStore.stableReviewData.reviewId,
                        this.driver.getCurrentUrl(),
                        dumpedFileDownloadUrl
                    )
                );
            }
        }

        // scrape featured
        try {
            employeeReviewLiElement.findElement(By.cssSelector("div.hreview div.featuredFlag"));
            // employeeReviewLiElement.findElement(By.cssSelector("div.hreview >
            // div.d-flex.justify-content-between > div > div.featuredFlag"));
            reviewDataStore.varyingReviewData.featured = true;
        } catch (final NoSuchElementException e) {}

        return true;
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
                        .findElement(By.cssSelector("ul li:nth-child(1) span.gdBars.gdRatings[title]"))
                        .getAttribute("title")
                        .strip()
                );
        } catch (final NoSuchElementException e) {}
        // throw warn when no value scraped
        if (reviewDataStore.stableReviewData.reviewRatingMetrics.workLifeBalanceRating.compareTo(Float.valueOf("0.0")) < 0) {
            final String htmlDumpPath =
                this.archiveManager.writeHtml("review:cannotLocateWorkLifeBalanceMetricValue", this.driver.getPageSource());
            Logger.warnAlsoSlack(
                String.format(
                    "cannot scrape rating metrics - work & life balance: `%s`, review id `%s`. <%s|Dumped S3 file>. Check to make sure the review really did not provide WLB rating.",
                    reviewDataStore.stableReviewData.reviewRatingMetrics.workLifeBalanceRating,
                    reviewDataStore.stableReviewData.reviewId,
                    this.archiveManager.getFullUrlOnS3FromFilePathBasedOnOrgDirectory(htmlDumpPath)
                )
            );
        }

        // culture and values rating
        try {
            reviewDataStore.stableReviewData.reviewRatingMetrics.cultureAndValuesRating =
                Float.parseFloat(
                    ratingMetricsElement
                        .findElement(By.cssSelector("ul li:nth-child(2) span.gdBars.gdRatings[title]"))
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
                        .findElement(By.cssSelector("ul li:nth-child(3) span.gdBars.gdRatings[title]"))
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
                        .findElement(By.cssSelector("ul li:nth-child(4) span.gdBars.gdRatings[title]"))
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
                        .findElement(By.cssSelector("ul li:nth-child(5) span.gdBars.gdRatings[title]"))
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
            " div.mt span.gdRatings[class*=subRatings]"
            // previously used selector:
            // " div.mt span.gdRatings div.subRatings"
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

        // main text (work dutation / type description) (also included in `rawParagraphs`)
        try {
            reviewDataStore.stableReviewData.reviewTextData.mainText =
                employeeReviewLiElement.findElement(By.cssSelector("div.gdReview div.mt p.mainText")).getText().strip();
        } catch (final Exception e) {}

        // pro text (also included in `rawParagraphs`)
        // TODO: cannot find element
        // reviewDataStore.reviewTextData.proText =
        // employeeReviewLiElement.findElement(By.cssSelector("div.hreview div.mt
        // div[class*=ReviewText]:nth-child(1) p:nth-child(2)")).getText().strip();

        // con text (also included in `rawParagraphs`)
        // TODO: cannot find element
        // reviewDataStore.reviewTextData.conText =
        // employeeReviewLiElement.findElement(By.cssSelector("div.hreview div.mt
        // div[class*=ReviewText]:nth-child(2) p:nth-child(2)")).getText().strip();

        // collect catch-all text contents in review body, this includes work duration, pros, cons, advice to management, and org's response
        //
        // <li class="empReview cf" id="empReview_12014907">
        //   <div class="gdReview">
        //     <div>
        // ->  <div class="row mt">
        final List<WebElement> paragraphElements = employeeReviewLiElement.findElements(By.cssSelector("div.gdReview div.mt p"));
        for (final WebElement paragraphElement : paragraphElements) {
            reviewDataStore.stableReviewData.reviewTextData.rawParagraphs.add(paragraphElement.getText().strip());
        }
    }

    public Double orderGarbageCollectionAgainstBrowser() {
        // start gc
        ((JavascriptExecutor) this.driver).executeScript("window.gc()");
        // also for current scraper java process
        System.gc();

        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            throw new ScraperShouldHaltException("Sleep interrupted: while garbage collecting for javascript");
        }

        // collect memory utilization stats
        final Double usedJsHeapSizeAfterGarbageCollection = (Double) ((JavascriptExecutor) this.driver).executeScript(
                "return window.performance.memory.usedJSHeapSize/1024/1024"
            );

        final String message = String.format(
            "*(%s) (current session %s)* High memory usage `%.2f MB` while ordering garbage collection",
            this.archiveManager.orgName,
            this.scraperSessionTimer.captureCurrentSessionElapseDurationString(),
            usedJsHeapSizeAfterGarbageCollection
        );

        if (usedJsHeapSizeAfterGarbageCollection > 200) {
            Logger.warnAlsoSlack(message);
            // give additional idle time to try to earn time for gc to apply
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                throw new ScraperShouldHaltException("Sleep interrupted: while garbage collecting for javascript");
            }
        }

        return usedJsHeapSizeAfterGarbageCollection;
    }

    @Override
    protected void postAction(final GlassdoorCompanyReviewParsedData parsedData) {
        this.sideEffect = parsedData;
    }
}
