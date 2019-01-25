package com.rackspacecloud.metrics.queryservice.providers;

import org.springframework.web.client.RestTemplate;

public interface RouteProvider {
    TenantRoutes getRoute(String tenantId, RestTemplate restTemplate);
}
