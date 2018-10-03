package com.rackspacecloud.metrics.queryservice.domains;

import lombok.Data;

import java.util.List;

@Data
public class QueryDomainOutput {
    private String name;
    private List<String> columns;
    private List<List<Object>> valuesCollection;
}
