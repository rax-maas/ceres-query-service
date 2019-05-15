package com.rackspacecloud.metrics.queryservice.providers;

import org.springframework.web.client.RestTemplate;

import java.util.Collection;

public interface RouteProvider {
    TenantRoutes getRoute(String tenantId, String measurement, RestTemplate restTemplate);
    Collection<String> getMeasurements(String tenantId, RestTemplate restTemplate);
}
