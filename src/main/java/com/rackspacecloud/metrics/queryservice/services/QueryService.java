package com.rackspacecloud.metrics.queryservice.services;

import java.time.Instant;
import org.influxdb.dto.QueryResult;

public interface QueryService {

    QueryResult getMeasurementsForTenant(String tenantId);

    QueryResult getMeasurementTags(String tenantId, String measurement);

    QueryResult getMeasurementFields(String tenantId, String measurement);

    QueryResult getMeasurementSeriesForTimeInterval(String tenantId, String measurement, Instant begin, Instant end);

    // general-purpose grafana/admin access
    QueryResult query(String dbName, String queryString);
}
