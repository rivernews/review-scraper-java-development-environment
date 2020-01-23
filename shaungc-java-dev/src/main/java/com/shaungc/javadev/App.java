package com.shaungc.javadev;

import org.openqa.selenium.WebDriver;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) throws Exception {
        // ContainerRemoteWebDriver driver = new ContainerRemoteWebDriver();
        WebDriver driver = WebDriverFactory.create();

        new LoginGlassdoorTask(driver);

        new ScrapeOrganizationGlassdoorTask(driver, "DigitalOcean");

        driver.close();
    }
}
