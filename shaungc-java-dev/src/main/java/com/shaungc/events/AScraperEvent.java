package com.shaungc.events;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;


public abstract class AScraperEvent<TParsedData, TPostActionSideEffect> {
    protected String cssSelector;
    protected WebDriver driver;

    // let class caller access values emit by postAction
    public TPostActionSideEffect sideEffect;

    public AScraperEvent(WebDriver passedInRemoteWebDriver) {
        this.driver = passedInRemoteWebDriver;
    }

    protected List<WebElement> locate(String passedInCssSelector) {
        if (this.cssSelector != null) {
            this.cssSelector = passedInCssSelector;
            return this.driver.findElements(By.cssSelector(this.cssSelector));
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