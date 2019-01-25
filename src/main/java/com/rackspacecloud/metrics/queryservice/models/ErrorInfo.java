package com.rackspacecloud.metrics.queryservice.models;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * This object is returned to the client when any exception is thrown by the service.
 */
@Data
@AllArgsConstructor
public class ErrorInfo {
    private String message;
    private String rootCause;
}
