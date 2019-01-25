package com.rackspacecloud.metrics.queryservice.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class QueryInput {
    @NotBlank
    @JsonProperty(value = "queryString")
    private String queryString;
}
