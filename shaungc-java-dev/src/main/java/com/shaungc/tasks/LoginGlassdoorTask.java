package com.shaungc.tasks;

import com.shaungc.javadev.Configuration;
import com.shaungc.utilities.Logger;
import com.shaungc.utilities.RequestAddressValidator;
import java.net.URL;
import org.openqa.selenium.By;
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

    public LoginGlassdoorTask(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Configuration.EXPECTED_CONDITION_WAIT_SECOND);

        this.launchTask();
    }

    public void launchTask() {
        // navigate login page
        URL loginPageUrl = RequestAddressValidator.toURL("https://www.glassdoor.com/profile/joinNow_input.htm");
        Navigation navigation = this.driver.navigate();
        navigation.to(loginPageUrl);

        // click "sign in" link
        this.driver.findElement(By.cssSelector("a[href*=signIn]")).click();

        // wait login modal pop up
        WebElement usernameInputElement = new WebDriverWait(driver, Configuration.EXPECTED_CONDITION_WAIT_SECOND)
        .until(ExpectedConditions.elementToBeClickable(By.cssSelector("input[name=username]")));

        // pull out modal element to improve searching element performance
        WebElement loginModalElement = this.driver.findElement(By.cssSelector("div#LoginModal"));

        // fill out login form
        usernameInputElement.sendKeys(Configuration.GLASSDOOR_USERNAME);
        loginModalElement.findElement(By.cssSelector("input[name=password]")).sendKeys(Configuration.GLASSDOOR_PASSWORD);

        // submit
        loginModalElement.findElement(By.cssSelector("button[type=submit]")).click();

        // confirm that login succeed
        final String judgeLoginSuccessElementXPath = "//*[@id=\"sc.keyword\"]";
        Logger.info("Waiting for login success page...");
        this.wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(judgeLoginSuccessElementXPath)));

        Logger.infoAlsoSlack("OK, glassdoor login complete for info " + Configuration.TEST_COMPANY_INFORMATION_STRING);
    }
}
