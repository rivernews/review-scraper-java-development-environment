package com.shaungc.javadev;

import java.net.MalformedURLException;
import java.net.URL;

import com.shaungc.dataStorage.ArchiveManager;

import org.openqa.selenium.WebDriver;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) throws MalformedURLException {
        WebDriver driver = null;
        ScrapeOrganizationGlassdoorTask scrapeCompanyTask = null;
        try {
            driver = WebDriverFactory.create();

            new LoginGlassdoorTask(driver);

            // launch scraping task against a company
            // TODO: scale up to accept a list of company inputs
            try {
                URL companyOverviewPageUrl = new URL(Configuration.TEST_COMPANY_INFORMATION_STRING);
                scrapeCompanyTask = new ScrapeOrganizationGlassdoorTask(driver, companyOverviewPageUrl);
            } catch (MalformedURLException e) {
                if (Configuration.TEST_COMPANY_INFORMATION_STRING != null) {
                    scrapeCompanyTask = new ScrapeOrganizationGlassdoorTask(driver, Configuration.TEST_COMPANY_INFORMATION_STRING);
                } else {
                    // new ScrapeOrganizationGlassdoorTask(driver, "DigitalOcean");
                    // new ScrapeOrganizationGlassdoorTask(driver, "Waymo");
                    scrapeCompanyTask = new ScrapeOrganizationGlassdoorTask(driver, "23AndMe");
                }
            }

            driver.quit();
        } catch (Exception e) {
            System.out.println("\n\nERROR: program ended in exception block...!\n\n\n");

            if (driver != null) {
                driver.quit();
            }
            
            throw e;
        }

        // data archiving
        if (scrapeCompanyTask != null) {
            ArchiveManager archiveManager = new ArchiveManager();
            archiveManager.jsonDump(scrapeCompanyTask.scrapedBasicData.companyName, scrapeCompanyTask.scrapedBasicData);
            archiveManager.jsonDump(scrapeCompanyTask.scrapedBasicData.companyName + " Reviews", scrapeCompanyTask.scrapedReviewData);
        }

        return;
    }
}
