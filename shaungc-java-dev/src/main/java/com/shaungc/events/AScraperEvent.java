package com.shaungc.events;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.ArrayList;
import java.util.List;

import com.shaungc.dataStorage.ArchiveManager;
import com.shaungc.exceptions.ScraperException;
import com.shaungc.javadev.Configuration;


public abstract class AScraperEvent<TParsedData, TPostActionSideEffect> {
    protected String cssSelector;
    protected WebDriver driver;
    protected final WebDriverWait wait;

    // let class caller access values emit by postAction
    public TPostActionSideEffect sideEffect;

    final protected ArchiveManager archiveManager;

    public AScraperEvent(WebDriver passedInRemoteWebDriver) {
        this.driver = passedInRemoteWebDriver;
        this.archiveManager = null;
        this.wait = new WebDriverWait(this.driver, Configuration.EXPECTED_CONDITION_WAIT_SECOND);
    }
    public AScraperEvent(WebDriver passedInRemoteWebDriver, ArchiveManager archiveManager) {
        this.driver = passedInRemoteWebDriver;
        this.archiveManager = archiveManager;
        this.wait = new WebDriverWait(this.driver, Configuration.EXPECTED_CONDITION_WAIT_SECOND);
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

    abstract protected TParsedData parser(List<WebElement> locatedElements) throws ScraperException;

    abstract protected void postAction(TParsedData parsedData);

    public void run() throws ScraperException {
        List<WebElement> webElements = locate();
        TParsedData parsedData = parser(webElements);
        postAction(parsedData);
    }
}