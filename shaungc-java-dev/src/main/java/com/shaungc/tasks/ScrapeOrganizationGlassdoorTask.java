package com.shaungc.tasks;

import com.shaungc.dataStorage.ArchiveManager;
import com.shaungc.dataTypes.BasicParsedData;
import com.shaungc.dataTypes.GlassdoorCompanyReviewParsedData;
import com.shaungc.events.JudgeQueryCompanyPageEvent;
import com.shaungc.events.ScrapeBasicDataFromCompanyNamePage;
import com.shaungc.events.ScrapeReviewFromCompanyReviewPage;
import com.shaungc.exceptions.ScraperException;
import com.shaungc.javadev.Configuration;
import com.shaungc.utilities.Logger;
import com.shaungc.utilities.PubSubSubscription;
import com.shaungc.utilities.ScraperMode;
import com.shaungc.utilities.Timer;
import java.net.URL;
import org.openqa.selenium.WebDriver;

/**
 * ScrapeOrganizationGlassdoorTask
 */
public class ScrapeOrganizationGlassdoorTask {
    private final WebDriver driver;
    private final String searchCompanyName;
    private final URL companyOverviewPageUrl;

    private Timer scraperTaskTimer;
    private ArchiveManager archiveManager;
    final PubSubSubscription pubSubSubscription;

    /** session stats */
    private String orgPrefixSlackString;
    private Integer processedReviewsCount;
    private Integer wentThroughReviewsCount;
    private Integer localReviewsCount;
    private Boolean doesCollidedReviewExist;
    private Boolean isFinalSession;

    public ScrapeOrganizationGlassdoorTask(final WebDriver driver, final PubSubSubscription pubSubSubscription, final String companyName)
        throws ScraperException {
        this.driver = driver;
        this.searchCompanyName = companyName;
        this.companyOverviewPageUrl = null;
        this.pubSubSubscription = pubSubSubscription;

        Logger.infoAlsoSlack("Scraper task started by company name: " + companyName);

        this.launch();
    }

    public ScrapeOrganizationGlassdoorTask(
        final WebDriver driver,
        final PubSubSubscription pubSubSubscription,
        final URL companyOverviewPageUrl
    )
        throws ScraperException {
        this.driver = driver;
        this.searchCompanyName = null;
        this.companyOverviewPageUrl = companyOverviewPageUrl;
        this.pubSubSubscription = pubSubSubscription;

        Logger.infoAlsoSlack("Scraper task started by url: " + companyOverviewPageUrl);

        this.launch();
    }

    /**
     * Scraper task for renewal mode
     */
    public ScrapeOrganizationGlassdoorTask(final WebDriver driver, final PubSubSubscription pubSubSubscription) throws ScraperException {
        this.driver = driver;
        this.searchCompanyName = null;
        this.companyOverviewPageUrl = null;
        this.pubSubSubscription = pubSubSubscription;

        Logger.infoAlsoSlack(
            "Renewal task for: " + Configuration.TEST_COMPANY_NAME + ", from review page " + Configuration.TEST_COMPANY_LAST_REVIEW_PAGE_URL
        );

        this.launch();
    }

    private void launch() throws ScraperException {
        if (Configuration.SCRAPER_MODE.equals(ScraperMode.REGULAR.getString())) {
            this.launchSessionScraper();
        } else if (Configuration.SCRAPER_MODE.equals(ScraperMode.RENEWAL.getString())) {
            this.continueCrossSessionScraper();
        }

        if (this.isFinalSession) {
            this.generateFinalSessionReport();
        } else {
            this.generateContinueSessionReport();
        }
    }

    private void continueCrossSessionScraper() throws ScraperException {
        this.scraperTaskTimer = new Timer(Configuration.TEST_COMPANY_LAST_PROGRESS_DURATION);

        this.archiveManager = new ArchiveManager(Configuration.TEST_COMPANY_NAME, Configuration.TEST_COMPANY_ID);

        final ScrapeReviewFromCompanyReviewPage scrapeReviewFromCompanyReviewPage = new ScrapeReviewFromCompanyReviewPage(
            driver,
            this.pubSubSubscription,
            archiveManager,
            scraperTaskTimer,
            this.orgPrefixSlackString
        );
        scrapeReviewFromCompanyReviewPage.run();

        // propogate session stats
        this.processedReviewsCount = scrapeReviewFromCompanyReviewPage.processedReviewsCount;
        this.wentThroughReviewsCount = scrapeReviewFromCompanyReviewPage.wentThroughReviewsCount;
        this.localReviewsCount = scrapeReviewFromCompanyReviewPage.localReviewCount;
        this.orgPrefixSlackString = "*(" + Configuration.TEST_COMPANY_NAME + ")* ";
        this.doesCollidedReviewExist = scrapeReviewFromCompanyReviewPage.doesCollidedReviewExist;
        this.isFinalSession = scrapeReviewFromCompanyReviewPage.isFinalSession;
    }

