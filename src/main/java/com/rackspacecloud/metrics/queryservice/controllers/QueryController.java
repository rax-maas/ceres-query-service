package com.rackspacecloud.metrics.queryservice.controllers;

import com.rackspacecloud.metrics.queryservice.services.QueryService;
import io.micrometer.core.annotation.Timed;
import org.influxdb.dto.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("")
public class QueryController {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueryController.class);

    @Autowired
    QueryService queryService;

    @GetMapping("/query")
    @Secured({"ROLE_COMPUTE_DEFAULT"})
    @Timed(value = "query.service", extraTags = {"query.type","query.grafana"})
    public QueryResult query(
            final @RequestParam("db") String dbName,
            final @RequestParam("q") String queryString) {
        return queryService.query(dbName, queryString);
    }
}
