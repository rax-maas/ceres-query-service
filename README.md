# Query Service
This service provides functionality to query metrics data.
## API
### Endpoint /{tenantId}/measurements/{measurement}
- Method: `POST`
- Request Payload:
  ```
  {
    "filter": {
        "from": "epoch start time",
        "to": "epoch end time",
        "tags": {
            "tag1": [
                    "tag1-value-1", 
                    "tag1-value-2"
            ],
            "tag2": ["tag2-value"]
        },
        "fields": {
            "field1": [ 
                ">= 0", 
                "< 500" 
            ],
            "field2": ["< 60"],
            "field3": ["> 0"]
        }
    },
    "select": {
        "columns": [
            "column1", 
            "column2", 
            "column3", 
            "column4"
        ]
    }
  }
  ```
  - `filter` node <br />
    This node construct the `WHERE` clause for the query. `from` and `to` are mandatory sub-nodes. `from` defines the start time and `to` defines the end time. Together they create the time window for the metrics search.
    - `tags` sub-node <br />
      This helps you filter measurements for the specified tags. "tag1" and "tag2" are the tags that I am trying to restrict my search on. "tag1" contains two values "tag1-value-1" and "tag1-value-2". **_Here ALL of the values in the array are `OR`ed in the resulting `WHERE` clause. This is the only place where `OR` operation is applied. Rest of the places in the `WHERE` clause, it's always `AND`ed._** "tag2" contains only one value "tag2-value".
    - `fields` sub-node <br />
      This helps you filter measurements for the specified fields. "field1", "field2", and "field3" are the fields that I am restricting my search on. "field1" contains two values. Here, in contrast to what you saw in `tags`, these values are `AND`ed in the resulting `WHERE` clause.
  - `select` node <br />
    This node creates the `SELECT` portion of the query. You can add the columns that you want to see in the array for `columns` key.
    
#### Example
##### Request Payload
```
{
	"filter": {
		"from": "1548301512",
		"to": "1548301623",
		"tags": {
		    "devicelabel": [
					"dummy-device-label-0", 
					"dummy-device-label-3", 
					"dummy-device-label-6"
				],
		    "collectionlabel": ["dummy-collection-label"]
		},
		"fields": {
		  "filesystem_avail": [ 
				">= 0", 
				"< 50000" 
			],
		  "filesystem_free": ["< 60000"],
		  "filesystem_used": ["> 0"]
	  }
	},
	
	"select": {
		"columns": [
			"filesystem_avail", 
			"filesystem_total", 
			"filesystem_free", 
			"entitysystemid", 
			"systemaccountid"
		]
	}
}
```
This results in following `InfluxQL` query: <br />
```
SELECT filesystem_avail,filesystem_total,filesystem_free,entitysystemid,systemaccountid 
FROM "agent_filesystem" 
WHERE (time > 1548301512000000000 AND time < 1548301623000000000) 
  AND (
    (
      "devicelabel" = 'dummy-device-label-0' OR 
      "devicelabel" = 'dummy-device-label-3' OR 
      "devicelabel" = 'dummy-device-label-6'
    ) 
    AND (
      "collectionlabel" = 'dummy-collection-label'
    )
  ) 
  AND ("filesystem_avail" >= 0 AND "filesystem_avail" < 50000) 
  AND ("filesystem_free" < 60000) 
  AND ("filesystem_used" > 0)
```
##### Response for the given example payload
```
[
  {
    "systemaccountid": "dummy-account-id-0",
    "filesystem_avail": 14505.0,
    "filesystem_total": 8839.0,
    "filesystem_free": 8651.0,
    "time": "2019-01-24T03:46:52Z",
    "entitysystemid": "dummy-entity-id-0"
  },
  {
    "systemaccountid": "dummy-account-id-3",
    "filesystem_avail": 22338.0,
    "filesystem_total": 1053.0,
    "filesystem_free": 1352.0,
    "time": "2019-01-24T03:46:53Z",
    "entitysystemid": "dummy-entity-id-3"
  },
  {
    "systemaccountid": "dummy-account-id-6",
    "filesystem_avail": 14259.0,
    "filesystem_total": 39414.0,
    "filesystem_free": 22929.0,
    "time": "2019-01-24T03:46:53Z",
    "entitysystemid": "dummy-entity-id-6"
  }
]
```
### Endpoint /{tenantId}
This endpoint is used for free form query. Use it only when you don't have other option. You need to write your own InfluxQL query to fetch any data. <br />
- Method: `POST`
- Request Payload:
  ```
  {
	"queryString": "select * from agent_filesystem where time > now() - 5d"
  }
  ```
  - `queryString` is obvious. Create your own `InfluxQL` query string and pass it on.

