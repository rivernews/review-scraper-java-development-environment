package com.shaungc.javadev;


/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) throws Exception {
        ContainerRemoteWebDriver driver = new ContainerRemoteWebDriver();

        new LoginGlassdoorTask(driver);

        new ScrapeOrganizationGlassdoorTask(driver, "DigitalOcean");

        driver.close();
    }
}
