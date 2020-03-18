package com.shaungc.javadev;

import com.shaungc.exceptions.ScraperShouldHaltException;
import com.shaungc.utilities.ExternalServiceMode;
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

    // public static WebDriver create(URL webDriverServiceUrl) {
    //     return (WebDriver) new RemoteWebDriver(webDriverServiceUrl, new ChromeOptions());
    // }

    public static WebDriver create() {
        final ChromeOptions chromeOptions = new ChromeOptions();

        if (Configuration.DEBUG) {
            Logger.info("======DEBUG MODE======");
        } else {
            Logger.info("======PRODUCTION MODE======");
        }

        if (Configuration.WEBDRIVER_MODE.equals(ExternalServiceMode.LOCAL_INSTALLED.getString())) {
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

            // enable garbage collection function
            chromeOptions.addArguments("-js-flags=--expose-gc");

            // clearer log
            // https://stackoverflow.com/a/20748376/9814131
            chromeOptions.addArguments("--log-level=3");

            return new ChromeDriver(chromeOptions);
        } else if (
            Configuration.WEBDRIVER_MODE.equals(ExternalServiceMode.SERVER_FROM_MACOS_DOCKER_CONTAINER.getString()) ||
            Configuration.WEBDRIVER_MODE.equals(ExternalServiceMode.SERVER_FROM_PORT_FORWARD.getString()) ||
            Configuration.WEBDRIVER_MODE.equals(ExternalServiceMode.SERVER_FROM_CUSTOM_HOST.getString())
        ) {
            String webDriverServiceUrl = Configuration.WEBDRIVER_MODE.equals(
                    ExternalServiceMode.SERVER_FROM_MACOS_DOCKER_CONTAINER.getString()
                )
                ? "host.docker.internal" // use local (on macos laptop) selenium server running in another docker container
                : Configuration.WEBDRIVER_MODE.equals(ExternalServiceMode.SERVER_FROM_PORT_FORWARD.getString())
                    ? "localhost" // use port-forwarding
                    : Configuration.SELENIUM_SERVER_CUSTOM_HOST;

            if (webDriverServiceUrl.strip().isEmpty()) {
                throw new ScraperShouldHaltException(
                    "Webdriver misconfigured. Did you set environment variable `SELENIUM_SERVER_CUSTOM_HOST`?"
                );
            }

            chromeOptions.addArguments("--disable-extensions");
            chromeOptions.addArguments("--start-maximized");
            // solving chrome driver issue
            // `UnknownError: session deleted because of page crash from tab crashed`
            // https://stackoverflow.com/a/53970825/9814131
            chromeOptions.addArguments("--no-sandbox");

            return (WebDriver) new RemoteWebDriver(
                RequestAddressValidator.toURL("http://" + webDriverServiceUrl + ":4444/wd/hub"),
                chromeOptions
            );
        } else {
            throw new ScraperShouldHaltException("Webdriver mode is misconfigured");
        }
    }
}
