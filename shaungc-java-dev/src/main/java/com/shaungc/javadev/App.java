package com.shaungc.javadev;

import java.net.MalformedURLException;
import java.net.URL;

import com.shaungc.exceptions.ScraperException;
import com.shaungc.tasks.LoginGlassdoorTask;
import com.shaungc.tasks.ScrapeOrganizationGlassdoorTask;
import com.shaungc.utilities.Logger;

import org.openqa.selenium.WebDriver;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) {
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
        } catch (ScraperException e) {
            Logger.info(e.getMessage());
            Logger.errorAlsoSlack("A scraper exception is raised and its message is logged above; which is not an error of the program, but more of the webpage the scraper is dealing with. There is something special with the webpage. Refer to the current url of the scraper to investigate more: " + driver.getCurrentUrl());
        } catch (Exception e) {
            Logger.error("Program ended in exception block...!\n\n");

            if (driver != null) {
                driver.quit();
            }
            
            throw e;
        }

        return;
    }
}
