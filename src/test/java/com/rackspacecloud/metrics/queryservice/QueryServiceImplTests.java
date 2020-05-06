package com.rackspacecloud.metrics.queryservice;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rackspacecloud.metrics.queryservice.exceptions.ErroredQueryResultException;
import com.rackspacecloud.metrics.queryservice.exceptions.InvalidQueryException;
import com.rackspacecloud.metrics.queryservice.exceptions.RouteNotFoundException;
import com.rackspacecloud.metrics.queryservice.providers.RouteProvider;
import com.rackspacecloud.metrics.queryservice.providers.TenantRoutes;
import com.rackspacecloud.metrics.queryservice.services.InfluxDBPool;
import com.rackspacecloud.metrics.queryservice.services.QueryServiceImpl;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.influxdb.InfluxDB;
import org.influxdb.dto.QueryResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringRunner.class)
@SpringBootTest
public class QueryServiceImplTests {
    @MockBean
    private RouteProvider routeProvider;

    @MockBean
    private RestTemplate restTemplate;

    @Mock
    private InfluxDB influxDB;

    @MockBean
    private InfluxDBPool influxDBPool;

    @Autowired
    private QueryServiceImpl queryService;

    @Before
    public void setup(){
        when(influxDBPool.getInstance(any()))
            .thenReturn(influxDB);
    }

    @Test
    public void query_createGrafanaDataSourcePrefix_returnsNotNullQueryResult() {
        mockedComponents("1234", "measurement", getQueryResult());

        QueryResult output = queryService.query("1234","SHOW RETENTION POLICIES on measurement");
        Assert.assertNotNull(output);
    }

    @Test
    public void query_grafanaGraphEditEntryWindowQueryPrefix_returnsNotNullQueryResult() {
        mockedComponents("1234", "measurement", getQueryResult());

        QueryResult output = queryService.query("1234",
                "SELECT mean(\"value\") FROM \"measurement\" WHERE time > now() - 5m");
        Assert.assertNotNull(output);
    }

    @Test
    public void query_grafanaMeasurementDropDownQueryPrefix_returnsMeasurements() {
        mockedComponents("1234", "measurement", getQueryResult());
        List<String> expectedMeasurements = Arrays.asList("measurement_1", "measurement_2");
        when(routeProvider.getMeasurements(anyString(), any())).thenReturn(expectedMeasurements);

        QueryResult output = queryService.query("1234","SHOW MEASUREMENTS ");
        List<List<Object>> actualMeasurements = output.getResults().get(0).getSeries().get(0).getValues();

        Assert.assertEquals("Measurements count doesn't match", 2, actualMeasurements.size());

        actualMeasurements.forEach(actualMeasurement ->
                Assert.assertTrue("Measurement " + actualMeasurement + " doesn't exist",
                        expectedMeasurements.contains(actualMeasurement.get(0))));
    }

    @Test
    public void query_prefixForShowFieldsForMeasurement_returnsFields() {
        QueryResult.Result result = new QueryResult.Result();
        QueryResult.Series series = new QueryResult.Series();
        List<String> columns = new LinkedList<>(Arrays.asList("fieldKey", "fieldType"));
        List<Object> firstValueObject = new LinkedList<>(Arrays.asList("value_1", "float"));
        List<List<Object>> values = new LinkedList<>();
        values.add(firstValueObject);
        series.setColumns(columns);
        series.setValues(values);
        result.setSeries(Arrays.asList(series));
        QueryResult expectedQueryResult = new QueryResult();
        expectedQueryResult.setResults(new ArrayList<>(Arrays.asList(result)));
        mockedComponents("1234", "measurement", expectedQueryResult);

        QueryResult qr = queryService.query("1234","SHOW FIELD KEYS FROM measurement");

        QueryResult.Series actualSeries = qr.getResults().get(0).getSeries().get(0);

        // Match columns
        for(int i = 0; i < 2; i++) {
            Assert.assertEquals(columns.get(i), actualSeries.getColumns().get(i));
        }

        // Match values
        for(int i = 0; i < 2; i++) {
            Assert.assertEquals(firstValueObject.get(i), actualSeries.getValues().get(0).get(i));
        }
    }

