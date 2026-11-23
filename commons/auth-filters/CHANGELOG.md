# Unreleased

## Upgrade notes

* Before upgrading, set the registered OpenID Connect client ID as a nonblank `client_id` in every
  OpenIdConnectModule `resolvers` entry, including entries that use `well-known` discovery.
  A missing, blank, or non-string value in any entry prevents the module from initializing.

# 21.0.0
## Changes
* CAF-249 Remove HttpAuthorizationModule and companion classes

# 20.0.0
## Changes

### Bugfix
* CAF-242 Forward Port: Remove cast to RSA key

# 3.1.6

## Changes
* Update to BOM version 4.2.0
* Add support for ESnnn signature algorithms

# 3.1.4

## Changes
* Update to BOM version 4.0.0


# 3.1.3

## Changes
* Update to BOM version 3.0.0


# 3.1.0

## Changes

### Minor
* Include the transaction id of requests in audit log entries