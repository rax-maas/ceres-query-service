package com.rackspacecloud.metrics.queryservice.services;

import org.influxdb.dto.QueryResult;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public interface QueryService {

    QueryResult getMeasurementsForTenant(String tenantId);

    QueryResult getMeasurementTags(String tenantId, String measurement);

    QueryResult getMeasurementFields(String tenantId, String measurement);

    QueryResult getMeasurementSeriesForTimeInterval(String tenantId, String measurement, LocalDateTime begin, LocalDateTime end);

    // general-purpose grafana/admin access
    QueryResult query(String dbName, String queryString);
}
