package com.rackspacecloud.metrics.queryservice.configuration;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class QueryServiceConfiguration {
    @Value("${database.urls}")
    private String influxdbUrls;

    @Bean
    Map<String, InfluxDB> urlInfluxDBInstanceMap(){
        String[] influxdbUrlsCollection = influxdbUrls.split(";");
        HashMap<String, InfluxDB> urlInstanceMap = new HashMap<>();
        for(int i = 0; i < influxdbUrlsCollection.length; i++) {
            String url = influxdbUrlsCollection[i];
            urlInstanceMap.put(url, InfluxDBFactory.connect(url));
        }
        return urlInstanceMap;
    }
}
