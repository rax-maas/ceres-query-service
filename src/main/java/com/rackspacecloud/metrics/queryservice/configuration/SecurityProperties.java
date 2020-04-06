package com.rackspacecloud.metrics.queryservice.configuration;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotEmpty;

@Component
@ConfigurationProperties(prefix = "security")
@Data
public class SecurityProperties {
    @NotEmpty
    private String whitelistedIpRange;
    @NotEmpty
    private String[] whitelistedRoles;
}
