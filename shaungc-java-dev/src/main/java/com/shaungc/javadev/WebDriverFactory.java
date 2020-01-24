package com.shaungc.javadev;

import java.net.MalformedURLException;
import java.net.URL;

import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;


/**
 * ContainerRemoteWebDriver
 */
public class WebDriverFactory {

    public static WebDriver create(URL webDriverServiceUrl) {
        return (WebDriver) new RemoteWebDriver(webDriverServiceUrl, new ChromeOptions());
    }

    public static WebDriver create() throws MalformedURLException {
        final ChromeOptions chromeOptions = new ChromeOptions();

        if (Configuration.DEBUG) {
            System.out.println("======DEBUG MODE======");

            String webDriverServiceUrl = Configuration.RUNNING_FROM_CONTAINER ? "host.docker.internal" : "localhost";

            chromeOptions.addArguments("--disable-extensions");
            chromeOptions.addArguments("--start-maximized");

            return (WebDriver) new RemoteWebDriver(
                new URL("http://" + webDriverServiceUrl + ":4444/wd/hub"),
                chromeOptions
            );
        } else {
            System.out.println("======PRODUCTION MODE======");

            // use headless mode to improve performance
            chromeOptions.addArguments("--headless");
            chromeOptions.addArguments("--disable-gpu");
            // chrome will fail on insecure connection in headless mode
            chromeOptions.addArguments("--ignore-certificate-errors");

            // added to solve no stdout issue
            chromeOptions.addArguments("--window-size=1920,1080");
            chromeOptions.addArguments("--disable-extensions");
            chromeOptions.addArguments("--proxy-server='direct://'");
            chromeOptions.addArguments("--proxy-bypass-list=*");
            chromeOptions.addArguments("--start-maximized");
            chromeOptions.addArguments("--no-sandbox");

            // clearer log
            // https://stackoverflow.com/a/20748376/9814131
            chromeOptions.addArguments("--log-level=3");

            return new ChromeDriver(chromeOptions);
        }
    }
}