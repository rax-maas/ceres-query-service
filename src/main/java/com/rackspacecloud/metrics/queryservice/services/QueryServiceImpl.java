package com.rackspacecloud.metrics.queryservice.services;

import com.rackspacecloud.metrics.queryservice.exceptions.ErroredQueryResultException;
import com.rackspacecloud.metrics.queryservice.exceptions.InvalidQueryException;
import com.rackspacecloud.metrics.queryservice.exceptions.RouteNotFoundException;
import com.rackspacecloud.metrics.queryservice.providers.RouteProvider;
import com.rackspacecloud.metrics.queryservice.providers.TenantRoutes;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class QueryServiceImpl implements QueryService {
    private static final String CREATE_GRAFANA_DATA_SOURCE_PREFIX = "SHOW RETENTION POLICIES on ";
    private static final String GRAFANA_GRAPH_EDIT_ENTRY_WINDOW_QUERY_PREFIX =
            "SELECT mean(\"value\") FROM \"measurement\" WHERE ";
    private static final String GRAFANA_MEASUREMENT_DROP_DOWN_QUERY_PREFIX = "SHOW MEASUREMENTS ";
    private static final String PREFIX_FOR_SHOW_FIELDS_FOR_MEASUREMENT = "SHOW FIELD KEYS FROM ";
    private static final String PREFIX_FOR_SHOW_TAGS_FOR_MEASUREMENT = "SHOW TAG KEYS FROM ";
    private static final String PREFIX_FOR_SHOW_TAG_VALUES_FOR_GIVEN_KEY = "SHOW TAG VALUES FROM ";

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
            LOGGER.error(errMsg, e);
            throw new RouteNotFoundException(errMsg, e);
        }
    }

    private Collection<String> getMeasurementsForGivenTenantId(String tenantId) {
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

        String[] strArr = trimmedString.split("from|FROM");
        if(strArr.length < 2) return null;

        String measurement = strArr[1].trim().split(" ")[0];
        if(StringUtils.isEmpty(measurement)) return null;

        TenantRoutes.TenantRoute route = getTenantRoutes(tenantId, measurement);

        Query query = new Query(queryString, route.getDatabaseName());

        InfluxDB influxDB = urlInfluxDBInstanceMap.get(route.getPath());
        if(influxDB == null) {
            influxDB = InfluxDBFactory.connect(route.getPath());
            urlInfluxDBInstanceMap.put(route.getPath(), influxDB);
        }
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

    private QueryResult getQueryResultForShowFieldsOrTagsKeysOrTagsValues(
            String tenantId, String queryString, String prefix) {
        String[] strArray = queryString.split(prefix);

        if(strArray.length < 2) return null;

        // Measurement name will be the first string in the 1st indexed String.
        String measurementName = strArray[1].trim().split(" ")[0];
        if(StringUtils.isEmpty(measurementName)) return null;

        TenantRoutes.TenantRoute route = getTenantRoutes(tenantId, measurementName);

        Query query = new Query(queryString, route.getDatabaseName());

        InfluxDB influxDB = urlInfluxDBInstanceMap.get(route.getPath());
        if(influxDB == null) {
            influxDB = InfluxDBFactory.connect(route.getPath());
            urlInfluxDBInstanceMap.put(route.getPath(), influxDB);
        }
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
