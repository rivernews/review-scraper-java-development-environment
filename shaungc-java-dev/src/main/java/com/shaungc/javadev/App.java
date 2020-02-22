package com.shaungc.javadev;

import java.net.MalformedURLException;
import java.net.URL;

import com.shaungc.exceptions.ScraperException;
import com.shaungc.tasks.LoginGlassdoorTask;
import com.shaungc.tasks.ScrapeOrganizationGlassdoorTask;
import com.shaungc.utilities.Logger;
import com.shaungc.utilities.PubSubSubscription;
import com.shaungc.utilities.ScraperJobMessageTo;
import com.shaungc.utilities.ScraperJobMessageType;

import org.openqa.selenium.WebDriver;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) {
        PubSubSubscription pubSubSubscription = new PubSubSubscription();

        WebDriver driver = null;
        ScrapeOrganizationGlassdoorTask scrapeCompanyTask = null;
        try {
            driver = WebDriverFactory.create();

            new LoginGlassdoorTask(driver);

            // launch scraping task against a company
            // TODO: scale up to accept a list of company inputs
            try {
                URL companyOverviewPageUrl = new URL(Configuration.TEST_COMPANY_INFORMATION_STRING);
                Logger.info("Scrape by url: " + companyOverviewPageUrl);
                scrapeCompanyTask = new ScrapeOrganizationGlassdoorTask(driver, companyOverviewPageUrl);
            } catch (MalformedURLException e) {
                Logger.info("Scrape by company name: " + Configuration.TEST_COMPANY_INFORMATION_STRING);
                if (Configuration.TEST_COMPANY_INFORMATION_STRING != null) {
                    scrapeCompanyTask = new ScrapeOrganizationGlassdoorTask(driver, Configuration.TEST_COMPANY_INFORMATION_STRING);
                } else {
                    // new ScrapeOrganizationGlassdoorTask(driver, "DigitalOcean");
                    // new ScrapeOrganizationGlassdoorTask(driver, "Waymo");
                    scrapeCompanyTask = new ScrapeOrganizationGlassdoorTask(driver, "23AndMe");
                }
            }

            pubSubSubscription.publish(String.format("%s:%s:%s", ScraperJobMessageType.FINISH.getString(), ScraperJobMessageTo.SLACK_MD_SVC.getString(), "OK!"));
            pubSubSubscription.cleanup();
            driver.quit();

            return;
        } catch (ScraperException e) {
            Logger.info(e.getMessage());
            Logger.errorAlsoSlack("A scraper exception is raised and its message is logged above; which is not an error of the program, but more of the webpage the scraper is dealing with. There is something special with the webpage. Refer to the current url of the scraper to investigate more: " + driver.getCurrentUrl());

            pubSubSubscription.publish(String.format("%s:%s:%s", ScraperJobMessageType.ERROR.getString(), ScraperJobMessageTo.SLACK_MD_SVC.getString(), "ScraperException: " + e.getMessage()));

            pubSubSubscription.cleanup();
            if (driver != null) {
                driver.quit();
            }

            return;
        } catch (Exception e) {
            Logger.error("Program ended in exception block...!\n\n");
            
            System.out.println(e);

            pubSubSubscription.publish(String.format("%s:%s:%s", ScraperJobMessageType.ERROR.getString(), ScraperJobMessageTo.SLACK_MD_SVC.getString(), "Exception: " + e.getMessage()));

            pubSubSubscription.cleanup();
            if (driver != null) {
                driver.quit();
            }
            
            throw e;
        }
    }
}
