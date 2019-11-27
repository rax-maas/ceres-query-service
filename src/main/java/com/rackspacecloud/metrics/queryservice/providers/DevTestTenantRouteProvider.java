package com.rackspacecloud.metrics.queryservice.providers;

import org.springframework.web.client.RestTemplate;

import java.util.Collection;

public class DevTestTenantRouteProvider implements RouteProvider {
    private String tenantRoutingServiceUrl;

    public DevTestTenantRouteProvider(String tenantRoutingServiceUrl) {
        this.tenantRoutingServiceUrl = tenantRoutingServiceUrl;
    }

    /**
     * Get stubbed tenant routes.
     * @param tenantId
     * @param measurement
     * @param restTemplate is used to connect to the routing service to get the route
     * @return
     * @throws Exception
     */
    @Override
    public TenantRoutes getRoute(String tenantId, String measurement, RestTemplate restTemplate) {
        String requestUrl = String.format("%s/%s/%s", tenantRoutingServiceUrl, tenantId, measurement);

        return getStubbedRoutes(tenantId, measurement);
    }

    /**
     * This method is a stub to generate the routes. Used for dev and test related work to break the dev
     * dependency on routing-service.
     * @param tenantId
     * @param measurement
     * @return
     */
    private TenantRoutes getStubbedRoutes(String tenantId, String measurement) {
        String tenantIdAndMeasurement = String.format("%s:%s", tenantId, measurement);

        TenantRoutes tenantRoutes = new TenantRoutes();
        tenantRoutes.setTenantIdAndMeasurement(tenantIdAndMeasurement);

        tenantRoutes.getRoutes().put("full", new TenantRoutes.TenantRoute(
                "http://localhost:8086",
                "db_0",
                "rp_5d",
                "5d"
        ));

        tenantRoutes.getRoutes().put("5m", new TenantRoutes.TenantRoute(
                "http://localhost:8086",
                "db_0",
                "rp_10d",
                "10d"
        ));

        tenantRoutes.getRoutes().put("20m", new TenantRoutes.TenantRoute(
                "http://localhost:8086",
                "db_0",
                "rp_20d",
                "20d"
        ));

        tenantRoutes.getRoutes().put("60m", new TenantRoutes.TenantRoute(
                "http://localhost:8086",
                "db_0",
                "rp_155d",
                "155d"
        ));

        tenantRoutes.getRoutes().put("240m", new TenantRoutes.TenantRoute(
                "http://localhost:8086",
                "db_0",
                "rp_300d",
                "300d"
        ));

        tenantRoutes.getRoutes().put("1440m", new TenantRoutes.TenantRoute(
                "http://localhost:8086",
                "db_0",
                "rp_1825d",
                "1825d"
        ));

        return tenantRoutes;
    }

    @Override
    public Collection<String> getMeasurements(String tenantId, RestTemplate restTemplate) {
        String requestUrl = String.format("%s/%s/measurements", tenantRoutingServiceUrl, tenantId);

        return restTemplate.getForObject(requestUrl, Collection.class);
    }
}
