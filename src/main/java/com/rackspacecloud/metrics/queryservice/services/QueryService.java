package com.rackspacecloud.metrics.queryservice.services;

import com.rackspacecloud.metrics.queryservice.domains.QueryDomainInput;
import com.rackspacecloud.metrics.queryservice.domains.QueryDomainOutput;
import com.rackspacecloud.metrics.queryservice.models.TenantRoute;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class QueryService implements IQueryService {
    private Map<String, InfluxDB> urlInfluxDBInstanceMap;

    @Value("${tenant-routing-service.url}")
    String tenantRoutingServiceUrl;

    private RestTemplate restTemplate;

    @Autowired
    public QueryService(Map<String, InfluxDB> urlInfluxDBInstanceMap, RestTemplate restTemplate){
        this.urlInfluxDBInstanceMap = urlInfluxDBInstanceMap;
        for(String key : this.urlInfluxDBInstanceMap.keySet()){
            this.urlInfluxDBInstanceMap.get(key).setLogLevel(InfluxDB.LogLevel.BASIC);
        }

        this.restTemplate = restTemplate;
    }

    @Override
    public List<QueryDomainOutput> find(final String tenantId, final QueryDomainInput input) throws Exception {
        if(tenantId == null || tenantId.trim() == "")
            throw new IllegalArgumentException("tenantId can't be null, empty or all whitespaces");

        // Get InfluxDB Url and database name from TenantRoutingService
        TenantRoute route = getRouteForGivenTenant(tenantId);

        String queryString = input.getQueryString().trim();

        if(!isValidQueryString(queryString))
            throw new IllegalArgumentException(String.format("Invalid query string [%s]", queryString));

        Query query = new Query(queryString, route.getDatabaseName());

        QueryResult queryResult = urlInfluxDBInstanceMap.get(route.getPath()).query(query);

        if(queryResult.hasError()){
            String error = queryResult.getError();
            throw new Exception("Query error."); // TODO: Create some exception or return something to user.
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

    private TenantRoute getRouteForGivenTenant(String tenantId) {
        //TODO: remove this stub and find a better solution
        if(tenantRoutingServiceUrl.equalsIgnoreCase("dummy")){
            TenantRoute temp = new TenantRoute();
            temp.setPath("http://localhost:8086");
            temp.setDatabaseName("db_" + tenantId);

            return temp;
        }

        String requestUrl = String.format("%s/%s", tenantRoutingServiceUrl, tenantId);
        return restTemplate.getForObject(requestUrl, TenantRoute.class);
    }

    private String getRouteForGivenDatabase(String dbName) {
        //TODO: call tenantRoutingService
        return "http://data-influxdb:8086";
//        return "http://localhost:8086";
    }

    public QueryResult query(String dbName, String queryString){
        Query query = new Query(queryString, dbName);

        // Get InfluxDB Url from TenantRoutingService
        String influxdbUrl = getRouteForGivenDatabase(dbName);

        QueryResult queryResult = urlInfluxDBInstanceMap.get(influxdbUrl).query(query);
        return queryResult;
    }
}
