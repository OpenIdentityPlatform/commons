Elasticsearch AuditEventHandler
===============================

The `ElasticsearchAuditEventHandler` writes audit events to Elasticsearch. It supports Basic Authentication and
SSL/TLS through the Elasticsearch Shield plugin. The handler supports Elasticsearch 7.x. The handler uses the
Elasticsearch [Index Api](https://www.elastic.co/guide/en/elasticsearch/reference/7.x/rest-apis.html), 
[Get Api](https://www.elastic.co/guide/en/elasticsearch/reference/7.x/docs-get.html), 
and [Search Api](https://www.elastic.co/guide/en/elasticsearch/reference/7.x/search.html).

## About this version

This handler-elasticsearch plugin version allows integration with ElasticSearch 7.
The main difference about ElasticSearch 7 and previous versions is [removal of mapping types](https://www.elastic.co/guide/en/elasticsearch/reference/7.x/removal-of-types.html).

This upgrade split previous unique index into four indices:

* audit_access
* audit_activity
* audit_authenticate
* audit_config

Index names start with `audit_` prefix, but you can change the prefix assigning a new value to the index name (that works in this version as a prefix).


## How to Configure Elasticsearch

1. Download and install Elasticsearch.

2. Create the audit_access index.

```
curl -X PUT -H "Content-Type: application/json" -d '{
    "settings":{

    },
    "mappings":{
        "_source":{
            "enabled":true
        },
        "properties":{
            "realm":{
                "type":"keyword"
            },
            "component":{
                "type":"keyword"
            },
            "timestamp":{
                "type":"date"
            },
            "eventName":{
                "type":"keyword"
            },
            "transactionId":{
                "type":"keyword"
            },
            "userId":{
                "type":"keyword"
            },
            "trackingIds":{
                "type":"keyword"
            },
            "server":{
                "properties":{
                    "ip":{
                        "type":"keyword"
                    },
                    "port":{
                        "type":"integer"
                    }
                }
            },
            "client":{
                "properties":{
                    "ip":{
                        "type":"keyword"
                    },
                    "port":{
                        "type":"integer"
                    }
                }
            },
            "request":{
                "properties":{
                    "protocol":{
                        "type":"keyword"
                    },
                    "operation":{
                        "type":"keyword"
                    },
                    "detail":{
                        "type":"nested"
                    }
                }
            },
            "http":{
                "properties":{
                    "request":{
                        "properties":{
                            "secure":{
                                "type":"boolean"
                            },
                            "method":{
                                "type":"keyword"
                            },
                            "path":{
                                "type":"keyword"
                            },
                            "queryParameters":{
                                "type":"nested"
                            },
                            "headers":{
                                "type":"nested"
                            },
                            "cookies":{
                                "type":"nested"
                            }
                        }
                    },
                    "response":{
                        "properties":{
                            "headers":{
                                "type":"nested"
                            }
                        }
                    }
                }
            },
            "response":{
                "properties":{
                    "status":{
                        "type":"keyword"
                    },
                    "statusCode":{
                        "type":"keyword"
                    },
                    "detail":{
                        "type":"object"
                    },
                    "elapsedTime":{
                        "type":"integer"
                    },
                    "elapsedTimeUnits":{
                        "type":"keyword"
                    }
                }
            }
        }
    }
}' "http://localhost:9200/audit_access"
```

3. Create the audit_activity index.

```
curl -X PUT -H "Content-Type: application/json" -d '{
    "settings":{

    },
    "mappings":{
        "_source":{
            "enabled":true
        },
        "properties":{
            "realm":{
                "type":"keyword"
            },
            "component":{
                "type":"keyword"
            },
            "timestamp":{
                "type":"date"
            },
            "eventName":{
                "type":"keyword"
            },
            "transactionId":{
                "type":"keyword"
            },
            "userId":{
                "type":"keyword"
            },
            "trackingIds":{
                "type":"keyword"
            },
            "runAs":{
                "type":"keyword"
            },
            "objectId":{
                "type":"keyword"
            },
            "operation":{
                "type":"keyword"
            },
            "before":{
                "type":"object"
            },
            "after":{
                "type":"object"
            },
            "changedFields":{
                "type":"keyword"
            },
            "revision":{
                "type":"keyword"
            }
        }
    }
}' "http://localhost:9200/audit_activity"
```

3. Create the audit_authentication index.

```
curl -X PUT -H "Content-Type: application/json" -d '{
    "settings":{

    },
    "mappings":{
        "_source":{
            "enabled":true
        },
        "properties":{
            "realm":{
                "type":"keyword"
            },
            "component":{
                "type":"keyword"
            },
            "timestamp":{
                "type":"date"
            },
            "eventName":{
                "type":"keyword"
            },
            "transactionId":{
                "type":"keyword"
            },
            "userId":{
                "type":"keyword"
            },
            "trackingIds":{
                "type":"keyword"
            },
            "result":{
                "type":"keyword"
            },
            "principal":{
                "type":"keyword"
            },
            "context":{
                "type":"nested"
            },
            "entries":{
                "properties":{
                    "moduleId":{
                        "type":"keyword"
                    },
                    "result":{
                        "type":"keyword"
                    },
                    "info":{
                        "type":"nested"
                    }
                }
            }
        }
    }
}' "http://localhost:9200/audit_authentication"
```

4.  Create the audit_config index.

```
curl -X PUT -H "Content-Type: application/json" -d '{
    "settings":{

    },
    "mappings":{
        "_source":{
            "enabled":true
        },
        "properties":{
            "realm":{
                "type":"keyword"
            },
            "component":{
                "type":"keyword"
            },
            "timestamp":{
                "type":"date"
            },
            "eventName":{
                "type":"keyword"
            },
            "transactionId":{
                "type":"keyword"
            },
            "userId":{
                "type":"keyword"
            },
            "trackingIds":{
                "type":"keyword"
            },
            "runAs":{
                "type":"keyword"
            },
            "objectId":{
                "type":"keyword"
            },
            "operation":{
                "type":"keyword"
            },
            "before":{
                "type":"object"
            },
            "after":{
                "type":"object"
            },
            "changedFields":{
                "type":"keyword"
            },
            "revision":{
                "type":"keyword"
            }
        }
    }
}' "http://localhost:9200/audit_config"
```
        
Elasticsearch is now setup to receive audit data.

## How to Configure ElasticsearchAuditEventHandler

To configure the `ElasticsearchAuditEventHandler` in the demo servlet project, edit the configuration in the file
forgerock-audit-servlet/src/main/resources/conf/audit-event-handlers.json, based on your
Elasticsearch environment. The example configuration in that file is as follows:

        {
              "class" : "org.forgerock.audit.handlers.elasticsearch.ElasticsearchAuditEventHandler",
              "config" : {
                "name" : "elasticsearch",
                "topics": [ "access", "activity", "config", "authentication" ],
                "connection" : {
                  "useSSL" : false,
                  "host" : "localhost",
                  "port" : 9200,
                  "username" : "myUsername",
                  "password" : "myPassword"
                },
                "indexMapping" : {
                  "indexName" : "audit"
                },
                "buffering" : {
                  "enabled" : true,
                  "maxSize" : 10000,
                  "writeInterval" : "250 millis",
                  "maxBatchedEvents" : 500
                }
              }
        }
        
## Create or Index Audit Events
Audit Events can be indexed or created in Elasticsearch using the following command:

        curl -X PUT -H "Content-Type: application/json" -d '{
            "timestamp": "2016-01-27T17:04:18+00:00",
            "eventName": "eventName",
            "transactionId": "transactionId",
            "userId": "userId",
            "trackingIds": ["trackingIds", "trackingIds2"],
            "server": {
                "ip": "127.0.0.1",
                "port": 8080
            },
            "client": {
                "ip": "127.0.0.1",
                "port": 8080
            },
            "request": {
                "protocol": "http",
                "operation": "create",
                "detail": {
                    "details" : "details"
                }
            },
            "http": {
                "request": {
                    "secure": false,
                    "method": "POST",
                    "path": "/audit",
                    "queryParameters": {
                        "param1" : "value1"
                    },
                    "headers": {
                        "header1" : "headerValue1"
                    },
                    "cookies": {
                        "cookie1" : "cookieValue1"
                    }
                },
                "response": {
                    "headers": {
                        "header1" : "headerValue1"
                    }
                }
            },
            "response": {
                "status": "SUCCESS",
                "statusCode": "200",
                "detail": "details",
                "elapsedTime": 100,
                "elapsedTimeUnits": "MS"
            }
        }' "http://localhost:8080/audit_access?_action=create"
        
This command indexes and creates an access event in the configured Elasticsearch index.

## To Read an Audit Event

Audit events can be read from the Elasticsearch audit event handler with the following command.

        curl -X GET -H "Content-Type: application/json" "http://localhost:8080/audit_access/{some_ID}"
        
This command reads the access audit event with some ID.

## To Query Audit Events

Audit events can be queried from Elasticsearch with the following command.
        
        curl -X GET -H "Content-Type: application/json" "http://localhost:8080/audit_access?_queryFilter=true"
        
This command retrieves the first 10 access audit events from elastic search.

Some notes when using query with Elasticsearch.

* Elasticsearch only returns the first 10 results by default when doing a query. To retrieve all records, you must use
pagination by setting the _pageOffset and _pageSize parameters to get more than the first 10 results.
* For the "contains", "starts with" or "equals" query operations to work on strings, the string parameter must be
declared as not_analyzed.
