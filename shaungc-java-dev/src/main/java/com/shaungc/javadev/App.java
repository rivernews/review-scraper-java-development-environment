package com.shaungc.javadev;

import org.openqa.selenium.WebDriver;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) {
        WebDriver driver = WebDriverFactory.create();
        try {
            new LoginGlassdoorTask(driver);

            new ScrapeOrganizationGlassdoorTask(driver, "DigitalOcean");

            driver.quit();
        } catch (Exception e) {
            driver.quit();
            throw e;
        }

        return;
    }
}
