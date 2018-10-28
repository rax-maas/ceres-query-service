package com.rackspacecloud.metrics.queryservice.models;

import lombok.Data;

@Data
public class TenantRoute {
    private String path;
    private String databaseName;
}
