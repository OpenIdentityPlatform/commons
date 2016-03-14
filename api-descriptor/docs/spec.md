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
 example: `frapi:openam:/:identities`, which could be used in a JSON Reference:
 `frapi:openam:/:identities#/user/xyz`

## Specification

### API Description (top level)

At the top level, the API is described by providing a collection of schema definitions and
a Paths object that describes what paths are available in the application.

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| -------------------------------------
id          | frURI                       | ✓          | The identifier of the API Descriptor
definitions | [Definition](#Definition)[] |            | Schema definitions
paths       | Paths                       | ✓          | The supported paths for this API.

### Definition

Each API may want one or more identified schemas to be added to the API Descriptor to represent
the structure of each of its requests/responses. This object defines a schema.

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
id          | frURI                       | ✓          | The identifier of the schema
title       | String                      | ✓          | A human-readable name of the schema
description | String                      | ✓          | A description of the schema
schema      | [Schema](#Schema)           | ✓          | The schema for this definition.

### Reference

Refer to something defined elsewhere.

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
$ref        | String                      | ✓          | A JSON Reference to the required object. The URI should be an frURI type, or a URL.

### Paths

A path supported by the API being described. As we support multiple versions of handlers this
specifies the particular handler only with the version number provided in the Path property.

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
`*`         | [Path](#Path)               | ✓          | A mapping of path string to Path definition - the path may contain path parameters

### Path

The version of the request handler at the particular path. Request handlers support multiple
versions therefore at this level we have to specify the version of the handler.

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
`[1-9][0-9]*(\.[1-9][0-9]*)*` | [VersionedResource](#VersionedResource)[] | ✓  | The supported versions of the resources at this path.

### VersionedResource

Specifies the descriptor at this path for a particular API version.

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
resourceSchema | [Schema](#Schema)        |            | The schema of the resource for this path. Required when any of create, read, update, delete, patch are supported.
title       | String                      | ✓          | The human-readable name of the endpoint.
description | String                      | ✓          | A description of the endpoint
create      | [Create](#Create)           |            | The create operation description, if supported
read        | [Operation](#Operation)     |            | The read operation description, if supported
update      | [Update](#Update)           |            | The update operation description, if supported
delete      | [Delete](#Delete)           |            | The delete operation description, if supported
patch       | [Patch](#Patch)             |            | The read operation description, if supported
actions     | [Action](#Action)[]         |            | The update operation description, if supported
queries     | [Query](#Query)[]           |            | The delete operation description, if supported
deprecated  | boolean                     |            | Whether this version of the resource path is deprecated.

### Context

Some contexts will be supported and/or required for a particular operation.

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
name        | String                      | ✓          | The name of the context
schema      | [Schema](#Schema)           | ✓          | The schema for the context
required    | boolean                     |            | Whether the context is required

### Operation

A basic operation type for operations without any special features. This is the supertype of
the supported operations in the CREST contract.

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
supportedContexts | [Context](#Context)[] |            | The supported contexts
supportedLocales | String[]               |            | Locale codes supported by the operation
fields      | String[]                    |            | The fields that can be selected for returning in the response payload
errors      | [Error](#Error)[]           |            | What errors may be returned by this operation
parameters  | [Parameter](#Parameter)[]   |            | Extra parameters supported by the operation

### Error

Defines one of the possible error responses that may be returned by a given
[Operation](#Operation).

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
name        | String                      | ✓          | The name of the error
description | String                      | ✓          | Description of what may cause error to occur.
schema      | [Schema](#Schema)           |            | Optional definition of a schema for the error.

When a schema is not provided, the default is:

```
{
  “_id”: “frapi:error:default”,
  “title”: “Resource error”,
  “schema”: {
    “type”: “object”,
    “required”: [ “code”, “message” ],
    “properties”: {
      “code”: {
        “type”: “integer”,
        “title”: “Code”,
        “description”: “3-digit error code, corresponding to HTTP status codes.”
      },
      “message”: {
        “type”: “string”,
        “title”: “Message”,
        “description”: “Error message.”
      },
      “reason”: {
        “type”: “string”,
        “title”: “Reason”,
        “description”: “Short description corresponding to error code.”
      },
      “detail”: {
        “type”: “string”,
        “title”: “Detail”,
        “description”: “Detailed error message.”
      },
      “cause”: {
        “$ref”: “frapi:error:default”
      }
    }
  }
}
```

### Parameter

Represents an extra parameter for the request, that is specific to the request handler for
this path.

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
name        | String                      | ✓          | The name of the parameter
type        | String                      | ✓          | The type of the parameter: `string`, `number`, `boolean`, and array variants
defaultValue | `*`                        |            | If exists, the default value
description | String                      | ✓          | The description of the parameter
source      | String                      | ✓          | Where the parameter comes from. May be: PATH or ADDITIONAL
required    | boolean                     | ✓          | Whether the parameter is required

Other appropriate fields as described in the
[JSON Schema Validation](http://json-schema.org/latest/json-schema-validation.html) spec
may also be used.

### Create

Additional fields for create requests. Extends [Operation](#Operation).

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
supportedModes | String[]                 | ✓          | Values accepted are: `ASSIGNED_ID`, `PROVIDED_ID`, `PROVIDED_ID_NONE_MATCH`.

### Update

Additional fields for update requests. Extends [Operation](#Operation).

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
mvccSupported | boolean                   | ✓          | Whether this resource supports MVCC update.

### Delete

Additional fields for delete requests. Extends [Operation](#Operation).

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
mvccSupported | boolean                   | ✓          | Whether this resource supports MVCC update.

### Patch

Additional fields for patch requests. Extends [Operation](#Operation).

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
mvccSupported | boolean                   | ✓          | Whether this resource supports MVCC update.

### Action

Additional fields for action requests. Extends [Operation](#Operation).

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
id          | String                      | ✓          | The action ID.
description | String                      |            | Describe this action.
request     | [Schema](#Schema)           |            | The schema of the request payload for this action.
response    | [Schema](#Schema)           | ✓          | The schema of the response payload for this action.

### Query

Additional fields for query requests. Extends [Operation](#Operation).

#### Properties

Key         | Type                        | Required?  | Description
----------- | --------------------------- |:----------:| ------------------------------------
type        | String                      | ✓          | Supported values are `ID`, `FILTER`, `EXPRESSION`.
pagingMode  | String                      | ✓          | Supported values are `COOKIE`, `OFFSET` or `NOT_SUPPORTED`.
supportedPagingCountPolicies | String[]   |            | Supported values are `NONE`, `ESTIMATE`, `EXACT`.
queryId     | String                      |            | Required if `type` is `ID`.
queryableFields | String[]                |            | Required if `type` is `FILTER`. Lists the fields in the `resourceSchema` that can be queried.
description | String                      | ✓          | Describes this query.
supportedSortKeys | String[]              |            | The keys that may be used to sort the filter results. A value of “*” can be used to state that all keys are supported.

### Schema

Specify the schema for something.

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
1. [JSON Schema: core definitions and terminology, draft-00](
http://json-schema.org/latest/json-schema-core.html)
1. [JSON Schema: interactive and non interactive validation, draft-00](
http://json-schema.org/latest/json-schema-validation.html)

