package com.rackspacecloud.metrics.queryservice.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class MeasurementQueryRequest {
    @JsonProperty(value = "filter")
    Filter filter;

    @JsonProperty(value = "select")
    Select select;

    @Data
    public static class Filter {
        private Long from;
        private Long to;

        private Map<String, List<String>> tags;
        private Map<String, List<String>> fields;
    }

    @Data
    public static class Select {
        private List<String> columns;
    }
}