    @Test
    public void query_prefixForShowTagsForMeasurement_returnsTags() {
        QueryResult.Result result = new QueryResult.Result();
        QueryResult.Series series = new QueryResult.Series();
        List<String> columns = new ArrayList<>(Arrays.asList("tagKey"));

        List<Object> tags = new ArrayList<>(Arrays.asList("tag1", "tag2", "tag3"));
        List<List<Object>> values = new ArrayList<>();
        tags.forEach(tag -> values.add(new ArrayList<>(Arrays.asList(tag))));
        series.setColumns(columns);
        series.setValues(values);
        result.setSeries(Arrays.asList(series));
        QueryResult expectedQueryResult = new QueryResult();
        expectedQueryResult.setResults(new ArrayList<>(Arrays.asList(result)));
        mockedComponents("1234", "measurement", expectedQueryResult);

        QueryResult qr = queryService.query("1234","SHOW TAG KEYS FROM measurement");

        QueryResult.Series actualSeries = qr.getResults().get(0).getSeries().get(0);

        // Match column
        Assert.assertEquals(columns.get(0), actualSeries.getColumns().get(0));

        // Match values size
        Assert.assertEquals(tags.size(), actualSeries.getValues().size());

        // Match individual values
        actualSeries.getValues().forEach(actualTag -> Assert.assertTrue(
                "Tag " + actualTag.get(0) + " is not expected.", tags.contains(actualTag.get(0))));
    }

    @Test
    public void query_prefixForShowTagValuesForGivenKey_returnsTagValues() {
        QueryResult.Result result = new QueryResult.Result();
        QueryResult.Series series = new QueryResult.Series();
        List<String> columns = new ArrayList<>(Arrays.asList("key", "value"));

        List<List<Object>> values = new ArrayList<>(Arrays.asList(
                new ArrayList<>(Arrays.asList("area", "heap")),
                new ArrayList<>(Arrays.asList("area", "nonheap"))
        ));

        series.setColumns(columns);
        series.setValues(values);
        result.setSeries(Arrays.asList(series));
        QueryResult expectedQueryResult = new QueryResult();
        expectedQueryResult.setResults(new ArrayList<>(Arrays.asList(result)));
        mockedComponents("1234", "jvm_memory_used", expectedQueryResult);

        QueryResult qr = queryService.query("1234",
                "SHOW TAG VALUES FROM \"jvm_memory_used\" WITH KEY = \"area\"");

        QueryResult.Series actualSeries = qr.getResults().get(0).getSeries().get(0);

        // Match column
        Assert.assertEquals(columns.get(0), actualSeries.getColumns().get(0));

        // Match values size
        Assert.assertEquals(values.size(), actualSeries.getValues().size());

        // Match individual values
        for(int i = 0; i < values.size(); i++) {
            Assert.assertEquals(values.get(i).get(0), actualSeries.getValues().get(i).get(0)); // match value's key
            Assert.assertEquals(values.get(i).get(1), actualSeries.getValues().get(i).get(1)); // match value's value
        }
    }

    @Test
    public void query_forGrafanaGetDataPointsForGivenQuery_returnsDataPoints() {
        QueryResult.Result result = new QueryResult.Result();
        QueryResult.Series series = new QueryResult.Series();
        List<String> columns = new ArrayList<>(Arrays.asList("time", "mean"));

        List<List<Object>> values = new ArrayList<>(Arrays.asList(
                new ArrayList<>(Arrays.asList("2019-05-09T10:31:00.00Z",96686457.16666667)),
                new ArrayList<>(Arrays.asList("2019-05-09T10:32:00.00Z",80639353.98095238)),
                new ArrayList<>(Arrays.asList("2019-05-09T10:33:00.00Z",76527140.57142857))
        ));

        series.setColumns(columns);
        series.setValues(values);
        result.setSeries(Arrays.asList(series));
        QueryResult expectedQueryResult = new QueryResult();
        expectedQueryResult.setResults(new ArrayList<>(Arrays.asList(result)));
        mockedComponents("1234", "jvm_memory_used", expectedQueryResult);

        QueryResult qr = queryService.query("1234",
                "SELECT mean(\"value\") FROM \"jvm_memory_used\" " +
                        "WHERE (\"area\" = 'heap') AND time >= now() - 6h GROUP BY time(1m) fill(null)");

        QueryResult.Series actualSeries = qr.getResults().get(0).getSeries().get(0);

        // Match columns
        for(int i = 0; i < 2; i++) {
            Assert.assertEquals(columns.get(i), actualSeries.getColumns().get(i));
        }

        // Match values size
        Assert.assertEquals(values.size(), actualSeries.getValues().size());

        // Match individual values
        for(int i = 0; i < values.size(); i++) {
            Assert.assertEquals(Instant.parse(values.get(i).get(0).toString())
                    .toEpochMilli(), actualSeries.getValues().get(i).get(0)); // match value's key
            Assert.assertEquals(values.get(i).get(1), actualSeries.getValues().get(i).get(1)); // match value's value
        }
    }


