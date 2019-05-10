package com.rackspacecloud.metrics.queryservice.providers;

import lombok.Data;
import lombok.RequiredArgsConstructor;

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
    @RequiredArgsConstructor
    public static class TenantRoute {
        private String path;
        private String databaseName;
        private String retentionPolicyName;
        private String retentionPolicy;

        public TenantRoute(String path, String databaseName, String retentionPolicyName,
                           String retentionPolicy){
            this.path = path;
            this.databaseName = databaseName;
            this.retentionPolicyName = retentionPolicyName;
            this.retentionPolicy = retentionPolicy;
        }
    }
}
