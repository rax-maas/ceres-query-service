package com.rackspacecloud.metrics.queryservice.controllers;

import com.rackspacecloud.metrics.queryservice.domains.QueryDomainInput;
import com.rackspacecloud.metrics.queryservice.domains.QueryDomainOutput;
import com.rackspacecloud.metrics.queryservice.models.QueryInput;
import com.rackspacecloud.metrics.queryservice.services.IQueryService;
import org.influxdb.dto.QueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.annotation.Secured;
import java.util.List;

@RestController
@RequestMapping("")
public class QueryController {

    @Autowired
    IQueryService queryService;

    @RequestMapping(
            value = "/{tenantId}",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    // Note Confirm this is the correct role for this method:
    @Secured({"ROLE_COMPUTE_DEFAULT"})
    public List<QueryDomainOutput> find(
            @PathVariable final String tenantId,
            @RequestBody final QueryInput queryInput) throws Exception {
        QueryDomainInput domainInput = inputToDomainInput(queryInput);

        List<QueryDomainOutput> domainOutputs = queryService.find(tenantId, domainInput);

        //return domainOutputToOutput(domainOutputs);
        return domainOutputs;
    }

//    // TODO: Most likely this will change when requirements are clearer.
//    // Also, purpose of this method is to decouple domain and model.
//    private List<String> domainOutputToOutput(List<QueryDomainOutput> domainOutput) {
//        return new ArrayList<>(domainOutput);
//    }

    private QueryDomainInput inputToDomainInput(QueryInput queryInput) {
        QueryDomainInput domainInput = new QueryDomainInput();
        domainInput.setQueryString(queryInput.getQueryString());
        return domainInput;
    }

    @RequestMapping(
            value = "/query",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Secured({"ROLE_COMPUTE_DEFAULT"})
    public QueryResult query(final @RequestParam("db") String dbName, final @RequestParam("q") String queryString){
        return queryService.query(dbName, queryString);
    }
}
