package com.rackspacecloud.metrics.queryservice.providers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

public class ProdTenantRouteProvider implements RouteProvider {
    @Value("${tenant-routing-service.url}")
    private String tenantRoutingServiceUrl;

    @Override
    public TenantRoutes getRoute(String tenantId, RestTemplate restTemplate) {
        String requestUrl = String.format("%s/%s", tenantRoutingServiceUrl, tenantId);

        //TODO: Work on any exception handling if restTemplate throws exception
        return restTemplate.getForObject(requestUrl, TenantRoutes.class);
    }

//    //TODO: REMOVE THIS ONCE LATEST TENANT-ROUTING-SERVICE IS DEPLOYED
//    @Override
//    public TenantRoutes getRoute(String tenantId, RestTemplate restTemplate) {
//        TenantRoutes tenantRoutes = new TenantRoutes();
//        tenantRoutes.setTenantId(tenantId);
//        Map<String, TenantRoutes.TenantRoute> routes = tenantRoutes.getRoutes();
//
//        routes.put("full", new TenantRoutes.TenantRoute(
//                "http://data-influxdb:8086",
//                "db_hybrid_1667601",
//                "rp_5d",
//                "5d"
//        ));
//
//        routes.put("5m", new TenantRoutes.TenantRoute(
//                "http://data-influxdb:8086",
//                "db_hybrid_1667601",
//                "rp_10d",
//                "10d"
//        ));
//
//        routes.put("20m", new TenantRoutes.TenantRoute(
//                "http://data-influxdb:8086",
//                "db_hybrid_1667601",
//                "rp_20d",
//                "20d"
//        ));
//
//        routes.put("60m", new TenantRoutes.TenantRoute(
//                "http://data-influxdb:8086",
//                "db_hybrid_1667601",
//                "rp_155d",
//                "155d"
//        ));
//
//        routes.put("240m", new TenantRoutes.TenantRoute(
//                "http://data-influxdb:8086",
//                "db_hybrid_1667601",
//                "rp_300d",
//                "300d"
//        ));
//
//        routes.put("1440m", new TenantRoutes.TenantRoute(
//                "http://data-influxdb:8086",
//                "db_hybrid_1667601",
//                "rp_1825d",
//                "1825d"
//        ));
//
//        return tenantRoutes;
//    }
}
