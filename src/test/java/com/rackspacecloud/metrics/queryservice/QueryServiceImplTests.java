package com.rackspacecloud.metrics.queryservice;

import com.rackspacecloud.metrics.queryservice.domains.QueryDomainInput;
import com.rackspacecloud.metrics.queryservice.domains.QueryDomainOutput;
import com.rackspacecloud.metrics.queryservice.exceptions.ErroredQueryResultException;
import com.rackspacecloud.metrics.queryservice.exceptions.InvalidQueryException;
import com.rackspacecloud.metrics.queryservice.exceptions.RouteNotFoundException;
import com.rackspacecloud.metrics.queryservice.models.MeasurementQueryRequest;
import com.rackspacecloud.metrics.queryservice.providers.DevTestTenantRouteProvider;
import com.rackspacecloud.metrics.queryservice.providers.RouteProvider;
import com.rackspacecloud.metrics.queryservice.providers.TenantRoutes;
import com.rackspacecloud.metrics.queryservice.services.QueryServiceImpl;
import org.influxdb.InfluxDB;
import org.influxdb.dto.QueryResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class QueryServiceImplTests {
    @Mock
    private RouteProvider routeProvider;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    Map<String, InfluxDB> urlInfluxDBInstanceMap;

    @InjectMocks
    private QueryServiceImpl queryService;

    @Before
    public void setup(){
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void find_validTenantIdAndQueryDomainInput_returnsListOfQueryDomainOutput() {
        mockedComponents();

        QueryDomainInput in = new QueryDomainInput();
        in.setQueryString("select * from agent_cpu where time > now() - 5d;drop");
        in.setFrom(1548301512L);
        in.setTo(1548301623L);

        List<QueryDomainOutput> output = queryService.find("1234", in);
        assertOutput(output);
    }

    @Test(expected = RouteNotFoundException.class)
    public void find_noRouteForTenantId_throwsRouteNotFoundException() {
        when(routeProvider.getRoute(anyString(), any())).thenThrow(RuntimeException.class);
        List<QueryDomainOutput> output = queryService.find("", new QueryDomainInput());
    }

    @Test(expected = InvalidQueryException.class)
    public void find_invalidQueryString_throwsInvalidQueryException() {
        mockedComponents();

        QueryDomainInput invalidQueryWithNoSelectClause = new QueryDomainInput();
        invalidQueryWithNoSelectClause.setQueryString("from agent_cpu where time > now() - 5d;drop");
        invalidQueryWithNoSelectClause.setFrom(1548301512L);
        invalidQueryWithNoSelectClause.setTo(1548301623L);

        List<QueryDomainOutput> output = queryService.find("1234", invalidQueryWithNoSelectClause);
    }

    @Test
    public void measurements_validTenantId_returnsListOfQueryDomainOutput() {
        mockedComponents();

        // Though this result output doesn't look like what "measurement" result should look like,
        // it's not important it to match like "measurement" one. This is anyway mocked data.
        List<QueryDomainOutput> output = queryService.measurements("1234");
        assertOutput(output);
    }

    @Test(expected = ErroredQueryResultException.class)
    public void measurements_erroredQueryResult_throwsErroredQueryResultException() {
        TenantRoutes routes = getTenantRoutes();
        when(routeProvider.getRoute(anyString(), any())).thenReturn(routes);

        InfluxDB influxDB = mock(InfluxDB.class);
        when(urlInfluxDBInstanceMap.get(any())).thenReturn(influxDB);

        QueryResult queryResult = new QueryResult();
        queryResult.setError("Test measurements_erroredQueryResult_throwsErroredQueryResultException");

        when(influxDB.query(any())).thenReturn(queryResult);

        List<QueryDomainOutput> output = queryService.measurements("1234");
    }

    @Test
    public void tags_validTenantIdAndMeasurement_returnsListOfQueryDomainOutput() {
        mockedComponents();

        // Though this result output doesn't look like what "tags" result should look like,
        // it's not important it to match like "tags" one. This is anyway mocked data.
        List<QueryDomainOutput> output = queryService.tags("1234", "dummy_measurement");
        assertOutput(output);
    }

    @Test
    public void tagsValues_validTenantIdMeasurementAndTagsAsCsv_returnsListOfQueryDomainOutput() {
        mockedComponents();

        // Though this result output doesn't look like what "tagValues" result should look like,
        // it's not important it to match like "tagValues" one. This is anyway mocked data.
        List<QueryDomainOutput> output = queryService.tagValues(
                "1234", "dummy_measurement", "tag1,tag2,tag3");
        assertOutput(output);
    }

    @Test
    public void fields_validTenantIdAndMeasurement_returnsListOfQueryDomainOutput() {
        mockedComponents();

        // Though this result output doesn't look like what "fields" result should look like,
        // it's not important it to match like "fields" one. This is anyway mocked data.
        List<QueryDomainOutput> output = queryService.fields("1234", "dummy_measurement");
        assertOutput(output);
    }

    @Test
    public void queryMeasurement_validTenantIdMeasurementAndQueryRequest_returnsListOfQueryDomainOutput() {
        mockedComponents();
        MeasurementQueryRequest request = new MeasurementQueryRequest();
        MeasurementQueryRequest.Filter filter = getFilter();
        request.setFilter(filter);

        MeasurementQueryRequest.Select select = new MeasurementQueryRequest.Select();
        select.setColumns(Arrays.asList("col1", "col2"));
        request.setSelect(select);

        // Though this result output doesn't look like what "queryRequest" result should look like,
        // it's not important it to match like "queryRequest" one. This is anyway mocked data.
        List<QueryDomainOutput> output = queryService.queryMeasurement(
                "1234", "dummy_measurement", request);
        assertOutput(output);
    }

    private MeasurementQueryRequest.Filter getFilter() {
        MeasurementQueryRequest.Filter filter = new MeasurementQueryRequest.Filter();
        filter.setFrom(1234567L);
        filter.setTo(9876543L);

        Map<String, List<String>> fields = new HashMap<>();
        fields.put("field1", new ArrayList<>(Arrays.asList("field1_Val1", "field1_Val2")));
        fields.put("field2", new ArrayList<>(Arrays.asList("field2_Val1", "field2_Val2")));
        filter.setFields(fields);

        Map<String, List<String>> tags = new HashMap<>();
        tags.put("tag1", new ArrayList<>(Arrays.asList("tag1_val1", "tag1_val2")));
        tags.put("tag2", new ArrayList<>(Arrays.asList("tag2_val1", "tag2_val2")));
        filter.setTags(tags);
        return filter;
    }

    private void mockedComponents() {
        TenantRoutes routes = getTenantRoutes();
        when(routeProvider.getRoute(anyString(), any())).thenReturn(routes);

        InfluxDB influxDB = mock(InfluxDB.class);
        when(urlInfluxDBInstanceMap.get(any())).thenReturn(influxDB);

        when(influxDB.query(any())).thenReturn(getQueryResult());
    }

    private void assertOutput(List<QueryDomainOutput> output) {
        Assert.assertEquals("dummy_measurement", output.get(0).getName());
        Assert.assertEquals("column1", output.get(0).getColumns().get(0));
        Assert.assertEquals("first_val1", output.get(0).getValuesCollection().get(0).get(0));
    }

    private QueryResult getQueryResult() {
        QueryResult.Series series = getSeries();
        QueryResult.Result result = new QueryResult.Result();
        result.setSeries(new ArrayList<>(Arrays.asList(series)));
        QueryResult queryResult = new QueryResult();
        queryResult.setResults(new ArrayList<>(Arrays.asList(result)));

        return queryResult;
    }

    private QueryResult.Series getSeries() {
        QueryResult.Series series = new QueryResult.Series();
        series.setName("dummy_measurement");
        series.setColumns(new ArrayList<>(Arrays.asList("column1", "column2", "column3")));
        Map<String, String> tags = new HashMap<>();
        tags.put("tag1", "tag1_value");
        tags.put("tag2", "tag2_value");
        series.setTags(tags);
        series.setValues(new ArrayList<>(Arrays.asList(
                new ArrayList<>(Arrays.asList("first_val1", "first_val2", "first_val3")),
                new ArrayList<>(Arrays.asList("second_val1", "second_val2", "second_val3")),
                new ArrayList<>(Arrays.asList("third_val1", "third_val2", "third_val3"))
        )));
        return series;
    }

    private TenantRoutes getTenantRoutes() {
        return DevTestTenantRouteProvider.getTenantRoutes("1234");
    }
}
