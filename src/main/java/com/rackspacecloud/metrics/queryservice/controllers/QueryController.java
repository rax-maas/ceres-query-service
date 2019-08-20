package com.rackspacecloud.metrics.queryservice.controllers;

import com.rackspacecloud.metrics.queryservice.domains.QueryDomainOutput;
import com.rackspacecloud.metrics.queryservice.exceptions.ErroredQueryResultException;
import com.rackspacecloud.metrics.queryservice.services.QueryService;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.dto.QueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("")
@Slf4j
public class QueryController {

    @Autowired
    QueryService queryService;

    final String showFields = "SHOW FIELD KEYS FROM \"";
    final String queryPart1 = "SELECT average, ";
    final String queryPart2 = "FROM \"MAAS_ping\" WHERE time >= now() - 1h LIMIT 1";

    @GetMapping("/query")
    @Secured({"ROLE_COMPUTE_DEFAULT"})
    @Timed(value = "query.service", extraTags = {"query.type","query.grafana"})
    public QueryResult query(
            final @RequestParam("db") String dbName,
            final @RequestParam("q") String queryString) {
        return queryService.query(dbName, queryString);
    }

    @GetMapping("/intelligence-format-query")
    @Secured({"ROLE_COMPUTE_DEFAULT"})
    @Timed(value = "query.service", extraTags = {"query.type","query.intelligence"})
    public List<?> intelligenceFormattedQuery(
            final @RequestParam("db") String dbName,
            final @RequestParam("q") String queryString) {
        log.debug("Called url:[{}] with tenantId: [{}], query string: [{}]",
                "/intelligence-format-query", dbName, queryString);
        QueryResult queryResult = query(dbName, queryString);

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

    @GetMapping("/field-units")
    @Secured({"ROLE_COMPUTE_DEFAULT"})
    public List<?> getFieldUnits(
        final @RequestParam("measurement") String measurement,
        final @RequestParam("db") String dbName,
        final @RequestParam("device") String device
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append(showFields);
        builder.append(dbName);
        builder.append("\"");

        QueryResult queryResult = this.query(dbName, builder.toString());
        StringBuilder secondQuery = new StringBuilder();
        secondQuery.append(queryPart1);
        queryResult.getResults().forEach((result) -> {
            builder.append(result.toString());
            builder.append("_unit");
        });
        secondQuery.append(queryPart2);


        /*
         * SELECT mean("${metric}") FROM "${table}"
    WHERE time >= ${endTime} - ${startTime} AND device = "${device}"
    GROUP BY time(1m) fill(null)`;
         */
        this.query(dbName, secondQuery.toString());

        return null;
    }
}
