package com.rackspacecloud.metrics.queryservice;

import com.rackspacecloud.metrics.queryservice.controllers.GlobalExceptionHandler;
import com.rackspacecloud.metrics.queryservice.controllers.QueryController;
import com.rackspacecloud.metrics.queryservice.domains.QueryDomainInput;
import com.rackspacecloud.metrics.queryservice.domains.QueryDomainOutput;
import com.rackspacecloud.metrics.queryservice.exceptions.InfluxDbQueryResultException;
import com.rackspacecloud.metrics.queryservice.exceptions.InvalidQueryException;
import com.rackspacecloud.metrics.queryservice.exceptions.RouteNotFoundException;
import com.rackspacecloud.metrics.queryservice.models.MeasurementQueryRequest;
import com.rackspacecloud.metrics.queryservice.models.QueryInput;
import com.rackspacecloud.metrics.queryservice.services.QueryServiceImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(value = QueryController.class)
public class QueryControllerTest {
    MockMvc mockMvc;

    @MockBean
    QueryServiceImpl queryServiceImpl;

    @InjectMocks
    QueryController controller;

    @Before
    public void setup(){
        MockitoAnnotations.initMocks(this);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    public void find_validTenantIdAndPayloadReturnsListOfMaps() throws Exception {
        List<QueryDomainOutput> domainOutputs = getQueryDomainOutputs();

        when(queryServiceImpl.find(anyString(), any(QueryDomainInput.class))).thenReturn(domainOutputs);

        List<?> outputs = controller.find("1234", new QueryInput());

        Map<String, Object> map = (HashMap <String, Object>) outputs.get(0);

        Assert.assertEquals("valueForCol1", map.get("column1"));
    }

    @Test(expected = InfluxDbQueryResultException.class)
    public void find_invalidQueryResultFromInfluxDB_throwsInfluxDbQueryResultException() throws Exception {
        QueryInput in = new QueryInput();
        in.setQueryString("select * from dummy where foo = bar");

        List<QueryDomainOutput> domainOutputs = new ArrayList<>();

        QueryDomainOutput output = new QueryDomainOutput();
        output.setName("test");
        output.setColumns(new ArrayList<>());
        List<String> cols = output.getColumns();
        cols.add("column1");
        cols.add("column2");
        output.setValuesCollection(new ArrayList<>());
        List<List<Object>> valCollection = output.getValuesCollection();
        valCollection.add(new ArrayList<>());
        List<Object> firstValues = valCollection.get(0);
        firstValues.add("valueForCol1");

        domainOutputs.add(output);

        when(queryServiceImpl.find(anyString(), any(QueryDomainInput.class))).thenReturn(domainOutputs);

        List<?> outputs = controller.find("1234", in);
    }

    @Test
    public void queryMeasurement_validTenantIdMeasurementAndPayloadReturnsListOfMaps() {
        List<QueryDomainOutput> domainOutputs = getQueryDomainOutputs();

        when(queryServiceImpl.queryMeasurement(anyString(), anyString(), any(MeasurementQueryRequest.class)))
                .thenReturn(domainOutputs);

        List<?> outputs = controller.queryMeasurement(
                "1234", "dummy", new MeasurementQueryRequest());

        Map<String, Object> map = (HashMap <String, Object>) outputs.get(0);

        Assert.assertEquals("valueForCol1", map.get("column1"));
    }

    @Test
    public void measurements_validTenantIdReturnsListOfMeasurements() {
        List<QueryDomainOutput> domainOutputs = getQueryDomainOutputs();

        when(queryServiceImpl.measurements(anyString())).thenReturn(domainOutputs);

        List<?> outputs = controller.measurements("1234");

        Assert.assertEquals("valueForCol1", outputs.get(0));
    }

    @Test
    public void tags_validTenantIdAndMeasurementReturnsListOfTags() {
        List<QueryDomainOutput> domainOutputs = getQueryDomainOutputs();

        when(queryServiceImpl.tags(anyString(), anyString())).thenReturn(domainOutputs);

        List<?> outputs = controller.tags("1234", "dummyMeasurement");

        Assert.assertEquals("valueForCol1", outputs.get(0));
    }

    @Test
    public void tagValues_validTenantIdMeasurementAndTagsReturnsMapOfTagsValues() {
        List<QueryDomainOutput> domainOutputs = getQueryDomainOutputs();

        when(queryServiceImpl.tagValues(anyString(), anyString(), anyString())).thenReturn(domainOutputs);

        List<?> outputs = controller.tagValues("1234", "dummyMeasurement", "tags");
        Map<String, Object> map = (HashMap <String, Object>) outputs.get(0);

        Assert.assertEquals("valueForCol1", map.get("column1"));
    }

    @Test
    public void test_GlobalExceptionHandler_getMethod_nonExistingTenant_throwsRouteNotFoundException() throws Exception {
        doThrow(RouteNotFoundException.class).when(queryServiceImpl).measurements(anyString());

        this.mockMvc.perform(get("/dummy/measurements").accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(content().string("{\"message\":null,\"rootCause\":null}"));
    }

    @Test
    public void test_GlobalExceptionHandler_getMethod_badQuery_throwsInvalidQueryException() throws Exception {
        doThrow(InvalidQueryException.class).when(queryServiceImpl).measurements(anyString());

        this.mockMvc.perform(get("/dummy/measurements").accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("{\"message\":null,\"rootCause\":null}"));
    }

    @Test
    public void test_GlobalExceptionHandler_getMethod_randomException_throwsException() throws Exception {
        doThrow(RuntimeException.class).when(queryServiceImpl).measurements(anyString());

        this.mockMvc.perform(get("/dummy/measurements").accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("{\"message\":null,\"rootCause\":null}"));
    }

    private List<QueryDomainOutput> getQueryDomainOutputs() {
        List<QueryDomainOutput> domainOutputs = new ArrayList<>();

        QueryDomainOutput output = new QueryDomainOutput();
        output.setName("test");
        output.setColumns(new ArrayList<>());
        List<String> cols = output.getColumns();
        cols.add("column1");
        output.setValuesCollection(new ArrayList<>());
        List<List<Object>> valCollection = output.getValuesCollection();
        valCollection.add(new ArrayList<>());
        List<Object> firstValues = valCollection.get(0);
        firstValues.add("valueForCol1");

        domainOutputs.add(output);
        return domainOutputs;
    }
}
