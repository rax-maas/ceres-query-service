package com.rackspacecloud.metrics.queryservice.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("rest-template")
public class RestTemplateConfigurationProperties {
    RequestConfiguration requestConfig;
    PoolingConnectionManager poolingHttpClientConnectionManager;

    @Data
    public static class RequestConfiguration {
        int connectionRequestTimeout;
        int connectTimeout;
        int socketTimeout;
    }

    @Data
    public static class PoolingConnectionManager {
        int maxTotal;
    }
}
