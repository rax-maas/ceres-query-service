package com.rackspacecloud.metrics.queryservice.services;

import com.rackspacecloud.metrics.queryservice.domains.QueryDomainInput;
import com.rackspacecloud.metrics.queryservice.domains.QueryDomainOutput;
import com.rackspacecloud.metrics.queryservice.models.MeasurementQueryRequest;
import org.influxdb.dto.QueryResult;

import java.util.List;

public interface QueryService {
    List<QueryDomainOutput> find(final String tenantId, final QueryDomainInput input) throws Exception;
    List<QueryDomainOutput> measurements(final String tenantId);
    List<QueryDomainOutput> tags(String tenantId, String measurement);
    List<QueryDomainOutput> tagValues(String tenantId, String measurement, String tag);
    List<QueryDomainOutput> fields(String tenantId, String measurement);

    List<QueryDomainOutput> queryMeasurement(String tenantId, String measurement, MeasurementQueryRequest queryRequest);

    QueryResult query(String dbName, String queryString);
}