    @Test(expected = RouteNotFoundException.class)
    public void find_noRouteForTenantId_throwsRouteNotFoundException() {
        when(routeProvider.getRoute(anyString(), anyString(), any())).thenThrow(RuntimeException.class);
        QueryResult output = queryService.query("",
                "SELECT mean(\"value\") FROM \"jvm_memory_used\" " +
                        "WHERE (\"area\" = 'heap') AND time >= now() - 6h GROUP BY time(1m) fill(null)");
    }

    @Test(expected = InvalidQueryException.class)
    public void find_invalidQueryString_throwsInvalidQueryException() {
        mockedComponents("1234", "measurement", null);

        QueryResult output = queryService.query("1234",
                "from agent_cpu where time > now() - 5d;drop");
    }

    @Test(expected = ErroredQueryResultException.class)
    public void measurements_erroredQueryResult_throwsErroredQueryResultException() {
        TenantRoutes routes = getTenantRoutes("1234", "measurement");
        when(routeProvider.getRoute(anyString(), anyString(), any())).thenReturn(routes);

        QueryResult queryResult = new QueryResult();
        queryResult.setError("Test measurements_erroredQueryResult_throwsErroredQueryResultException");

        when(influxDB.query(any())).thenReturn(queryResult);

        QueryResult qr = queryService.query("1234",
                "SELECT mean(\"value\") FROM \"jvm_memory_used\" " +
                        "WHERE (\"area\" = 'heap') AND time >= now() - 6h GROUP BY time(1m) fill(null)");
    }

    @Test
    public void validateGetMeasurementTags() {
        QueryResult.Result result = new QueryResult.Result();
        QueryResult.Series series = new QueryResult.Series();
        List<String> columns = new ArrayList<>(Arrays.asList("tagKey"));

        List<Object> tags = new ArrayList<>(Arrays.asList("tag1", "tag2", "tag3"));
        List<List<Object>> values = new ArrayList<>();
        tags.forEach(tag -> values.add(new ArrayList<>(Arrays.asList(tag))));
        series.setColumns(columns);
        series.setValues(values);
        result.setSeries(Arrays.asList(series));
        QueryResult expectedQueryResult = new QueryResult();
        expectedQueryResult.setResults(new ArrayList<>(Arrays.asList(result)));
        mockedComponents("1234", "measurement", expectedQueryResult);

        QueryResult qr = queryService.getMeasurementTags("1234", "measurement");

        QueryResult.Series actualSeries = qr.getResults().get(0).getSeries().get(0);

        // Match column
        Assert.assertEquals(columns.get(0), actualSeries.getColumns().get(0));

        // Match values size
        Assert.assertEquals(tags.size(), actualSeries.getValues().size());

        // Match individual values
        actualSeries.getValues().forEach(actualTag -> Assert.assertTrue(
                "Tag " + actualTag.get(0) + " is not expected.", tags.contains(actualTag.get(0))));
    }

    @Test
    public void validateGetMeasurementFields() {
        QueryResult.Result result = new QueryResult.Result();
        QueryResult.Series series = new QueryResult.Series();
        List<String> columns = new LinkedList<>(Arrays.asList("fieldKey", "fieldType"));
        List<Object> firstValueObject = new LinkedList<>(Arrays.asList("value_1", "float"));
        List<List<Object>> values = new LinkedList<>();
        values.add(firstValueObject);
        series.setColumns(columns);
        series.setValues(values);
        result.setSeries(Arrays.asList(series));
        QueryResult expectedQueryResult = new QueryResult();
        expectedQueryResult.setResults(new ArrayList<>(Arrays.asList(result)));
        mockedComponents("1234", "measurement", expectedQueryResult);

        QueryResult qr = queryService.getMeasurementFields("1234", "measurement");

        QueryResult.Series actualSeries = qr.getResults().get(0).getSeries().get(0);

        // Match columns
        for(int i = 0; i < 2; i++) {
            Assert.assertEquals(columns.get(i), actualSeries.getColumns().get(i));
        }

        // Match values
        for(int i = 0; i < 2; i++) {
            Assert.assertEquals(firstValueObject.get(i), actualSeries.getValues().get(0).get(i));
        }
    }

