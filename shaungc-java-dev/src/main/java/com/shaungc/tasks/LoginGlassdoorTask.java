package com.shaungc.tasks;

import com.shaungc.dataStorage.ArchiveManager;
import com.shaungc.exceptions.ScraperShouldHaltException;
import com.shaungc.javadev.Configuration;
import com.shaungc.utilities.Logger;
import com.shaungc.utilities.RequestAddressValidator;
import java.net.URL;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriver.Navigation;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * LoginGlassdoorTask
 */
public class LoginGlassdoorTask {
    WebDriver driver;
    WebDriverWait wait;

    static final Integer TIMEOUT_RETRIES = 2;
    private Integer timeoutRetryCount = 0;

    public LoginGlassdoorTask(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Configuration.EXPECTED_CONDITION_WAIT_SECOND_LONGER);

        while (this.timeoutRetryCount <= LoginGlassdoorTask.TIMEOUT_RETRIES) {
            try {
                this.launchTask();
                return;
            } catch (TimeoutException e) {
                this.timeoutRetryCount++;
            }
        }
    }

    public void launchTask() {
        // navigate login page
        URL loginPageUrl = RequestAddressValidator.toURL("https://www.glassdoor.com/profile/joinNow_input.htm");
        Navigation navigation = this.driver.navigate();
        navigation.to(loginPageUrl);

        // click "sign in" link
        try {
            this.driver.findElement(By.cssSelector("a[href*=signIn]")).click();
        } catch (NoSuchElementException e) {
            final ArchiveManager headlessArchiveManager = new ArchiveManager();
            final String htmlDumpPath = headlessArchiveManager.writeHtml("login:cannotLocateSignInButton", this.driver.getPageSource());
            throw new ScraperShouldHaltException(
                String.format(
                    "Cannot locate sign in button. <%s|Download dumped html on s3>, scraper was facing `%s`.",
                    headlessArchiveManager.getFullUrlOnS3FromFilePathBasedOnOrgDirectory(htmlDumpPath),
                    this.driver.getCurrentUrl()
                )
            );
        }

        // wait login modal pop up
        WebElement usernameInputElement = this.wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("input[name=username]")));

        // pull out modal element to improve searching element performance
        WebElement loginModalElement = this.driver.findElement(By.cssSelector("div#LoginModal"));

        // fill out login form
        usernameInputElement.sendKeys(Configuration.GLASSDOOR_USERNAME);
        loginModalElement.findElement(By.cssSelector("input[name=password]")).sendKeys(Configuration.GLASSDOOR_PASSWORD);

        // submit
        loginModalElement.findElement(By.cssSelector("button[type=submit]")).click();

        // confirm that login succeed
        final String judgeLoginSuccessElementXPath = "//*[@id=\"sc.keyword\"]";
        final String judgeLoginSuccessElementCssSelector = "input#sc.keyword";
        Logger.info("Waiting for login success page...");
        this.wait.until(
                ExpectedConditions.or(
                    ExpectedConditions.visibilityOfElementLocated(By.xpath(judgeLoginSuccessElementXPath)),
                    ExpectedConditions.visibilityOfElementLocated(By.cssSelector(judgeLoginSuccessElementCssSelector))
                )
            );

        Logger.infoAlsoSlack("OK, glassdoor login complete for info " + Configuration.TEST_COMPANY_INFORMATION_STRING);
    }
}
