package com.rackspacecloud.metrics.queryservice.providers;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class TenantRoutes {
    private String tenantId;
    private Map<String, TenantRoute> routes;

    public TenantRoutes() {
        routes = new HashMap<>();
    }

    @Data
    public static class TenantRoute {
        private String path;
        private String databaseName;
        private String retentionPolicyName;
        private String retentionPolicy;
        private int maxSeriesCount;

        public TenantRoute(String path, String databaseName, String retentionPolicyName,
                           String retentionPolicy, int maxSeriesCount){
            this.path = path;
            this.databaseName = databaseName;
            this.retentionPolicyName = retentionPolicyName;
            this.retentionPolicy = retentionPolicy;
            this.maxSeriesCount = maxSeriesCount;
        }
    }
}
