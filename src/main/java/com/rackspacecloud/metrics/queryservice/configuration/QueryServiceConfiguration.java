package com.rackspacecloud.metrics.queryservice.configuration;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueryServiceConfiguration {
    @Value("${database.url}")
    private String influxdbUrl;

    @Bean
    InfluxDB influxDB(){
        return InfluxDBFactory.connect(influxdbUrl);
    }
}
