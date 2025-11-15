package com.xianchao.proxyserver;

import java.util.HashMap;
import java.util.Map;

public class HttpRequest {

    private final String method;
    private final String url;
    private final String version;
    private final Map<String, String> header;

    public HttpRequest(String method, String url, String version) {
        this.method = method;
        this.url = url;
        this.version = version;
        this.header = new HashMap<>();
    }

    public String getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }

    public String getVersion() {
        return version;
    }

    public void putHeader(String key, String value) {
        header.put(key, value);
    }

    public String getHeader(String key) {
        return header.get(key);
    }
}
