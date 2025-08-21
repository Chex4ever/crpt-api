package ru.selsup.test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class Config {
    private String baseUrl;
    private String authEndpoint;
    private String createDocumentEndpoint;
    private Duration connectionTimeout = Duration.ofSeconds(10);
    private Duration readTimeout = Duration.ofSeconds(30);
    private Duration authenticateTimeout = Duration.ofSeconds(15);
    private Duration createDocumentTimeout = Duration.ofSeconds(30);
    private int requestLimit = 10;
    private TimeUnit timeUnit = TimeUnit.SECONDS;

    // Конструкторы
    public Config() {}

    public Config(String baseUrl, String authEndpoint, String createDocumentEndpoint) {
        this.baseUrl = baseUrl;
        this.authEndpoint = authEndpoint;
        this.createDocumentEndpoint = createDocumentEndpoint;
    }

    // Геттеры и сеттеры
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getAuthEndpoint() { return authEndpoint; }
    public void setAuthEndpoint(String authEndpoint) { this.authEndpoint = authEndpoint; }

    public String getCreateDocumentEndpoint() { return createDocumentEndpoint; }
    public void setCreateDocumentEndpoint(String createDocumentEndpoint) { this.createDocumentEndpoint = createDocumentEndpoint; }

    public Duration getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(Duration connectionTimeout) { this.connectionTimeout = connectionTimeout; }

    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }

    public Duration getAuthenticateTimeout() { return authenticateTimeout; }
    public void setAuthenticateTimeout(Duration authenticateTimeout) { this.authenticateTimeout = authenticateTimeout; }

    public Duration getCreateDocumentTimeout() { return createDocumentTimeout; }
    public void setCreateDocumentTimeout(Duration createDocumentTimeout) { this.createDocumentTimeout = createDocumentTimeout; }

    public int getRequestLimit() { return requestLimit; }
    public void setRequestLimit(int requestLimit) { this.requestLimit = requestLimit; }

    public TimeUnit getTimeUnit() { return timeUnit; }
    public void setTimeUnit(TimeUnit timeUnit) { this.timeUnit = timeUnit; }
}