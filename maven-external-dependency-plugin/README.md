# maven-external-dependency-plugin
[![Build Status](https://travis-ci.org/openam-org-ru/maven-external-dependency-plugin.svg)](https://travis-ci.org/openam-org-ru/maven-external-dependency-plugin)

Forked from https://code.google.com/archive/p/maven-external-dependency-plugin/

## Download retry configuration

To make external artifact downloads resilient to transient network errors
(e.g. `java.net.ConnectException: Connection timed out`), the
`resolve-external` goal retries failed downloads.

Mojo-level configuration parameters (apply to every `<artifactItem>` unless
overridden on the artifact itself):

| Parameter               | Default | Description                                                  |
|-------------------------|---------|--------------------------------------------------------------|
| `downloadRetryAttempts` | `5`     | Number of attempts before giving up on a single download.    |
| `downloadTimeout`       | `10000` | Per-attempt connection/transfer timeout in milliseconds.     |
| `downloadRetryDelay`    | `2000`  | Delay in milliseconds between retry attempts.                |

Per-artifact overrides (defined inside `<artifactItem>`): `timeout`,
`retryAttempts`, `retryDelay`. When set, they take precedence over the
Mojo-level defaults.

Authorization failures are not retried; the build fails immediately on
`AuthorizationException`.
