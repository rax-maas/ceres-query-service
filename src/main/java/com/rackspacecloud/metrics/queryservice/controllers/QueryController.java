package com.rackspacecloud.metrics.queryservice.controllers;

import com.rackspacecloud.metrics.queryservice.domains.QueryDomainOutput;
import com.rackspacecloud.metrics.queryservice.exceptions.ErroredQueryResultException;
import com.rackspacecloud.metrics.queryservice.services.QueryService;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.dto.QueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.websocket.server.PathParam;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("")
@Slf4j
public class QueryController {

    @Autowired
    QueryService queryService;

    @GetMapping("/grafana-query")
    @Timed(value = "query.service", extraTags = {"query.type","query.grafana"})
    public QueryResult query(
            final @RequestParam("db") String dbName, //dbName = tenantId
            final @RequestParam("q") String queryString) {
        return queryService.query(dbName, queryString);
    }

    @GetMapping("/intelligence-format-query")
    @Timed(value = "query.service", extraTags = {"query.type","query.intelligence"})
    public List<?> intelligenceFormattedQuery(
            final @RequestParam("db") String dbName,
            final @RequestParam("q") String queryString) {
        log.debug("Called url:[{}] with tenantId: [{}], query string: [{}]",
                "/intelligence-format-query", dbName, queryString);

        QueryResult queryResult = queryService.query(dbName, queryString);

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
