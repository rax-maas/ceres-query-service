package com.rackspacecloud.metrics.queryservice.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotEmpty;

@Component
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {
    @NotEmpty
    @Getter
    @Setter
    private String whitelistedIpRange;
    @NotEmpty
    @Getter
    @Setter
    private String[] whitelistedRoles;
}
