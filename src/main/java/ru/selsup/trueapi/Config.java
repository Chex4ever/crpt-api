package ru.selsup.trueapi;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class Config {
    private String baseUrlV3;
    private String baseUrlV4;
    private String authGetDataEndpoint;
    private String authGetTokenEndpoint;
    private String createDocumentEndpoint;
    private Duration connectionTimeout = Duration.ofSeconds(10);
    private Duration readTimeout = Duration.ofSeconds(30);
    private Duration authenticateTimeout = Duration.ofSeconds(15);
    private Duration createDocumentTimeout = Duration.ofSeconds(30);
    private int requestLimit = 10;
    private TimeUnit timeUnit = TimeUnit.SECONDS;
    public Config() {
    }

    public String getBaseUrlV4() {
        return baseUrlV4;
    }

    public void setBaseUrlV4(String baseUrlV4) {
        this.baseUrlV4 = baseUrlV4;
    }

    public String getBaseUrlV3() {
        return baseUrlV3;
    }

    public void setBaseUrlV3(String baseUrlV3) {
        this.baseUrlV3 = baseUrlV3;
    }

    public String getCreateDocumentEndpoint() {
        return createDocumentEndpoint;
    }

    public void setCreateDocumentEndpoint(String createDocumentEndpoint) {
        this.createDocumentEndpoint = createDocumentEndpoint;
    }

    public String getAuthGetTokenEndpoint() {
        return authGetTokenEndpoint;
    }

    public void setAuthGetTokenEndpoint(String authGetTokenEndpoint) {
        this.authGetTokenEndpoint = authGetTokenEndpoint;
    }

    public String getAuthGetDataEndpoint() {
        return authGetDataEndpoint;
    }

    public void setAuthGetDataEndpoint(String authGetDataEndpoint) {
        this.authGetDataEndpoint = authGetDataEndpoint;
    }

    public Duration getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Duration connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public Duration getAuthenticateTimeout() {
        return authenticateTimeout;
    }

    public void setAuthenticateTimeout(Duration authenticateTimeout) {
        this.authenticateTimeout = authenticateTimeout;
    }

    public Duration getCreateDocumentTimeout() {
        return createDocumentTimeout;
    }

    public void setCreateDocumentTimeout(Duration createDocumentTimeout) {
        this.createDocumentTimeout = createDocumentTimeout;
    }

    public int getRequestLimit() {
        return requestLimit;
    }

    public void setRequestLimit(int requestLimit) {
        this.requestLimit = requestLimit;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }
}