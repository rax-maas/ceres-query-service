package com.rackspacecloud.metrics.queryservice.domains;

import lombok.Data;

import java.util.Map;

@Data
public class QueryDomainInput {
    private long from;
    private long to;
    private String queryString;
}
