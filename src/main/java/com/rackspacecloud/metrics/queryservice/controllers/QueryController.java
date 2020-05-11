package com.rackspacecloud.metrics.queryservice.controllers;

import com.rackspacecloud.metrics.queryservice.domains.QueryDomainOutput;
import com.rackspacecloud.metrics.queryservice.exceptions.ErroredQueryResultException;
import com.rackspacecloud.metrics.queryservice.services.QueryService;
import io.micrometer.core.annotation.Timed;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.dto.QueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("")
@Slf4j
@Validated
public class QueryController {

    @Autowired
    QueryService queryService;

    /**
     * Admin or support level query. Intended to be used by a grafana frontend.
     * Grafana will append <code>/query</code> to the URL of the InfluxDB datasource configured.
     * Allow more free-form querying.
     * @param dbName - the alphanumeric tenant ID
     * @param queryString - an influxdb query
     * @return
     */
    @GetMapping("/query")
    @Timed(value = "query.service", extraTags = {"query.type","query.grafana"})
    public QueryResult grafanaQuery(
            final @RequestParam(value = "db", required = true) String dbName, //dbName = tenantId
            final @RequestParam("q") String queryString) {
        return queryService.query(dbName, queryString);
    }

    /******/
    /**
     * End-user or intelligence queries for a specific tenant derived from the
     * repose header
     * Use path variable instead of header or authentication principal to allow impersonation
     */

    /**
     * @param tenantId TenantID used for measurement lookup in tenant database
     * @return a list of measurements for tenant
     */
    @GetMapping("/v1.0/tenant/{tenantId}/intelligence-format-query/measurements")
    @Timed(value = "query.service", extraTags = {"query.type","query.intelligence.measurements"})
    public List<?> intelligenceFormattedQueryGetMeasurements(
            final @PathVariable String tenantId) { // Use repose tenantId
        log.debug("Called url:[{}] with tenantId: [{}]",
                "/intelligence-format-query/measurements", tenantId);
        return convertQueryResultToList(queryService.getMeasurementsForTenant(tenantId));
    }

    /**
     * @param measurement The specific measurement to be described
     * @param tenantId The id of the tenant
     * @return a list of measurement tags
     */
    @GetMapping("/v1.0/tenant/{tenantId}/intelligence-format-query/measurement-tags")
    @Timed(value = "query.service", extraTags = {"query.type","query.intelligence.measurement-tags"})
    public List<?> intelligenceFormattedQueryGetMeasurementTags(
            final @RequestParam String measurement,
            final @PathVariable String tenantId) { // Use repose tenantId
        log.debug("Called url:[{}] with tenantId: [{}], measurement: [{}]",
                "/intelligence-format-query/measurement-tags", tenantId, measurement);
        return convertQueryResultToList(queryService.getMeasurementTags(tenantId, measurement));
    }

    /**
     * @param measurement The specific measurement to be described
     * @param tenantId The id of the tenant
     * @return a list of fields for measurement
     */
    @GetMapping("/v1.0/tenant/{tenantId}/intelligence-format-query/measurement-fields")
    @Timed(value = "query.service", extraTags = {"query.type","query.intelligence.measurements-fields"})
    public List<?> intelligenceFormattedQueryGetMeasurementDescription(
            final @RequestParam String measurement,
            final @PathVariable String tenantId) { // Use repose tenantId
        log.debug("Called url:[{}] with tenantId: [{}], measurement: [{}]",
                "/intelligence-format-query/measurement-fields", tenantId, measurement);
        return convertQueryResultToList(queryService.getMeasurementFields(tenantId, measurement));
    }

    /**
     * @param measurement the measurement to query
     * @param begin beginning of time interval ISO 8601
     * @param end end of time interval ISO 8601
     * @param tenantId The tenantID for the measurement (from repose)
     * @return data points for a particular measurement within a time range
     */
    @GetMapping("/v1.0/tenant/{tenantId}/intelligence-format-query/measurement-series-by-time")
    @Timed(value = "query.service", extraTags = {"query.type","query.intelligence.measurement-series-by-time"})
    public List<?> intelligenceFormattedQueryGetMeasurementSeriesByTime(
            final @RequestParam("measurement") String measurement,
            // ISO 8601
            final @RequestParam Instant begin,
            final @RequestParam Instant end,
            final @PathVariable String tenantId) { // Use repose tenantId

        log.debug("Called url:[{}] with tenantId: [{}], measurement: [{}]" +
                ", begin: [{}], end: [{}]",
                "/intelligence-format-query", tenantId, measurement, begin, end);
        return convertQueryResultToList(
                queryService.getMeasurementSeriesForTimeInterval(tenantId, measurement, begin, end)
        );
    }

    private List<?> convertQueryResultToList(QueryResult queryResult) {
        if (queryResult.hasError()){
            String error = queryResult.getError();
            throw new ErroredQueryResultException("Query error: [" + error + "]");
        }

        List<QueryResult.Result> results = queryResult.getResults();

        List<QueryDomainOutput> outputs = new ArrayList<>();

        for (QueryResult.Result result : results) {
            if (result.hasError()) {
                throw new ErroredQueryResultException("Query error: [" + result.getError() + "]");
            }
            if (result.getSeries() != null) {
                for (QueryResult.Series series : result.getSeries()) {
                    QueryDomainOutput output = new QueryDomainOutput();
                    output.setName(series.getName());
                    output.setColumns(series.getColumns());
                    output.setTags(series.getTags());
                    output.setValuesCollection(series.getValues());
                    outputs.add(output);
                }
            }
        }

        List<Map<String, Object>> response = new ArrayList<>();
        outputs.forEach(out -> response.addAll(out.getQueryResponse()));

        return response;
    }
}
