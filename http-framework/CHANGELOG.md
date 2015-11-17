# 2.1.0

## Changes

### Minor
* Inbound HTTP requests will have a TransactionIdContext injected into the Context hierarchy
* Introduce new Client send method which takes a Context as an argument

### Bug Fixes
* Update max file upload limit to 1GB
* Improved parsing of single valued headers