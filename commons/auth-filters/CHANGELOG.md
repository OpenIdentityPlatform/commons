# Unreleased

## Upgrade notes

* Before upgrading, set the registered OpenID Connect client ID as a nonblank `client_id` in every
  OpenIdConnectModule `resolvers` entry, including entries that use `well-known` discovery.
  A missing, blank, or non-string value in any entry prevents the module from initializing.

## IWA Kerberos and SPNEGO configuration

IWA now uses keytab-backed acceptor credentials for inbound Kerberos and
SPNEGO service tickets.

- IWA ignores `kerberosServerName`. Set `kerberosRealm` when `servicePrincipal`
  lacks a realm. If your deployment relied on IWA to configure
  Kerberos for the JVM, move those settings to `krb5.conf` or set both
  `java.security.krb5.realm` and `java.security.krb5.kdc` as JVM properties.
- Set `servicePrincipal` to a specific service principal and point
  `keytabFileName` to an existing keytab that contains its keys and is readable
  by the JVM. IWA rejects the wildcard principal `*`.

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