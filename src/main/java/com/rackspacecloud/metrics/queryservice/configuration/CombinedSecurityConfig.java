/*
 *    Copyright 2018 Rackspace US, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package com.rackspacecloud.metrics.queryservice.configuration;

import com.rackspacecloud.metrics.queryservice.services.ReposeHeaderFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.util.Assert;

/**
 * @author Geoff Bourne
 * @author zacksh
 * @since Mar 2017
 */

@Slf4j
@EnableWebSecurity
public class CombinedSecurityConfig {

    private final Environment env;
    private final SecurityProperties securityProperties;

    @Autowired
    public CombinedSecurityConfig(Environment env, SecurityProperties securityProperties) {
        this.env = env;
        this.securityProperties = securityProperties;
    }

    @Configuration
    @Order(1)
    public class GrafanaWebConfig extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            log.debug("Locking grafana to {}", securityProperties.getWhitelistedIpRange());
            http
                .httpBasic().disable()
                .csrf().disable();

            // narrow security filter chain scope for grafana access URLs
            final HttpSecurity scoped = http.antMatcher("/query/**");

            if (env.acceptsProfiles(Profiles.of("production"))) {
                Assert.hasText(securityProperties.getWhitelistedIpRange(),
                    "whitelistedIpRange is required");

                scoped
                    .authorizeRequests()
                    .anyRequest()
                    .hasIpAddress(securityProperties.getWhitelistedIpRange());
            } else {
                scoped
                    .authorizeRequests()
                    .anyRequest().permitAll();
            }
        }
    }

    @Configuration
    public class ReposeWebConfig extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            log.debug("Configuring tenant web security");
            http.csrf().disable();

            // narrow security filter chain scope for intelligence access URLs
            http.antMatcher("/v1.0/tenant/**")
                // extract authorization/role from repose headers
                .addFilterBefore(
                    new ReposeHeaderFilter(),
                    BasicAuthenticationFilter.class
                )
                .authorizeRequests()
                // applies to any request in this security filter chain scope
                .anyRequest()
                // and require an expected role
                .hasAnyRole(securityProperties.getWhitelistedRoles());
        }
    }
}
