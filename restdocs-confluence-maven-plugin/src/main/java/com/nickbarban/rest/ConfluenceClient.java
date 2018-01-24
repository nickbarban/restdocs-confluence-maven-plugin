package com.nickbarban.rest;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nickbarban.model.ConfluencePage;
import com.nickbarban.model.ConfluencePageFactory;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConfluenceClient {
    private ObjectMapper objectMapper = new ObjectMapper();

    private final OkHttpClient.Builder client;

    private final String credentials;

    private final Log log;

    private final HttpUrl endpoint;

    private final String space;

    public ConfluenceClient(String userName, String password, HttpUrl endpoint, Log log, String space) {
        this.log = log;
        this.space = space;
        this.credentials = Credentials.basic(userName, password);
        client = new OkHttpClient.Builder()
                .authenticator((route, response) -> response.request().newBuilder()
                        .header("Authorization", this.credentials).build());
        this.endpoint = endpoint;
        objectMapper.configure(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY, true);
        objectMapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);

    }

    public ConfluencePage getPage(String id) throws MojoExecutionException {
        HttpUrl url = endpoint.newBuilder()
                .addPathSegment("content")
                .addPathSegment(id)
                .addQueryParameter("expand", "body.storage,version")
                .build();
        Response response = getRequest(url);
        JsonNode jsonResult;
        ConfluencePage result;

        try {
            jsonResult = objectMapper.readTree((response.body().string()));
            log.info(String.format("Fetched page: %s", jsonResult.toString()));
        } catch (IOException e) {
            throw new MojoExecutionException(String.format("Can not read response from %s", url.toString()), e);
        }

        try {
            result = objectMapper.readValue(jsonResult.toString(), ConfluencePage.class);
            if (result == null) {
                log.info(String.format("There are no pages with id: %s", id));
                return null;
            } else {
                log.info(String.format("Mapped page: %s", result.getTitle()));
            }
        } catch (IOException e) {
            throw new MojoExecutionException(String.format("Can not map response to ConfluencePage from %s", url.toString()), e);
        }

        return result;
    }

    public ConfluencePage getPageByTitle(String title) throws MojoExecutionException {
        HttpUrl url = endpoint.newBuilder()
                .addPathSegment("content")
                .addPathSegment("search")
                .addQueryParameter("cql", String.format("title=\"%s\"", title))
                .build();
        Response response = getRequest(url);
        JsonNode results;
        ConfluencePage[] result;

        try {
            results = objectMapper.readTree((response.body().string())).get("results");
            log.info(String.format("Fetched page: %s", results.toString()));
        } catch (IOException e) {
            throw new MojoExecutionException(String.format("Can not read response from %s", url.toString()), e);
        }

        try {
            result = objectMapper.readValue(results.toString(), ConfluencePage[].class);
            if (result.length == 0) {
                log.info(String.format("There are no pages with title: %s", title));
                return null;
            } else {
                log.info(String.format("Mapped page: %s",
                        Stream.of(result).map(ConfluencePage::getTitle).collect(Collectors.joining(", ", "[", "]"))));
            }
        } catch (IOException e) {
            throw new MojoExecutionException(String.format("Can not map response array to ConfluencePage[] from %s", url.toString()), e);
        }

        return result[0];
    }

    public List<ConfluencePage> getPages(String anchestorId) throws MojoExecutionException {
        HttpUrl url = endpoint.newBuilder()
                .addPathSegment("content")
                .addPathSegment(anchestorId)
                .addPathSegment("child")
                .addQueryParameter("expand", "page")
                .build();
        Response response = getRequest(url);
        JsonNode results;
        ConfluencePage[] result;

        try {
            results = objectMapper.readTree((response.body().string())).get("page").get("results");
            log.info(String.format("Fetched pages: %s", results.toString()));
        } catch (IOException e) {
            throw new MojoExecutionException(String.format("Can not read response from %s", url.toString()), e);
        }

        try {
            result = objectMapper.readValue(results.toString(), ConfluencePage[].class);
            log.info(String.format("Mapped pages: %s",
                    Stream.of(result).map(ConfluencePage::getTitle).collect(Collectors.joining(", ", "[", "]"))));
        } catch (IOException e) {
            throw new MojoExecutionException(String.format("Can not map response array to ConfluencePage[] from %s", url.toString()), e);
        }

        return Arrays.asList(result);
    }

    public String createPage(String ancestor, String content, String title) throws MojoExecutionException {
        log.info(String.format("Page %s will be created under ancestor: %s", title, ancestor));

        HttpUrl url = endpoint.newBuilder()
                .addPathSegment("content")
                .build();
        ConfluencePage page = ConfluencePageFactory.createStoragePage(title, content, space, ancestor);
        log.debug(String.format("Page %s has content: %s", page.getTitle(), page.getBody().getStorage().getValue()));
        String json;
        try {
            json = objectMapper.writeValueAsString(page);
            log.debug(String.format("Page %s is mapped to json: %s", title, json));
        } catch (JsonProcessingException e) {
            throw new MojoExecutionException(String.format("Can not map json from page %s", title), e);
        }
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        Response response = postRequest(url, body);

        try {
            return objectMapper.readTree((response.body().string())).get("id").asText();
        } catch (IOException e) {
            throw new MojoExecutionException(String.format("Can not read response from %s", url.toString()), e);
        }
    }

    public String updatePage(ConfluencePage page, String content) throws MojoExecutionException {
        log.info(String.format("Page %s will be updated", page.getTitle()));

        HttpUrl url = endpoint.newBuilder()
                .addPathSegment("content")
                .addPathSegment(page.getId())
                .build();
        String json;
        try {
            json = objectMapper.writeValueAsString(page);
        } catch (JsonProcessingException e) {
            throw new MojoExecutionException(String.format("Can not map json from page %s", page.getTitle()), e);
        }
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        Response response = putRequest(url, body);

        try {
            return objectMapper.readTree((response.body().string())).get("id").asText();
        } catch (IOException e) {
            throw new MojoExecutionException(String.format("Can not read response from %s", url.toString()), e);
        }
    }

    private Response putRequest(HttpUrl url, RequestBody body) throws MojoExecutionException {
        Request request = new Request.Builder()
                .header("Authorization", credentials)
                .url(url)
                .put(body)
                .build();
        try {
            return client.build().newCall(request).execute();
        } catch (IOException e) {
            throw new MojoExecutionException(String.format("Can not perform PUT request to %s", url.toString()), e);
        }
    }

    private Response getRequest(HttpUrl url) throws MojoExecutionException {
        Request request = new Request.Builder()
                .header("Authorization", credentials)
                .url(url)
                .get()
                .build();
        try {
            return client.build().newCall(request).execute();
        } catch (IOException e) {
            throw new MojoExecutionException(String.format("Can not perform GET request to %s", url.toString()), e);
        }
    }

    private Response postRequest(HttpUrl url, RequestBody body) throws MojoExecutionException {
        Request request = new Request.Builder()
                .header("Authorization", credentials)
                .url(url)
                .post(body)
                .build();
        try {
            return client.build().newCall(request).execute();
        } catch (IOException e) {
            throw new MojoExecutionException(String.format("Can not perform POST request to %s", url.toString()), e);
        }
    }
}
