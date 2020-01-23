package com.shaungc.javadev;

import java.net.MalformedURLException;

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

            // new ScrapeOrganizationGlassdoorTask(driver, "DigitalOcean");
            // new ScrapeOrganizationGlassdoorTask(driver, "Waymo");
            new ScrapeOrganizationGlassdoorTask(driver, "23AndMe");

            driver.quit();
        } catch (Exception e) {
            if (driver != null) {
                driver.quit();
            }
            
            if (Configuration.DEBUG) {
                System.out.println("\n\nERROR: program ended in exception block...!\n\n\n");
                throw e;
            }
        }

        return;
    }
}
