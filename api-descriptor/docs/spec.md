# CREST API Descriptor

[CREST](https://stash.forgerock.org/projects/COMMONS/repos/forgerock-rest) is ForgeRock's
standard library for exposing RESTful web services from a product. This document describes
the description options available for services that are implemented using CREST to describe
themselves to clients such as static documentation generators, live documentation websites
and other tooling.

## Status

This document is a working draft, version `0.2`.

### Revision History

Version   | Description
---------:| -------------------
0.2       | Adds: ability to specify common resources in `services`, collection semantics using `items`, parent-child relations using `subresources`, complex read states, complex field validation policies, and binary field support.
0.1       | Initial version.

## Conventions

The key words "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT",
"RECOMMENDED", "MAY", and "OPTIONAL" in this document are to be interpreted as described
in RFC 2119.

## Types

In this document the following type names are used:

* frURI - A ForgeRock API Descriptor URI. These start with the `frapi:` scheme, and are
 followed by colon-separated heirarchical component IDs to make a unique identifier. For
 example: `frapi:openam:identities`, which could be used in a JSON Reference:
 `frapi:openam:/:identities#/definitions/xyz`

## Specification

### ApiDescription (top level)

At the top level, the API is described by providing a collection of schema definitions and
a Paths object that describes what paths are available in the application. The top-level must contain at least one
of _definitions_, _errors_, or _paths_.

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| -------------------------------------
id          | String                      | ✓          | The frURI identifier of the API Descriptor
version     | String                      | ✓          | The version of the API.
description | String                      |            | Short description of the API Descriptor
definitions | [Definitions](#definitions) |            | Locally defined schema definitions
services    | [Services](#services)       |            | Locally defined service definitions
errors      | [Errors](#errors)           |            | Locally defined error definitions
paths       | [Paths](#paths)             |            | The supported paths for this API.

### Definitions

Locally defined schema definitions, that can be referred to via JSON References.

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
*           | [Schema](#schema)           | ✓          | The schema definitions

### Services

Locally defined service definitions, that can be referred to via JSON References.

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
*           | [Resource](#resource)       | ✓          | The service definitions

### Errors

Locally defined schema definitions, that can be referred to via JSON References.

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
*           | [ApiError](#apiError)       | ✓          | The error definitions

### Reference

Use JSON Reference syntax to refer to schemas defined locally or externally.

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
$ref        | String                      | ✓          | A JSON Reference ($ref) to the required object. The URI should be an frURI type, or a URL.

### Paths

A path supported by the API being described. Paths may contain the names of Path [Parameter](#parameter)s, contained
in curly braces. Paths _must_ contain at least one _VersionedPath_.

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
`*`         | [VersionedPath](#versionedPath) |        | A mapping of path string to VersionedPath definition.

### VersionedPath

The optional version of the request handler at the top of a particular path. API Descriptors without the need to
version individual API endpoints may omit this level of the Path hierarchy. The resource may be specified using a
[Reference](#reference).

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
`*`         | [Resource](#resource)       | ✓          | The supported versions of the resources at this path. Format: `[1-9][0-9]*(\.[1-9][0-9]*)*`

### Resource

Specifies the descriptor at a given path. At least one _Operation_ (e.g., _create_) is required.

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
title       | String                      |            | Service title used for documentation purposes
description | String                      |            | Service description used for documentation purposes
resourceSchema | [Schema](#schema)        |            | The schema of the resource for this path. Required when any of create, read, update, delete, or patch are supported.
create      | [Create](#create)           |            | The create operation description, if supported
read        | [Read](#read)               |            | The read operation description, if supported
update      | [Update](#update)           |            | The update operation description, if supported
delete      | [Delete](#delete)           |            | The delete operation description, if supported
patch       | [Patch](#patch)             |            | The patch operation description, if supported
actions     | [Action](#action)[]         |            | The action operation descriptions, if supported
queries     | [Query](#query)[]           |            | The query operation descriptions, if supported. Resource queries arrays can include up to one query filter operation, one query expression operation, and multiple queries by ID.
subresources | [SubResources](#subresources) |         | Sub-resources of this resource. Sub-resources use the same version as their parent resource, so are not separately versioned.
items       | [Items](#items)             |            | The items description in the resource. Used if the resource is a collection.
mvccSupported | boolean                   | ✓          | Whether this resource supports MVCC create.
parameters  | [Parameter](#parameter)[]   |            | Extra parameters supported by the resource

### Items

Descriptor for collection operations. At least one _Operation_ (e.g., _create_) is required.

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
title       | String                      |            | Service title used for documentation purposes
description | String                      |            | Service description used for documentation purposes
create      | [Create](#create)           |            | The create operation description, if supported
read        | [Read](#read)               |            | The read operation description, if supported
update      | [Update](#update)           |            | The update operation description, if supported
delete      | [Delete](#delete)           |            | The delete operation description, if supported
patch       | [Patch](#patch)             |            | The patch operation description, if supported
actions     | [Action](#action)[]         |            | The action operation descriptions, if supported
parameters  | [Parameter](#parameter)[]   |            | Extra parameters supported by the resource


### SubResources

Sub-resources are resources that are a component part of their parent. As such, they share the version of the parent
from the parent's path binding. If a sub-path is separately versioned from a parent path, it should be listed as a
separate path in the [Paths](#paths) object, rather than as a sub-resource of the resource at the parent path.

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
`*`         | [Resource](#resource)       | ✓          | A map of sub-resource paths to resource definitions.

### Operation

A basic operation type for operations without any special features. This is the supertype of
the supported operations in the CREST contract.

_Read_ operations are defined at this level, and are used to read existing resources.

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
description | String                      |            | A description of the operation
supportedLocales | String[]               |            | [Locale codes](https://en.wikipedia.org/wiki/Language_localisation#Language_tags_and_codes) supported by the operation
errors      | [ApiError](#apiError)[]     |            | Errors known be returned by this operation
parameters  | [Parameter](#parameter)[]   |            | Extra parameters supported by the operation
stability   | String                      |            | Stability of the endpoint. Supported values are: "internal", "stable" (default), "evolving", "deprecated", or "removed".

### ApiError

Defines one of the possible error responses that are known to be returned by a given
[Operation](#operation). All standard CREST errors are defined under _frapi:common_, which is an API Descriptor
that will always be available by default. Endpoints may overload any error _code_ and unique _description_ to define
custom errors. CREST API clients should be prepared to handle undocumented/unexpected errors. It is a best practice to
define a minimum ApiError array definition, with 500 Internal Server Error, as follows,

```
"errors" : [
    {"$ref" : "frapi:common#/errors/internalServerError"}
]
```

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
code        | Integer                     | ✓          | Three digit error code, corresponding to HTTP status codes.
description | String                      | ✓          | Description of what may cause an error to occur.
schema      | [Schema](#schema)           |            | Optional definition of a schema for the error-detail.

The schema for an error response is:

```
{
  "type" : "object",
  "required" : [ "code", "message" ],
  "properties" : {
    "code" : {
      "type" : "integer",
      "title" : "Code",
      "description" : "3-digit error code, corresponding to HTTP status codes."
    },
    "message" : {
      "type" : "string",
      "title" : "Message",
      "description" : "Error message."
    },
    "reason" : {
      "type" : "string",
      "title" : "Reason",
      "description" : "Short description corresponding to error code."
    },
    "detail" : {
      "type" : "string",
      "title" : "Detail",
      "description" : "Detailed error message."
    },
    "cause" : {
      "$ref" : "frapi:error:default"
    }
  }
}
```

### Parameter

Defines an _ADDITIONAL_ parameter for an operation, separate from a _resourceSchema_ request-payload,
or a _PATH_ parameter, surrounded by curly braces in the Path.

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
name        | String                      | ✓          | The name of the parameter
type        | String                      | ✓          | The semantics/format of the parameter: `string`, `number`, `boolean`, and array variants
defaultValue | String                     |            | The default value, if applicable
description | String                      |            | The description of the parameter
source      | String                      | ✓          | Where the parameter comes from. Supported values are: PATH or ADDITIONAL
required    | boolean                     |            | Whether the parameter is required
enumValues  | String[]                    |            | One or more values that must match
enumTitles  | String[]                    |            | `options/enum_titles` - string descriptions in the same order as the enum values.

Other appropriate fields as described in the
[JSON Schema Validation](http://json-schema.org/latest/json-schema-validation.html) spec
may also be used.

### Create

Creates a new resource. Extends [Operation](#operation).

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
mode        | String                      | ✓          | Supported values are: `ID_FROM_CLIENT`, `ID_FROM_SERVER`.
singleton   | boolean                     |            | Specifies that create operates on a singleton as opposed to a collection.

### Read

Reads the contents of an existing resource. Extends [Operation](#operation).

#### Properties

No additional properties.

### Update

Replaces the contents of an existing resource. Extends [Operation](#operation).

#### Properties

No additional properties.

### Delete

Deletes a resource. Extends [Operation](#operation).

#### Properties

No additional properties.

### Patch

Partially modifies the contents of an existing resource. Extends [Operation](#operation).

The content of the request-payload is a JSON array whose elements are the modifications which should be applied to the
resource. For example,

```
[
  {
    "operation" : "add",
    "field" : "roles",
    "value" : "admin"
  },
  {
    "operation" : "remove",
    "field" : "field/subfield",
    "value" : "valueToBeRemoved"
  }
]
```
The Schema for the Patch request-payload is the following, but note that the semantics differ based on the
_resourceSchema_ and Patch-operation being performed:

```
{
   "type":"array",
   "items":{
      "type":"object",
      "properties":{
         "operation":{
            "type":"string",
            "enum":[
               "add",
               "remove",
               "replace",
               "increment",
               "move",
               "copy",
               "transform"
            ],
            "required":true
         },
         "field":{
            "type":"string"
         },
         "from":{
            "type":"string"
         },
         "value":{
            "type":"string"
         }
      }
   }
}
```

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
operations  | String[]                    | ✓          | Set of supported patch operations. Supported values are:`ADD`, `REMOVE`, `REPLACE`, `INCREMENT`, `MOVE`, `COPY`, `TRANSFORM`.

### Action

Actions are additional operations provided by a resource container. Extends [Operation](#operation).

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
name        | String                      | ✓          | The action name.
request     | [Schema](#schema)           |            | The schema of the request payload for this action.
response    | [Schema](#schema)           | ✓          | The schema of the response payload for this action.

### Query

Search or list the resources contained within a resource container. Extends [Operation](#operation).

Resource queries arrays can include up to one query filter operation, one query expression operation, and multiple queries by ID.

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
type        | String                      | ✓          | Supported values are:`ID`, `FILTER`, `EXPRESSION`.
pagingMode  | String[]                    |            | Supported values are:`COOKIE`, `OFFSET`. Paging is not supported if omitted.
countPolicy | String[]                    |            | Supported values are:`ESTIMATE`, `EXACT`, `NONE`. Counts are not provided if omitted.
queryId     | String                      | `type:ID`  | Required if `type` is `ID`.
queryableFields | String[]                | `type:FILTER` | Required if `type` is `FILTER`. Lists the fields in the `resourceSchema` that can be queried. A value of “*” can be used to state that all fields can be queried.
supportedSortKeys | String[]              |            | The keys that may be used to sort the filter results. A value of “*” can be used to state that all keys are supported.

The following example shows a resource that supports all three types of query:

```
"paths": {
  "/openidm/managed/user": {
    "queries": [{
      "type": "EXPRESSION",
      "description": "Return the results of a SQL query. For example, `_queryExpression=select+%2A+from+managed_user`"
    }, {
      "type": "FILTER",
      "description": "Return resources matching the filter. TODO get explanation of query filters from Javadoc.",
      "queryableFields": ["*"]
    }, {
      "type": "ID",
      "queryId": "query-all-ids"
      "description": "Return all resources.",
    }]
  }
}
```

### Schema

CREST API Descriptors use schemas to represent request/response-payloads and error responses.

Supports either a [Reference](#reference) to a defined schema, or
[OpenAPI-extended JSON schema](https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md#schemaObject)
with the following additional extensions:

#### Ordering of fields

In order to specify the order that fields in an object ought to be displayed in a UI, an
extra field can be added to properties definitions:

##### Properties

Key         | Type                        | Description
----------- | --------------------------- | ------------------------------------
propertyOrder | number                    | The number that is specified can be used to compare the order of different properties. See also https://github.com/jdorn/json-editor/issues/110

#### Complex read/write states

To handle complex states we are going to introduce a new schema field.

##### Properties

Key         | Type                        | Description
----------- | --------------------------- | ------------------------------------
readPolicy  | String                      | Supported values are:<br>USER: visible in the user-interface. This is the default value if no value is specified.<br>CLIENT: hidden from user-interface, but readable via client APIs.<br>SERVER: available internally, but not exposed to client APIs.
writePolicy | String                      | Relevant only for "properties" definitions where the readOnly flag is false. Supported values are:<br>WRITE_ON_CREATE: the property MAY be set in the create request, but not thereafter.<br>WRITE_ONCE: the property MAY be set only if the current value is NULL.<br>WRITABLE: the property can be set at any time. This is the default value if no value is specified.
errorOnWritePolicyFailure | boolean       | Whether the application will error (or ignore) when a WRITE_ON_CREATE or WRITE_ONCE field is attempted to be updated erroneously (default: `false`).
returnOnDemand | boolean                  | `true` when a field is available, but must be explicitly requested, or `false` (default) when always returned.

#### Enumeration value descriptions

Enum titles are not supported officially in JSON enum type. The representation that we
already use in existing projects comes from
[JSON Editor](https://github.com/jdorn/json-editor/issues/240#issuecomment-53990339).

##### Properties

Key Path    | Type                        | Description
----------- | --------------------------- | ------------------------------------
options/enum_titles | String[]            | String descriptions in the same order as the enum values.

##### Example

```
{
  "type": "string",
  "enum": ["value1","value2"],
  "options": {
    "enum_titles": ["title1","title2"]
  }
}
```


## References

1. [OpenAPI Specification, v2.0](
https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md)
1. [JSON Reference, draft-03](
http://tools.ietf.org/html/draft-pbryan-zyp-json-ref-03)
1. [JSON Schema: core definitions and terminology, draft-00](
http://json-schema.org/latest/json-schema-core.html)
1. [JSON Schema: interactive and non interactive validation, draft-00](
http://json-schema.org/latest/json-schema-validation.html)

***

Copyright 2016 ForgeRock AS.
