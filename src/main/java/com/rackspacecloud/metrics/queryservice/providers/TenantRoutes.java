package com.rackspacecloud.metrics.queryservice.providers;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
public class TenantRoutes {
    private String tenantIdAndMeasurement;
    private Map<String, TenantRoute> routes;

    public TenantRoutes() {
        routes = new HashMap<>();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TenantRoute {
        private String path;
        private String databaseName;
        private String retentionPolicyName;
        private String retentionPolicy;
    }
}