    @Test
    public void validateGetMeasurementSeriesForTimeInterval() {
        QueryResult.Result result = new QueryResult.Result();
        QueryResult.Series series = new QueryResult.Series();
        List<String> columns = new ArrayList<>(Arrays.asList("time", "mean"));

        List<List<Object>> values = new ArrayList<>(Arrays.asList(
                new ArrayList<>(Arrays.asList("2019-05-09T10:31:00.00Z",96686457.16666667)),
                new ArrayList<>(Arrays.asList("2019-05-09T10:32:00.00Z",80639353.98095238)),
                new ArrayList<>(Arrays.asList("2019-05-09T10:33:00.00Z",76527140.57142857))
        ));

        series.setColumns(columns);
        series.setValues(values);
        result.setSeries(Arrays.asList(series));
        QueryResult expectedQueryResult = new QueryResult();
        expectedQueryResult.setResults(new ArrayList<>(Arrays.asList(result)));
        mockedComponents("1234", "jvm_memory_used", expectedQueryResult);

        QueryResult qr = queryService.getMeasurementSeriesForTimeInterval("1234", "jvm_memory_used",
                LocalDateTime.now().minusHours(6), LocalDateTime.now());

        QueryResult.Series actualSeries = qr.getResults().get(0).getSeries().get(0);

        // Match columns
        for(int i = 0; i < 2; i++) {
            Assert.assertEquals(columns.get(i), actualSeries.getColumns().get(i));
        }

        // Match values size
        Assert.assertEquals(values.size(), actualSeries.getValues().size());

        // Match individual values
        for(int i = 0; i < values.size(); i++) {
            Assert.assertEquals(values.get(i).get(0).toString(), actualSeries.getValues().get(i).get(0));
            Assert.assertEquals(values.get(i).get(1), actualSeries.getValues().get(i).get(1)); // match value's value
        }
    }

    private void mockedComponents(String tenantId, String measurement, QueryResult queryResult) {
        TenantRoutes routes = getTenantRoutes(tenantId, measurement);
        when(routeProvider.getRoute(anyString(), anyString(), any())).thenReturn(routes);

        when(influxDB.query(any())).thenReturn(queryResult);
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

    private TenantRoutes getTenantRoutes(String tenantId, String measurement) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            TenantRoutes routes = mapper.readValue(ROUTES, TenantRoutes.class);
            routes.setTenantIdAndMeasurement(String.format("%s:%s", tenantId, measurement));
            return routes;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private final static String ROUTES = "{\n" +
            "  \"tenantIdAndMeasurement\": \"\",\n" +
            "  \"routes\": {\n" +
            "    \"60m\": {\n" +
            "      \"path\": \"http://localhost:8086\",\n" +
            "      \"databaseName\": \"db_0\",\n" +
            "      \"retentionPolicyName\": \"rp_155d\",\n" +
            "      \"retentionPolicy\": \"155d\"\n" +
            "    },\n" +
            "    \"240m\": {\n" +
            "      \"path\": \"http://localhost:8086\",\n" +
            "      \"databaseName\": \"db_0\",\n" +
            "      \"retentionPolicyName\": \"rp_300d\",\n" +
            "      \"retentionPolicy\": \"300d\"\n" +
            "    },\n" +
            "    \"full\": {\n" +
            "      \"path\": \"http://localhost:8086\",\n" +
            "      \"databaseName\": \"db_0\",\n" +
            "      \"retentionPolicyName\": \"rp_5d\",\n" +
            "      \"retentionPolicy\": \"5d\"\n" +
            "    },\n" +
            "    \"1440m\": {\n" +
            "      \"path\": \"http://localhost:8086\",\n" +
            "      \"databaseName\": \"db_0\",\n" +
            "      \"retentionPolicyName\": \"rp_1825d\",\n" +
            "      \"retentionPolicy\": \"1825d\"\n" +
            "    },\n" +
            "    \"5m\": {\n" +
            "      \"path\": \"http://localhost:8086\",\n" +
            "      \"databaseName\": \"db_0\",\n" +
            "      \"retentionPolicyName\": \"rp_10d\",\n" +
            "      \"retentionPolicy\": \"10d\"\n" +
            "    },\n" +
            "    \"20m\": {\n" +
            "      \"path\": \"http://localhost:8086\",\n" +
            "      \"databaseName\": \"db_0\",\n" +
            "      \"retentionPolicyName\": \"rp_20d\",\n" +
            "      \"retentionPolicy\": \"20d\"\n" +
            "    }\n" +
            "  }\n" +
            "}";
}
