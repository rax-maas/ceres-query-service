package com.rackspacecloud.metrics.queryservice.controllers;

import com.rackspacecloud.metrics.queryservice.domains.QueryDomainOutput;
import com.rackspacecloud.metrics.queryservice.exceptions.ErroredQueryResultException;
import com.rackspacecloud.metrics.queryservice.services.QueryService;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.dto.QueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.websocket.server.PathParam;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.rackspacecloud.metrics.queryservice.services.ReposeHeaderFilter.HEADER_TENANT;

@RestController
@RequestMapping("")
@Slf4j
@Validated
public class QueryController {

    @Autowired
    QueryService queryService;

    /**
     * Admin or support level query. Intended to be used by a grafana frontend.
     * Allow more free-form querying.
     * @param dbName
     * @param queryString
     * @return
     */
    @GetMapping("/grafana-query")
    @Timed(value = "query.service", extraTags = {"query.type","query.grafana"})
    public QueryResult query(
            final @RequestParam(value = "db", required = true) @Valid @Pattern(regexp="-?\\d+") String dbName, //dbName = tenantId
            final @RequestParam("q") String queryString) {
        return queryService.query(dbName, queryString);
    }

    /******/
    /**
     * End-user or intelligence queries for a specific tenant derived from the
     * repose header
     */

    /**
     * Return a list of measurements for tenant
     * @param tenantId TenantID used for measurement lookup in tenant database
     * @return
     */
    @GetMapping("/intelligence-format-query/measurements")
    @Timed(value = "query.service", extraTags = {"query.type","query.intelligence.measurements"})
    public List<?> intelligenceFormattedQueryGetMeasurements(
            final @RequestHeader(HEADER_TENANT) String tenantId) { // Use repose tenantId
        log.debug("Called url:[{}] with tenantId: [{}]",
                "/intelligence-format-query/measurements", tenantId);
        return convertQueryResultToList(queryService.getMeasurementsForTenant(tenantId));
    }

    /**
     * Return description of measurement, i.e. list of fields and tags.
     * @param measurement The specific measurement to be described
     * @param tenantId The id of the tenant
     * @return
     */
    @GetMapping("/intelligence-format-query/measurement-tags")
    @Timed(value = "query.service", extraTags = {"query.type","query.intelligence.measurement-tags"})
    public List<?> intelligenceFormattedQueryGetMeasurementTags(
            final @RequestParam("measurement") String measurement,
            final @RequestHeader(HEADER_TENANT) String tenantId) { // Use repose tenantId
        log.debug("Called url:[{}] with tenantId: [{}], measurement: [{}]",
                "/intelligence-format-query/measurement-tags", tenantId, measurement);
        return convertQueryResultToList(queryService.getMeasurementTags(tenantId, measurement));
    }

    /**
     * Return fields for measurement
     * @param measurement The specific measurement to be described
     * @param tenantId The id of the tenant
     * @return
     */
    @GetMapping("/intelligence-format-query/measurement-fields")
    @Timed(value = "query.service", extraTags = {"query.type","query.intelligence.measurements-fields"})
    public List<?> intelligenceFormattedQueryGetMeasurementDescription(
            final @RequestParam("measurement") String measurement,
            final @RequestHeader(HEADER_TENANT) String tenantId) { // Use repose tenantId
        log.debug("Called url:[{}] with tenantId: [{}], measurement: [{}]",
                "/intelligence-format-query/measurement-fields", tenantId, measurement);
        return convertQueryResultToList(queryService.getMeasurementFields(tenantId, measurement));
    }

    /**
     * Return datapoints for a particular measurement within a time range
     * @param measurement the measurement to query
     * @param begin beginning of time interval ISO 8601
     * @param end end of time interval ISO 8601
     * @param tenantId The tenantID for the measurement (from repose)
     * @return
     */
    @GetMapping("/intelligence-format-query/measurement-series-by-time")
    @Timed(value = "query.service", extraTags = {"query.type","query.intelligence.measurement-series-by-time"})
    public List<?> intelligenceFormattedQueryGetMeasurementSeriesByTime(
            final @RequestParam("measurement") String measurement,
            // ISO 8601
            final @RequestParam("begin") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime begin,
            final @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime end,
            final @RequestHeader(HEADER_TENANT) String tenantId) { // Use repose tenantId

        log.debug("Called url:[{}] with tenantId: [{}], measurement: [{}]" +
                ", begin: [{}], end: [{}]",
                "/intelligence-format-query", tenantId, measurement, begin, end);
        return convertQueryResultToList(
                queryService.getMeasurementSeriesForTimeInterval(tenantId, measurement, begin, end)
        );
    }

    private List<?> convertQueryResultToList(QueryResult queryResult) {
        if(queryResult.hasError()){
            String error = queryResult.getError();
            throw new ErroredQueryResultException("Query error: [" + error + "]");
        }

        List<QueryResult.Result> results = queryResult.getResults();

        List<QueryDomainOutput> outputs = new ArrayList<>();

        for(QueryResult.Result result : results){
            for(QueryResult.Series series : result.getSeries()){
                QueryDomainOutput output = new QueryDomainOutput();
                output.setName(series.getName());
                output.setColumns(series.getColumns());
                output.setTags(series.getTags());
                output.setValuesCollection(series.getValues());
                outputs.add(output);
            }
        }

        List<Map<String, Object>> response = new ArrayList<>();
        outputs.forEach(out -> response.addAll(out.getQueryResponse()));

        return response;
    }
}
