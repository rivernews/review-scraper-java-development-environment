package com.shaungc.javadev;

import org.openqa.selenium.WebDriver;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) {
        WebDriver driver = null;
        try {
            driver = WebDriverFactory.create();

            new LoginGlassdoorTask(driver);

            new ScrapeOrganizationGlassdoorTask(driver, "DigitalOcean");

            driver.quit();
        } catch (Exception e) {
            if (driver != null) {
                driver.quit();
            }
            
            if (Configuration.DEBUG) {
                System.out.println("ERROR: program ended in exception block...!");
            }
        }

        return;
    }
}
