package com.shaungc.utilities;

import com.shaungc.javadev.Configuration;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

/**
 * SlackService
 */
public class SlackService {
    static final String SENDER_TITLE = "Qualitative Data Scraper";

    static final URI WEBHOOK_URL = Configuration.SLACK_WEBHOOK_URL;

    public static CompletableFuture<HttpResponse<String>> asyncSendMessage(final String message) {
        final HashMap<String, String> data = new HashMap<String, String>();

        data.put("username", SlackService.SENDER_TITLE);
        data.put("text", message);

        return HttpService.asyncPost(data, SlackService.WEBHOOK_URL);
    }

    public static HttpResponse<String> sendMessage(final String message) {
        return SlackService.sendMessage(message, null);
    }

    public static HttpResponse<String> sendMessageToErrorChannel(final String message) {
        return SlackService.sendMessage(message, "#error");
    }

    public static HttpResponse<String> sendMessage(final String message, final String channel) {
        final HashMap<String, String> data = new HashMap<String, String>();

        data.put("username", SlackService.SENDER_TITLE);
        data.put("text", message);
        if (channel != null) {
            data.put("channel", channel);
        }

        return HttpService.post(data, SlackService.WEBHOOK_URL);
    }
}
