package com.shaungc.utilities;

import com.shaungc.exceptions.ScraperException;
import com.shaungc.exceptions.ScraperShouldHaltException;
import com.shaungc.javadev.Configuration;
import io.lettuce.core.RedisClient;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import java.util.ArrayList;

// Writing PubSub adapter
// https://www.baeldung.com/java-redis-lettuce#pubsub
public class PubSubSubscription extends RedisPubSubAdapter<String, String> {
    RedisClient subscriberRedisClient;
    RedisClient publisherRedisClient;
    StatefulRedisPubSubConnection<String, String> subscriberRedisConnection;
    StatefulRedisPubSubConnection<String, String> publisherRedisConnection;
    RedisPubSubCommands<String, String> subscriberCommands;
    RedisPubSubCommands<String, String> publisherCommands;

    // subscribing
    // https://lettuce.io/core/release/reference/#pubsub.subscribing

    public PubSubSubscription() {
        final String redisUrl = Configuration.DEBUG ? "redis://host.docker.internal:6379/5" : "redis://localhost:6379/5";
        this.subscriberRedisClient = RedisClient.create(redisUrl);
        this.publisherRedisClient = RedisClient.create(redisUrl);
        this.subscriberRedisConnection = this.subscriberRedisClient.connectPubSub();
        this.publisherRedisConnection = this.publisherRedisClient.connectPubSub();
        this.subscriberCommands = this.subscriberRedisConnection.sync();
        this.publisherCommands = this.publisherRedisConnection.sync();

        this.subscriberRedisConnection.addListener(this);
        this.subscriberCommands.subscribe(RedisPubSubChannelName.SCRAPER_JOB_CHANNEL.getString());
    }

    @Override
    public void subscribed(String channel, long count) {
        super.subscribed(channel, count);

        Logger.info("Subscribed to PubSub");
        this.publish(
                String.format(
                    "%s:%s:%s",
                    ScraperJobMessageType.PREFLIGHT.getString(),
                    ScraperJobMessageTo.SLACK_MD_SVC.getString(),
                    "BeginPubsubCommunication"
                )
            );
    }

    public void publish(String message) {
        this.publisherCommands.publish(RedisPubSubChannelName.SCRAPER_JOB_CHANNEL.getString(), message);
    }

    @Override
    public void message(String channel, String message) {
        super.message(channel, message);

        if (channel.equals(RedisPubSubChannelName.SCRAPER_JOB_CHANNEL.getString())) {
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
                // TODO: receive ack msg from slack md svc -> can now proceed (communication w/ slack md svc confirmed)

                if (messageType.equals(ScraperJobMessageType.PREFLIGHT.getString())) {
                    Logger.infoAlsoSlack("Received acked from slack md svc, pubsub communication channel confirmed");
                }
            } else {
                Logger.debug("Ignoring message that's not for ours: " + message);
            }
        }
    }

    @Override
    public void unsubscribed(String channel, long count) {
        // TODO Auto-generated method stub
        super.unsubscribed(channel, count);
    }

    public void cleanup() {
        this.subscriberRedisConnection.close();
        this.subscriberRedisClient.shutdown();
    }
}
