package com.rackspacecloud.metrics.queryservice.exceptions;

/**
 * This exception is thrown when route information is not found for the given tenantId.
 */
public class RouteNotFoundException extends RuntimeException {
    public RouteNotFoundException(String tenantId, Throwable e) {
        super("TenantId [" + tenantId + "] is not found in the repository.", e);
    }
}
