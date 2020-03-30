package com.rackspacecloud.metrics.queryservice.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {
    String whitelistedIpRange;
    String[] whitelistedRoles;
}
