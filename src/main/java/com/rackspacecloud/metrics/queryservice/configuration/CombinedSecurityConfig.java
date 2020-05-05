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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * @author Geoff Bourne
 * @author zacksh
 * @since Mar 2017
 */

@Slf4j
@EnableWebSecurity
public class CombinedSecurityConfig {

    private final SecurityProperties securityProperties;

    @Autowired
    public CombinedSecurityConfig(SecurityProperties securityProperties) {
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
                    .csrf().disable()
                    .antMatcher("/query/**")
                    .authorizeRequests().antMatchers("/query/**")
                    .hasIpAddress(securityProperties.getWhitelistedIpRange());
        }
    }

    @Configuration
    public class ReposeWebConfig extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            log.debug("Configuring tenant web security");
            http.addFilterBefore(
                    new ReposeHeaderFilter(),
                    BasicAuthenticationFilter.class);
            http.csrf().disable();
            http.antMatcher("/intelligence-format-query/**")
                    .authorizeRequests()
                    .antMatchers("/intelligence-format-query/**")
                    .hasAnyRole(securityProperties.getWhitelistedRoles());
        }
    }
}
