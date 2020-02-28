package com.rackspacecloud.metrics.queryservice.services;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

/**
 * This Spring Security filter integrates with Repose by consuming the headers populated by the
 * <a href="https://repose.atlassian.net/wiki/x/CAALAg">Repose Keystone v2 filter</a> and translates
 * that into an authenticated {@link PreAuthenticatedToken}.
 */
@Slf4j
public class ReposeHeaderFilter extends PreAuthenticatedFilter {

    public static final String HEADER_X_ROLES = "X-Roles";
    public static final String HEADER_X_IMPERSONATOR_ROLES = "X-Impersonator-Roles";
    public static final String HEADER_TENANT = "X-Tenant-Id";

    public ReposeHeaderFilter() {
        super(HEADER_TENANT, Arrays.asList(HEADER_X_ROLES, HEADER_X_IMPERSONATOR_ROLES));
    }
}
