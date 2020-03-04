/*
 * Copyright 2019 Rackspace US, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rackspacecloud.metrics.queryservice.services;

import org.hamcrest.core.StringStartsWith;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.GrantedAuthority;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;

import static com.rackspacecloud.metrics.queryservice.services.ReposeHeaderFilter.*;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ReposeFunctionalTest {

  @Mock
  HttpServletRequest servletRequest;

  @Test
  public void testGetToken() {
    PreAuthenticatedFilter preAuthenticatedFilter = new PreAuthenticatedFilter(HEADER_TENANT,
        Arrays.asList(HEADER_X_ROLES, HEADER_X_IMPERSONATOR_ROLES));
    String userRoles = "monitoring:admin,dedicated:default,ticketing:admin,identity:user-admin";
    String impersonationRoles = "custom:my-admin,salus:admin";
    String tenantId = "12345";

    when(servletRequest.getHeader(HEADER_X_ROLES))
        .thenReturn(userRoles);

    when(servletRequest.getHeader(HEADER_X_IMPERSONATOR_ROLES))
        .thenReturn(impersonationRoles);

    when(servletRequest.getHeader(HEADER_TENANT))
        .thenReturn(tenantId);

    Optional<PreAuthenticatedToken> token = preAuthenticatedFilter.getToken(servletRequest);

    assertTrue(token.isPresent());
    assertThat(token.get().getAuthorities(), hasSize(6));
    for (GrantedAuthority authority : token.get().getAuthorities()) {
      assertThat(authority.getAuthority(), StringStartsWith.startsWith("ROLE_"));
    }
  }

  @Test
  public void testGetTokenNoRoles() {
    PreAuthenticatedFilter preAuthenticatedFilter = new PreAuthenticatedFilter(HEADER_TENANT,
        Arrays.asList(HEADER_X_ROLES, HEADER_X_IMPERSONATOR_ROLES));
    String tenantId = "12345";

    when(servletRequest.getHeader(HEADER_X_ROLES))
        .thenReturn(null);

    when(servletRequest.getHeader(HEADER_X_IMPERSONATOR_ROLES))
        .thenReturn(null);

    when(servletRequest.getHeader(HEADER_TENANT))
        .thenReturn(tenantId);

    Optional<PreAuthenticatedToken> token = preAuthenticatedFilter.getToken(servletRequest);

    assertFalse(token.isPresent());
  }

  @Test
  public void testGetTokenSomeRoles() {
    PreAuthenticatedFilter preAuthenticatedFilter = new PreAuthenticatedFilter(HEADER_TENANT,
        Arrays.asList(HEADER_X_ROLES, HEADER_X_IMPERSONATOR_ROLES));
    String userRoles = "monitoring:admin,dedicated:default,ticketing:admin,identity:user-admin";
    String tenantId = "12345";

    when(servletRequest.getHeader(HEADER_X_ROLES))
        .thenReturn(userRoles);

    when(servletRequest.getHeader(HEADER_X_IMPERSONATOR_ROLES))
        .thenReturn(null);

    when(servletRequest.getHeader(HEADER_TENANT))
        .thenReturn(tenantId);

    Optional<PreAuthenticatedToken> token = preAuthenticatedFilter.getToken(servletRequest);

    assertTrue(token.isPresent());
    assertThat(token.get().getAuthorities(), hasSize(4));
    for (GrantedAuthority authority : token.get().getAuthorities()) {
      assertThat(authority.getAuthority(), StringStartsWith.startsWith("ROLE_"));
    }
  }

  @Test
  public void testGetTokenNoTenant() {
    PreAuthenticatedFilter preAuthenticatedFilter = new PreAuthenticatedFilter(HEADER_TENANT,
        Arrays.asList(HEADER_X_ROLES, HEADER_X_IMPERSONATOR_ROLES));
    String userRoles = "monitoring:admin,dedicated:default,ticketing:admin,identity:user-admin";
    String impersonationRoles = "custom:my-admin,salus:admin";

    when(servletRequest.getHeader(HEADER_X_ROLES))
        .thenReturn(userRoles);

    when(servletRequest.getHeader(HEADER_X_IMPERSONATOR_ROLES))
        .thenReturn(impersonationRoles);

    when(servletRequest.getHeader(HEADER_TENANT))
        .thenReturn(null);

    Optional<PreAuthenticatedToken> token = preAuthenticatedFilter.getToken(servletRequest);

    assertFalse(token.isPresent());
  }
}
