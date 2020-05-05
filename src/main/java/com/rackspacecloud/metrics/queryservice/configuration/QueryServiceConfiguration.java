package com.rackspacecloud.metrics.queryservice.configuration;

import com.rackspacecloud.metrics.queryservice.providers.DevTestTenantRouteProvider;
import com.rackspacecloud.metrics.queryservice.providers.ProdTenantRouteProvider;
import com.rackspacecloud.metrics.queryservice.providers.RouteProvider;
import com.rackspacecloud.metrics.queryservice.services.InfluxDBPool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class QueryServiceConfiguration {
    @Value("${tenant-routing-service.url}")
    private String tenantRoutingServiceUrl;

    @Bean(name = "routeProvider")
    @Profile({"development", "test"})
    public RouteProvider devTestTenantRouteProvider(InfluxDBPool influxDBPool) {
        return new DevTestTenantRouteProvider(tenantRoutingServiceUrl, influxDBPool);
    }

    @Bean(name = "routeProvider")
    @Profile("production")
    public RouteProvider prodTenantRouteProvider() {
        return new ProdTenantRouteProvider(tenantRoutingServiceUrl);
    }
}
