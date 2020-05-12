package com.rackspacecloud.metrics.queryservice.services;

import com.rackspacecloud.metrics.queryservice.exceptions.ErroredQueryResultException;
import com.rackspacecloud.metrics.queryservice.exceptions.InvalidQueryException;
import com.rackspacecloud.metrics.queryservice.exceptions.RouteNotFoundException;
import com.rackspacecloud.metrics.queryservice.providers.RouteProvider;
import com.rackspacecloud.metrics.queryservice.providers.TenantRoutes;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.InfluxDB;
import org.influxdb.dto.BoundParameterQuery;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class QueryServiceImpl implements QueryService {
    private static final String CREATE_GRAFANA_DATA_SOURCE_PREFIX = "SHOW RETENTION POLICIES on ";
    private static final String GRAFANA_GRAPH_EDIT_ENTRY_WINDOW_QUERY_PREFIX =
            "SELECT mean(\"value\") FROM \"measurement\" WHERE ";
    private static final String GRAFANA_MEASUREMENT_DROP_DOWN_QUERY_PREFIX = "SHOW MEASUREMENTS ";
    private static final String PREFIX_FOR_SHOW_FIELDS_FOR_MEASUREMENT = "SHOW FIELD KEYS FROM ";
    private static final String PREFIX_FOR_SHOW_TAGS_FOR_MEASUREMENT = "SHOW TAG KEYS FROM ";
    private static final String PREFIX_FOR_SHOW_TAG_VALUES_FOR_GIVEN_KEY = "SHOW TAG VALUES FROM ";
    private final InfluxDBPool influxDBPool;

    private ConcurrentMap<String, InfluxDB> urlInfluxDBInstanceMap;
    private RouteProvider routeProvider;

    private RestTemplate restTemplate;

    @Autowired
    public QueryServiceImpl(
            InfluxDBPool influxDBPool,
            RestTemplate restTemplate,
            RouteProvider routeProvider) {
        this.influxDBPool = influxDBPool;
        this.restTemplate = restTemplate;
        this.routeProvider = routeProvider;
    }

    /**
     * Get tenant routes for given tenantId and measurement from routing service
     * @param tenantId
     * @param measurement
     * @return
     */
    private TenantRoutes.TenantRoute getTenantRoutes(String tenantId, String measurement) {
        TenantRoutes tenantRoutes;

        try {
            tenantRoutes = routeProvider.getRoute(tenantId, measurement, restTemplate);
            return tenantRoutes.getRoutes().get("full");
        }
        catch(Exception e) {
            String errMsg = String.format(
                    "Failed to get routes for tenantId [%s] and measurement [%s]", tenantId, measurement);
            log.error(errMsg, e);
            throw new RouteNotFoundException(errMsg, e);
        }
    }

    public Collection<String> getMeasurementsForGivenTenantId(String tenantId) {
        return routeProvider.getMeasurements(tenantId, restTemplate);
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

    @Override
    public QueryResult getMeasurementsForTenant(String tenantId) {
        return getQueryResultForMeasurementDropDown(tenantId);
    }

    @Override
    public QueryResult getMeasurementTags(String tenantId, String measurement) {
        TenantRoutes.TenantRoute route = getTenantRoutes(tenantId, measurement);

        Query query = BoundParameterQuery.QueryBuilder.newQuery(
            String.format("%s %s", PREFIX_FOR_SHOW_TAGS_FOR_MEASUREMENT, measurement))
            .forDatabase(route.getDatabaseName())
            .create();

        InfluxDB influxDB = getInfluxDB(route);
        return influxDB.query(query);
    }

    @Override
    public QueryResult getMeasurementFields(String tenantId, String measurement) {
        TenantRoutes.TenantRoute route = getTenantRoutes(tenantId, measurement);

        Query query = BoundParameterQuery.QueryBuilder.newQuery(
            String.format("%s %s", PREFIX_FOR_SHOW_FIELDS_FOR_MEASUREMENT, measurement))
            .forDatabase(route.getDatabaseName())
            .create();

        InfluxDB influxDB = getInfluxDB(route);
        return influxDB.query(query);
    }

    @Override
    public QueryResult getMeasurementSeriesForTimeInterval(String tenantId, String measurement, Instant begin, Instant end) {
        TenantRoutes.TenantRoute route = getTenantRoutes(tenantId, measurement);

        Query query = BoundParameterQuery.QueryBuilder.newQuery(
            String.format("SELECT * from %s where time >= $begin and time <= $end", measurement))
            .forDatabase(route.getDatabaseName())
            .bind("begin", DateTimeFormatter.ISO_INSTANT.format(begin))
            .bind("end", DateTimeFormatter.ISO_INSTANT.format(end))
            .create();

        InfluxDB influxDB = getInfluxDB(route);
        return influxDB.query(query);
    }

    @Override
    public QueryResult query(final String tenantId, final String queryString) {
        // This query is used to create the data source in Grafana
        // query string that comes from Grafana should be like "SHOW RETENTION POLICIES on \"blah\""
        if(queryString.startsWith(CREATE_GRAFANA_DATA_SOURCE_PREFIX) ||
                queryString.startsWith(GRAFANA_GRAPH_EDIT_ENTRY_WINDOW_QUERY_PREFIX)) {
            return new QueryResult();
        }
        // Get data for Measurement dropdown
        else if(queryString.startsWith(GRAFANA_MEASUREMENT_DROP_DOWN_QUERY_PREFIX)) {
            QueryResult queryResult = getQueryResultForMeasurementDropDown(tenantId);

            return queryResult;
        }
        // Get data for "field" dropdown
        else if(queryString.startsWith(PREFIX_FOR_SHOW_FIELDS_FOR_MEASUREMENT)) {
            return getQueryResultForShowFieldsOrTagsKeysOrTagsValues(
                    tenantId, queryString, PREFIX_FOR_SHOW_FIELDS_FOR_MEASUREMENT);
        }
        // Get data for "tags" dropdown in the WHERE clause
        else if(queryString.startsWith(PREFIX_FOR_SHOW_TAGS_FOR_MEASUREMENT)) {
            return getQueryResultForShowFieldsOrTagsKeysOrTagsValues(
                    tenantId, queryString, PREFIX_FOR_SHOW_TAGS_FOR_MEASUREMENT);
        }
        // Get data for tag's values in the WHERE clause
        else if(queryString.startsWith(PREFIX_FOR_SHOW_TAG_VALUES_FOR_GIVEN_KEY)) {
            return getQueryResultForShowFieldsOrTagsKeysOrTagsValues(
                    tenantId, queryString, PREFIX_FOR_SHOW_TAG_VALUES_FOR_GIVEN_KEY);
        }
        else {
            return getDataPointsForGivenQuery(tenantId, queryString);
        }
    }

    private QueryResult getDataPointsForGivenQuery(String tenantId, String queryString) {
        /**
         * Query string could contain ";" to pass on multiple queries in one.
         * Take only first command and reject rest of the query string (that comes after first ";")
         */

        String trimmedString = queryString;
        if(queryString.contains(";")) {
            trimmedString = queryString.substring(0, trimmedString.indexOf(";"));
        }

        if(!isValidQueryString(trimmedString)) {
            throw new InvalidQueryException(
                    String.format("Invalid query string [%s] for tenantId", queryString, tenantId)
            );
        }

        String regexString = "(?i)from"; // regex string for case-insensitive "from"
        String[] strArr = trimmedString.split(regexString);
        if(strArr.length < 2) {
            log.error("Query String: [{}]. Split regex is: [{}]. " +
                            "Expected split array length to be at least 2, but found [{}]",
                    queryString, regexString, strArr.length);
            return null;
        }

        String measurement = getCleanedMeasurementName(queryString, strArr[1].trim().split(" ")[0]);
        if (measurement == null) return null;

        TenantRoutes.TenantRoute route = getTenantRoutes(tenantId, measurement);

        Query query = new Query(queryString, route.getDatabaseName());

        InfluxDB influxDB = getInfluxDB(route);
        QueryResult queryResult = influxDB.query(query);

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

    private String getCleanedMeasurementName(String queryString, String measurement) {
        // Measurement name coming from Grafana is wrapped up in double quotes. So, we need to remove those
        // double quote characters to get the actual measurement name string
        // For example: Measurement name foo_bar from Grafana will show up as "foo_bar"
        if(measurement.startsWith("\"") && measurement.endsWith("\"")) {
            measurement = measurement.substring(1, measurement.length() - 1);
        }

        if(StringUtils.isEmpty(measurement)) {
            log.error("Measurement name is empty in the query string [{}]", queryString);
            return null;
        }
        return measurement;
    }

    private InfluxDB getInfluxDB(TenantRoutes.TenantRoute route) {
        return influxDBPool.getInstance(route.getPath());
    }

    private QueryResult getQueryResultForShowFieldsOrTagsKeysOrTagsValues(
            String tenantId, String queryString, String prefix) {
        String[] strArray = queryString.split(prefix);

        if(strArray.length < 2) {
            log.error("Query String: [{}]. Split regex is: [{}]. " +
                            "Expected split array length to be at least 2, but found [{}]",
                    queryString, prefix, strArray.length);
            return null;
        }

        // Measurement name will be the first string in the 1st indexed String.
        String measurementName = getCleanedMeasurementName(queryString, strArray[1].trim().split(" ")[0]);
        if (measurementName == null) return null;

        TenantRoutes.TenantRoute route = getTenantRoutes(tenantId, measurementName);

        log.debug("Route information - db:[{}],rp:[{}],path:[{}]", route.getDatabaseName(),
                route.getRetentionPolicy(), route.getPath());

        Query query = new Query(queryString, route.getDatabaseName());

        InfluxDB influxDB = getInfluxDB(route);
        QueryResult queryResult = influxDB.query(query);

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

    private QueryResult getQueryResultForMeasurementDropDown(final String tenantId) {
        Collection<String> measurements = getMeasurementsForGivenTenantId(tenantId);
        QueryResult.Series series = new QueryResult.Series();
        series.setName("measurements");
        series.setColumns(Arrays.asList("name"));

        List<List<Object>> measurementsResult = new ArrayList<>();

        measurements.forEach(item -> {
            measurementsResult.add(Arrays.asList(item));
        });
        series.setValues(measurementsResult);

        QueryResult.Result result = new QueryResult.Result();
        result.setSeries(Arrays.asList(series));

        QueryResult queryResult = new QueryResult();
        queryResult.setResults(Arrays.asList(result));
        return queryResult;
    }

    private List<QueryResult.Result> getProcessedResults(List<QueryResult.Result> results) {
        List<QueryResult.Result> newResults = new ArrayList<>();

        results.forEach(result -> {
            QueryResult.Result res = new QueryResult.Result();

            List<QueryResult.Series> seriesList = new ArrayList<>();

            if(result.getSeries() != null) {
                result.getSeries().forEach(series -> {
                    QueryResult.Series s = new QueryResult.Series();
                    s.setName(series.getName());

                    Map<String, String> tags = series.getTags();
                    s.setTags(tags);

                    List<String> columns = series.getColumns();
                    s.setColumns(columns);

                    List<List<Object>> valuesCollection = series.getValues();
                    List<List<Object>> newValuesCollection = new ArrayList<>();

                    for (List<Object> valueCollection : valuesCollection) {
                        if (valueCollection.size() == 0) continue; // skip this record
                        Object[] objects = new Object[valueCollection.size()];

                        for (int i = 0; i < valueCollection.size(); i++) {
                            objects[i] = columns.get(i).equalsIgnoreCase("time")
                                    ? Long.valueOf(Instant.parse(
                                            valueCollection.get(i).toString()).getEpochSecond()) * 1000
                                    : valueCollection.get(i);
                        }

                        newValuesCollection.add(Arrays.asList(objects));
                    }

                    s.setValues(newValuesCollection);
                    seriesList.add(s);
                });
            }

            res.setSeries(seriesList);

            newResults.add(res);
        });
        return newResults;
    }
}
