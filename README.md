# Query Service
This service provides functionality to query metrics data. At this moment, it just provides 
query service for raw-data only. Query for rolledup data is not built yet.

## API
### Endpoint /query
* Method: `GET`
* parameters
    * `db` - value for this parameter is `tenantId`
    * `q` - this parameter contains the query string
    
#### Example
**Call from REST client:**
`http://localhost:8089/query?db=ENCORE-MAAS-account-name-0&q=select * from MAAS_cpu where time >= now()-8h`

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

### Endpoint /intelligence-format-query
This endpoint is provides nothing but formatted data for the same query result. In `/query` endpoint,
you get the response as if you are getting it from InfluxDB. But, this endpoint wants same data in key-value pair.
 
* Method: `GET`
* parameters
    * `db` - value for this parameter is `tenantId`
    * `q` - this parameter contains the query string

#### Example
**Call from REST client:**
`http://localhost:8089/intelligence-format-query?db=ENCORE-MAAS-account-name-0&q=select * from MAAS_cpu where time >= now()-8h`
##### Response for the given call
  ```
  [
    {
      "percentage_user_usage": 8622.0,
      "accountType": "CORE",
      "monitoringZone": "\"\"",
      "deviceLabel": "\"dummy-device-label-id-0\"",
      "entityId": "\"dummy-entity-id-id-0\"",
      "percentage_system_usage": 30076.0,
      "monitoringSystem": "\"MAAS\"",
      "collectionName": "\"cpu\"",
      "accountId": "\"dummy-account-id-id-0\"",
      "percentage_idle": 12091.0,
      "collectionLabel": "\"cpu-collection-label\"",
      "name": "MAAS_cpu",
      "tenantId": "CORE-MAAS-account-name-0",
      "time": 1574815565000,
      "checkId": "\"dummy-check-id-id-0\"",
      "device": "\"id-0\"",
      "account": "MAAS-account-name-0"
    },
    {
      "percentage_user_usage": 27967.0,
      "accountType": "ENCORE",
      "monitoringZone": "\"\"",
      "deviceLabel": "\"dummy-device-label-id-0\"",
      "entityId": "\"dummy-entity-id-id-0\"",
      "percentage_system_usage": 7331.0,
      "monitoringSystem": "\"MAAS\"",
      "collectionName": "\"cpu\"",
      "accountId": "\"dummy-account-id-id-0\"",
      "percentage_idle": 10062.0,
      "collectionLabel": "\"cpu-collection-label\"",
      "name": "MAAS_cpu",
      "tenantId": "ENCORE-MAAS-account-name-0",
      "time": 1574815565000,
      "checkId": "\"dummy-check-id-id-0\"",
      "device": "\"id-0\"",
      "account": "MAAS-account-name-0"
    },
    {
      "percentage_user_usage": 37602.0,
      "accountType": "CORE",
      "monitoringZone": "\"\"",
      "deviceLabel": "\"dummy-device-label-id-1\"",
      "entityId": "\"dummy-entity-id-id-1\"",
      "percentage_system_usage": 12361.0,
      "monitoringSystem": "\"MAAS\"",
      "collectionName": "\"cpu\"",
      "accountId": "\"dummy-account-id-id-1\"",
      "percentage_idle": 42874.0,
      "collectionLabel": "\"cpu-collection-label\"",
      "name": "MAAS_cpu",
      "tenantId": "CORE-MAAS-account-name-0",
      "time": 1574815565000,
      "checkId": "\"dummy-check-id-id-1\"",
      "device": "\"id-1\"",
      "account": "MAAS-account-name-0"
    },
    {
      "percentage_user_usage": 27207.0,
      "accountType": "ENCORE",
      "monitoringZone": "\"\"",
      "deviceLabel": "\"dummy-device-label-id-1\"",
      "entityId": "\"dummy-entity-id-id-1\"",
      "percentage_system_usage": 49922.0,
      "monitoringSystem": "\"MAAS\"",
      "collectionName": "\"cpu\"",
      "accountId": "\"dummy-account-id-id-1\"",
      "percentage_idle": 33289.0,
      "collectionLabel": "\"cpu-collection-label\"",
      "name": "MAAS_cpu",
      "tenantId": "ENCORE-MAAS-account-name-0",
      "time": 1574815565000,
      "checkId": "\"dummy-check-id-id-1\"",
      "device": "\"id-1\"",
      "account": "MAAS-account-name-0"
    }
  ]
  ```
  
## Setup
Install docker. Once done with that, you can use [`test-infrastructure`](https://github.com/racker/ceres-test-infrastructure) repository to install and run `Kafka`, `InfluxDB` and `Redis`. 
Please follow instruction from that repository to install them. 
Query service needs `InfluxDB` only. But to support this service we would 
need `Kafka` as well to get the data into `InfluxDB`.

### Prerequisite
To run or test Query Service locally:
- Make sure you got `test-infrastructure` repo and you went through repo's docs. This will ensure that you have
`kafka`, `redis` and `InfluxDB` running as docker containers.
- Get repo [`test-data-generator`](https://github.com/racker/ceres-test-data-generator) and after building it. 
  - Go to `test-data-generator` folder locally
  - Run `java -jar target/test-data-generator-0.0.1-SNAPSHOT.jar` This will create raw test data into Kafka.
- Run `ingestion-service` with `raw-data-consumer` and `development` profiles
  - command to run `java -Dspring.profiles.active=raw-data-consumer,development -DTEST_KAFKA_BOOTSTRAP_SERVERS=localhost:9092 -jar target/ingestion-service-0.0.1-SNAPSHOT.jar` This should create data in `InfluxDB` and now you should be able to run the `query-service` to fetch data.

Now, you should have some data to play with.
- On terminal window, run command `docker exec -it influxdb influx` to get to `influx-cli`
- Run `use db_0` to use database `db-0`
- Run `select * from MAAS_cpu limit 10` to get some records, so that we can get `tenantId` to query
- When you have some results showing up for the query, look for the column `tenantId` and 
grab one `tenantId` from the rows. For example: `ENCORE-MAAS-account-name-0`

Now, you can run `query-service` and use any REST client (for example, Insomnia) to play
around with the service.
- Open REST client `Insomnia` (or REST client tool of your choice)
    - use HTTP method `GET`
    - url example: `http://localhost:8089/intelligence-format-query?db=ENCORE-MAAS-account-name-0&q=select * from MAAS_cpu where time >= now()-8h`
  
  