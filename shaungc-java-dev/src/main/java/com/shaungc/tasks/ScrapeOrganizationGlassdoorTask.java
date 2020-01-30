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

        SlackService.asyncSendMessage("scraper task started by company name: " + companyName);

        this.launchScraper();
    }

    public ScrapeOrganizationGlassdoorTask(final WebDriver driver, final URL companyOverviewPageUrl) throws ScraperException {
        this.driver = driver;
        this.searchCompanyName = null;
        this.companyOverviewPageUrl = companyOverviewPageUrl;

        SlackService.asyncSendMessage("scraper task started by url: " + companyOverviewPageUrl);

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
                    "Either having multiple results or no result. Please check the webpage, and modify company name if necesary. There's also chance where the company has no review yet; or indeed there's no such company in Glassdoor yet.\n\n"
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

        Logger.infoAlsoSlack("Basic data parsing completed, elasped time:\n" + scraperTaskTimer.getElapseDurationString());
        
        // short circuit if no review data
        if (scrapeBasicDataFromCompanyNamePage.sideEffect.reviewNumberText == "==") {
            Logger.infoAlsoSlack("Review number is -- so no need to scrape review page.");
            return;
        }

        // scrape review page
        final ScrapeReviewFromCompanyReviewPage scrapeReviewFromCompanyReviewPage = new ScrapeReviewFromCompanyReviewPage(
                driver, archiveManager);
        scrapeReviewFromCompanyReviewPage.run();
        
        // expose data pack
        this.scrapedBasicData = scrapeBasicDataFromCompanyNamePage.sideEffect;
        this.scrapedReviewData = scrapeReviewFromCompanyReviewPage.sideEffect;

        // extract company basic info
        Logger.infoAlsoSlack("Success!" + 
            "\nProcessed reviews count: " + this.scrapedReviewData.reviewMetadata.localReviewCount +
            "\nDuration: " + scraperTaskTimer.getElapseDurationString());
    }
}