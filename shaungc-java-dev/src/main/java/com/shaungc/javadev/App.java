package com.shaungc.javadev;

import java.net.MalformedURLException;
import java.net.URL;

import org.openqa.selenium.WebDriver;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) throws MalformedURLException {
        WebDriver driver = null;
        try {
            driver = WebDriverFactory.create();

            new LoginGlassdoorTask(driver);

            try {
                URL companyOverviewPageUrl = new URL(Configuration.TEST_COMPANY_INFORMATION_STRING);
                new ScrapeOrganizationGlassdoorTask(driver, companyOverviewPageUrl);
            } catch (MalformedURLException e) {
                if (Configuration.TEST_COMPANY_INFORMATION_STRING != null) {
                    new ScrapeOrganizationGlassdoorTask(driver, Configuration.TEST_COMPANY_INFORMATION_STRING);
                } else {
                    // new ScrapeOrganizationGlassdoorTask(driver, "DigitalOcean");
                    // new ScrapeOrganizationGlassdoorTask(driver, "Waymo");
                    new ScrapeOrganizationGlassdoorTask(driver, "23AndMe");
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

        return;
    }
}
