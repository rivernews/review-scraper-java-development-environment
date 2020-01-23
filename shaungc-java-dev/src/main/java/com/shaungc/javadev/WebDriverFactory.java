package com.shaungc.javadev;

import java.net.MalformedURLException;
import java.net.URL;

import org.openqa.selenium.remote.DesiredCapabilities;
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
        if (Configuration.DEBUG) {
            System.out.println("======DEBUG MODE======");

            String webDriverServiceUrl = Configuration.RUNNING_FROM_CONTAINER ? "host.docker.internal" : "localhost";

            return (WebDriver) new RemoteWebDriver(
                new URL("http://" + webDriverServiceUrl + ":4444/wd/hub"),
                new ChromeOptions()
            );
        } else {
            System.out.println("======PRODUCTION MODE======");

            final ChromeOptions chromeOptions = new ChromeOptions();
            chromeOptions.addArguments("--headless");
            chromeOptions.addArguments("--disable-gpu");
            // chromeOptions.addArguments("--window-size=1920,1080");
            // chromeOptions.addArguments("--disable-extensions");
            // chromeOptions.addArguments("--proxy-server='direct://'");
            // chromeOptions.addArguments("--proxy-bypass-list=*");
            // chromeOptions.addArguments("--start-maximized");
            // chromeOptions.addArguments("--no-sandbox");
            chromeOptions.addArguments("--ignore-certificate-errors");

            // deprecated
            // final DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
            // desiredCapabilities.setJavascriptEnabled(true);
            // desiredCapabilities.setCapability(ChromeOptions.CAPABILITY, chromeOptions);

            return new ChromeDriver(chromeOptions);
        }
    }
}