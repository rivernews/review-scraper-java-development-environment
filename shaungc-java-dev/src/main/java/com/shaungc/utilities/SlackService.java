package com.shaungc.utilities;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import com.shaungc.javadev.Configuration;

/**
 * SlackService
 */
public class SlackService {

    static final String SENDER_TITLE = "Qualitative Data Scraper";

    static final URI WEBHOOK_URL = Configuration.SLACK_WEBHOOK_URL;

    static public CompletableFuture<HttpResponse<String>> asyncSendMessage(String message) {
        HashMap<String, String> data = new HashMap<String, String>();

        data.put("username", SlackService.SENDER_TITLE);
        data.put("text", message);

        return HttpService.asyncPost(data, SlackService.WEBHOOK_URL);
    }

    static public HttpResponse<String> sendMessage(String message) {
        HashMap<String, String> data = new HashMap<String, String>();

        data.put("username", SlackService.SENDER_TITLE);
        data.put("text", message);

        return HttpService.post(data, SlackService.WEBHOOK_URL);
    }
}