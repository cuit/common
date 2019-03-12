package com.cm.util.http;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Response;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author shichao.xia
 * @date 2019/3/12 14:57
 */
public class HttpRequest {

    private static final String DEFAULT_CHARSET = "UTF-8";

    private static final int CONNECT_TIME_OUT = 5000;

    private static final int MAX_CONNECT = 30;

    private static AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();

    static {
        builder.setConnectTimeout(CONNECT_TIME_OUT);
        builder.setReadTimeout(3000);
        builder.setMaxConnections(MAX_CONNECT);
    }

    private AsyncHttpClient asyncHttpClient;

    private RequestEntity entity;

    public HttpRequest createRequestEntity(RequestEntity entity) {
        this.entity = entity;
        return this;
    }

    private HttpRequest(AsyncHttpClientConfig.Builder builder) {
        this.asyncHttpClient = new AsyncHttpClient(builder.build());
    }

    public static HttpRequest getClient() {
        return new HttpRequest(builder);
    }

    public String getResultAsString() {
        try {
            return get().get().getResponseBody(DEFAULT_CHARSET);
        } catch (InterruptedException | ExecutionException | IOException e) {
            e.printStackTrace();
        }
        return StringUtils.EMPTY;
    }

    public Future<Response> get() {
        return get(entity.getUrl(), entity.getParams(), entity.getHeaders());
    }

    private Future<Response> get(String url, Map<String, String> params, Map<String, String> headers) {
        AsyncHttpClient asyncHttpClient = getClient().asyncHttpClient;
        AsyncHttpClient.BoundRequestBuilder builder = asyncHttpClient.prepareGet(Objects.requireNonNull(url, "The request url can`t be empty!"));
        builder.setBodyEncoding(DEFAULT_CHARSET);
        if (MapUtils.isNotEmpty(params)) {
            Set<String> keys = params.keySet();
            for (String key : keys) {
                builder.addQueryParam(key, params.get(key));
            }
        }
        if (MapUtils.isNotEmpty(headers)) {
            Set<String> keys = headers.keySet();
            for (String key : keys) {
                builder.addHeader(key, headers.get(key));
            }
        }
        return builder.execute();
    }

    public Future<Response> post() {
        return post(entity.getUrl(), entity.getBody(), entity.getParams(), entity.getHeaders());
    }

    private Future<Response> post(String url, String body, Map<String, String> params, Map<String, String> headers) {
        AsyncHttpClient asyncHttpClient = getClient().asyncHttpClient;
        AsyncHttpClient.BoundRequestBuilder builder = asyncHttpClient.preparePost(Objects.requireNonNull(url, "The request url can`t be empty!"));
        builder.setBodyEncoding(DEFAULT_CHARSET);
        if (MapUtils.isNotEmpty(headers)) {
            Set<String> keys = headers.keySet();
            for (String key : keys) {
                builder.setHeader(key, headers.get(key));
            }
        }
        if (MapUtils.isNotEmpty(params)) {
            Set<String> keys = params.keySet();
            for (String key : keys) {
                builder.addFormParam(key, params.get(key));
            }
        } else if (body != null) {
            builder.setBody(body);
        }
        return builder.execute();
    }
}
