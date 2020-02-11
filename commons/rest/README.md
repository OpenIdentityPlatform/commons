# ForgeRock Commons REST (CREST)

## What is CREST?

CREST stands for "Commons REST". CREST is the collective name given to 1) a
RESTful [protocol](Protocol.md) specified by ForgeRock, 2) a [Java framework](json-resource/README.md) for implementing
CREST applications, and 3) a CHF based [HTTP implementation](json-resource-http/README.md) of the RESTful protocol.

CREST is not a generic framework for implementing any REST API. It is not another
Restlet or JAX-RS. It is also not a complete REST API because it only defines the protocol
for interacting with resources. Specifically, it only defines the operations (verbs) and their parameters, and
does not specify any endpoints nor specific types of resources (users, groups, etc). It is the responsibility of CREST
applications to specify the following:

* the end-points - top-level end-points may be defined using a
  [Router](json-resource/src/main/java/org/forgerock/json/resource/Router.java)
* the schema used for resources passed to and from the CREST endpoints. At some
  point we do expect to define a common schema for common types of resource, such as
  users and groups
* the set of extended "actions" which may be performed against a resource. Only
  the core operations of "create", "delete", "patch", "query", "read", and "update"
  are specified.

The CREST project is split into several modules, of which the following three are most important:

* [json-resource](json-resource/README.md) - Java based framework for implementing CREST applications
* [json-resource-http](json-resource-http/README.md) - HTTP based client / server implementation of CREST using CHF
* [json-resource-examples](json-resource-examples/README.md) - runnable examples illustrating CREST application
development

## The CREST HTTP Protocol

The specification of the various CREST operations and how they map onto the HTTP protocol can be
found [here](Protocol.md).

## Getting started

You can quickly try out a [simple CREST HTTP application](json-resource-examples/src/main/java/org/forgerock/json/resource/http/examples)
using the following commands in a terminal:

```
$ git clone https://stash.forgerock.org/scm/commons/forgerock-commons.git
$ cd forgerock-commons
$ mvn clean:install
$ cd rest/json-resource-examples
$ mvn jetty:run
```

Then in a separate terminal:

```
$ curl -H "Content-Type: application/json" -d '{ "uid" : "bjensen", "firstName" : "Babs", "surname" : "Jensen", "role" : [ "developers", "admins" ] }' 'http://localhost:8080/users?_action=create&_prettyPrint=true'
{
  "uid" : "bjensen",
  "firstName" : "Babs",
  "surname" : "Jensen",
  "role" : [ "developers", "admins" ],
  "_id" : "0",
  "_rev" : "0"
}

$ curl -H "Content-Type: application/json" -d '{ "uid" : "scarter", "firstName" : "Sam", "surname" : "Carter", "role"
: [ "developers" ] }' 'http://localhost:8080/users?_action=create&_prettyPrint=true'
{
  "uid" : "scarter",
  "firstName" : "Sam",
  "surname" : "Carter",
  "role" : [ "developers" ],
  "_id" : "1",
  "_rev" : "0"
}

$ curl 'http://localhost:8080/users?_queryFilter=true&_prettyPrint=true'
{
  "result" : [ {
    "uid" : "bjensen",
    "firstName" : "Babs",
    "surname" : "Jensen",
    "role" : [ "developers", "admins" ],
    "_id" : "0",
    "_rev" : "0"
  }, {
    "uid" : "scarter",
    "firstName" : "Sam",
    "surname" : "Carter",
    "role" : [ "developers" ],
    "_id" : "1",
    "_rev" : "0"
  } ],
  "resultCount" : 2,
  "pagedResultsCookie" : null,
  "totalPagedResultsPolicy" : "NONE",
  "totalPagedResults" : -1,
  "remainingPagedResults" : -1
}
```
* * *

The contents of this file are subject to the terms of the Common Development and
Distribution License (the License). You may not use this file except in compliance with the
License.

You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
specific language governing permission and limitations under the License.

When distributing Covered Software, include this CDDL Header Notice in each file and include
the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
Header, with the fields enclosed by brackets [] replaced by your own identifying
information: "Portions copyright [year] [name of copyright owner]".

Copyright 2015 ForgeRock AS.
