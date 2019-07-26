package com.xsc.util.http;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * @author shichao.xia
 * @date 2019/3/12 17:02
 */
public class RequestEntity {

    private String url;

    private Map<String, String> params;

    private Map<String, String> headers;

    private String body;

    private RequestEntity(Builder builder) {
        this.url = builder.url;
        this.body = builder.body;
        this.params = builder.params;
        this.headers = builder.headers;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getUrl() {
        return url;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public String getBody() {
        return body;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public static class Builder {

        private String url;

        private Map<String, String> params = Maps.newHashMap();

        private Map<String, String> headers = Maps.newHashMap();

        private String body;

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder param(String key, String val) {
            params.putIfAbsent(key, val);
            return this;
        }

        public Builder params(Map<String, String> map) {
            this.params.putAll(map);
            return this;
        }

        public Builder header(String key, String val) {
            headers.putIfAbsent(key, val);
            return this;
        }

        public Builder headers(Map<String, String> map) {
            this.headers.putAll(map);
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public RequestEntity build() {
            return new RequestEntity(this);
        }
    }

}