# 4.1.1

## Changes
* Support the defacto standard for RESTful create requests over HTTP (POST without query parameter):
[CREST-380](https://bugster.forgerock.org/jira/browse/CREST-380)
* Enable annotation-based request handlers to be written, and to be created using the `Resources` class:
[CREST-383](https://bugster.forgerock.org/jira/browse/CREST-383)

# 4.1.0

Mistaken release - no change from 4.0.3.

# 4.0.3

## Changes
* Updated forgerock-bom dependency from 3.0.0 to 4.0.0


# 4.0.2

## Changes
* Requests that require the "Content-Type" header to be set will return a Bad Request error if the 
header is not set
* PUT Requests with a "If-None-Match" header is set with a non-* value will return a Bad Request 
error


# 4.0.1

## Changes
* Updated forgerock-http-framework (CHF) dependency from 2.0.0 to 2.1.0
