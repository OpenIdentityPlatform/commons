Commons Audit
-------------
The purpose of the commons audit (CAUD) project is to provide a common framework and approach to audit logging.

More information can be found here: https://wikis.forgerock.org/confluence/display/COMPLAN/CAUD+-+Commons+Audit


Deploying the demo application
------------------------------
First make sure it is built:

    mvn clean install

Then to get it running:

    mvn jetty:run -f forgerock-audit-servlet/pom.xml

Examples audit call invocations are currently listed on the wiki page above 

To query all records of a specific topic such as access call:

    curl --request GET \
      --url 'http://localhost:8080/audit/access?_queryFilter=true' \
      --header 'content-type: application/json'
