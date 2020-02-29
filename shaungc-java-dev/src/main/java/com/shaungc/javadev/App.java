package com.shaungc.javadev;

import com.shaungc.dataStorage.S3Service;
import com.shaungc.dataTypes.ScraperJobData;
import com.shaungc.dataTypes.ScraperProgressData;
import com.shaungc.exceptions.ScraperException;
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

            new LoginGlassdoorTask(driver);

            // launch scraping task against a company
            if (Configuration.SCRAPER_MODE.equals(ScraperMode.RENEWAL.getString())) {
                scrapeCompanyTask = new ScrapeOrganizationGlassdoorTask(driver, pubSubSubscription);
            } else {
                try {
                    URL companyOverviewPageUrl = new URL(Configuration.TEST_COMPANY_INFORMATION_STRING);
                    scrapeCompanyTask = new ScrapeOrganizationGlassdoorTask(driver, pubSubSubscription, companyOverviewPageUrl);
                } catch (MalformedURLException e) {
                    if (Configuration.TEST_COMPANY_INFORMATION_STRING != null) {
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
                pubSubSubscription.publish(
                    String.format("%s:%s:%s", ScraperJobMessageType.FINISH.getString(), ScraperJobMessageTo.SLACK_MD_SVC.getString(), "OK!")
                );
            } else {
                // handle env var value needs quotes when value contains spaces
                final String doubleQuotedOrgName = "\"" + (scrapeCompanyTask.archiveManager.orgName) + "\"";

                pubSubSubscription.publish(
                    String.format(
                        "%s:%s:%s",
                        ScraperJobMessageType.FINISH.getString(),
                        ScraperJobMessageTo.SLACK_MD_SVC.getString(),
                        S3Service.serializeJavaObjectAsJsonStyle(
                            new ScraperJobData(
                                scrapeCompanyTask.archiveManager.orgId,
                                doubleQuotedOrgName,
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

            pubSubSubscription.cleanup();
            driver.quit();

            return;
        } catch (ScraperException e) {
            Logger.info(e.getMessage());
            Logger.errorAlsoSlack(
                "A scraper exception is raised and its message is logged above; which is not an error of the program, but more of the webpage the scraper is dealing with. There is something special with the webpage. Refer to the current url of the scraper to investigate more: " +
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

            pubSubSubscription.cleanup();
            if (driver != null) {
                driver.quit();
            }

            return;
        } catch (Exception e) {
            Logger.error("Program ended in exception block...!\n\n");

            System.out.println(e);

            pubSubSubscription.publish(
                String.format(
                    "%s:%s:%s",
                    ScraperJobMessageType.ERROR.getString(),
                    ScraperJobMessageTo.SLACK_MD_SVC.getString(),
                    "Exception: " + e.getMessage()
                )
            );

            pubSubSubscription.cleanup();
            if (driver != null) {
                driver.quit();
            }

            throw e;
        }
    }
}
