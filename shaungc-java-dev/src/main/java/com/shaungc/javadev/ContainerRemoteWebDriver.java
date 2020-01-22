package com.shaungc.javadev;

import java.net.MalformedURLException;
import java.net.URL;

import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.chrome.ChromeOptions;


/**
 * ContainerRemoteWebDriver
 */
public class ContainerRemoteWebDriver extends RemoteWebDriver{

    public ContainerRemoteWebDriver(URL webDriverServiceUrl) {
        super(webDriverServiceUrl, new ChromeOptions());
    }
    public ContainerRemoteWebDriver() throws MalformedURLException {
        // super(new URL("http://host.docker.internal:4444/wd/hub"), new ChromeOptions());
        super(new URL("http://localhost:4444/wd/hub"), Configuration.DEBUG ? new ChromeOptions() : ContainerRemoteWebDriver.createDesiredCapabilities());
    }
    public static DesiredCapabilities createDesiredCapabilities() {
        DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
        ChromeOptions chromeOptions = new ChromeOptions();

        // chromeOptions.setBinary("/tmp/chromedriver_linux");
        // chromeOptions.addArguments("--headless");
        chromeOptions.addArguments("--no-sandbox");
        desiredCapabilities.setCapability(ChromeOptions.CAPABILITY, chromeOptions);

        return desiredCapabilities;
    }
}