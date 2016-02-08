Elasticsearch AuditEventHandler
===============================

The `ElasticsearchAuditEventHandler` writes audit events to elasticsearch. It supports basic authentication and 
ssl/tls through the elasticsearch shield plugin. The handler supports Elasticsearch 2.x. The handler uses the 
Elasticsearch [Index Api](https://www.elastic.co/guide/en/elasticsearch/reference/2.0/docs-index_.html), 
[Get Api](https://www.elastic.co/guide/en/elasticsearch/reference/2.0/docs-get.html), 
and [Search Api](https://www.elastic.co/guide/en/elasticsearch/reference/2.0/search.html).

## How Configure Elasticsearch
1. Download and install Elasticsearch.

2. Create the audit index.

        curl -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache" -H "Postman-Token: 23bc18a1-e839-5a17-b3c0-5880ac6fc13d" -d '{
            "settings" : {},
            "mappings" : {
                "access" : {
                    "_source" : { "enabled" : true },
                    "properties" : {
                        "_id": {
                            "type": "string",
                            "index": "not_analyzed"
                        },
                        "timestamp": {
                            "type": "date"
                        },
                        "eventName": {
                            "type": "string",
                            "index": "not_analyzed"
                        },
                        "transactionId": {
                            "type": "string",
                            "index": "not_analyzed"
                        },
                        "userId": {
                            "type": "string",
                            "index": "not_analyzed"
                        },
                        "trackingIds": {
                            "type": "string",
                            "index": "not_analyzed"
                        },
                        "server": {
                            "properties": {
                                "ip": {
                                    "type": "string",
                                    "index": "not_analyzed"
                                },
                                "port": {
                                    "type": "integer"
                                }
                            }
                        },
                        "client": {
                            "properties": {
                                "ip": {
                                    "type": "string",
                                    "index": "not_analyzed"
                                },
                                "port": {
                                    "type": "integer"
                                }
                            }
                        },
                        "request": {
                            "properties": {
                                "protocol": {
                                    "type": "string",
                                    "index": "not_analyzed"
                                },
                                "operation": {
                                    "type": "string",
                                    "index": "not_analyzed"
                                },
                                "detail": {
                                    "type" : "nested"
                                }
                            }
                        },
                        "http": {
                            "properties": {
                                "request": {
                                    "properties": {
                                        "secure": {
                                            "type": "boolean"
                                        },
                                        "method": {
                                            "type": "string",
                                            "index": "not_analyzed"
                                        },
                                        "path": {
                                            "type": "string",
                                            "index": "not_analyzed"
                                        },
                                        "queryParameters": {
                                            "type" : "nested"
                                        },
                                        "headers": {
                                            "type" : "nested"
                                        },
                                        "cookies": {
                                            "type" : "nested"
                                        }
                                    }
                                },
                                "response": {
                                    "properties": {
                                        "headers": {
                                            "type" : "nested"
                                        }
                                    }
                                }
                            }
                        },
                        "response": {
                            "properties": {
                                "status": {
                                    "type": "string",
                                    "index": "not_analyzed"
                                },
                                "statusCode": {
                                    "type": "string",
                                    "index": "not_analyzed"
                                },
                                "detail": {
                                    "type": "string",
                                    "index": "not_analyzed"
                                },
                                "elapsedTime": {
                                    "type": "integer"
                                },
                                "elapsedTimeUnits": {
                                    "type": "string",
                                    "index": "not_analyzed"
                                }
                            }
                        }
                    }
                },
                "activity" : {
                    "_source" : { "enabled" : true},
                    "properties" : {
                        "_id": {
                            "type": "string",
                            "index": "not_analyzed"
                        },
                        "timestamp": {
                            "type": "date"
                        },
                        "eventName": {
                            "type": "string",
                            "index": "not_analyzed"
                        },
                        "transactionId": {
                            "type": "string",
                            "index": "not_analyzed"
                        },
                        "userId": {
                            "type": "string",
                            "index": "not_analyzed"
                        },
                        "trackingIds": {
                            "type": "string",
                            "index": "not_analyzed"
                        },
                        "runAs": {
                            "type": "string",
                            "index": "not_analyzed"
                        },
                        "objectId": {
                            "type": "string",
                            "index": "not_analyzed"
                        },
                        "operation": {
                            "type": "string",
                            "index": "not_analyzed"
                        },
                        "before": {
                            "type": "object"
                        },
                        "after": {
                            "type": "object"
                        },
                        "changedFields": {
                            "type": "string",
                            "index": "not_analyzed"
                        },
                        "revision": {
                            "type": "string",
                            "index": "not_analyzed"
                        }
                    }
                },
                "authentication" : {
                    "_source" : { "enabled" : true},
                    "properties" : {
                        "_id": {
                            "type": "string",
                            "index": "not_analyzed"
                        },
                        "timestamp": {
                            "type": "date"
                        },
                        "eventName": {
                            "type": "string",
                            "index": "not_analyzed"
                        },
                        "transactionId": {
                            "type": "string",
                            "index": "not_analyzed"
                        },
                        "userId": {
                            "type": "string",
                            "index": "not_analyzed"
                        },
                        "trackingIds": {
                            "type": "string",
                            "index": "not_analyzed"
                        },
                        "result": {
                            "type": "string",
                            "index": "not_analyzed"
                        },
                        "principal": {
                            "type": "string",
                            "index": "not_analyzed"
                        },
                        "context": {
                            "type": "nested"
                        },
                        "entries": {
                            "properties": {
                                "moduleId": {
                                    "type": "string",
                                    "index": "not_analyzed"
                                },
                                "result": {
                                    "type": "string",
                                    "index": "not_analyzed"
                                },
                                "info": {
                                    "type": "nested"
                                }
                            }
                        }    
                    }
                },
                "config" : {
                    "_source" : { "enabled" : true},
                    "properties" : {
                        "_id": {
                            "type": "string",
                            "index": "not_analyzed"
                        },
                        "timestamp": {
                            "type": "date"
                        },
                        "eventName": {
                            "type": "string",
                            "index": "not_analyzed"
                        },
                        "transactionId": {
                            "type": "string",
                            "index": "not_analyzed"
                        },
                        "userId": {
                            "type": "string",
                            "index": "not_analyzed"
                        },
                        "trackingIds": {
                            "type": "string",
                            "index": "not_analyzed"
                        },
                        "runAs": {
                            "type": "string",
                            "index": "not_analyzed"
                        },
                        "objectId": {
                            "type": "string",
                            "index": "not_analyzed"
                        },
                        "operation": {
                            "type": "string",
                            "index": "not_analyzed"
                        },
                        "before": {
                            "type": "object"
                        },
                        "after": {
                            "type": "object"
                        },
                        "changedFields": {
                            "type": "string",
                            "index": "not_analyzed"
                        },
                        "revision": {
                            "type": "string",
                            "index": "not_analyzed"
                        }    
                    }
                }
            }
        }' "http://localhost:9200/audit"
        
The Elasticsearch is now setup to receive audit data.

## How Configure ElasticsearchAuditEventHandler

To configure the `ElasticsearchAuditEventHandler` in the demo servlet project the configuration stored in the file 
forgerock-audit-servlet/src/main/resources/conf/audit-event-handlers.json will need to be edited based off the 
Elasticsearch environment. This is the example configuration stored in that file.

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
                  "enabled" : false,
                  "maxSize" : 10000,
                  "writeInterval" : "250 millis",
                  "maxBatchedEvents" : 500
                }
              }
        }
        
## Index/Create Audit Event
Audit Events can be indexed or created in Elasticsearch using the following command.

        curl -X POST -H "Content-Type: application/json" -d '{
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
        }' "http://localhost:8080/audit/access/id"
        
That command will index/create an access event with the id "id" in the configured Elasticsearch index.

## Read an Audit Event

Audit events can be read from the Elasticsearch audit event handler with the following command.

        curl -X GET -H "Content-Type: application/json" -H "Cache-Control: no-cache" "http://localhost:8080/audit/access/id"
        
That command will read the access audit event with the id "id".

## Query Audit Events

Audit events can be queried from Elasticsearch with the following command.
        
        curl -X GET -H "Content-Type: application/json" "http://localhost:8080/audit/access?_queryFilter=true"
        
That command will retrieve the first 10 access audit events from elastic search.

Some notes when using query with Elasticsearch.

* Elasticsearch by default only returns the first 10 results when doing a query. To retrieve each record it is 
necessary to use pagination by setting the _pageOffset and _pageSize parameters get more than the first 10 results.
* For the contains, starts with or equals query operations to work on strings the string parameter much be declared as 
not_analyzed.

