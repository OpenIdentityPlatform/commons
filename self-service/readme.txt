Common Self Service
-------------------
The purpose of the commons self service (CSS) project is to provide a common approach to forgotten password and user registration.

More information can be found here: https://wikis.forgerock.org/confluence/display/COMPLAN/CSS+-+Commons+Self+Service


Deploying the demo application
------------------------------
First make sure it is built:

    mvn clean install

Then to get it running:

    mvn jetty:run -f forgerock-selfservice-example/pom.xml -Dmailserver.username=? -Dmailserver.password=?

It is necessary to give values to the above properties. These properties represent:

    mailserver.username - mail server username (use your forgerock email address)
    mailserver.password - mail server password (use your forgerock email password)

Once the service is running you can visit this page:

    http://localhost:9999/example

First it is necessary to go through the self registration process to create some users. Once a user is created the password reset can be played with.

To query all demo users run:

    curl --request GET \
      --url 'http://localhost:9999/example/selfservice/internal/users?_queryFilter=true' \
      --header 'content-type: application/json'