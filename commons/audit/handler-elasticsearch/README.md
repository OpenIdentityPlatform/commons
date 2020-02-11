Elasticsearch AuditEventHandler
===============================

The `ElasticsearchAuditEventHandler` writes audit events to Elasticsearch. It supports Basic Authentication and
SSL/TLS through the Elasticsearch Shield plugin. The handler supports Elasticsearch 2.x. The handler uses the
Elasticsearch [Index Api](https://www.elastic.co/guide/en/elasticsearch/reference/2.0/docs-index_.html), 
[Get Api](https://www.elastic.co/guide/en/elasticsearch/reference/2.0/docs-get.html), 
and [Search Api](https://www.elastic.co/guide/en/elasticsearch/reference/2.0/search.html).

## How to Configure Elasticsearch
1. Download and install Elasticsearch.

2. Create the audit index.

        curl -X POST -H "Content-Type: application/json" -d '{
            "settings" : {},
            "mappings" : {
                "access" : {
                    "_source" : { "enabled" : true },
                    "properties" : {
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
        }' "http://localhost:8080/audit/access?_action=create"
        
This command indexes and creates an access event in the configured Elasticsearch index.

## To Read an Audit Event

Audit events can be read from the Elasticsearch audit event handler with the following command.

        curl -X GET -H "Content-Type: application/json" "http://localhost:8080/audit/access/{some_ID}"
        
This command reads the access audit event with some ID.

## To Query Audit Events

Audit events can be queried from Elasticsearch with the following command.
        
        curl -X GET -H "Content-Type: application/json" "http://localhost:8080/audit/access?_queryFilter=true"
        
This command retrieves the first 10 access audit events from elastic search.

Some notes when using query with Elasticsearch.

* Elasticsearch only returns the first 10 results by default when doing a query. To retrieve all records, you must use
pagination by setting the _pageOffset and _pageSize parameters to get more than the first 10 results.
* For the "contains", "starts with" or "equals" query operations to work on strings, the string parameter must be
declared as not_analyzed.
