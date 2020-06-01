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

                driver = WebDriverFactory.create(pubSubSubscription);

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
                                new ScrapeOrganizationGlassdoorTask(
                                    driver,
                                    pubSubSubscription,
                                    Configuration.TEST_COMPANY_INFORMATION_STRING
                                );
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
                        String.format(
                            "%s:%s:%s",
                            ScraperJobMessageType.FINISH.getString(),
                            ScraperJobMessageTo.SLACK_MD_SVC.getString(),
                            "OK!"
                        )
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
                                    ScraperMode.RENEWAL.getString(),
                                    Configuration.TEST_COMPANY_STOP_AT_PAGE,
                                    Configuration.TEST_COMPANY_SHARD_INDEX
                                )
                            )
                        )
                    );
                }
            } catch (ScraperException | ScraperShouldHaltException e) {
                Logger.info(e.getMessage());
                Logger.errorAlsoSlack(
                    (new StringBuilder()).append(
                            "A scraper exception is raised and its message is logged; which is not an error of the program, but more of the webpage the scraper is dealing with (or the selenium server issue). Refer to the current url of the scraper to investigate more: "
                        )
                        .append(driver.getCurrentUrl())
                        .append("\n```")
                        .append(e.getMessage())
                        .append("```\n")
                        .toString()
                );

                pubSubSubscription.publish(
                    String.format(
                        "%s:%s:%s",
                        ScraperJobMessageType.ERROR.getString(),
                        ScraperJobMessageTo.SLACK_MD_SVC.getString(),
                        "ScraperException: " + e.getMessage()
                    )
                );
            } catch (Exception e) {
                System.out.println(e);

                // TODO: we cannot dump html because `archiveManager` is not yet initialized
                // because org name and id are not yet scraped
                // if we want archiveManager to write to a "global" folder, we need to make
                // some methods a static one in ArchiveManager and S3Server

                pubSubSubscription.publish(
                    String.format(
                        "%s:%s:%s",
                        ScraperJobMessageType.ERROR.getString(),
                        ScraperJobMessageTo.SLACK_MD_SVC.getString(),
                        "Exception: " + e.getMessage()
                    )
                );

                // in this exception catch block we cannot guarantee anything for `driver`,
                // `driver` might be null, or not null but its remote session corrupted / be
                // removed
                // so we need to surround any code that uses `driver` by try-catch
                String currentFacingPage = null;
                if (driver != null) {
                    try {
                        currentFacingPage = (driver != null ? "Last webpage: `" + driver.getCurrentUrl() + "`" : "");
                    } catch (Exception driverException) {
                        currentFacingPage = "(Cannot get current url because driver session corrupted): " + driverException.getMessage();
                    }
                }
                Logger.errorAlsoSlack(
                    (new StringBuilder()).append("`")
                        .append(Configuration.SUPERVISOR_PUBSUB_CHANNEL_NAME)
                        .append("` ")
                        .append("Program ended in exception block! ")
                        .append("`")
                        .append(currentFacingPage)
                        .append("`")
                        .append("\nException message:\n```")
                        .append(e.getMessage())
                        .append("```\n")
                        .toString()
                );
            } finally {
                pubSubSubscription.cleanup();

                if (driver != null) {
                    // best effort to clean up driver session
                    try {
                        driver.quit();
                    } finally {}
                }
            }
        } catch (Exception lastException) {
            System.out.println("🛑 There was extra exception happening in catch block in App:");
            System.out.println(lastException);
        }

        System.out.println("App process completed all code execution");
    }
}
