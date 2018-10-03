package com.rackspacecloud.metrics.queryservice.services;

import com.rackspacecloud.metrics.queryservice.domains.QueryDomainInput;
import com.rackspacecloud.metrics.queryservice.domains.QueryDomainOutput;

import java.util.List;

public interface IQueryService {
    List<QueryDomainOutput> find(final String tenantId, final QueryDomainInput input) throws Exception;
}
