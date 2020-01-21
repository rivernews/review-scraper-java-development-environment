package com.shaungc.events;

import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;


public abstract class AScraperEvent<TParsedData, TPostActionSideEffect> {
    protected String cssSelector;
    protected RemoteWebDriver remoteWebDriver;

    // let class caller access values emit by postAction
    public TPostActionSideEffect sideEffect;

    public AScraperEvent(RemoteWebDriver passedInRemoteWebDriver) {
        this.remoteWebDriver = passedInRemoteWebDriver;
    }

    protected List<WebElement> locate(String passedInCssSelector) {
        if (this.cssSelector != null) {
            this.cssSelector = passedInCssSelector;
            return this.remoteWebDriver.findElementsByCssSelector(this.cssSelector);
        }
        
        return new ArrayList<>();
    }
    protected List<WebElement> locate() {
        return locate(this.cssSelector);
    }

    abstract protected TParsedData parser(List<WebElement> locatedElements);

    abstract protected void postAction(TParsedData parsedData);

    public void run() {
        List<WebElement> webElements = locate();
        TParsedData parsedData = parser(webElements);
        postAction(parsedData);
    }
}