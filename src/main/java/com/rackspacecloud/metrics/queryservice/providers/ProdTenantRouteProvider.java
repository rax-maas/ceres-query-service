package com.rackspacecloud.metrics.queryservice.providers;

import org.springframework.web.client.RestTemplate;

import java.util.Collection;

public class ProdTenantRouteProvider implements RouteProvider {
    private String tenantRoutingServiceUrl;

    public ProdTenantRouteProvider(String tenantRoutingServiceUrl) {
        this.tenantRoutingServiceUrl = tenantRoutingServiceUrl;
    }

    @Override
    public TenantRoutes getRoute(String tenantId, String measurement, RestTemplate restTemplate) {
        String requestUrl = String.format("%s/%s/%s", tenantRoutingServiceUrl, tenantId, measurement);

        //TODO: Work on any exception handling if restTemplate throws exception
        return restTemplate.getForObject(requestUrl, TenantRoutes.class);
    }

    @Override
    public Collection<String> getMeasurements(String tenantId, RestTemplate restTemplate) {
        String requestUrl = String.format("%s/%s/measurements", tenantRoutingServiceUrl, tenantId);

        return restTemplate.getForObject(requestUrl, Collection.class);
    }
}
