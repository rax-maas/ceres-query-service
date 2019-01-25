package com.rackspacecloud.metrics.queryservice.controllers;

import com.rackspacecloud.metrics.queryservice.domains.QueryDomainInput;
import com.rackspacecloud.metrics.queryservice.domains.QueryDomainOutput;
import com.rackspacecloud.metrics.queryservice.models.MeasurementQueryRequest;
import com.rackspacecloud.metrics.queryservice.models.QueryInput;
import com.rackspacecloud.metrics.queryservice.services.QueryService;
import org.influxdb.dto.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.annotation.Secured;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("")
public class QueryController {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueryController.class);

    @Autowired
    QueryService queryService;

    /**
     * This endpoint is used for free form query. Use it only when you don't have other option.
     * You need to write your own InfluxQL query to fetch any data.
     * @param tenantId tenantId who has access to this endpoint
     * @param queryInput payload containing query string
     * @return
     * @throws Exception
     */
    @RequestMapping(
            value = "/{tenantId}",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    // Note Confirm this is the correct role for this method:
    @Secured({"ROLE_COMPUTE_DEFAULT"})
    public List<?> find(
            @NotBlank @PathVariable final String tenantId,
            @Valid @RequestBody final QueryInput queryInput) throws Exception {

        LOGGER.info("find: request received for tenantId [{}]", tenantId);
        LOGGER.debug("Query string is [{}]", queryInput.getQueryString());

        QueryDomainInput domainInput = inputToDomainInput(queryInput);

        List<QueryDomainOutput> domainOutputs = queryService.find(tenantId, domainInput);

        List<Map<String, Object>> response = new ArrayList<>();
        domainOutputs.forEach(queryResult -> response.addAll(queryResult.getQueryResponse()));

        return response;
    }

    /**
     * Using this method you can query measurement for given tags and/or fields.
     * Request Payload example:
     * {
     * 	"filter": {
     * 		"from": "1548301512",
     * 		"to": "1548301623",
     * 		"tags": {
     * 		    "devicelabel": [
     * 					"dummy-device-label-0",
     * 					"dummy-device-label-3",
     * 					"dummy-device-label-6"
     * 				],
     * 		    "collectionlabel": ["dummy-collection-label"]
     *                },
     * 		"fields": {
     * 		  "filesystem_avail": [
     * 				">= 0",
     * 				"< 50000"
     * 			],
     * 		  "filesystem_free": ["< 60000"],
     * 		  "filesystem_used": ["> 0"]
     *      }    * 	},
     *
     * 	"select": {
     * 		"columns": [
     * 			"filesystem_avail",
     * 			"filesystem_total",
     * 			"filesystem_free",
     * 			"entitysystemid",
     * 			"systemaccountid"
     * 		]
     * 	}
     * }
     *
     * This results in following `InfluxQL` query:
     * SELECT filesystem_avail,filesystem_total,filesystem_free,entitysystemid,systemaccountid
     * FROM "agent_filesystem"
     * WHERE (time > 1548301512000000000 AND time < 1548301623000000000)
     *   AND (
     *     (
     *       "devicelabel" = 'dummy-device-label-0' OR
     *       "devicelabel" = 'dummy-device-label-3' OR
     *       "devicelabel" = 'dummy-device-label-6'
     *     )
     *     AND (
     *       "collectionlabel" = 'dummy-collection-label'
     *     )
     *   )
     *   AND ("filesystem_avail" >= 0 AND "filesystem_avail" < 50000)
     *   AND ("filesystem_free" < 60000)
     *   AND ("filesystem_used" > 0)
     *
     * Following is the response JSON:
     * [
     *   {
     *     "systemaccountid": "dummy-account-id-0",
     *     "filesystem_avail": 14505.0,
     *     "filesystem_total": 8839.0,
     *     "filesystem_free": 8651.0,
     *     "time": "2019-01-24T03:46:52Z",
     *     "entitysystemid": "dummy-entity-id-0"
     *   },
     *   {
     *     "systemaccountid": "dummy-account-id-3",
     *     "filesystem_avail": 22338.0,
     *     "filesystem_total": 1053.0,
     *     "filesystem_free": 1352.0,
     *     "time": "2019-01-24T03:46:53Z",
     *     "entitysystemid": "dummy-entity-id-3"
     *   },
     *   {
     *     "systemaccountid": "dummy-account-id-6",
     *     "filesystem_avail": 14259.0,
     *     "filesystem_total": 39414.0,
     *     "filesystem_free": 22929.0,
     *     "time": "2019-01-24T03:46:53Z",
     *     "entitysystemid": "dummy-entity-id-6"
     *   }
     * ]
     *
     * @param tenantId tenantId who can access this measurement.
     * @param measurement measurement to query
     * @param queryRequest request payload as shown in the example above.
     * @return
     */
    @RequestMapping(
            value = "/{tenantId}/measurements/{measurement}",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Secured({"ROLE_COMPUTE_DEFAULT"})
    public List<?> queryMeasurement(
            @NotBlank @PathVariable final String tenantId,
            @NotBlank @PathVariable final String measurement,
            @Valid @RequestBody final MeasurementQueryRequest queryRequest) {

        LOGGER.info("find: request received for tenantId [{}]", tenantId);
        LOGGER.debug("Query Request is [{}]", queryRequest);

        List<QueryDomainOutput> out = queryService.queryMeasurement(tenantId, measurement, queryRequest);

        List<Map<String, Object>> response = new ArrayList<>();
        out.forEach(queryResult -> response.addAll(queryResult.getQueryResponse()));

        return response;
    }

    /**
     * Get all of the measurements that belong to given tenantId
     * @param tenantId tenantId who has access to this endpoint
     * @return
     */
    @RequestMapping(
            value = "/{tenantId}/measurements",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Secured({"ROLE_COMPUTE_DEFAULT"})
    public List<?> measurements(@NotBlank @PathVariable final String tenantId) {
        LOGGER.info("measurements: request received for tenantId [{}]", tenantId);

        List<QueryDomainOutput> out = queryService.measurements(tenantId);
        List<String> response = new ArrayList<>();
        out.forEach(queryResult -> response.addAll(queryResult.getValuesAsCollection()));

        return response;
    }

    /**
     * Get all of the tags for given measurement
     * @param tenantId tenantId who has access to this endpoint
     * @param measurement measurement name
     * @return
     */
    @RequestMapping(
            value = "/{tenantId}/measurements/{measurement}/tags",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Secured({"ROLE_COMPUTE_DEFAULT"})
    public List<?> tags(
            @NotBlank @PathVariable final String tenantId,
            @NotBlank @PathVariable final String measurement) {
        LOGGER.info("tags: request received for tenantId [{}], measurement [{}]", tenantId, measurement);

        List<QueryDomainOutput> out = queryService.tags(tenantId, measurement);
        List<String> response = new ArrayList<>();
        out.forEach(queryResult -> response.addAll(queryResult.getValuesAsCollection()));

        return response;
    }

    /**
     * Get all of the values for a given tag in a measurement
     * @param tenantId tenantId who has access to this endpoint
     * @param measurement measurement name
     * @param tagsAsCsv comma separated tag names
     * @return
     */
    @RequestMapping(
            value = "/{tenantId}/measurements/{measurement}/tags/{tagsAsCsv}/values",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Secured({"ROLE_COMPUTE_DEFAULT"})
    public List<?> tagValues(
            @NotBlank @PathVariable final String tenantId,
            @NotBlank @PathVariable final String measurement,
            @NotBlank @PathVariable final String tagsAsCsv) {
        LOGGER.info("tagValues: request received for tenantId [{}], measurement [{}] and tag [{}]",
                tenantId, measurement, tagsAsCsv);

        List<QueryDomainOutput> out = queryService.tagValues(tenantId, measurement, tagsAsCsv);
        List<Map<String, Object>> response = new ArrayList<>();
        out.forEach(queryResult -> response.addAll(queryResult.getQueryResponse()));

        return response;
    }

    /**
     * Get all of the fields for given measurement
     * @param tenantId tenantId who has access to this endpoint
     * @param measurement measurement name
     * @return
     */
    @RequestMapping(
            value = "/{tenantId}/measurements/{measurement}/fields",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Secured({"ROLE_COMPUTE_DEFAULT"})
    public List<?> fields(
            @NotBlank @PathVariable final String tenantId,
            @NotBlank @PathVariable final String measurement) {
        LOGGER.info("fields: request received for tenantId [{}], measurement [{}]", tenantId, measurement);

        List<QueryDomainOutput> out = queryService.fields(tenantId, measurement);
        List<Map<String, Object>> response = new ArrayList<>();
        out.forEach(queryResult -> response.addAll(queryResult.getQueryResponse()));

        return response;
    }

    private QueryDomainInput inputToDomainInput(QueryInput queryInput) {
        QueryDomainInput domainInput = new QueryDomainInput();
        domainInput.setQueryString(queryInput.getQueryString());
        return domainInput;
    }

    @RequestMapping(
            value = "/query",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Secured({"ROLE_COMPUTE_DEFAULT"})
    public QueryResult query(final @RequestParam("db") String dbName, final @RequestParam("q") String queryString){
        return queryService.query(dbName, queryString);
    }
}
