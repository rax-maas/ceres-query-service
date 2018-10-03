package com.rackspacecloud.metrics.queryservice.services;

import com.rackspacecloud.metrics.queryservice.domains.QueryDomainInput;
import com.rackspacecloud.metrics.queryservice.domains.QueryDomainOutput;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class QueryService implements IQueryService {
    private InfluxDB influxDB;

    @Value("${influxdb.url}")
    private String influxdbUrl;

    @Autowired
    public QueryService(InfluxDB influxDB){
        this.influxDB = influxDB;
        this.influxDB.setLogLevel(InfluxDB.LogLevel.BASIC);
    }


    @Override
    public List<QueryDomainOutput> find(final String tenantId, final QueryDomainInput input) throws Exception {
        if(tenantId == null || tenantId.trim() == "")
            throw new IllegalArgumentException("tenantId can't be null, empty or all whitespaces");

        //TODO: get the port# using tenantRoutingService
        //String pathToRepository = "InfluxdbUrlToConnectTo";
        String pathToRepository = influxdbUrl;

        String queryString = input.getQueryString().trim();

        if(!isValidQueryString(queryString))
            throw new IllegalArgumentException(String.format("Invalid query string [%s]", queryString));

        Query query = new Query(queryString, tenantId);

        QueryResult queryResult = influxDB.query(query);

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

    private int getRoute(String tenantId) {
//        // TODO: Work on routing given tenant to specific Influxdb instance
//        return (databaseCountForTest < (MAX_DATABASE_COUNT_FOR_TEST / 2)) ? 81 : 80;

        // TODO: Temporary routing solution.
        return ((tenantId.hashCode())%2 == 0) ? 80 : 81;
    }
}