    private void launchSessionScraper() throws ScraperException {
        this.scraperTaskTimer = new Timer();

        // Access company overview page
        if (this.companyOverviewPageUrl != null) {
            this.driver.get(this.companyOverviewPageUrl.toString());
        } else if (this.searchCompanyName != null) {
            this.driver.get(
                    String.format(
                        "https://www.glassdoor.com/Reviews/company-reviews.htm?suggestCount=10&suggestChosen=false&clickSource=searchBtn&typedKeyword=%s&sc.keyword=%s&locT=C&locId=&jobType=",
                        this.searchCompanyName,
                        this.searchCompanyName
                    )
                );
        } else {
            Logger.info("WARN: no company overview url or name provided, will not scrape for this company.");
            return;
        }

        // identify no result / exactly one / multiple results
        final JudgeQueryCompanyPageEvent judgeQueryCompanyPageEvent = new JudgeQueryCompanyPageEvent(this.driver);
        judgeQueryCompanyPageEvent.run();
        if (!judgeQueryCompanyPageEvent.sideEffect) {
            Logger.infoAlsoSlack(
                "Either having multiple results or no result. Please check the webpage, and modify company name if necesary. **If you provided an url, make sure it is the OVERVIEW page not the REVIEW page.** There's also chance where the company has no review yet; or indeed there's no such company in Glassdoor yet.\n\n" +
                "Searching company name: " +
                this.searchCompanyName +
                "\nScraper looking at: " +
                this.driver.getCurrentUrl()
            );

            return;
        }

        // prepare to write data
        this.archiveManager = new ArchiveManager();

        // scrape company basic info
        final ScrapeBasicDataFromCompanyNamePage scrapeBasicDataFromCompanyNamePage = new ScrapeBasicDataFromCompanyNamePage(
            this.driver,
            this.archiveManager
        );
        scrapeBasicDataFromCompanyNamePage.run();

        final String orgPrefixSlackString = "*(" + scrapeBasicDataFromCompanyNamePage.sideEffect.companyName + ")* ";

        Logger.infoAlsoSlack(
            orgPrefixSlackString + "Basic data parsing completed, elasped time: " + scraperTaskTimer.captureOverallElapseDurationString()
        );

        // short circuit if no review data
        if (scrapeBasicDataFromCompanyNamePage.sideEffect.reviewNumberText.equals("--")) {
            Logger.infoAlsoSlack(orgPrefixSlackString + "Review number is -- so no need to scrape review page.");
            return;
        }

        // scrape review page
        final ScrapeReviewFromCompanyReviewPage scrapeReviewFromCompanyReviewPage = new ScrapeReviewFromCompanyReviewPage(
            driver,
            this.pubSubSubscription,
            this.archiveManager,
            scraperTaskTimer,
            scrapeBasicDataFromCompanyNamePage.sideEffect,
            this.orgPrefixSlackString
        );
        scrapeReviewFromCompanyReviewPage.run();

        // propogate session stats
        this.processedReviewsCount = scrapeReviewFromCompanyReviewPage.processedReviewsCount;
        this.wentThroughReviewsCount = scrapeReviewFromCompanyReviewPage.wentThroughReviewsCount;
        this.localReviewsCount = scrapeReviewFromCompanyReviewPage.localReviewCount;
        this.orgPrefixSlackString = "*(" + scrapeBasicDataFromCompanyNamePage.sideEffect.companyName + ")* ";
        this.doesCollidedReviewExist = scrapeReviewFromCompanyReviewPage.doesCollidedReviewExist;
        this.isFinalSession = scrapeReviewFromCompanyReviewPage.isFinalSession;
    }

    private void generateFinalSessionReport() {
        // validate scraper session
        final Float REVIEW_LOST_RATE_ALERT_THRESHOLD = Float.valueOf("0.03");
        final Float reviewLostRate = (float) (this.localReviewsCount - this.processedReviewsCount) / this.localReviewsCount;
        final Float reviewLostRatePercentage = reviewLostRate * (float) 100.0;
        if (reviewLostRate >= REVIEW_LOST_RATE_ALERT_THRESHOLD) {
            final String htmlDumpPath = this.archiveManager.writeHtml("reviewDataLostWarning", this.driver.getPageSource());

            Logger.warnAlsoSlack(
                this.orgPrefixSlackString +
                "Low processing rate " +
                reviewLostRatePercentage +
                "% " +
                "(" +
                this.processedReviewsCount +
                "/" +
                this.localReviewsCount +
                ")" +
                ". Last html stored at S3: `" +
                htmlDumpPath +
                "`" +
                "\nYou can access the last processed webpage at " +
                this.driver.getCurrentUrl() +
                ", see if there is indeed no next page available & that's all we can get." +
                "\nIf you are running for an org w/ existing review pool, you can ignore this warning."
            );
        }

        // send other session warnings
        if (this.doesCollidedReviewExist) {
            Logger.warnAlsoSlack(
                this.orgPrefixSlackString +
                "This session has collided / duplicated review data. Please refer to travisci log and check the collision(s) in s3."
            );
        }

        // extract company basic info
        Logger.infoAlsoSlack(
            "======= Success! =======\n" +
            this.orgPrefixSlackString +
            "Processed reviews count: " +
            this.processedReviewsCount +
            "/" +
            this.localReviewsCount +
            ", duration: " +
            this.scraperTaskTimer.captureOverallElapseDurationString()
        );
    }

    private void generateContinueSessionReport() {
        Logger.infoAlsoSlack(
            String.format(
                "=== Session finished, continuing cross session === %sprocessed/wentThrough/total, %s/%s/%s, duration %s",
                this.orgPrefixSlackString,
                this.processedReviewsCount,
                this.wentThroughReviewsCount,
                this.localReviewsCount,
                this.scraperTaskTimer.captureOverallElapseDurationString()
            )
        );
    }
}
