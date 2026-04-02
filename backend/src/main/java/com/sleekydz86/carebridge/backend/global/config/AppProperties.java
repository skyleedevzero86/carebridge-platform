package com.sleekydz86.carebridge.backend.global.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Cors cors, Security security, Tcp tcp, Simulator simulator, Pagination pagination) {
    public record Cors(List<String> allowedOrigins)  {}
    public record Security(String tokenSecret, long tokenExpirationMinutes, long tokenRefreshThresholdMinutes)  {}
    public record Tcp(int port)  {}
    public record Simulator(boolean enabled, String host, int port, long intervalMillis, long initialDelayMillis)  {}
    public record Pagination(int chatPageSize, int deviceEventPageSize)  {}
}