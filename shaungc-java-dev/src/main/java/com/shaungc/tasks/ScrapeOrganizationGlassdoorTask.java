package com.shaungc.tasks;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.Date;

import com.shaungc.dataStorage.ArchiveManager;
import com.shaungc.dataTypes.BasicParsedData;
import com.shaungc.dataTypes.GlassdoorCompanyReviewParsedData;
import com.shaungc.events.JudgeQueryCompanyPageEvent;
import com.shaungc.events.ScrapeBasicDataFromCompanyNamePage;
import com.shaungc.events.ScrapeReviewFromCompanyReviewPage;
import com.shaungc.exceptions.ScraperException;
import com.shaungc.utilities.Logger;
import com.shaungc.utilities.SlackService;
import com.shaungc.utilities.Timer;

import org.openqa.selenium.WebDriver;


/**
 * ScrapeOrganizationGlassdoorTask
 */
public class ScrapeOrganizationGlassdoorTask {
    private final WebDriver driver;
    final private String searchCompanyName;
    final private URL companyOverviewPageUrl;

    public GlassdoorCompanyReviewParsedData scrapedReviewData = null;
    public BasicParsedData scrapedBasicData = null;

    public ScrapeOrganizationGlassdoorTask(final WebDriver driver, final String companyName) throws ScraperException {
        this.driver = driver;
        this.searchCompanyName = companyName;
        this.companyOverviewPageUrl = null;

        SlackService.asyncSendMessage("Scraper task started by company name: " + companyName);

        this.launchScraper();
    }

    public ScrapeOrganizationGlassdoorTask(final WebDriver driver, final URL companyOverviewPageUrl) throws ScraperException {
        this.driver = driver;
        this.searchCompanyName = null;
        this.companyOverviewPageUrl = companyOverviewPageUrl;

        Logger.infoAlsoSlack("Scraper task started by url: " + companyOverviewPageUrl);

        this.launchScraper();
    }

    private void launchScraper() throws ScraperException {
        Timer scraperTaskTimer = new Timer();

        // Access company overview page
        if (this.companyOverviewPageUrl != null) {
            this.driver.get(this.companyOverviewPageUrl.toString());
        } else if (this.searchCompanyName != null) {
            this.driver.get(String.format(
                    "https://www.glassdoor.com/Reviews/company-reviews.htm?suggestCount=10&suggestChosen=false&clickSource=searchBtn&typedKeyword=%s&sc.keyword=%s&locT=C&locId=&jobType=",
                    this.searchCompanyName, this.searchCompanyName));
        } else {
            Logger.info("WARN: no company overview url or name provided, will not scrape for this company.");
            return;
        }

        // TODO: handle timeout - if failed to get company overview page 

        // identify no result / exactly one / multiple results
        final JudgeQueryCompanyPageEvent judgeQueryCompanyPageEvent = new JudgeQueryCompanyPageEvent(this.driver);
        judgeQueryCompanyPageEvent.run();
        if (!judgeQueryCompanyPageEvent.sideEffect) {
            Logger.infoAlsoSlack(
                    "Either having multiple results or no result. Please check the webpage, and modify company name if necesary. **If you provided an url, make sure it is the OVERVIEW page not the REVIEW page.** There's also chance where the company has no review yet; or indeed there's no such company in Glassdoor yet.\n\n"
                            + "Searching company name: " + this.searchCompanyName + "\nScraper looking at: "
                            + this.driver.getCurrentUrl());

            return;
        }

        // prepare to write data
        ArchiveManager archiveManager = new ArchiveManager();

        // scrape company basic info
        final ScrapeBasicDataFromCompanyNamePage scrapeBasicDataFromCompanyNamePage = new ScrapeBasicDataFromCompanyNamePage(
                this.driver, archiveManager);
        scrapeBasicDataFromCompanyNamePage.run();

        Logger.infoAlsoSlack(
            "*(" + scrapeBasicDataFromCompanyNamePage.sideEffect.companyName + ")* " +
            "Basic data parsing completed, elasped time:\n" + scraperTaskTimer.captureElapseDurationString()
        );
        
        // short circuit if no review data
        if (scrapeBasicDataFromCompanyNamePage.sideEffect.reviewNumberText.equals("--")) {
            Logger.infoAlsoSlack(
                "*(" + scrapeBasicDataFromCompanyNamePage.sideEffect.companyName + ")* " +
                "Review number is -- so no need to scrape review page."
            );
            return;
        }

        // scrape review page
        final ScrapeReviewFromCompanyReviewPage scrapeReviewFromCompanyReviewPage = new ScrapeReviewFromCompanyReviewPage(
                driver, archiveManager, scraperTaskTimer, scrapeBasicDataFromCompanyNamePage.sideEffect);
        scrapeReviewFromCompanyReviewPage.run();
        
        // expose data pack
        this.scrapedBasicData = scrapeBasicDataFromCompanyNamePage.sideEffect;
        this.scrapedReviewData = scrapeReviewFromCompanyReviewPage.sideEffect;

        // validate scraper session
        final Float REVIEW_LOST_RATE_ALERT_THRESHOLD = Float.valueOf("0.03");
        final Float reviewLostRate = (float) (this.scrapedReviewData.reviewMetadata.localReviewCount - scrapeReviewFromCompanyReviewPage.processedReviewsCount) / this.scrapedReviewData.reviewMetadata.localReviewCount;
        final Float reviewLostRatePercentage = reviewLostRate * (float) 100.0;
        if (reviewLostRate >= REVIEW_LOST_RATE_ALERT_THRESHOLD) {
            final String htmlDumpPath = archiveManager.writeHtml("reviewDataLostWarning", this.driver.getPageSource());
            
            Logger.warnAlsoSlack("WARN: major review data lost rate " + reviewLostRatePercentage + "% " +
                "(" + scrapeReviewFromCompanyReviewPage.processedReviewsCount + "/" + this.scrapedReviewData.reviewMetadata.localReviewCount + ")" +
                ". Last html stored at S3: `" + htmlDumpPath + "`" +
                "\nYou can access the last processed webpage at " + this.driver.getCurrentUrl() + 
                ", see if there is indeed no next page available & that's all we can get." + 
                "\nIf you are running for an org w/ existing review pool, you can ignore this warning."
            );
        }

        // send other session warnings
        if (scrapeReviewFromCompanyReviewPage.doesCollidedReviewExist) {
            Logger.warnAlsoSlack(
                "(" + scrapeBasicDataFromCompanyNamePage.sideEffect.companyName + ") " +
                "This session has collided / duplicated review data. Please refer to travisci log and check the collision(s) in s3."
            );
        }

        // extract company basic info
        Logger.infoAlsoSlack("======= Success! =======" + 
            "\nProcessed reviews count: " + scrapeReviewFromCompanyReviewPage.processedReviewsCount + "/" + this.scrapedReviewData.reviewMetadata.localReviewCount +
            "\nDuration: " + scraperTaskTimer.captureElapseDurationString());
    }
}
