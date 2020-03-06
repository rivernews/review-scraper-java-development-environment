package com.shaungc.utilities;

import com.shaungc.exceptions.ScraperShouldHaltException;
import com.shaungc.javadev.Configuration;
import com.shaungc.utilities.ScraperJobMessageTo;
import com.shaungc.utilities.ScraperJobMessageType;
import io.lettuce.core.RedisClient;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import java.util.concurrent.CountDownLatch;

// Writing PubSub adapter
// https://www.baeldung.com/java-redis-lettuce#pubsub
public class PubSubSubscription extends RedisPubSubAdapter<String, String> {
    public final String redisPubsubChannelName = String.format(
        "%s:%s:%s",
        RedisPubSubChannelPrefix.SCRAPER_JOB_CHANNEL.getString(),
        // We should check `TEST_COMPANY_NAME` first, because that indicates a renewal job where the channel has to use `TEST_COMPANY_NAME`
        // For `TEST_COMPANY_INFORMATION_STRING`, it could be provided regardless of regular or renewal job
        !Configuration.TEST_COMPANY_NAME.isEmpty()
            ? "\"" + Configuration.TEST_COMPANY_NAME + "\""
            : Configuration.TEST_COMPANY_INFORMATION_STRING,
        Configuration.TEST_COMPANY_LAST_PROGRESS_SESSION
    );
    private final RedisClient subscriberRedisClient;
    private final RedisClient publisherRedisClient;
    private final StatefulRedisPubSubConnection<String, String> subscriberRedisConnection;
    private final StatefulRedisPubSubConnection<String, String> publisherRedisConnection;
    private final RedisPubSubCommands<String, String> subscriberCommands;
    private final RedisPubSubCommands<String, String> publisherCommands;

    public final CountDownLatch supervisorCountDownLatch;

    // subscribing
    // https://lettuce.io/core/release/reference/#pubsub.subscribing

    public PubSubSubscription() {
        if (Configuration.SUPERVISOR_PUBSUB_REDIS_DB.isEmpty()) {
            throw new ScraperShouldHaltException("SUPERVISOR_PUBSUB_REDIS_DB is not set.");
        }

        final String redisUrl =
            (Configuration.DEBUG ? "redis://host.docker.internal:6379/" : "redis://localhost:6379/") +
            Configuration.SUPERVISOR_PUBSUB_REDIS_DB;

        this.subscriberRedisClient = RedisClient.create(redisUrl);
        this.publisherRedisClient = RedisClient.create(redisUrl);
        this.subscriberRedisConnection = this.subscriberRedisClient.connectPubSub();
        this.publisherRedisConnection = this.publisherRedisClient.connectPubSub();
        this.subscriberCommands = this.subscriberRedisConnection.sync();
        this.publisherCommands = this.publisherRedisConnection.sync();

        supervisorCountDownLatch = new CountDownLatch(1);

        this.subscriberRedisConnection.addListener(this);
        this.subscriberCommands.subscribe(this.redisPubsubChannelName);
    }

    @Override
    public void subscribed(final String channel, final long count) {
        super.subscribed(channel, count);

        Logger.infoAlsoSlack("Scraper subscribed to PubSub channle `" + channel + "`");

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

        if (messageTo.equals(ScraperJobMessageTo.SCRAPER.getString())) {
            // TODO: receive ack msg from slack md svc -> can now proceed (communication w/
            // slack md svc confirmed)

            if (messageType.equals(ScraperJobMessageType.PREFLIGHT.getString())) {
                Logger.infoAlsoSlack(
                    "Received acked from supervisor, pubsub confirmed. Redis db `" + Configuration.SUPERVISOR_PUBSUB_REDIS_DB + "`"
                );
                this.supervisorCountDownLatch.countDown();
            }
        } else {
            Logger.debug("Ignoring message that's not for ours: " + message);
        }
    }

    @Override
    public void unsubscribed(final String channel, final long count) {
        // TODO Auto-generated method stub
        super.unsubscribed(channel, count);
    }

    public void cleanup() {
        this.subscriberRedisConnection.close();
        this.subscriberRedisClient.shutdown();
    }
}
