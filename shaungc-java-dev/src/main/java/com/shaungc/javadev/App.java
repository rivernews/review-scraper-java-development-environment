package com.shaungc.javadev;

import java.net.URL;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) throws Exception {
        // DesiredCapabilities dcap = DesiredCapabilities.chrome();
        ChromeOptions chromeOptions = new ChromeOptions();
        
        // String driverPath = System.getProperty("user.dir") + "/tmp/chromedriver";
        // System.setProperty("webdriver.chrome.driver", driverPath);

        // You should check the Port No here.
        URL gamelan = new URL("http://host.docker.internal:4444/wd/hub");
        WebDriver driver = new RemoteWebDriver(gamelan, chromeOptions);
        // Get URL
        driver.get("https://www.google.com/");
        // Print Title
        System.out.println(driver.getTitle());
        driver.close();
    }
}
