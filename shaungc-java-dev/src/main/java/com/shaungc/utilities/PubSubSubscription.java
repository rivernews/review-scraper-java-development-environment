package com.shaungc.utilities;

import com.shaungc.exceptions.ScraperShouldHaltException;
import com.shaungc.javadev.Configuration;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;

// Writing PubSub adapter
// https://www.baeldung.com/java-redis-lettuce#pubsub
public class PubSubSubscription extends RedisPubSubAdapter<String, String> {
    public final String redisPubsubChannelName;
    private RedisClient subscriberRedisClient;
    private RedisClient publisherRedisClient;
    private StatefulRedisPubSubConnection<String, String> subscriberRedisConnection;
    private StatefulRedisPubSubConnection<String, String> publisherRedisConnection;
    private final RedisPubSubCommands<String, String> subscriberCommands;
    private final RedisPubSubCommands<String, String> publisherCommands;

    public final CountDownLatch supervisorCountDownLatch;
    public Boolean receivedTerminationRequest = false;

    // subscribing
    // https://lettuce.io/core/release/reference/#pubsub.subscribing

    public PubSubSubscription() {
        if (Configuration.SUPERVISOR_PUBSUB_REDIS_DB.isBlank()) {
            throw new ScraperShouldHaltException("SUPERVISOR_PUBSUB_REDIS_DB is not set.");
        }

        if (Configuration.SUPERVISOR_PUBSUB_CHANNEL_NAME.isBlank()) {
            throw new ScraperShouldHaltException("SUPERVISOR_PUBSUB_CHANNEL_NAME is not set.");
        }

        final String redisUrl = String.format(
            "redis://:%s@%s:%s/%s",
            Configuration.REDIS_PASSWORD,
            Configuration.REDIS_MODE.equals(ExternalServiceMode.SERVER_FROM_MACOS_DOCKER_CONTAINER.getString())
                ? "host.docker.internal"
                : Configuration.REDIS_MODE.equals(ExternalServiceMode.SERVER_FROM_PORT_FORWARD.getString())
                    ? "localhost"
                    : Configuration.REDIS_MODE.equals(ExternalServiceMode.SERVER_FROM_CUSTOM_HOST.getString())
                        ? Configuration.REDIS_CUSTOM_HOST
                        : "",
            Configuration.REDIS_PORT,
            Configuration.SUPERVISOR_PUBSUB_REDIS_DB
        );

        if (redisUrl.strip().isEmpty()) {
            throw new ScraperShouldHaltException("Redis misconfigured, redisUrl is empty");
        }

        this.redisPubsubChannelName = Configuration.SUPERVISOR_PUBSUB_CHANNEL_NAME;

        // prepare redis client
        // https://github.com/lettuce-io/lettuce-core/wiki/Client-Options
        ClientOptions clientOptions = ClientOptions
            .builder()
            .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(60)))
            .autoReconnect(true)
            .pingBeforeActivateConnection(true)
            .build();

        // initiate redis clients
        this.subscriberRedisClient = RedisClient.create(redisUrl);
        this.subscriberRedisClient.setOptions(clientOptions);
        this.publisherRedisClient = RedisClient.create(redisUrl);
        this.publisherRedisClient.setOptions(clientOptions);

        // connect
        this.subscriberRedisConnection = this.subscriberRedisClient.connectPubSub();
        this.publisherRedisConnection = this.publisherRedisClient.connectPubSub();
        this.subscriberCommands = this.subscriberRedisConnection.sync();
        this.publisherCommands = this.publisherRedisConnection.sync();

        supervisorCountDownLatch = new CountDownLatch(1);

        this.subscriberRedisConnection.addListener(this);
        this.subscriberCommands.subscribe(this.redisPubsubChannelName, RedisPubSubChannelPrefix.ADMIN.getString());
    }

    @Override
    public void subscribed(final String channel, final long count) {
        super.subscribed(channel, count);

        Logger.infoAlsoSlack("Scraper subscribed to PubSub channel `" + channel + "`");

        this.publish(
                String.format(
                    "%s:%s:%s",
                    ScraperJobMessageType.PREFLIGHT.getString(),
                    ScraperJobMessageTo.SLACK_MD_SVC.getString(),
                    "BeginPubsubCommunication"
                )
            );
    }

    public void publish(final String message) {
        this.publisherCommands.publish(this.redisPubsubChannelName, message);
    }

    @Override
    public void message(final String channel, final String message) {
        super.message(channel, message);

        final String[] messageTokens = message.split(":", 3);

        // message at least has to contain type and recipient info
        // otherwise raise error
        if (messageTokens.length < 2) {
            throw new ScraperShouldHaltException("Received invalid message: " + message + ", in channel " + channel);
        }
        final String messageType = messageTokens[0];
        final String messageTo = messageTokens[1];
        final String payload = messageTokens.length == 3 ? messageTokens[2] : "";

        if (messageTo.equals(ScraperJobMessageTo.ALL.getString())) {
            if (messageType.equals(ScraperJobMessageType.TERMINATE.getString())) {
                Logger.info("Received terminate signal from supervisor: `" + payload + "`");
                this.receivedTerminationRequest = true;
            }
        } else if (messageTo.equals(ScraperJobMessageTo.SCRAPER.getString())) {
            if (messageType.equals(ScraperJobMessageType.PREFLIGHT.getString())) {
                Logger.infoAlsoSlack(
                    "Received acked from supervisor, pubsub confirmed. Redis db `" + Configuration.SUPERVISOR_PUBSUB_REDIS_DB + "`"
                );

                // received ack msg from slack md svc -> can now proceed (communication w/
                // slack md svc confirmed)
                this.supervisorCountDownLatch.countDown();
            }
        } else {
            Logger.debug("Ignoring message that's not for ours: " + message);
        }
    }

    public void publishProgress(
        final Integer processedReviewsCount,
        final Integer wentThroughReviewsCount,
        final Integer localReviewCount,
        final String elapsedTimeString
    ) {
        this.publish(
                String.format(
                    "%s:%s:{\"processed\":%d,\"wentThrough\":%d,\"total\":%d,\"elapsedTimeString\":\"%s\"}",
                    ScraperJobMessageType.PROGRESS.getString(),
                    ScraperJobMessageTo.SLACK_MD_SVC.getString(),
                    processedReviewsCount,
                    wentThroughReviewsCount,
                    localReviewCount,
                    elapsedTimeString
                )
            );
    }

    public void publishProgress(final String elapsedTimeString) {
        this.publish(
                String.format(
                    "%s:%s:{\"processed\":%d,\"wentThrough\":%d,\"total\":%d,\"elapsedTimeString\":\"%s\"}",
                    ScraperJobMessageType.PROGRESS.getString(),
                    ScraperJobMessageTo.SLACK_MD_SVC.getString(),
                    Configuration.TEST_COMPANY_LAST_PROGRESS_PROCESSED,
                    Configuration.TEST_COMPANY_LAST_PROGRESS_WENTTHROUGH,
                    Configuration.TEST_COMPANY_LAST_PROGRESS_TOTAL,
                    elapsedTimeString
                )
            );
    }

    @Override
    public void unsubscribed(final String channel, final long count) {
        // TODO Auto-generated method stub
        super.unsubscribed(channel, count);
    }

    public void cleanup() {
        this.subscriberRedisConnection.close();
        this.subscriberRedisClient.shutdown();
        this.publisherRedisConnection.close();
        this.publisherRedisClient.shutdown();

        // after calling close() and shutdown(), they can no longer be used
        // to avoid accidental usage, setting them to null
        this.subscriberRedisConnection = null;
        this.subscriberRedisClient = null;
        this.publisherRedisConnection = null;
        this.publisherRedisClient = null;
    }
}
