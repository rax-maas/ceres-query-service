package com.rackspacecloud.metrics.queryservice.providers;

import com.rackspacecloud.metrics.queryservice.services.InfluxDBPool;
import java.util.Collection;
import java.util.stream.Collectors;
import org.influxdb.InfluxDB;
import org.influxdb.dto.BoundParameterQuery.QueryBuilder;
import org.influxdb.dto.QueryResult;
import org.springframework.web.client.RestTemplate;

public class DevTestTenantRouteProvider implements RouteProvider {

    private final String stubbedInfluxDbUrl = "http://localhost:8086";
    private final String stubbedInfluxDbName = "db_0";
    private final String stubbedFullRetentionPolicy = "rp_5d";
    private String tenantRoutingServiceUrl;
    private final InfluxDBPool influxDBPool;

    public DevTestTenantRouteProvider(String tenantRoutingServiceUrl,
                                      InfluxDBPool influxDBPool) {
        this.tenantRoutingServiceUrl = tenantRoutingServiceUrl;
        this.influxDBPool = influxDBPool;
    }

    /**
     * Get stubbed tenant routes.
     * @param tenantId
     * @param measurement
     * @param restTemplate is used to connect to the routing service to get the route
     * @return
     * @throws Exception
     */
    @Override
    public TenantRoutes getRoute(String tenantId, String measurement, RestTemplate restTemplate) {
        String requestUrl = String.format("%s/%s/%s", tenantRoutingServiceUrl, tenantId, measurement);

        return getStubbedRoutes(tenantId, measurement);
    }

    /**
     * This method is a stub to generate the routes. Used for dev and test related work to break the dev
     * dependency on routing-service.
     * @param tenantId
     * @param measurement
     * @return
     */
    private TenantRoutes getStubbedRoutes(String tenantId, String measurement) {
        String tenantIdAndMeasurement = String.format("%s:%s", tenantId, measurement);

        TenantRoutes tenantRoutes = new TenantRoutes();
        tenantRoutes.setTenantIdAndMeasurement(tenantIdAndMeasurement);

        tenantRoutes.getRoutes().put("full", new TenantRoutes.TenantRoute(
            stubbedInfluxDbUrl,
            stubbedInfluxDbName,
            stubbedFullRetentionPolicy,
                "5d"
        ));

        tenantRoutes.getRoutes().put("5m", new TenantRoutes.TenantRoute(
            stubbedInfluxDbUrl,
            stubbedInfluxDbName,
                "rp_10d",
                "10d"
        ));

        tenantRoutes.getRoutes().put("20m", new TenantRoutes.TenantRoute(
            stubbedInfluxDbUrl,
            stubbedInfluxDbName,
                "rp_20d",
                "20d"
        ));

        tenantRoutes.getRoutes().put("60m", new TenantRoutes.TenantRoute(
            stubbedInfluxDbUrl,
            stubbedInfluxDbName,
                "rp_155d",
                "155d"
        ));

        tenantRoutes.getRoutes().put("240m", new TenantRoutes.TenantRoute(
            stubbedInfluxDbUrl,
            stubbedInfluxDbName,
                "rp_300d",
                "300d"
        ));

        tenantRoutes.getRoutes().put("1440m", new TenantRoutes.TenantRoute(
            stubbedInfluxDbUrl,
            stubbedInfluxDbName,
                "rp_1825d",
                "1825d"
        ));

        return tenantRoutes;
    }

    @Override
    public Collection<String> getMeasurements(String tenantId, RestTemplate restTemplate) {
        final InfluxDB client = influxDBPool.getInstance(stubbedInfluxDbUrl);
        final QueryResult results = client.query(
            QueryBuilder.newQuery("SHOW MEASUREMENTS")
            .forDatabase(stubbedInfluxDbName)
            .create()
        );

        if (results.hasError()) {
            throw new IllegalStateException("Unable to retrieve measurements from InfluxDB: " +
                results.getError());
        }

        // SHOW MEASUREMENTS always has one result, one series (measurements), with one column (name)
        return results.getResults().get(0)
            .getSeries().get(0)
            .getValues()
            .stream()
            .map(row -> (String)row.get(0))
            .collect(Collectors.toList());
    }
}
