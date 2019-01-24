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
