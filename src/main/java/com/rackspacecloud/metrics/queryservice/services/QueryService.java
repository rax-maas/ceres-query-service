package com.rackspacecloud.metrics.queryservice.services;

import org.influxdb.dto.QueryResult;

public interface QueryService {
    QueryResult query(String dbName, String queryString);
}
