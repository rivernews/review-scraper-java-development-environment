package com.shaungc.utilities;

import java.util.ArrayList;

import com.shaungc.exceptions.ScraperException;
import com.shaungc.exceptions.ScraperShouldHaltException;
import com.shaungc.javadev.Configuration;

import io.lettuce.core.RedisClient;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;


enum RedisPubSubChannelName {
    SCRAPER_JOB_CHANNEL("scraperJobChannel");

    private final String string;

    private RedisPubSubChannelName(String string) {
        this.string = string;
    }

    public String getString() {
        return this.string;
    }
}

enum ScraperJobMessageType {
    PREFLIGHT("preflight"),
    PROGRESS("progress"),
    FINISH("finish"),
    ERROR("error");

    private final String string;

    private ScraperJobMessageType(String string) {
        this.string = string;
    }

    public String getString() {
        return this.string;
    }
}

enum ScraperJobMessageTo {
    SLACK_MD_SVC("slackMiddlewareService"),
    SCRAPER("scraper");

    private final String string;

    private ScraperJobMessageTo(String string) {
        this.string = string;
    }

    public String getString() {
        return this.string;
    }
}

// Writing PubSub adapter
// https://www.baeldung.com/java-redis-lettuce#pubsub
public class PubSubSubscription extends RedisPubSubAdapter<String, String> {
    RedisClient redisClient;
    StatefulRedisPubSubConnection<String, String> redisPubSubConnection;
    RedisPubSubCommands<String, String> pubsubCommands;

    // subscribing
    // https://lettuce.io/core/release/reference/#pubsub.subscribing

    public PubSubSubscription() {
        this.redisClient = RedisClient.create(
            Configuration.RUNNING_FROM_CONTAINER ? 
                "redis://host.docker.internal:6379/5" :
                "redis://localhost:6379/5"
        );
        this.redisPubSubConnection = redisClient.connectPubSub();
        redisPubSubConnection.addListener(this);
        this.pubsubCommands = this.redisPubSubConnection.sync();

        this.pubsubCommands.subscribe(RedisPubSubChannelName.SCRAPER_JOB_CHANNEL.getString());
    }

    @Override
    public void subscribed(String channel, long count) {
        super.subscribed(channel, count);

        Logger.info("Subscribed to PubSub");

        this.pubsubCommands.publish(
            RedisPubSubChannelName.SCRAPER_JOB_CHANNEL.getString(),
            String.format(
                "%s:%s:%s",
                ScraperJobMessageType.PREFLIGHT,
                ScraperJobMessageTo.SLACK_MD_SVC,
                "BeginPubsubCommunication"
            )
        );
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
        this.redisPubSubConnection.close();
        this.redisClient.shutdown();
    }
}
