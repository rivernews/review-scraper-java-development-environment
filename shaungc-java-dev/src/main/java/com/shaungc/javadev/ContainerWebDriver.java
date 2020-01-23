package com.shaungc.javadev;

import java.net.MalformedURLException;
import java.net.URL;

import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.chrome.ChromeOptions;


/**
 * ContainerRemoteWebDriver
 */
public class ContainerWebDriver extends RemoteWebDriver{

    public ContainerWebDriver(URL webDriverServiceUrl) {
        super(webDriverServiceUrl, new ChromeOptions());
    }
    public ContainerWebDriver() throws MalformedURLException {
        // super(new URL("http://host.docker.internal:4444/wd/hub"), new ChromeOptions());
        
        super(new URL("http://localhost:4444/wd/hub"), Configuration.DEBUG ? new ChromeOptions() : ContainerWebDriver.createDesiredCapabilities());
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