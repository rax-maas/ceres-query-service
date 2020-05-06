package com.rackspacecloud.metrics.queryservice.services;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.springframework.stereotype.Component;

/**
 * Provides a mechanism to lazy-create {@link InfluxDB} instances for requested instance URLs.
 */
@Component
public class InfluxDBPool {
  private ConcurrentMap<String/*InfluxDB URL*/, InfluxDB> urlInfluxDBInstanceMap =
      new ConcurrentHashMap<>();

  public InfluxDB getInstance(String influxDbUrl) {
    return urlInfluxDBInstanceMap.computeIfAbsent(influxDbUrl, this::connect);
  }

  private InfluxDB connect(String influxDbUrl) {
    return InfluxDBFactory.connect(influxDbUrl)
      .setLogLevel(InfluxDB.LogLevel.BASIC);
  }
}
