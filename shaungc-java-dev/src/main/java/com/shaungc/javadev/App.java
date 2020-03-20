package com.shaungc.javadev;

import com.shaungc.dataStorage.S3Service;
import com.shaungc.dataTypes.ScraperJobData;
import com.shaungc.dataTypes.ScraperProgressData;
import com.shaungc.exceptions.ScraperException;
import com.shaungc.exceptions.ScraperShouldHaltException;
import com.shaungc.tasks.LoginGlassdoorTask;
import com.shaungc.tasks.ScrapeOrganizationGlassdoorTask;
import com.shaungc.utilities.Logger;
import com.shaungc.utilities.PubSubSubscription;
import com.shaungc.utilities.ScraperJobMessageTo;
import com.shaungc.utilities.ScraperJobMessageType;
import com.shaungc.utilities.ScraperMode;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.WebDriver;

/**
 * Hello world!
 *
 */
public class App {

    public static void main(String[] args) {
        PubSubSubscription pubSubSubscription = new PubSubSubscription();
        WebDriver driver = null;

        try {
            Boolean pubsubAcked;
            final Integer pubsubAckTimedoutSeconds = Configuration.DEBUG ? 30 : 60;
            try {
                Logger.info("waiting for pubsub countdown latch");
                pubsubAcked = pubSubSubscription.supervisorCountDownLatch.await(pubsubAckTimedoutSeconds, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new ScraperException("Pubsub ack countdown latch interrupted: " + e.getMessage());
            }

            if (!pubsubAcked) {
                throw new ScraperException(
                    String.format(
                        "Waiting for supervisor's pubsub confirmation timed out for %d seconds, will now abort scraper. Does the supervisor at slackMdSvc subscribing to channel `%s` and is still active?",
                        pubsubAckTimedoutSeconds,
                        pubSubSubscription.redisPubsubChannelName
                    )
                );
            }

            Logger.info("countdown latch passed; confirmed pubsub with supervisor");

            ScrapeOrganizationGlassdoorTask scrapeCompanyTask = null;

            driver = WebDriverFactory.create();

            Logger.debug("web driver created");

            if (pubSubSubscription.receivedTerminationRequest) {
                throw new ScraperException("Terminating per request");
            }

            new LoginGlassdoorTask(driver);

            // launch scraping task against a company
            if (Configuration.SCRAPER_MODE.equals(ScraperMode.RENEWAL.getString())) {
                scrapeCompanyTask = new ScrapeOrganizationGlassdoorTask(driver, pubSubSubscription);
            } else {
                try {
                    // `TEST_COMPANY_INFORMATION_STRING` is an url
                    URL companyOverviewPageUrl = new URL(Configuration.TEST_COMPANY_INFORMATION_STRING);
                    scrapeCompanyTask = new ScrapeOrganizationGlassdoorTask(driver, pubSubSubscription, companyOverviewPageUrl);
                } catch (MalformedURLException e) {
                    if (Configuration.TEST_COMPANY_INFORMATION_STRING != null) {
                        // `TEST_COMPANY_INFORMATION_STRING` is a company name
                        scrapeCompanyTask =
                            new ScrapeOrganizationGlassdoorTask(driver, pubSubSubscription, Configuration.TEST_COMPANY_INFORMATION_STRING);
                    } else {
                        // new ScrapeOrganizationGlassdoorTask(driver, "DigitalOcean");
                        // new ScrapeOrganizationGlassdoorTask(driver, "Waymo");
                        scrapeCompanyTask = new ScrapeOrganizationGlassdoorTask(driver, pubSubSubscription, "23AndMe");
                    }
                }
            }

            // tell FINAL to supervisor
            if (scrapeCompanyTask.isFinalSession) {
                // return finish message
                pubSubSubscription.publish(
                    String.format("%s:%s:%s", ScraperJobMessageType.FINISH.getString(), ScraperJobMessageTo.SLACK_MD_SVC.getString(), "OK!")
                );
            } else {
                // return renewal info along with finish message
                pubSubSubscription.publish(
                    String.format(
                        "%s:%s:%s",
                        ScraperJobMessageType.FINISH.getString(),
                        ScraperJobMessageTo.SLACK_MD_SVC.getString(),
                        S3Service.serializeJavaObjectAsJsonStyle(
                            new ScraperJobData(
                                scrapeCompanyTask.archiveManager.orgId,
                                scrapeCompanyTask.archiveManager.orgName,
                                new ScraperProgressData(
                                    scrapeCompanyTask.processedReviewsCount,
                                    scrapeCompanyTask.wentThroughReviewsCount,
                                    scrapeCompanyTask.localReviewsCount,
                                    scrapeCompanyTask.scraperTaskTimer.captureOverallElapseDurationInMilliAsString(),
                                    scrapeCompanyTask.processedReviewPages,
                                    Configuration.TEST_COMPANY_LAST_PROGRESS_SESSION + 1
                                ),
                                driver.getCurrentUrl(),
                                ScraperMode.RENEWAL.getString()
                            )
                        )
                    )
                );
            }

            return;
        } catch (ScraperException | ScraperShouldHaltException e) {
            Logger.info(e.getMessage());
            Logger.errorAlsoSlack(
                "A scraper exception is raised and its message is logged; which is not an error of the program, but more of the webpage the scraper is dealing with (or the selenium server issue). Refer to the current url of the scraper to investigate more: " +
                driver.getCurrentUrl()
            );

            pubSubSubscription.publish(
                String.format(
                    "%s:%s:%s",
                    ScraperJobMessageType.ERROR.getString(),
                    ScraperJobMessageTo.SLACK_MD_SVC.getString(),
                    "ScraperException: " + e.getMessage()
                )
            );

            return;
        } catch (Exception e) {
            Logger.errorAlsoSlack(
                "Program ended in exception block...! Might be a problem in either the scraper itself not handled, or an unknown change in the webpage that disrupts the scraper process. Please check the scraper log for error detail. " +
                (driver != null ? "Last webpage: `" + driver.getCurrentUrl() + "`" : "")
            );

            // TODO: we cannot dump html because `archiveManager` is not yet initialized
            // because org name and id are not yet scraped
            // if we want archiveManager to write to a "global" folder, we need to make
            // some methods a static one in ArchiveManager and S3Server

            System.out.println(e);

            pubSubSubscription.publish(
                String.format(
                    "%s:%s:%s",
                    ScraperJobMessageType.ERROR.getString(),
                    ScraperJobMessageTo.SLACK_MD_SVC.getString(),
                    "Exception: " + e.getMessage()
                )
            );

            throw e;
        } finally {
            pubSubSubscription.cleanup();

            if (driver != null) {
                driver.quit();
            }
        }
    }
}
