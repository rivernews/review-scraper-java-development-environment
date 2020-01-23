package com.shaungc.javadev;

import java.net.MalformedURLException;
import java.net.URL;

import com.shaungc.events.JudgeQueryCompanyPageEvent;
import com.shaungc.events.ScrapeBasicDataFromCompanyNamePage;
import com.shaungc.events.ScrapeReviewFromCompanyReviewPage;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

/**
 * ScrapeOrganizationGlassdoorTask
 */
public class ScrapeOrganizationGlassdoorTask {
    private final WebDriver driver;
    final private String searchCompanyName;
    final private URL companyOverviewPageUrl;

    public ScrapeOrganizationGlassdoorTask(final WebDriver driver, final String companyName) {
        this.driver = driver;
        this.searchCompanyName = companyName;
        this.companyOverviewPageUrl = null;

        this.launchScraper();
    }

    public ScrapeOrganizationGlassdoorTask(final WebDriver driver, final URL companyOverviewPageUrl) {
        this.driver = driver;
        this.searchCompanyName = null;
        this.companyOverviewPageUrl = companyOverviewPageUrl;

        this.launchScraper();
    }

    private void launchScraper() {
        // Access company overview page
        if (this.companyOverviewPageUrl != null) {
            this.driver.get(this.companyOverviewPageUrl.toString());
        } else if (this.searchCompanyName != null) {
            this.driver.get(String.format(
                    "https://www.glassdoor.com/Reviews/company-reviews.htm?suggestCount=10&suggestChosen=false&clickSource=searchBtn&typedKeyword=%s&sc.keyword=%s&locT=C&locId=&jobType=",
                    this.searchCompanyName, this.searchCompanyName));
        } else {
            System.out.println("WARN: no company overview url or name provided, will not scrape for this company.");
            return;
        }

        // TODO: handle timeout

        // Print Title
        System.out.println(this.driver.getTitle());

        // identify no result / exactly one / multiple results
        final JudgeQueryCompanyPageEvent judgeQueryCompanyPageEvent = new JudgeQueryCompanyPageEvent(this.driver);
        judgeQueryCompanyPageEvent.run();

        if (!judgeQueryCompanyPageEvent.sideEffect) {
            System.out.println(
                    "Either having multiple results or no result. Please check the webpage, and modify company name if necesary. There's also chance where the company has no review yet; or indeed there's no such company in Glassdoor yet.\n\n"
                            + "Searching company name: " + this.searchCompanyName + "\nScraper looking at: "
                            + this.driver.getCurrentUrl());

            return;
        }

        final ScrapeBasicDataFromCompanyNamePage scrapeBasicDataFromCompanyNamePage = new ScrapeBasicDataFromCompanyNamePage(
                this.driver);
        scrapeBasicDataFromCompanyNamePage.run();

        if (scrapeBasicDataFromCompanyNamePage.sideEffect.reviewNumberText == "==") {
            System.out.println("Review number is -- so no need to scrape review page.");
            return;
        }

        // navigate to reviews page
        this.driver.findElement(By.cssSelector("article[id*=WideCol] a.eiCell.reviews")).click();

        // scrape review page
        final ScrapeReviewFromCompanyReviewPage scrapeReviewFromCompanyReviewPage = new ScrapeReviewFromCompanyReviewPage(
                driver);
        scrapeReviewFromCompanyReviewPage.run();

        // extract company basic info
        System.out.println("Success!");
    }
}