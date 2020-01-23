package com.shaungc.javadev;

import java.net.MalformedURLException;
import java.net.URL;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WebDriver.Navigation;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * LoginGlassdoorTask
 */
public class LoginGlassdoorTask {
    ChromeDriver driver;

    public LoginGlassdoorTask(ChromeDriver driver) throws MalformedURLException {
        this.driver = driver;

        this.launchTask();
    }

    public void launchTask() throws MalformedURLException {
        // navigate login page
        URL loginPageUrl = new URL("https://www.glassdoor.com/profile/joinNow_input.htm");
        Navigation navigation = this.driver.navigate();
        navigation.to(loginPageUrl);

        // click "sign in" link
        this.driver.findElementByCssSelector("a[href*=signIn]").click();

        // wait login modal pop up
        Integer waitLoginModalTimeout = 10;
        WebElement usernameInputElement = new WebDriverWait(driver, waitLoginModalTimeout)
                .until(ExpectedConditions.elementToBeClickable(By.cssSelector("input[name=username]")));

        // pull out modal element to improve searching element performance
        WebElement loginModalElement = this.driver.findElementByCssSelector("div#LoginModal");

        // fill out login form
        usernameInputElement.sendKeys(Configuration.GLASSDOOR_USERNAME);
        loginModalElement.findElement(By.cssSelector("input[name=password]")).sendKeys(Configuration.GLASSDOOR_PASSWORD);

        // submit
        loginModalElement.findElement(By.cssSelector("button[type=submit]")).click();

        // confirm that login succeed
        new WebDriverWait(driver, waitLoginModalTimeout).until(ExpectedConditions.and(
            ExpectedConditions.elementToBeClickable(By.cssSelector("header nav div.container-menu"))
        ));

        System.out.println("\n\n\nOK, login complete!");
    }
}