package com.rackspacecloud.metrics.queryservice.exceptions;

/**
 * This exception is thrown when query is invalid for the given tenantId.
 */
public class InvalidQueryException extends RuntimeException {
    public InvalidQueryException(String tenantId) {
        super("Invalid query for [" + tenantId + "].");
    }
}
