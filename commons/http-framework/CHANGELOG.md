# 3.0.0

## Changes

### Major
* Removed ClientContext localName and remoteHost to prevent DNS lookups

### Bug fixes
* Ensure only one value for the Transaction Id header


# 2.2.0

## Changes

### Minor
* Allow SSL protocols and cipher suites to be set on requests 

### Bug fixes
* Headers.copyAsMultiMapOfStrings will now return a case-innsensitive map


# 2.1.0

## Changes

### Minor
* Inbound HTTP requests will have a TransactionIdContext injected into the Context hierarchy
* Introduce new Client send method which takes a Context as an argument

### Bug Fixes
* Update max file upload limit to 1GB
* Improved parsing of single valued headers