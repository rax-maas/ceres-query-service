package com.rackspacecloud.metrics.queryservice.exceptions;

/**
 * This exception is thrown when query result has some error.
 */
public class ErroredQueryResultException extends RuntimeException {
    public ErroredQueryResultException(String tenantId) {
        super("Errored query result for [" + tenantId + "].");
    }
}
