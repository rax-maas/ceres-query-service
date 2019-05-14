package com.rackspacecloud.metrics.queryservice.configuration;

import com.rackspacecloud.metrics.queryservice.providers.DevTestTenantRouteProvider;
import com.rackspacecloud.metrics.queryservice.providers.ProdTenantRouteProvider;
import com.rackspacecloud.metrics.queryservice.providers.RouteProvider;
import org.influxdb.InfluxDB;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Configuration
public class QueryServiceConfiguration {
    @Value("${tenant-routing-service.url}")
    private String tenantRoutingServiceUrl;

    @Bean
    ConcurrentMap<String, InfluxDB> urlInfluxDBInstanceMap() {
        return new ConcurrentHashMap<>();
    }

    @Bean(name = "routeProvider")
    @Profile({"development", "test"})
    public RouteProvider devTestTenantRouteProvider() {
        return new DevTestTenantRouteProvider(tenantRoutingServiceUrl);
    }

    @Bean(name = "routeProvider")
    @Profile("production")
    public RouteProvider prodTenantRouteProvider() {
        return new ProdTenantRouteProvider(tenantRoutingServiceUrl);
    }
}
