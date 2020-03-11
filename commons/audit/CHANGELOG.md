# 6.2.0

## Changes

### Bug Fixes
* CAUD-331: Disabled handlers should not utilize services until enabled.
* CAUD-332: Tibco support for JMS Audit Handler: use JMS 2.0 dependency, and address JNDI lookup classpath issues.
* CAUD-333: NPE when connection wasn't established with unhelpful error messages.
* CAUD-335: Repair unstable JmsAuditEventHandlerTest and RotatableWriterTest.

# 6.1.0

## Changes

### Bug Fixes
* CAUD-328: Small delay needed to simulated message send in batch tests for JMS.  Improved test stability.

# 6.0.0

## Changes

### Major
* CAUD-320 Declare whether a handler can be used for queries in the configuration class instead of the handler class

### Bug Fixes
* CAUD-328 Utilize context classloader passed to JndiJmsContextManager for OSGi compatibility, revert to JMS 1.1 api.

# 5.1.0

## Changes

### Major
* CAUD-313 Added JMS audit event handler.

### Bug Fixes
* CAUD-325 Change type of response detail

# 5.0.0

## Changes

### Major
* CAUD-320 Audit event handlers need to supply whether they support query

### Minor
* CAUD-305 Elasticsearch audit event handler now uses promises in a non blocking way

### Bug Fixes
* CAUD-311 Fix an error in the logic of DatabaseWriterTask.run()

# 4.3.0

## Changes

### Bug Fixes
* CAUD-310 Elasticsearch event handler now builds a HTTP client if none is provided
* CAUD-312 Add a null check for boolean parameter
* CAUD-309 Fix errors in the elasticsearch README
* CAUD-308 Report batch index failures for Elasticsearch Audit Event Handler,
  and refactor so that ElasticsearchBatchIndexer catches/logs errors thrown by 
  ElasticsearchBatchAuditEventHandler implementation
* CAUD-306 Fix minor spelling errors, mistakes in javadocs, and incorrect exception 
  messages found in the Elasticsearch Event Handler
* CAUD-304 Elasticsearch Handler: replace use of JsonValue.toString()

# 4.2.0

## Changes

### Major
* CAUD-303 Added Elasticsearch audit event handler

# 4.1.1

## Changes

### Bug Fixes
* CAUD-298 Allow JDBC handler to save null numeric values
* CAUD-397 The list of archived files may be empty
* CAUD-293 CsvSecureVerifier class uses hard-coded CSV preference
* CAUD-281 NPE when enabling secure feature in CSV Handler with empty keystore

# 4.1.0

## Changes

### Bug Fixes
* CAUD-291 Default CSV rotation suffix should use HH rather than kk for hours
* OPENIG-750 Allow empty configuration to build an AuditServiceConfiguration
* CAUD-287 Deadlock when secure logging is enabled and there are rotation &/or retention policies
* CAUD-266 Add prefix to tamper evident CSV files
* CAUD-288 Remove sensitive info from debug output
* Increase sleep time to 3s in RotatableWriterTest
* Cosmetic commits on: RotatableWriter, FileBasedEventHandlerConfiguration, StandardCsvWriter
* Cache the conversion of the rotation in milliseconds
* CAUD-283 Delay the first execution of the rotation
* CAUD-283 Force the last rotation time to be now if the file do not yet exist
* CAUD-283 Flush in the post rotation hooks in order to force the headers to be written

# 4.0.0

## Changes

### Minor
* CAUD-278 Remove client.host from the access event
* CAUD-278 Remove server.host from the access event

### Bug Fixes
* CAUD-275 The jdbc audit event handler needs to beable to handle all possible number and integer types
* CAUD-282 Make CsvFormatter threadsafe
* CAUD-262 Improve CsvAuditEventHandler thread synchronization
* CAUD-277 Disable auto commit when aquiring the connection
* CAUD-276 The before and after objects for the config and activity topic should be declared type object
* CAUD-274 Update the client and server port schema to have the type integer