package com.shaungc.events;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.shaungc.dataStorage.ArchiveManager;
import com.shaungc.dataTypes.BasicParsedData;
import com.shaungc.events.AScraperEvent;
import com.shaungc.javadev.Logger;
import com.shaungc.javadev.ScraperException;

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
    public ScrapeBasicDataFromCompanyNamePage(final WebDriver webDriver, ArchiveManager archiveManager) {
        super(webDriver, archiveManager);
    }

    @Override
    protected List<WebElement> locate() {

        final List<WebElement> locatedElements = new ArrayList<WebElement>();

        locatedElements.add(this.driver.findElement(By.cssSelector("article[id*=WideCol]")));

        locatedElements.add(this.driver.findElement(By.cssSelector("article[id*=MainCol]")));

        return locatedElements;
    }

    @Override
    protected BasicParsedData parser(final List<WebElement> locatedElements) throws ScraperException {
        final WebElement companyHeader = locatedElements.get(0);
        final WebElement companyOverview = locatedElements.get(1);

        // parse company id
        String companyId = companyHeader.findElement(By.cssSelector("div#EmpHero")).getAttribute("data-employer-id").strip();
        if (companyId == "") {
            Logger.warn("Failed to scrape companyId in header. HTML content:\n" + companyHeader.getText());
            throw new ScraperException("Cannot scrape company id, so we shall not proceed. The `companyHeader`'s HTML text is logged above.'");
        }

        // parse company name
        String companyName = "";
        companyName = companyHeader.findElement(By.cssSelector("div.header.info h1")).getAttribute("data-company").strip();

        // parse logo image
        String companyLogoImageUrl = "";
        try {
            final WebElement companyLogoImgElement = companyHeader
                    .findElement(By.cssSelector("div[id=EmpHeroAndEmpInfo] div.empInfo span.sqLogo img"));

            companyLogoImageUrl = companyLogoImgElement.getAttribute("src");
        } catch (final NoSuchElementException e) {
            Logger.info("WARN: cannot parse company logo: " + e.getMessage());
        }

        // parse review #
        final String reviewNumberText = companyHeader.findElement(By.cssSelector("a.eiCell.reviews span.num")).getText()
                .strip();

        // parse company overview: website, size, founded, location
        final List<WebElement> companyOverviewEntityElements = companyOverview
                .findElements(By.cssSelector("div[id*=EmpBasicInfo] div.info div.infoEntity span.value"));
        final String companySizeText = companyOverviewEntityElements.get(2).getText().strip();
        final String companyFoundYearText = companyOverviewEntityElements.get(3).getText().strip();
        final String companyLocationText = companyOverviewEntityElements.get(1).getText().strip();
        String companyWebsiteUrl = "";
        try {
            companyWebsiteUrl = companyOverviewEntityElements.get(0).findElement(By.cssSelector("a.link"))
                    .getAttribute("href");
        } catch (final NoSuchElementException e) {

        }

        Logger.info("Basic data parsing completed!");

        return new BasicParsedData(companyId, companyLogoImageUrl, reviewNumberText, companySizeText, companyFoundYearText,
                companyLocationText, companyWebsiteUrl, companyName);
    }

    @Override
    protected void postAction(final BasicParsedData parsedData) {
        // write data to archive
        parsedData.scrapedTimestamp = new Date();
        // store org name, also for later other event use
        this.archiveManager.orgName = parsedData.companyName;
        this.archiveManager.orgId = parsedData.companyId;
        this.archiveManager.writeGlassdoorOrganizationMetadata(parsedData);

        this.sideEffect = parsedData;
        parsedData.debugPrintAllFields();
    }
}
