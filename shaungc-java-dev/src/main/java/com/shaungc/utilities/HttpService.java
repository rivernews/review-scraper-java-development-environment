package com.shaungc.utilities;

import com.shaungc.dataStorage.ArchiveManager;
import com.shaungc.dataStorage.S3Service;
import com.shaungc.exceptions.ScraperShouldHaltException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

/**
 * HttpService writing requests:
 * https://openjdk.java.net/groups/net/httpclient/recipes.html#post writing
 * async request: https://openjdk.java.net/groups/net/httpclient/intro.html
 */
public class HttpService {

    public static CompletableFuture<HttpResponse<String>> asyncPost(HashMap<String, String> data, URI uri) {
        String serializedData = S3Service.serializeJavaObject(data);

        Logger.debug("serialized data: " + serializedData);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpService.getHttpRequest(uri, serializedData);

        return client
            .sendAsync(request, BodyHandlers.ofString())
            .thenApply(
                res -> {
                    Logger.info(res.toString());
                    return res;
                }
            );
    }

    public static HttpResponse<String> post(HashMap<String, String> data, URI uri) {
        HttpResponse<String> res = null;
        String serializedData = S3Service.serializeJavaObject(data);

        Logger.debug("serialized data: " + serializedData);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpService.getHttpRequest(uri, serializedData);

        try {
            res = client.send(request, BodyHandlers.ofString());
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new ScraperShouldHaltException("InterruptedException while requesting POST - " + e.toString());
        } catch (IOException e) {
            e.printStackTrace();
            throw new ScraperShouldHaltException("IOException while requesting POST - " + e.toString());
        }

        return res;
    }

    private static HttpRequest getHttpRequest(URI uri, String serializedData) {
        return HttpRequest.newBuilder(uri).POST(BodyPublishers.ofString(serializedData)).build();
    }
}
