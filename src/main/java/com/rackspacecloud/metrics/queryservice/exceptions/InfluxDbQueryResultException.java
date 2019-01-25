package com.rackspacecloud.metrics.queryservice.exceptions;

/**
 * This exception is thrown when query result returned by InfluxDB has mismatched columns and their values.
 */
public class InfluxDbQueryResultException extends RuntimeException {
    public InfluxDbQueryResultException(String tenantId) {
        super("Mismatched columns with their values. [" + tenantId + "].");
    }
}
