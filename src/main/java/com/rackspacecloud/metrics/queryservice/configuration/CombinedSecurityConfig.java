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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * @author Geoff Bourne
 * @author zacksh
 * @since Mar 2017
 */

public class CombinedSecurityConfig {
    @Configuration
    @Order(1)
    public class GrafanaWebConfig extends WebSecurityConfigurerAdapter {
        @Value("${security.whitelistedIpRange}")
        private String whitelistedIpRange;

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                    .httpBasic().disable()
                    .csrf().disable()
                    .antMatcher("/grafana-query/**")
                    .authorizeRequests().antMatchers("/grafana-query/**")
                    .hasIpAddress(whitelistedIpRange);
        }
    }

    @Configuration
    public class ReposeWebConfig extends WebSecurityConfigurerAdapter {
        @Value("${security.whitelistedRoles}")
        private String[] whitelistedRoles;

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.addFilterBefore(
                    new ReposeHeaderFilter(),
                    UsernamePasswordAuthenticationFilter.class);
            http.csrf().disable();
            http.antMatcher("/intelligence-format-query/**")
                    .authorizeRequests()
                    .antMatchers("/intelligence-format-query/**")
                    .hasAnyRole(whitelistedRoles);
        }
    }
}

