package com.shaungc.javadev;

import com.shaungc.exceptions.ScraperShouldHaltException;
import com.shaungc.utilities.ExternalServiceMode;
import com.shaungc.utilities.Logger;
import com.shaungc.utilities.RequestAddressValidator;
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
            // https://stackoverflow.com/a/54789156/9814131
            chromeOptions.addArguments("-js-flags=--expose-gc");

            // extra stuff trying to avoid rendering timeout
            // https://stackoverflow.com/a/52340526
            chromeOptions.addArguments("enable-automation");
            chromeOptions.addArguments("--disable-infobars");
            chromeOptions.addArguments("--disable-dev-shm-usage");
            chromeOptions.addArguments("--disable-browser-side-navigation");

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

            // enable garbage collection function
            // https://stackoverflow.com/a/54789156/9814131
            chromeOptions.addArguments("-js-flags=--expose-gc");

            // TODO: remove this
            // further measures on improving performance / reducing memory utilization
            // use headless mode to improve performance
            // note that headless mode may increase the chance for NoSuchElement exceptions
            // because headless mode is fast and webpage may not be fully loaded yet
            // but travis build are using headless and succeeded for many so should not be
            // an issue; just increase wait timeout, and perhaps retry where possible
            chromeOptions.addArguments("--headless");
            chromeOptions.addArguments("--disable-gpu");
            // chrome will fail on insecure connection in headless mode
            chromeOptions.addArguments("--ignore-certificate-errors");

            // added to solve no stdout issue
            chromeOptions.addArguments("--window-size=1920,1080");
            chromeOptions.addArguments("--proxy-server='direct://'");
            chromeOptions.addArguments("--proxy-bypass-list=*");

            chromeOptions.addArguments("--log-level=5");

            return (WebDriver) new RemoteWebDriver(
                RequestAddressValidator.toURL("http://" + webDriverServiceUrl + ":4444/wd/hub"),
                chromeOptions
            );
        } else {
            throw new ScraperShouldHaltException("Webdriver mode is misconfigured");
        }
    }
}
