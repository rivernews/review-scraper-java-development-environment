package com.shaungc.tasks;

import com.shaungc.dataStorage.ArchiveManager;
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
import java.time.Duration;
import org.openqa.selenium.WebDriver;

/**
 * ScrapeOrganizationGlassdoorTask
 */
public class ScrapeOrganizationGlassdoorTask {
    private final String searchCompanyName;
    private final URL companyOverviewPageUrl;

    /** session utilities */
    private final WebDriver driver;
    public Timer scraperTaskTimer;
    public ArchiveManager archiveManager;
    private final PubSubSubscription pubSubSubscription;

    /** session stats */
    private String orgPrefixSlackString;
    public Integer processedReviewsCount;
    public Integer wentThroughReviewsCount;
    public Integer localReviewsCount;
    public Integer processedReviewPages;

    public Boolean isFinalSession;

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
     * Constructor for renewal mode
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
        if (this.pubSubSubscription.receivedTerminationRequest) {
            throw new ScraperException("Terminating per request");
        }

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
        this.scraperTaskTimer =
            new Timer(
                Configuration.TEST_COMPANY_LAST_PROGRESS_DURATION,
                Duration.ofMinutes(Configuration.CROSS_SESSION_TIME_LIMIT_MINUTES)
            );

        this.archiveManager = new ArchiveManager(Configuration.TEST_COMPANY_NAME, Configuration.TEST_COMPANY_ID);

        this.orgPrefixSlackString = "*(" + Configuration.TEST_COMPANY_NAME + ")* ";
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
        this.processedReviewPages = scrapeReviewFromCompanyReviewPage.processedReviewPages;
        // this.orgPrefixSlackString should be assigned before review scraper
        this.isFinalSession = scrapeReviewFromCompanyReviewPage.isFinalSession;
    }

    private void launchSessionScraper() throws ScraperException {
        this.scraperTaskTimer = new Timer(Duration.ofMinutes(Configuration.CROSS_SESSION_TIME_LIMIT_MINUTES));

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

        this.orgPrefixSlackString = "*(" + scrapeBasicDataFromCompanyNamePage.sideEffect.companyName + ")* ";

        Logger.infoAlsoSlack(
            this.orgPrefixSlackString +
            "Basic data parsing completed, elasped time: " +
            this.scraperTaskTimer.captureOverallElapseDurationString()
        );

        // short circuit if no review data
        if (scrapeBasicDataFromCompanyNamePage.sideEffect.reviewNumberText.equals("--")) {
            Logger.infoAlsoSlack(this.orgPrefixSlackString + "Review number is -- so no need to scrape review page.");
            return;
        }

        // scrape review page
        final ScrapeReviewFromCompanyReviewPage scrapeReviewFromCompanyReviewPage = new ScrapeReviewFromCompanyReviewPage(
            driver,
            this.pubSubSubscription,
            this.archiveManager,
            this.scraperTaskTimer,
            scrapeBasicDataFromCompanyNamePage.sideEffect,
            this.orgPrefixSlackString
        );
        scrapeReviewFromCompanyReviewPage.run();

        // propogate session stats
        this.processedReviewsCount = scrapeReviewFromCompanyReviewPage.processedReviewsCount;
        this.wentThroughReviewsCount = scrapeReviewFromCompanyReviewPage.wentThroughReviewsCount;
        this.localReviewsCount = scrapeReviewFromCompanyReviewPage.localReviewCount;
        this.processedReviewPages = scrapeReviewFromCompanyReviewPage.processedReviewPages;
        // this.orgPrefixSlackString is set above
        this.isFinalSession = scrapeReviewFromCompanyReviewPage.isFinalSession;
    }

    private void generateFinalSessionReport() {
        // validate scraper session
        // process rate is expected to be at least 90% or higher, since the major factor of 'lost',
        // except running for an existing org, is that a page might contain a duplicated review from
        // previous page, and this should only occur up to 1 review per page. Given that 1 page contains
        // 10 reviews, the upper bound of lost rate should be 10%.
        // We raise the process rate alert to 98% is just to be more careful - you can easily check if
        // indeed there's no next page link by visiting the last processed review page
        final Float REVIEW_WENTTHROUGH_RATE_ALERT_THRESHOLD = Float.valueOf("0.98");
        final Float reviewStoredRate = (float) (this.processedReviewsCount) / this.localReviewsCount;
        final Float reviewWentThroughRate = (float) (this.wentThroughReviewsCount) / this.localReviewsCount;
        final Float reviewStoredRatePercentage = reviewStoredRate * (float) 100.0;
        final Float reviewWentThroughRatePercentage = reviewWentThroughRate * (float) 100.0;
        if (reviewWentThroughRate < REVIEW_WENTTHROUGH_RATE_ALERT_THRESHOLD) {
            Logger.warnAlsoSlack(
                String.format(
                    "%s Low went through rate %.2f%% (write rate %.2f%%), %d/%d/%d. Visit <%s|last processed review page> and see if indeed no next page available. If next page available, please check why scraper did not capture the next page link. <%s|Download dumped html file>.",
                    this.orgPrefixSlackString,
                    reviewWentThroughRatePercentage,
                    reviewStoredRatePercentage,
                    this.processedReviewsCount,
                    this.wentThroughReviewsCount,
                    this.localReviewsCount,
                    this.driver.getCurrentUrl(),
                    this.archiveManager.getFullUrlOnS3FromFilePathBasedOnOrgDirectory(
                            this.archiveManager.writeHtml("reviewLowWentThroughRateWarning", this.driver.getPageSource())
                        )
                )
            );
        }

        // extract company basic info
        Logger.infoAlsoSlack(
            String.format(
                "======= Success! =======\n" + "%s(%s) Processed reviews count %s/%s/%s, `%s` sessions used.",
                this.orgPrefixSlackString,
                this.scraperTaskTimer.captureOverallElapseDurationString(),
                this.processedReviewsCount,
                this.wentThroughReviewsCount,
                this.localReviewsCount,
                (Configuration.TEST_COMPANY_LAST_PROGRESS_SESSION + 1)
            )
        );
    }

    private void generateContinueSessionReport() {
        Logger.infoAlsoSlack(
            String.format(
                "=== Session finished, continuing cross session ===\n" + "%s(%s) Processed reviews count %s/%s/%s, `%d` sessions used.",
                this.orgPrefixSlackString,
                this.scraperTaskTimer.captureOverallElapseDurationString(),
                this.processedReviewsCount,
                this.wentThroughReviewsCount,
                this.localReviewsCount,
                (Configuration.TEST_COMPANY_LAST_PROGRESS_SESSION + 1)
            )
        );
    }
}
