package com.shaungc.javadev;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) throws Exception {
        // ContainerRemoteWebDriver driver = new ContainerRemoteWebDriver();
        ChromeDriver driver = new ChromeDriver();

        new LoginGlassdoorTask(driver);

        new ScrapeOrganizationGlassdoorTask(driver, "DigitalOcean");

        driver.close();
    }
}
