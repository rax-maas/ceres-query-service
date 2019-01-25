package com.rackspacecloud.metrics.queryservice.controllers;

import com.rackspacecloud.metrics.queryservice.domains.QueryDomainInput;
import com.rackspacecloud.metrics.queryservice.domains.QueryDomainOutput;
import com.rackspacecloud.metrics.queryservice.models.QueryInput;
import com.rackspacecloud.metrics.queryservice.services.QueryServiceImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@WebMvcTest(value = QueryController.class)
public class QueryControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    QueryServiceImpl queryServiceImpl;

    @InjectMocks
    QueryController controller;

    @Before
    public void setup(){
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void find_validTenantIdAndPayloadReturnsQueryOutputCollection() throws Exception {
        List<QueryDomainOutput> domainOutputs = new ArrayList<>();

        QueryDomainOutput output = new QueryDomainOutput();
        output.setName("test");

        domainOutputs.add(output);

        when(queryServiceImpl.find(anyString(), any(QueryDomainInput.class))).thenReturn(domainOutputs);

        List<?> outputs = controller.find("1234", new QueryInput());

        Assert.assertEquals("test", ((QueryDomainOutput) outputs.get(0)).getName());
    }
}
