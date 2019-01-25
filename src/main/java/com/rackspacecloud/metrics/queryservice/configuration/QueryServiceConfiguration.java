package com.rackspacecloud.metrics.queryservice.configuration;

import com.rackspacecloud.metrics.queryservice.providers.DevTestTenantRouteProvider;
import com.rackspacecloud.metrics.queryservice.providers.ProdTenantRouteProvider;
import com.rackspacecloud.metrics.queryservice.providers.RouteProvider;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.naming.ConfigurationException;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class QueryServiceConfiguration {
    @Value("${database.urls}")
    private String influxDbUrls;

    @Bean
    Map<String, InfluxDB> urlInfluxDBInstanceMap() throws ConfigurationException {
        String[] influxDbUrlsCollection = influxDbUrls.split(";");

        if(influxDbUrlsCollection.length == 0) throw new ConfigurationException("No database URLs found.");

        HashMap<String, InfluxDB> urlInstanceMap = new HashMap<>();

        for(int i = 0; i < influxDbUrlsCollection.length; i++) {
            String url = influxDbUrlsCollection[i];
            urlInstanceMap.put(url, InfluxDBFactory.connect(url));
        }
        return urlInstanceMap;
    }

    @Bean(name = "routeProvider")
    @Profile({"development", "test"})
    public RouteProvider devTestTenantRouteProvider() {
        return new DevTestTenantRouteProvider();
    }

    @Bean(name = "routeProvider")
    @Profile("production")
    public RouteProvider prodTenantRouteProvider() {
        return new ProdTenantRouteProvider();
    }
}
