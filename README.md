# Query Service
This service provides functionality to query metrics data. At this moment, it just provides 
query service for raw-data only. Query for rolledup data is not built yet.

## API
### `/query`

* Method: `GET`
* parameters
    * `db` - value for this parameter is the "db_" bucket routed for the tenant,measurement
    * `q` - this parameter contains the query string
    
#### Example
**Call from REST client:**
```
curl --request GET \
  --url 'http://localhost:8080/query?db=db_0&q=select%20*%20from%20MAAS_cpu%20where%20time%20%3C%3D%20now()'
```

##### Response for the given call
```
{
  "results": [
    {
      "series": [
        {
          "name": "MAAS_cpu",
          "tags": null,
          "columns": [
            "time",
            "account",
            "accountId",
            "accountType",
            "checkId",
            "collectionLabel",
            "collectionName",
            "device",
            "deviceLabel",
            "entityId",
            "monitoringSystem",
            "monitoringZone",
            "percentage_idle",
            "percentage_system_usage",
            "percentage_user_usage",
            "tenantId"
          ],
          "values": [
            [
              1574815565000,
              "MAAS-account-name-0",
              "\"dummy-account-id-id-0\"",
              "CORE",
              "\"dummy-check-id-id-0\"",
              "\"cpu-collection-label\"",
              "\"cpu\"",
              "\"id-0\"",
              "\"dummy-device-label-id-0\"",
              "\"dummy-entity-id-id-0\"",
              "\"MAAS\"",
              "\"\"",
              12091.0,
              30076.0,
              8622.0,
              "CORE-MAAS-account-name-0"
            ],
            [
              1574815565000,
              "MAAS-account-name-0",
              "\"dummy-account-id-id-0\"",
              "ENCORE",
              "\"dummy-check-id-id-0\"",
              "\"cpu-collection-label\"",
              "\"cpu\"",
              "\"id-0\"",
              "\"dummy-device-label-id-0\"",
              "\"dummy-entity-id-id-0\"",
              "\"MAAS\"",
              "\"\"",
              10062.0,
              7331.0,
              27967.0,
              "ENCORE-MAAS-account-name-0"
            ],
            [
              1574815565000,
              "MAAS-account-name-0",
              "\"dummy-account-id-id-1\"",
              "CORE",
              "\"dummy-check-id-id-1\"",
              "\"cpu-collection-label\"",
              "\"cpu\"",
              "\"id-1\"",
              "\"dummy-device-label-id-1\"",
              "\"dummy-entity-id-id-1\"",
              "\"MAAS\"",
              "\"\"",
              42874.0,
              12361.0,
              37602.0,
              "CORE-MAAS-account-name-0"
            ]
          ]
        }
      ],
      "error": null
    }
  ],
  "error": null
}
```

### `/v1.0/tenant/{tenantId}/intelligence-format-query/measurements`

* Method: `GET`

### `/v1.0/tenant/{tenantId}/intelligence-format-query/measurement-tags`

* Method: `GET`
* Query parameters
    * `measurement`

### `/v1.0/tenant/{tenantId}/intelligence-format-query/measurement-fields`

* Method: `GET`
* Query parameters
    * `measurement`

### `/v1.0/tenant/{tenantId}/intelligence-format-query/measurement-series-by-time`

* Method: `GET`
* Query parameters
    * `measurement`
    * `begin` : ISO Date Format yyyy-MM-ddThh:mm
    * `end` : ISO Date Format yyyy-MM-ddThh:mm
  
## Setup

Refer to the [ceres-bundle](https://github.com/racker/ceres-bundle) to get the "main" Docker infrastructure containers running. The query service requires access to at least the InfluxDB container with the development profile.

### Prerequisite
- Generate some test data into Kafka by running the `test/data-generator` module [in the test directory of the bundle](https://github.com/racker/ceres-bundle/tree/master/test) by using `mvn spring-boot-:run`
- In the [apps directory of the bundle](https://github.com/racker/ceres-bundle/tree/master/apps) go into the `ingestion-service` and run it using `mvn spring-boot:run`.

Now, you should have some data to play with.
- On terminal window, run command `docker exec -it influxdb influx` to access the `influx-cli`
- Run `use db_0` to use database `db-0`
- Run `select * from MAAS_cpu limit 10` to get some records, so that we can get `tenantId` to query
- When you have some results showing up for the query, look for the column `tenantId` and 
grab one `tenantId` from the rows. For example: `MAAS-account-name-0`

When accessing the `/v1.0/tenant` APIs in a local development environment, you can simulate the behavior of Repose or make the invocation via Repose.

To simulate the behavior of Repose, access the query service directly at port 8080 and pass the
headers:
- `X-Tenant-Id`
- `X-Roles`, where development and test profiles default to allow "compute:default"

To access the tenant APIs via Repose, ensure the `repose` service is started from the `test/instructure` bundle module and access the API via port 8180. The following header needs to be set:
- `X-Auth-Token`: a valid Identity token