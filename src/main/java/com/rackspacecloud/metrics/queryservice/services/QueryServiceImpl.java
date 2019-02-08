package com.rackspacecloud.metrics.queryservice.services;

import com.rackspacecloud.metrics.queryservice.domains.QueryDomainInput;
import com.rackspacecloud.metrics.queryservice.domains.QueryDomainOutput;
import com.rackspacecloud.metrics.queryservice.exceptions.ErroredQueryResultException;
import com.rackspacecloud.metrics.queryservice.exceptions.InvalidQueryException;
import com.rackspacecloud.metrics.queryservice.exceptions.RouteNotFoundException;
import com.rackspacecloud.metrics.queryservice.models.MeasurementQueryRequest;
import com.rackspacecloud.metrics.queryservice.providers.RouteProvider;
import com.rackspacecloud.metrics.queryservice.providers.TenantRoutes;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class QueryServiceImpl implements QueryService {

    private static final String SHOW_MEASUREMENTS = "SHOW MEASUREMENTS";
    private static final String SHOW_TAGS_FOR_MEASUREMENT = "SHOW TAG KEYS FROM \"%s\"";
    private static final String SHOW_TAG_VALUES_FOR_MEASUREMENT = "SHOW TAG VALUES FROM \"%s\" WITH KEY IN (%s)";
    private static final String SHOW_FIELDS_FOR_MEASUREMENT = "SHOW FIELD KEYS FROM \"%s\"";

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryServiceImpl.class);
    private Map<String, InfluxDB> urlInfluxDBInstanceMap;
    private RouteProvider routeProvider;

    private RestTemplate restTemplate;

    @Autowired
    public QueryServiceImpl(
            Map<String, InfluxDB> urlInfluxDBInstanceMap,
            RestTemplate restTemplate,
            RouteProvider routeProvider) {

        this.urlInfluxDBInstanceMap = urlInfluxDBInstanceMap;
        for(String key : this.urlInfluxDBInstanceMap.keySet()){
            this.urlInfluxDBInstanceMap.get(key).setLogLevel(InfluxDB.LogLevel.BASIC);
        }

        this.restTemplate = restTemplate;
        this.routeProvider = routeProvider;
    }

    @Override
    public List<QueryDomainOutput> find(@NotBlank final String tenantId, @Valid final QueryDomainInput input) {
        TenantRoutes.TenantRoute route = getTenantRoutes(tenantId);

        String trimmedString = input.getQueryString().trim();

        /**
         * Query string could contain ";" to pass on multiple queries in one.
         * Take only first command and reject rest of the query string (that comes after first ";")
         */
        String queryString = trimmedString;
        if(trimmedString.contains(";")) {
            queryString = trimmedString.substring(0, trimmedString.indexOf(";"));
        }

        if(!isValidQueryString(queryString))
            throw new InvalidQueryException(String.format("Invalid query string [%s]", queryString));

        return getQueryResult(queryString, route);
    }

    @Override
    public List<QueryDomainOutput> measurements(@NotBlank final String tenantId) {
        TenantRoutes.TenantRoute route = getTenantRoutes(tenantId);
        return getQueryResult(SHOW_MEASUREMENTS, route);
    }

    @Override
    public List<QueryDomainOutput> tags(@NotBlank String tenantId, @NotBlank String measurement) {
        TenantRoutes.TenantRoute route = getTenantRoutes(tenantId);
        return getQueryResult(String.format(SHOW_TAGS_FOR_MEASUREMENT, measurement), route);
    }

    @Override
    public List<QueryDomainOutput> tagValues(
            @NotBlank String tenantId,
            @NotBlank String measurement,
            @NotBlank String tagsAsCsv) {

        TenantRoutes.TenantRoute route = getTenantRoutes(tenantId);
        String[] tags = tagsAsCsv.split(",");
        for(int i = 0; i < tags.length; i++){
            tags[i] = String.format("\"%s\"", tags[i]);
        }
        return getQueryResult(String.format(
                SHOW_TAG_VALUES_FOR_MEASUREMENT, measurement, String.join(",", tags)), route);
    }

    @Override
    public List<QueryDomainOutput> fields(@NotBlank String tenantId, @NotBlank String measurement) {
        TenantRoutes.TenantRoute route = getTenantRoutes(tenantId);
        return getQueryResult(String.format(SHOW_FIELDS_FOR_MEASUREMENT, measurement), route);
    }

    @Override
    public List<QueryDomainOutput> queryMeasurement(
            @NotBlank String tenantId,
            @NotBlank String measurement,
            MeasurementQueryRequest queryRequest) {

        TenantRoutes.TenantRoute route = getTenantRoutes(tenantId);

        String queryString = createQueryString(measurement, queryRequest);

        return getQueryResult(queryString, route);
    }

    private String createQueryString(String measurement, MeasurementQueryRequest queryRequest) {
        MeasurementQueryRequest.Filter filter = queryRequest.getFilter();
        MeasurementQueryRequest.Select select = queryRequest.getSelect();

        String whereClause = getWhereClauseString(filter);

        String selectString = String.join(",", select.getColumns());

        String queryString = String.format("SELECT %s FROM \"%s\" WHERE %s", selectString, measurement, whereClause);

        return queryString;
    }

    private String getWhereClauseString(MeasurementQueryRequest.Filter filter) {
        long from = filter.getFrom() * 1000_000_000L; // convert epoch seconds to InfluxDB precision level
        long to = filter.getTo() * 1000_000_000L; // convert epoch seconds to InfluxDB precision level

        String timeFilter = String.format("(time > %d AND time < %d)", from, to);

        String finalTagsFilterExpression = getTagsFilterString(filter);

        String finalFieldsFilterExpression = getFieldsFilterString(filter);

        return String.join(" AND ",
                timeFilter, finalTagsFilterExpression, finalFieldsFilterExpression);
    }

    private String getFieldsFilterString(MeasurementQueryRequest.Filter filter) {
        Map<String, List<String>> fieldsAndFilterValues = filter.getFields();

        List<String> fieldsFilterCollection = new ArrayList<>();

        for(String field : fieldsAndFilterValues.keySet()) {
            List<String> fieldsFilterExpressions = new ArrayList<>();

            fieldsAndFilterValues.get(field).forEach(
                    value -> fieldsFilterExpressions.add(String.format("\"%s\" %s", field, value))
            );

            fieldsFilterCollection.add(String.format("(%s)", String.join(" AND ", fieldsFilterExpressions)));
        }

        return String.join(" AND ", fieldsFilterCollection);
    }

    private String getTagsFilterString(MeasurementQueryRequest.Filter filter) {
        Map<String, List<String>> tagsAndFilterValues = filter.getTags();

        List<String> tagsFilterCollection = new ArrayList<>();

        for(String tag : tagsAndFilterValues.keySet()){
            List<String> tagFilterExpressions = new ArrayList<>();
            List<String> values = tagsAndFilterValues.get(tag);
            values.forEach((value) -> tagFilterExpressions.add(String.format("\"%s\" = \'%s\'", tag, value)));

            String tagFilterExpressionString =
                    String.format("(%s)", String.join(" OR ", tagFilterExpressions));

            tagsFilterCollection.add(tagFilterExpressionString);
        }

        return String.format("(%s)", String.join(" AND ", tagsFilterCollection));
    }


    private List<QueryDomainOutput> getQueryResult(String command, TenantRoutes.TenantRoute route) {
        Query query = new Query(command, route.getDatabaseName());

        QueryResult queryResult = urlInfluxDBInstanceMap.get(route.getPath()).query(query);

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
                output.setValuesCollection(series.getValues());
                outputs.add(output);
            }
        }

        return outputs;
    }

    private TenantRoutes.TenantRoute getTenantRoutes(String tenantId) {

        try {
            TenantRoutes tenantRoutes = routeProvider.getRoute(tenantId, restTemplate);
            return tenantRoutes.getRoutes().get("full");
        }
        catch(Exception e) {
            String errMsg = String.format("Failed to get routes for tenantId [%s]", tenantId);
            LOGGER.error(errMsg, e);
            throw new RouteNotFoundException(errMsg, e);
        }
    }

    private boolean isValidQueryString(String queryString){
        String stringToMatch = queryString.trim();

        // Regex to match "select" clause (case-insensitive), where string starts with "select" word
        String selectRegex = "(?i:^select .*)";
        String fromRegex = "(?i:.* from .*)"; // Regex to match "from" clause (case-insensitive)
        String whereRegex = "(?i:.* where .*)"; // Regex to match "where" clause (case-insensitive)

        return (Pattern.matches(selectRegex, stringToMatch) &&
                Pattern.matches(fromRegex, stringToMatch) &&
                Pattern.matches(whereRegex, stringToMatch));
    }

    private String getRouteForGivenDatabase(String dbName) {
        //TODO: call tenantRoutingService
        return "http://data-influxdb:8086";
//        return "http://localhost:8086";
    }

    public QueryResult query(String dbName, String queryString){
        Query query = new Query(queryString, dbName);

        // Get InfluxDB Url from TenantRoutingService
        String influxDbUrl = getRouteForGivenDatabase(dbName);

        QueryResult queryResult = urlInfluxDBInstanceMap.get(influxDbUrl).query(query);

        if(queryResult.hasError()){
            String error = queryResult.getError();
            throw new ErroredQueryResultException("Query error: [" + error + "]");
        }

        QueryResult qr = new QueryResult();

        List<QueryResult.Result> results = queryResult.getResults();

        List<QueryResult.Result> newResults = getProcessedResults(results);

        qr.setResults(newResults);

        return qr;
    }

    private List<QueryResult.Result> getProcessedResults(List<QueryResult.Result> results) {
        List<QueryResult.Result> newResults = new ArrayList<>();

        results.forEach(result -> {
            QueryResult.Result res = new QueryResult.Result();

            List<QueryResult.Series> seriesList = new ArrayList<>();
            result.getSeries().forEach(series -> {
                QueryResult.Series s = new QueryResult.Series();
                s.setName(series.getName());

                List<String> columns = series.getColumns();
                s.setColumns(columns);

                List<List<Object>> valuesCollection = series.getValues();
                List<List<Object>> newValuesCollection = new ArrayList<>();

                for(List<Object> valueCollection : valuesCollection){
                    if(valueCollection.size() == 0) continue; // skip this record
                    Object[] objects = new Object[valueCollection.size()];

                    for(int i = 0; i < valueCollection.size(); i++){
                        objects[i] = columns.get(i).equalsIgnoreCase("time")
                                ? Long.valueOf(Instant.parse(valueCollection.get(i).toString()).getEpochSecond()) * 1000
                                : valueCollection.get(i);
                    }

                    newValuesCollection.add(Arrays.asList(objects));
                }

                s.setValues(newValuesCollection);
                seriesList.add(s);
            });

            res.setSeries(seriesList);

            newResults.add(res);
        });
        return newResults;
    }
}
