# CREST API Descriptor

[CREST](https://stash.forgerock.org/projects/COMMONS/repos/forgerock-rest) is ForgeRock's
standard library for exposing RESTful web services from a product. This document describes
the description options available for services that are implemented using CREST to describe
themselves to clients such as static documentation generators, live documentation websites
and other tooling.

## Status

This document is a working draft, version `0.1`.

### Revision History

Version   | Description
---------:| -------------------
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

### API Description (top level)

At the top level, the API is described by providing a collection of schema definitions and
a Paths object that describes what paths are available in the application. The top-level must contain at least one
of _definitions_, _errors_, or _paths_.

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| -------------------------------------
id          | frURI                       | ✓          | The identifier of the API Descriptor
definitions | [Schema](#Schemas)          |            | Locally defined schema definitions
description | String                      |            | Short description of the API Descriptor
errors      | [Error](#Error)             |            | Locally defined error definitions
paths       | [Paths](#Paths)             |            | The supported paths for this API.

### Schemas

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
*           | Schema                      | ✓          | The schema definitions

### Reference

Use JSON Reference syntax to refer to schemas defined locally or externally.

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
$ref        | String                      | ✓          | A JSON Reference to the required object. The URI should be an frURI type, or a URL.

### Paths

A path supported by the API being described. Paths may contain the names of Path [Parameter](#Parameter)s, contained
in curly braces. Paths _must_ contain at least one _VersionedPath_ or _Resource_.

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
`*`         | [VersionedPath](#VersionedPath) |        | A mapping of path string to VersionedPath definition.
`*`         | [Resource](#Resource)       |            | Resources mapped to a path, without a version.

### VersionedPath

The optional version of the request handler at the top of a particular path. API Descriptors without the need to
version individual API endpoints may omit this level of the Path hierarchy.

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
`[1-9][0-9]*(\.[1-9][0-9]*)*` | [Resource](#Resource)  | ✓ | The supported versions of the resources at this path.

### Resource

Specifies the descriptor at a given path. At least one _Operation_ (e.g., _create_) is required.

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
resourceSchema | [Schema](#Schema)        |            | The schema of the resource for this path. Required when any of create, read, update, delete, or patch are supported.
create      | [Create](#Create)           |            | The create operation description, if supported
read        | [Operation](#Operation)     |            | The read operation description, if supported
update      | [Update](#Update)           |            | The update operation description, if supported
delete      | [Delete](#Delete)           |            | The delete operation description, if supported
patch       | [Patch](#Patch)             |            | The patch operation description, if supported
actions     | [Action](#Action)[]         |            | The action operation descriptions, if supported
queries     | [Query](#Query)[]           |            | The query operation descriptions, if supported. Resource queries arrays can include up to one query filter operation, one query expression operation, and multiple queries by ID.

### Context

Context contains metadata and/or state-data associated with a CREST API request.

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
name        | String                      | ✓          | The name of the context
schema      | [Schema](#Schema)           | ✓          | The schema for the context
required    | boolean                     |            | Whether the context is required

### Operation

A basic operation type for operations without any special features. This is the supertype of
the supported operations in the CREST contract.

_Read_ operations are defined at this level, and are used to read existing resources.

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
description | String                      |            | A description of the operation
supportedContexts | [Context](#Context)[] |            | The supported contexts
supportedLocales | String[]               |            | [Locale codes](https://en.wikipedia.org/wiki/Language_localisation#Language_tags_and_codes) supported by the operation
fields      | String[]                    |            | Fields selected for inclusion in the response payload. All fields are included by default.
errors      | [Error](#Error)[]           |            | Errors known be returned by this operation
parameters  | [Parameter](#Parameter)[]   |            | Extra parameters supported by the operation
stability   | String                      |            | Stability of the endpoint. One of "internal", "stable" (default), "evolving", "deprecated", or "removed".

### Error

Defines one of the possible error responses that are known to be returned by a given
[Operation](#Operation). All standard CREST errors are defined under _frapi:common_, which is an API Descriptor
that will always be available by default. Endpoints may overload any error _code_ and unique _description_ to define
custom errors. CREST API clients should be prepared to handle undocumented/unexpected errors. It is a best practice to
define a minimum Error array definition, with 500 Internal Server Error, as follows,

```
"errors" : [
    {"$ref" : "frapi:common#/errors/internalServerError"}
]
```

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
code        | Number                      | ✓          | Three digit error code, corresponding to HTTP status codes.
description | String                      | ✓          | Description of what may cause an error to occur.
detailSchema | [Schema](#Schema)          |            | Optional definition of a schema for the error-detail.

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
      "$ref" : "frapi :error : default"
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
type        | String                      | ✓          | The type of the parameter: `string`, `number`, `boolean`, and array variants
defaultValue | `*`                        |            | The default value, if applicable
description | String                      |            | The description of the parameter
source      | String                      | ✓          | Where the parameter comes from. May be: PATH or ADDITIONAL
required    | boolean                     |            | Whether the parameter is required
enum        | String[]                    |            | One or more values that must match
options/enum_titles | String[]            |            | String descriptions in the same order as the enum values.

Other appropriate fields as described in the
[JSON Schema Validation](http://json-schema.org/latest/json-schema-validation.html) spec
may also be used.

### Create

Creates a new resource. Extends [Operation](#Operation).

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
mode        | String                      | ✓          | Values accepted are: `ID_FROM_CLIENT`, `ID_FROM_SERVER`.
singleton   | boolean                     |            | Specifies that create operates on a singleton as opposed to a collection.
mvccSupported | boolean                   | ✓          | Whether this resource supports MVCC create.

### Update

Replaces the contents of an existing resource. Extends [Operation](#Operation).

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
mvccSupported | boolean                   | ✓          | Whether this resource supports MVCC update.

### Delete

Deletes a resource. Extends [Operation](#Operation).

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
mvccSupported | boolean                   | ✓          | Whether this resource supports MVCC update.

### Patch

Partially modifies the contents of an existing resource. Extends [Operation](#Operation).

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
operations  | String[]                    | ✓          | Set of supported patch operations. Supported values are `ADD`, `REMOVE`, `REPLACE`, `INCREMENT`, `MOVE`, `COPY`, `TRANSFORM`.
mvccSupported | boolean                   | ✓          | Whether this resource supports MVCC update.

### Action

Actions are additional operations provided by a resource container. Extends [Operation](#Operation).

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
name        | String                      | ✓          | The action name.
request     | [Schema](#Schema)           |            | The schema of the request payload for this action.
response    | [Schema](#Schema)           | ✓          | The schema of the response payload for this action.

### Query

Search or list the resources contained within a resource container. Extends [Operation](#Operation).

Resource queries arrays can include up to one query filter operation, one query expression operation, and multiple queries by ID.

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
type        | String                      | ✓          | Supported values are `ID`, `FILTER`, `EXPRESSION`.
pagingMode  | String[]                    |            | Supported values are `COOKIE`, `OFFSET`. Paging is not supported if omitted.
countPolicy | String[]                    |            | Supported values are `ESTIMATE`, `EXACT`. Counts are not provided if omitted.
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

Supports either a [Reference](#Reference) to a defined schema, or
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
writePolicy | String                      | Relevant only for "properties" definitions where the readOnly flag is false. Supported values are:<br>WRITE_ON_CREATE: the property MAY be set in the create request, but not thereafter.<br>WRITE_ONCE: the property MAY be set only if the current value is NULL.<br>WRITABLE: the property can be set at any time. This is the default value if no value is specified.
errorOnWritePolicyFailure | boolean       | Whether the application will error (or ignore) when a WRITE_ON_CREATE or WRITE_ONCE field is attempted to be updated erroneously (default: `false`).

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