- Response: <br />
  ```
  [
	  {
	    "systemaccountid": "dummy-account-id-0",
	    "filesystem_free_files": 5399.0,
	    "monitoringsystem": "MAAS",
	    "filesystem_total": 8839.0,
	    "filesystem_free_unit": "KILOBYTES",
	    "filesystem_total_unit": "KILOBYTES",
	    "filesystem_used_unit": "KILOBYTES",
	    "devicelabel": "dummy-device-label-0",
	    "filesystem_avail_unit": "KILOBYTES",
	    "filesystem_files": 12393.0,
	    "entitysystemid": "dummy-entity-id-0",
	    "filesystem_used": 32073.0,
	    "collectionlabel": "dummy-collection-label",
	    "filesystem_free_files_unit": "free_files",
	    "filesystem_avail": 14505.0,
	    "filesystem_free": 8651.0,
	    "time": "2019-01-24T03:46:52Z",
	    "filesystem_files_unit": "files"
	  },
	  ...
	  {
	    "systemaccountid": "dummy-account-id-4",
	    "filesystem_free_files": 44985.0,
	    "monitoringsystem": "MAAS",
	    "filesystem_total": 1806.0,
	    "filesystem_free_unit": "KILOBYTES",
	    "filesystem_total_unit": "KILOBYTES",
	    "filesystem_used_unit": "KILOBYTES",
	    "devicelabel": "dummy-device-label-4",
	    "filesystem_avail_unit": "KILOBYTES",
	    "filesystem_files": 23525.0,
	    "entitysystemid": "dummy-entity-id-4",
	    "filesystem_used": 21448.0,
	    "collectionlabel": "dummy-collection-label",
	    "filesystem_free_files_unit": "free_files",
	    "filesystem_avail": 39829.0,
	    "filesystem_free": 4633.0,
	    "time": "2019-01-24T03:46:53Z",
	    "filesystem_files_unit": "files"
	  }
  ]
  ```
### Endpoint /{tenantId}/measurements
This endpoint provides all of the measurements for the given tenantId.
- Method: `GET`
- Response:
  ```
  [
  	"agent_filesystem"
  ]
  ```
### Endpoint /{tenantId}/measurements/{measurement}/tags
This endpoint provides all of the tags in a given measurement.
- Method: `GET`
- Response:
  ```
  [
	  "collectionlabel",
	  "devicelabel",
	  "entitysystemid",
	  "filesystem_avail_unit",
	  "filesystem_files_unit",
	  "filesystem_free_files_unit",
	  "filesystem_free_unit",
	  "filesystem_total_unit",
	  "filesystem_used_unit",
	  "monitoringsystem",
	  "systemaccountid"
  ]
  ```
### Endpoint /{tenantId}/measurements/{measurement}/fields
This endpoint provides all of the fields in a given measurement.
- Method: `GET`
- Response:
  ```
  [
	  {
	    "fieldKey": "filesystem_avail",
	    "fieldType": "float"
	  },
	  {
	    "fieldKey": "filesystem_files",
	    "fieldType": "float"
	  },
	  {
	    "fieldKey": "filesystem_free",
	    "fieldType": "float"
	  },
	  {
	    "fieldKey": "filesystem_free_files",
	    "fieldType": "float"
	  },
	  {
	    "fieldKey": "filesystem_total",
	    "fieldType": "float"
	  },
	  {
	    "fieldKey": "filesystem_used",
	    "fieldType": "float"
	  }
  ]
  ```
### Endpoint /{tenantId}/measurements/{measurement}/tags/tag1,tag2/values
This endpoint provides all of the values for given tags in a given measurement. Notice that you can pass on multiple comma-delimitted tags to get their values. For example: `/tenant1/measurements/agent_filesystem/tags/devicelabel,entitysystemid/values`
- Method: `GET`
- Response:
  ```
  [
	  {
	    "value": "dummy-device-label-0",
	    "key": "devicelabel"
	  },
	  {
	    "value": "dummy-device-label-1",
	    "key": "devicelabel"
	  },
	  ...
	  {
	    "value": "dummy-device-label-9",
	    "key": "devicelabel"
	  },
	  {
	    "value": "dummy-entity-id-0",
	    "key": "entitysystemid"
	  },
	  ...
	  {
	    "value": "dummy-entity-id-4",
	    "key": "entitysystemid"
	  },
	  {
	    "value": "dummy-entity-id-9",
	    "key": "entitysystemid"
	  }
  ]
  ```
## Setup
Install docker. Once done with that, you can use [`test-infrastructure`](https://github.com/racker/ceres-test-infrastructure) repository to install and run `Kafka`, `InfluxDB` and `Redis`. Please follow instruction from that repository to install them. Query service needs `InfluxDB` only. But to support this service we would need `Kafka` as well to get the data into `InfluxDB`. <br />
**WORK-IN-PROGRESS** <br />
Ideally we should have all of these components in `docker-compose`, but for now, we might have to follow a little manual process. <br />
To run or test Query Service locally:
- Get repo `ingestion-service-functional-test` and after building it. 
  - Go to `ingestion-service-functional-test` folder locally
  - Run `java -jar target/kafka-influxdb-functional-test-0.0.1-SNAPSHOT.jar` This will create raw test data into Kafka.
- Run `ingestion-service` with `raw-data-consumer` and `development` profiles
  - command to run `java -Dspring.profiles.active=raw-data-consumer,development -DTEST_KAFKA_BOOTSTRAP_SERVERS=localhost:9092 -jar target/ingestion-service-0.0.1-SNAPSHOT.jar` This should create data in `InfluxDB` and now you should be able to run the `query-service` to fetch data.

