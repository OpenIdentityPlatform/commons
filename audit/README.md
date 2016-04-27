# Commons Audit
The purpose of the commons audit (CAUD) project is to provide a common framework and approach to audit logging.

More information can be found here: https://wikis.forgerock.org/confluence/display/COMPLAN/CAUD+-+Commons+Audit

## Commons Audit Topics
Commons audit includes the following common audit events: access, authentication, activity, and config.

### Access
The access audit topic logs system boundary events. For example, the initial request and final response for a given 
request to the system being audited.

### Activity
The activity audit topic logs audit event on resources. For example, if the system being audited had a user resource, 
operations on that user resource would be audited.

### Authentication
The authentication audit topic logs authentication attempts and their success or failure. This event can also log 
logout events.

#### CAF Authentication Logging
The product that implements the commons audit framework [AuditApi](https://stash.forgerock.org/projects/COMMONS/repos/forgerock-auth-filters/browse/forgerock-authn-filter/forgerock-jaspi-runtime/src/main/java/org/forgerock/caf/authentication/framework/AuditApi.java)
must direct log data to the commons audit authentication endpoint.

### Configuration
The config audit topic logs operations on the systems configuration.

##Deploying the demo application
First make sure it is built:

    mvn clean install

Then to get it running:

    mvn jetty:run -f forgerock-audit-servlet/pom.xml

Examples audit call invocations are currently listed on the wiki page above 

To query all records of a specific topic such as access call:

    curl --request GET \
      --url 'http://localhost:8080/audit/access?_queryFilter=true' \
      --header 'content-type: application/json'
