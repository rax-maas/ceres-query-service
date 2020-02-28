package com.rackspacecloud.metrics.queryservice;

import com.rackspacecloud.metrics.queryservice.controllers.GlobalExceptionHandler;
import com.rackspacecloud.metrics.queryservice.controllers.QueryController;
import com.rackspacecloud.metrics.queryservice.exceptions.InvalidQueryException;
import com.rackspacecloud.metrics.queryservice.exceptions.RouteNotFoundException;
import com.rackspacecloud.metrics.queryservice.services.QueryServiceImpl;
import org.influxdb.dto.QueryResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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
    public void query_validTenantIdAndPayloadReturnsListOfMaps() throws Exception {
        when(queryServiceImpl.query(anyString(), anyString())).thenReturn(new QueryResult());

        QueryResult queryResult = controller.query("1234", "queryString");

        Assert.assertNotNull("output is null", queryResult);
    }

    @Test
    public void test_GlobalExceptionHandler_getMethod_nonExistingTenant_throwsRouteNotFoundException() throws Exception {
        doThrow(RouteNotFoundException.class).when(queryServiceImpl).query(anyString(), anyString());

        this.mockMvc.perform(get("/grafana-query?db=telegraf&q=SELECT mean(\"usage_user\")" +
                "FROM \"cpu\" WHERE time >= 1549513924084ms and time <= 1549514901143ms GROUP BY time(10s) fill(0)"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("{\"message\":null,\"rootCause\":null}"));
    }

    @Test
    public void test_GlobalExceptionHandler_getMethod_badQuery_throwsInvalidQueryException() throws Exception {
        doThrow(InvalidQueryException.class).when(queryServiceImpl).query(anyString(), anyString());

        this.mockMvc.perform(get("/grafana-query?db=telegraf&q=SELECT mean(\"usage_user\")" +
                "FROM \"cpu\" WHERE time >= 1549513924084ms and time <= 1549514901143ms GROUP BY time(10s) fill(0)"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("{\"message\":null,\"rootCause\":null}"));
    }

    @Test
    public void test_GlobalExceptionHandler_getMethod_randomException_throwsException() throws Exception {
        doThrow(RuntimeException.class).when(queryServiceImpl).query(anyString(), anyString());

        this.mockMvc.perform(get("/grafana-query?db=telegraf&q=SELECT mean(\"usage_user\")" +
                "FROM \"cpu\" WHERE time >= 1549513924084ms and time <= 1549514901143ms GROUP BY time(10s) fill(0)"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("{\"message\":null,\"rootCause\":null}"));
    }
}
