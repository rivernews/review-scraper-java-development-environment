package com.shaungc.javadev;

import com.shaungc.utilities.Logger;
import com.shaungc.utilities.RequestAddressValidator;
import java.net.URL;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * ContainerRemoteWebDriver
 */
public class WebDriverFactory {

    public static WebDriver create(URL webDriverServiceUrl) {
        return (WebDriver) new RemoteWebDriver(webDriverServiceUrl, new ChromeOptions());
    }

    public static WebDriver create() {
        final ChromeOptions chromeOptions = new ChromeOptions();

        if (Configuration.DEBUG) {
            Logger.info("======DEBUG MODE======");

            String webDriverServiceUrl = Configuration.RUNNING_FROM_CONTAINER ? "host.docker.internal" : "localhost";

            chromeOptions.addArguments("--disable-extensions");
            chromeOptions.addArguments("--start-maximized");

            return (WebDriver) new RemoteWebDriver(
                RequestAddressValidator.toURL("http://" + webDriverServiceUrl + ":4444/wd/hub"),
                chromeOptions
            );
        } else {
            Logger.info("======PRODUCTION MODE======");

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
