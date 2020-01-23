package com.shaungc.events;

import java.util.ArrayList;
import java.util.List;

import com.shaungc.dataTypes.BasicParsedData;
import com.shaungc.events.AScraperEvent;

import org.openqa.selenium.WebElement;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.By;

/**
 * ScrapeBasicDataFromCompanyNamePage
 */



public class ScrapeBasicDataFromCompanyNamePage extends AScraperEvent<BasicParsedData, BasicParsedData> {

    public ScrapeBasicDataFromCompanyNamePage(final WebDriver webDriver) {
        super(webDriver);
    }

    @Override
    protected List<WebElement> locate() {

        final List<WebElement> locatedElements = new ArrayList<WebElement>();

        locatedElements.add(this.driver.findElement(By.cssSelector("article[id*=WideCol]")));

        locatedElements.add(this.driver.findElement(By.cssSelector("article[id*=MainCol]")));

        return locatedElements;
    }

    @Override
    protected BasicParsedData parser(final List<WebElement> locatedElements) {
        final WebElement companyHeader = locatedElements.get(0);
        final WebElement companyOverview = locatedElements.get(1);

        System.out.println("\n\n\nlocated elements get!");

        // parse logo image
        String companyLogoImageUrl = "";
        try {
            final WebElement companyLogoImgElement = companyHeader
                    .findElement(By.cssSelector("div[id=EmpHeroAndEmpInfo] div.empInfo span.sqLogo img"));

            companyLogoImageUrl = companyLogoImgElement.getAttribute("src");
        } catch (final NoSuchElementException e) {
            System.out.println("WARN: cannot parse company logo: " + e.getMessage());
        }

        // parse review #
        final String reviewNumberText = companyHeader.findElement(By.cssSelector("a.eiCell.reviews span.num")).getText()
                .trim();

        // parse company overview: website, size, founded, location
        final List<WebElement> companyOverviewEntityElements = companyOverview
                .findElements(By.cssSelector("div[id*=EmpBasicInfo] div.info div.infoEntity span.value"));
        final String companySizeText = companyOverviewEntityElements.get(2).getText().trim();
        final String companyFoundYearText = companyOverviewEntityElements.get(3).getText().trim();
        final String companyLocationText = companyOverviewEntityElements.get(1).getText().trim();
        String companyWebsiteUrl = "";
        try {
            companyWebsiteUrl = companyOverviewEntityElements.get(0).findElement(By.cssSelector("a.link"))
                    .getAttribute("href");
        } catch (final NoSuchElementException e) {

        }

        System.out.println("\n\n\nall parsing completed!");

        return new BasicParsedData(companyLogoImageUrl, reviewNumberText, companySizeText, companyFoundYearText,
                companyLocationText, companyWebsiteUrl);
    }

    @Override
    protected void postAction(final BasicParsedData parsedData) {
        this.sideEffect = parsedData;
        parsedData.debugPrintAllFields();
    }
}
